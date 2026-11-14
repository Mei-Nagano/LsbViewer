package sb.linux.client.data

import android.content.Context
import org.json.JSONObject

internal class SourceReplyDraftStore(context: Context) {
    private val prefs = context.getSharedPreferences("lsb_reply_drafts_v1", Context.MODE_PRIVATE)
    private fun key(userId: Long, topicId: Long) = "$userId:$topicId"

    fun read(userId: Long, topicId: Long): String? {
        if (userId <= 0 || topicId <= 0) return null
        return runCatching {
            val data = JSONObject(prefs.getString(key(userId, topicId), "{}").orEmpty())
            if (System.currentTimeMillis() - data.optLong("updatedAt") > 7L * 24 * 60 * 60 * 1000) null
            else data.optString("body").takeIf(String::isNotBlank)
        }.getOrNull()
    }

    fun save(userId: Long, topicId: Long, body: String) {
        if (userId <= 0 || topicId <= 0) return
        if (body.isBlank()) { remove(userId, topicId); return }
        prefs.edit().putString(key(userId, topicId), JSONObject().put("body", body)
            .put("updatedAt", System.currentTimeMillis()).toString()).apply()
    }

    fun remove(userId: Long, topicId: Long) { prefs.edit().remove(key(userId, topicId)).apply() }
}
