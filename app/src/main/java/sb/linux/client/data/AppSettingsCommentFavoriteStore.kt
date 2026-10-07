package sb.linux.client.data

import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import sb.linux.client.domain.community.CommentFavorite

internal class AppSettingsCommentFavoriteStore(private val prefs: SharedPreferences) {
    private fun commentFavArray(): JSONArray =
        runCatching { JSONArray(prefs.getString("local_comment_favorites", "[]") ?: "[]") }.getOrDefault(JSONArray())

    /** 收藏一条评论：同条去重置顶，最多保留 500 条 */
    fun addCommentFavorite(
        replyId: Long, topicId: Long, topicTitle: String, floor: Int,
        authorId: Long, authorName: String, avatarUrl: String, content: String,
    ) {
        if (replyId <= 0 || topicId <= 0) return
        val old = commentFavArray()
        val out = JSONArray()
        out.put(
            JSONObject()
                .put("replyId", replyId).put("topicId", topicId)
                .put("topicTitle", topicTitle).put("floor", floor)
                .put("authorId", authorId).put("authorName", authorName)
                .put("avatarUrl", avatarUrl).put("content", content)
                .put("at", System.currentTimeMillis())
        )
        for (i in 0 until old.length()) {
            val o = old.optJSONObject(i) ?: continue
            if (o.optLong("replyId") == replyId) continue
            out.put(o)
        }
        while (out.length() > 500) out.remove(out.length() - 1)
        prefs.edit().putString("local_comment_favorites", out.toString()).apply()
    }

    fun commentFavoriteList(): List<CommentFavorite> {
        val arr = commentFavArray()
        val fmt = java.text.SimpleDateFormat("MM-dd HH:mm", java.util.Locale.getDefault())
        return buildList {
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                val at = o.optLong("at", 0L)
                add(
                    CommentFavorite(
                        replyId = o.optLong("replyId"),
                        topicId = o.optLong("topicId"),
                        topicTitle = o.optString("topicTitle"),
                        floor = o.optInt("floor", 0),
                        authorId = o.optLong("authorId"),
                        authorName = o.optString("authorName"),
                        avatarUrl = o.optString("avatarUrl"),
                        content = o.optString("content"),
                        at = at,
                        timeText = if (at > 0) fmt.format(java.util.Date(at)) else "",
                    )
                )
            }
        }
    }

    fun removeCommentFavorite(replyId: Long) {
        val old = commentFavArray()
        val out = JSONArray()
        for (i in 0 until old.length()) {
            val o = old.optJSONObject(i) ?: continue
            if (o.optLong("replyId") != replyId) out.put(o)
        }
        prefs.edit().putString("local_comment_favorites", out.toString()).apply()
    }

    fun clearCommentFavorites() = prefs.edit().remove("local_comment_favorites").apply()

    // ---------------- 本地聚合私信（收信来自通知，去信为发送记录；纯本地，不额外访问源站） ----------------
}
