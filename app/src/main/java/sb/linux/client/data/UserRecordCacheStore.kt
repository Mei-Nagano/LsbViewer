package sb.linux.client.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import sb.linux.client.domain.account.UserPageContent
import sb.linux.client.domain.account.UserProfile
import sb.linux.client.domain.community.NotificationItem
import sb.linux.client.domain.community.PointRow
import java.io.File

/** 磁盘快照：内容，以及落盘时间（墙钟），用于判断是否还在新鲜窗口内。 */
internal class UserRecordSnapshot(val content: UserPageContent, val savedAt: Long)

/**
 * 积分明细与通知历史的本地快照，一个账号一个标签一个 JSON 文件。
 *
 * 这两张列表只能逐页访问源站拼出来，进程重启后没有任何内存状态，用户每次进来都要重新等一遍。
 * 这里保存最近一次取到的记录：下次进入先按快照渲染，必要时再续接刷新。
 * 不用 SharedPreferences —— 它会在主线程整体解析整份 XML，而这些列表可能很长；
 * 文件读写因此要求调用方在 IO 线程执行。
 */
internal class UserRecordCacheStore(context: Context) {
    private val dir = File(context.filesDir, "user_records")

    fun load(userId: Long, tab: String): UserRecordSnapshot? = runCatching {
        val file = fileOf(userId, tab)
        if (!file.isFile) return@runCatching null
        val record = JSONObject(file.readText())
        val rows = record.optJSONArray("rows")
        val profile = readProfile(record.optJSONObject("profile"))
        val content = when (tab) {
            TAB_POINTS -> UserPageContent(
                profile,
                points = readPoints(rows),
                loadedPages = record.optInt("loadedPages"),
                totalPages = record.optInt("totalPages"),
                complete = record.optBoolean("complete"),
            )
            TAB_NOTIFICATIONS -> UserPageContent(
                profile,
                notifications = readNotifications(rows),
                loadedPages = record.optInt("loadedPages"),
                totalPages = record.optInt("totalPages"),
                complete = record.optBoolean("complete"),
            )
            else -> return@runCatching null
        }
        UserRecordSnapshot(content, record.optLong("savedAt"))
    }.getOrNull()

    fun save(userId: Long, tab: String, content: UserPageContent) {
        runCatching {
            val record = JSONObject()
                .put("savedAt", System.currentTimeMillis())
                .put("loadedPages", content.loadedPages)
                .put("totalPages", content.totalPages)
                .put("complete", content.complete)
                .put("profile", profileJson(content.profile))
            val rows = JSONArray()
            when (tab) {
                TAB_POINTS -> content.points.forEach { rows.put(pointJson(it)) }
                TAB_NOTIFICATIONS -> content.notifications.forEach { rows.put(notificationJson(it)) }
                else -> return@runCatching
            }
            record.put("rows", rows)
            if (!dir.isDirectory) dir.mkdirs()
            fileOf(userId, tab).writeText(record.toString())
            prune()
        }
    }

    fun clearAll() {
        runCatching { dir.listFiles()?.forEach { it.delete() } }
    }

    private fun fileOf(userId: Long, tab: String) = File(dir, "${tab}_$userId.json")

    /** 只留最近写入的若干份快照，别让浏览过的账号无限堆积。 */
    private fun prune() {
        val files = dir.listFiles()?.filter { it.isFile } ?: return
        if (files.size <= MAX_FILES) return
        files.sortedByDescending { it.lastModified() }.drop(MAX_FILES).forEach { it.delete() }
    }

    /** 只保留渲染积分页与用户卡需要的字段：用户名、头像、头衔、积分余额与统计。 */
    private fun profileJson(profile: UserProfile?): JSONObject? = profile?.let {
        JSONObject()
            .put("uid", it.userId).put("name", it.username).put("avatar", it.avatarUrl)
            .put("rank", it.rankText).put("points", it.points).put("bio", it.bio)
            .put("following", it.following).put("followers", it.followers).put("wallet", it.wallet)
            .put("stats", JSONArray().apply {
                it.stats.forEach { (label, value) -> put(JSONArray().put(label).put(value)) }
            })
    }

    private fun readProfile(record: JSONObject?): UserProfile? = record?.let {
        val stats = it.optJSONArray("stats")
        UserProfile(
            userId = it.optLong("uid"),
            username = it.optString("name"),
            avatarUrl = it.optString("avatar"),
            rankText = it.optString("rank"),
            points = it.optString("points"),
            bio = it.optString("bio"),
            stats = (0 until (stats?.length() ?: 0)).mapNotNull { index ->
                stats?.optJSONArray(index)?.let { pair -> pair.optString(0) to pair.optString(1) }
            },
            following = it.optString("following"),
            followers = it.optString("followers"),
            wallet = it.optString("wallet"),
        )
    }

    private fun pointJson(row: PointRow): JSONObject = JSONObject()
        .put("t", row.timeText).put("r", row.reason).put("c", row.change)
        .put("p", row.positive).put("ts", row.timestamp).put("tid", row.topicId)
        .put("er", row.eventReason)

    private fun readPoints(rows: JSONArray?): List<PointRow> = (0 until (rows?.length() ?: 0)).mapNotNull { index ->
        rows?.optJSONObject(index)?.let { row ->
            PointRow(
                timeText = row.optString("t"),
                reason = row.optString("r"),
                change = row.optString("c"),
                positive = row.optBoolean("p"),
                timestamp = row.optLong("ts"),
                topicId = row.optLong("tid"),
                eventReason = row.optString("er"),
            )
        }
    }

    private fun notificationJson(item: NotificationItem): JSONObject = JSONObject()
        .put("u", item.fromUser).put("ty", item.typeText).put("tm", item.timeText)
        .put("ct", item.content).put("lk", item.link).put("ur", item.unread)
        .put("it", item.isTopic).put("pi", item.partnerId).put("pa", item.partnerAvatar)
        .put("qt", item.quoteText).put("sy", item.isSystem).put("lp", item.lotteryPrize)
        .put("rc", item.redemptionCode).put("rt", item.cardRedemptionTitle)
        .put("cc", item.cardRedemptionCost)

    private fun readNotifications(rows: JSONArray?): List<NotificationItem> =
        (0 until (rows?.length() ?: 0)).mapNotNull { index ->
            rows?.optJSONObject(index)?.let { row ->
                NotificationItem(
                    fromUser = row.optString("u"),
                    typeText = row.optString("ty"),
                    timeText = row.optString("tm"),
                    content = row.optString("ct"),
                    link = row.optString("lk"),
                    unread = row.optBoolean("ur"),
                    isTopic = row.optBoolean("it"),
                    partnerId = row.optLong("pi"),
                    partnerAvatar = row.optString("pa"),
                    quoteText = row.optString("qt"),
                    isSystem = row.optBoolean("sy"),
                    lotteryPrize = row.optString("lp"),
                    redemptionCode = row.optString("rc"),
                    cardRedemptionTitle = row.optString("rt"),
                    cardRedemptionCost = row.optString("cc"),
                )
            }
        }

    private companion object {
        const val TAB_POINTS = "points_rewards"
        const val TAB_NOTIFICATIONS = "notifications"
        const val MAX_FILES = 24
    }
}
