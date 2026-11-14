package sb.linux.client.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

internal data class SourceUploadedAttachment(val key: String, val name: String, val markdown: String)

/** 与源站一样，每个账号保留最近 100 个上传结果；移除列表项不会删除服务器附件。 */
internal class SourceAttachmentHistoryStore(context: Context) {
    private val prefs = context.getSharedPreferences("lsb_attachment_upload_history_v1", Context.MODE_PRIVATE)

    fun read(userId: Long): List<SourceUploadedAttachment> {
        if (userId <= 0) return emptyList()
        return runCatching {
            val array = JSONArray(prefs.getString(userId.toString(), "[]"))
            (0 until array.length()).mapNotNull { index ->
                val item = array.optJSONObject(index) ?: return@mapNotNull null
                val key = item.optString("key")
                val markdown = item.optString("markdown")
                if (key.isBlank() || markdown.isBlank()) null
                else SourceUploadedAttachment(key, item.optString("name"), markdown)
            }.distinctBy { it.key }.takeLast(100)
        }.getOrDefault(emptyList())
    }

    fun write(userId: Long, items: List<SourceUploadedAttachment>) {
        if (userId <= 0) return
        val array = JSONArray()
        items.distinctBy { it.key }.takeLast(100).forEach {
            array.put(JSONObject().put("key", it.key).put("name", it.name).put("markdown", it.markdown))
        }
        prefs.edit().putString(userId.toString(), array.toString()).apply()
    }
}
