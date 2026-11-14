package sb.linux.client.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import sb.linux.client.domain.topic.SourceEditorDraft
import sb.linux.client.domain.topic.SourceEditorDraftRepository

internal class SourceEditorDraftStore(context: Context) : SourceEditorDraftRepository {
    private val prefs = context.getSharedPreferences("lsb_editor_drafts", Context.MODE_PRIVATE)
    private val maxAge = 7L * 24 * 60 * 60 * 1000

    override fun list(userId: Long, topicId: Long): List<SourceEditorDraft> = read()
        .filter { it.userId == userId && it.topicId == topicId }.sortedByDescending { it.updatedAt }

    override fun save(draft: SourceEditorDraft) {
        require(draft.userId > 0)
        val records = read().filterNot { it.userId == draft.userId && it.id == draft.id } + draft
        write(records.groupBy(SourceEditorDraft::userId).values.flatMap { items ->
            items.sortedByDescending(SourceEditorDraft::updatedAt).take(20)
        })
    }

    override fun remove(userId: Long, id: String) = write(read().filterNot { it.userId == userId && it.id == id })

    private fun read(): List<SourceEditorDraft> = runCatching {
        val array = JSONArray(prefs.getString("drafts", "[]"))
        val now = System.currentTimeMillis()
        (0 until array.length()).mapNotNull { index ->
            val record = array.optJSONObject(index) ?: return@mapNotNull null
            val at = record.optLong("updatedAt")
            if (at <= 0 || now - at > maxAge) return@mapNotNull null
            val fields = record.optJSONArray("fields") ?: return@mapNotNull null
            SourceEditorDraft(record.optString("id"), record.optLong("userId"), record.optLong("topicId"), at,
                record.optString("extensionName"), (0 until fields.length()).mapNotNull { fieldIndex ->
                    fields.optJSONObject(fieldIndex)?.let { it.optString("name") to it.optString("value") }
                }, record.optBoolean("automatic"))
        }
    }.getOrDefault(emptyList())

    private fun write(records: List<SourceEditorDraft>) {
        val array = JSONArray()
        records.forEach { draft ->
            val fields = JSONArray()
            draft.fields.forEach { (name, value) -> fields.put(JSONObject().put("name", name).put("value", value)) }
            array.put(JSONObject().put("id", draft.id).put("userId", draft.userId).put("topicId", draft.topicId)
                .put("updatedAt", draft.updatedAt).put("extensionName", draft.extensionName)
                .put("automatic", draft.automatic).put("fields", fields))
        }
        prefs.edit().putString("drafts", array.toString()).apply()
    }
}
