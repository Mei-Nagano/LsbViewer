package sb.linux.client.data

import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import sb.linux.client.domain.topic.TopicCard

internal class AppSettingsHistoryStore(private val prefs: SharedPreferences) {
    private fun historyArray(): JSONArray =
        runCatching { JSONArray(prefs.getString("local_history", "[]") ?: "[]") }.getOrDefault(JSONArray())

    /** 记录一次浏览：同一帖子去重置顶，最多保留 200 条 */
    fun addHistory(
        topicId: Long, title: String, authorId: Long, authorName: String,
        forumId: Long, forumName: String, avatarUrl: String, titleColor: String,
    ) {
        if (topicId <= 0) return
        val old = historyArray()
        val out = JSONArray()
        out.put(
            JSONObject()
                .put("topicId", topicId).put("title", title)
                .put("authorId", authorId).put("authorName", authorName)
                .put("forumId", forumId).put("forumName", forumName)
                .put("avatarUrl", avatarUrl).put("titleColor", titleColor)
                .put("at", System.currentTimeMillis())
        )
        for (i in 0 until old.length()) {
            val o = old.optJSONObject(i) ?: continue
            if (o.optLong("topicId") == topicId) continue
            out.put(o)
        }
        while (out.length() > 200) out.remove(out.length() - 1)
        prefs.edit().putString("local_history", out.toString()).apply()
    }

    /** 本地浏览历史（最新在前），timeText 为本地浏览时间 */
    fun historyList(): List<sb.linux.client.domain.topic.TopicCard> {
        val arr = historyArray()
        val fmt = java.text.SimpleDateFormat("MM-dd HH:mm", java.util.Locale.getDefault())
        return buildList {
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                val at = o.optLong("at", 0L)
                add(
                    TopicCard(
                        topicId = o.optLong("topicId"),
                        title = o.optString("title"),
                        authorId = o.optLong("authorId"),
                        authorName = o.optString("authorName"),
                        forumId = o.optLong("forumId"),
                        forumName = o.optString("forumName"),
                        replies = 0,
                        lastReplier = "",
                        timeText = if (at > 0) fmt.format(java.util.Date(at)) else "",
                        avatarUrl = o.optString("avatarUrl"),
                        titleColor = o.optString("titleColor"),
                    )
                )
            }
        }
    }

    fun removeHistory(topicId: Long) {
        val old = historyArray()
        val out = JSONArray()
        for (i in 0 until old.length()) {
            val o = old.optJSONObject(i) ?: continue
            if (o.optLong("topicId") != topicId) out.put(o)
        }
        prefs.edit().putString("local_history", out.toString()).apply()
    }

    fun clearHistory() = prefs.edit().remove("local_history").apply()

    // ---------------- 发卡兑换自动记录 ----------------
}
