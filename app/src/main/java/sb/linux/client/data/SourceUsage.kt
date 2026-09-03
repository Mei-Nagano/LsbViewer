package sb.linux.client.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.json.JSONObject

/** 完整拉取个人主页分页后一次提交，失败时不覆盖上次成功的快照。 */
suspend fun Session.syncSourceUsage() = withContext(Dispatchers.IO) {
    val uid = loginState.userId
    check(loginState.loggedIn && uid > 0) { "请先登录" }
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
            if (tab == "points_rewards") points += HtmlParser.parsePointRows(response.html)
            last = maxOf(last, doc.select(".pagination-bar a[href], .pagination a[href], .pagination option[value]").mapNotNull {
                Regex("[?&]p=(\\d+)").find(it.attr("href").ifBlank { it.attr("value") })?.groupValues?.get(1)?.toIntOrNull()
            }.maxOrNull() ?: 1)
            page++
        } while (page <= last)
        totals[tab] = total
    }
    check(loginState.userId == uid && loginState.loggedIn) { "账号已切换，请重新同步" }
    settings.sourceUsageJson = JSONObject().put("userId", uid).put("topics", totals["topics"])
        .put("replies", totals["replies"]).put("favorites", totals["favorites"])
        .put("syncedAt", System.currentTimeMillis()).toString()
    settings.saveSourcePoints(points)
}
