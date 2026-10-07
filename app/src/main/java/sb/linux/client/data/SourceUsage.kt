package sb.linux.client.data

import sb.linux.client.domain.account.LoginState
import sb.linux.client.domain.community.PointRow
import sb.linux.client.domain.community.SourceUsageTotals
import sb.linux.client.domain.community.UsageEvent
import sb.linux.client.domain.community.UsageStatisticsRepository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.json.JSONObject
import sb.linux.client.common.collection.TopicCollectionTab

/** 完整拉取个人主页分页后一次提交，失败时不覆盖上次成功的快照。 */
internal class SourceUsageRepository(
    private val client: LsbClient,
    private val settings: AppSettings,
    private val topicCollections: sb.linux.client.service.TopicCollectionService,
    private val currentLogin: () -> LoginState,
) : UsageStatisticsRepository {
    override fun events(): List<UsageEvent> = settings.usageEvents()
    override fun recordEvent(type: String, topicId: Long, title: String, value: Int) =
        settings.recordUsageEvent(type, topicId, title, value)

    override fun sourceTotals(userId: Long): SourceUsageTotals? = runCatching {
        JSONObject(settings.sourceUsageJson).takeIf { it.optLong("userId") == userId }?.let {
            SourceUsageTotals(
                topics = it.optInt("topics"),
                replies = it.optInt("replies"),
                favorites = it.optInt("favorites"),
                collections = it.optInt("collections"),
            )
        }
    }.getOrNull()

    override fun points(): List<PointRow> = settings.sourcePoints()
    override fun aiSummary(): String = settings.usageAiSummary
    override fun saveAiSummary(value: String) { settings.usageAiSummary = value }
    override fun clear() = settings.clearUsageStats()

    override suspend fun sync() = withContext(Dispatchers.IO) {
    val uid = currentLogin().userId
    check(currentLogin().loggedIn && uid > 0) { "请先登录" }
    val totals = mutableMapOf<String, Int>()
    val points = mutableListOf<PointRow>()
    for (tab in listOf("topics", "replies", "favorites", "points_rewards")) {
        var page = 1
        var last = 1
        var total = 0
        val fingerprints = mutableSetOf<String>()
        do {
            val response = client.get("/user/$uid?tab=$tab&p=$page")
            check(response.code in 200..299 && !response.url.contains("login")) { "同步失败，请检查网络及登录状态" }
            val doc = Jsoup.parse(response.html)
            check(doc.selectFirst(".profile-toolbar") != null) { "未获取到个人主页，已保留上次统计" }
            val rows = doc.select(".forum-main ul.post-list > li.post-item")
            val fingerprint = rows.outerHtml()
            check(rows.isEmpty() || fingerprints.add(fingerprint)) { "源站分页重复，已保留上次统计" }
            total += rows.size
            if (tab == "points_rewards") points += PointsHtmlParser.parsePointRows(response.html)
            last = maxOf(last, doc.select(".pagination-bar a[href], .pagination a[href], .pagination option[value]").mapNotNull {
                Regex("[?&]p=(\\d+)").find(it.attr("href").ifBlank { it.attr("value") })?.groupValues?.get(1)?.toIntOrNull()
            }.maxOrNull() ?: 1)
            page++
        } while (page <= last)
        totals[tab] = total
    }
    run {
        // 源站淘帖列表当前固定返回一页（p 参数会被忽略），不要按假分页重复请求。
        totals["collections"] = topicCollections
            .list(TopicCollectionTab.MINE)
            .items
            .distinctBy { it.collectionId }
            .size
    }
    check(currentLogin().userId == uid && currentLogin().loggedIn) { "账号已切换，请重新同步" }
    settings.sourceUsageJson = JSONObject().put("userId", uid).put("topics", totals["topics"])
        .put("replies", totals["replies"]).put("favorites", totals["favorites"])
        .put("collections", totals["collections"])
        .put("syncedAt", System.currentTimeMillis()).toString()
    settings.saveSourcePoints(points)
    }
}
