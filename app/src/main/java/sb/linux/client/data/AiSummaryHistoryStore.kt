package sb.linux.client.data

import sb.linux.client.domain.ai.AiSummaryRecord
import sb.linux.client.domain.ai.AiSummaryHistoryRepository


import android.content.SharedPreferences

internal class AiSummaryHistoryStore(
    private val prefs: SharedPreferences,
    private val cache: MutableMap<Long, String>,
) : AiSummaryHistoryRepository {
    override fun save(topicId: Long, summary: String, title: String) {
        if (summary.isBlank()) return
        cache[topicId] = summary
        val record = org.json.JSONObject()
            .put("topicId", topicId).put("title", title)
            .put("summary", summary).put("savedAt", System.currentTimeMillis())
        val recordId = "${topicId}_${java.util.UUID.randomUUID()}"
        prefs.edit().putString(recordId, record.toString()).apply()
    }

    override fun cached(topicId: Long): String? {
        cache[topicId]?.let { return it }
        val summary = history().firstOrNull { it.topicId == topicId }?.summary.orEmpty()
        return summary.takeIf { it.isNotBlank() }?.also { cache[topicId] = it }
    }

    override fun history(): List<AiSummaryRecord> = prefs.all.mapNotNull { (key, raw) ->
        runCatching {
            val o = org.json.JSONObject(raw as String)
            AiSummaryRecord(o.optLong("topicId"), o.optString("title"), o.optString("summary"), o.optLong("savedAt"), key, o.optBoolean("favorite"))
        }.getOrNull()?.takeIf { it.topicId > 0 && it.summary.isNotBlank() }
    }.sortedByDescending { it.savedAt }

    override fun remove(record: AiSummaryRecord) {
        cache.remove(record.topicId)
        prefs.edit().remove(record.recordId).apply()
    }

    override fun setFavorite(record: AiSummaryRecord, favorite: Boolean) {
        val raw = prefs.getString(record.recordId, null) ?: return
        val value = org.json.JSONObject(raw).put("favorite", favorite)
        prefs.edit().putString(record.recordId, value.toString()).apply()
    }

    override fun clearCachedSummaries() {
        cache.clear()
    }

    override fun clear() {
        clearCachedSummaries()
        prefs.edit().clear().apply()
    }

}
