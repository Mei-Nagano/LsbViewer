package sb.linux.client.data

import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import sb.linux.client.domain.topic.TopicCard

internal class AppSettingsFavoriteStore(private val prefs: SharedPreferences) {
    private fun favoriteArray(): JSONArray =
        runCatching { JSONArray(prefs.getString("local_favorites", "[]") ?: "[]") }.getOrDefault(JSONArray())

    /** 收藏一个帖子：同帖去重置顶，最多保留 500 条 */
    fun addFavorite(
        topicId: Long, title: String, authorId: Long, authorName: String,
        forumId: Long, forumName: String, avatarUrl: String, titleColor: String,
    ) {
        if (topicId <= 0) return
        val old = favoriteArray()
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
        while (out.length() > 500) out.remove(out.length() - 1)
        prefs.edit().putString("local_favorites", out.toString()).apply()
    }

    /** 本地收藏内容（最新收藏在前；源站同步来的条目沿用源站时间/回复数） */
    fun favoriteList(): List<sb.linux.client.domain.topic.TopicCard> {
        val arr = favoriteArray()
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
                        replies = o.optInt("replies", 0),
                        lastReplier = "",
                        timeText = if (at > 0) fmt.format(java.util.Date(at)) else o.optString("timeText"),
                        avatarUrl = o.optString("avatarUrl"),
                        titleColor = o.optString("titleColor"),
                    )
                )
            }
        }
    }

    /** 合并源站收藏（用户页 tab=favorites 解析结果）：本地没有的追加到末尾，已有的不覆盖 */
    fun mergeFavorites(cards: List<TopicCard>) {
        if (cards.isEmpty()) return
        val old = favoriteArray()
        val have = buildSet {
            for (i in 0 until old.length()) add(old.optJSONObject(i)?.optLong("topicId") ?: -1L)
        }
        val out = JSONArray()
        for (i in 0 until old.length()) out.put(old.optJSONObject(i))
        cards.forEach { c ->
            if (c.topicId > 0 && c.topicId !in have) {
                out.put(
                    JSONObject()
                        .put("topicId", c.topicId).put("title", c.title)
                        .put("authorId", c.authorId).put("authorName", c.authorName)
                        .put("forumId", c.forumId).put("forumName", c.forumName)
                        .put("avatarUrl", c.avatarUrl).put("titleColor", c.titleColor)
                        .put("at", 0L)
                        .put("timeText", c.timeText)
                        .put("replies", c.replies)
                )
            }
        }
        prefs.edit().putString("local_favorites", out.toString()).apply()
    }

    fun removeFavorite(topicId: Long) {
        val old = favoriteArray()
        val out = JSONArray()
        for (i in 0 until old.length()) {
            val o = old.optJSONObject(i) ?: continue
            if (o.optLong("topicId") != topicId) out.put(o)
        }
        prefs.edit().putString("local_favorites", out.toString()).apply()
    }

    fun clearFavorites() = prefs.edit().remove("local_favorites").apply()

    // ---------------- 本地收藏评论（纯本地快照，34） ----------------
}
