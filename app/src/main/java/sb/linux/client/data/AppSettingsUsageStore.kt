package sb.linux.client.data

import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import sb.linux.client.domain.community.UsageEvent
import sb.linux.client.domain.community.PointRow

internal class AppSettingsUsageStore(private val prefs: SharedPreferences) {
    private fun usageEventArray(): JSONArray = runCatching {
        JSONArray(prefs.getString("usage_events", "[]") ?: "[]")
    }.getOrDefault(JSONArray())

    /** 仅记录应用内实际发生的操作；最多保留 2000 条，避免统计数据无限增长。 */
    fun recordUsageEvent(type: String, topicId: Long = 0, title: String = "", value: Int = 0) {
        if (type !in setOf("view", "favorite", "topic", "reply", "coin", "donate", "gacha")) return
        val old = usageEventArray()
        val out = JSONArray().put(
            JSONObject().put("type", type).put("topicId", topicId).put("title", title)
                .put("value", value).put("at", System.currentTimeMillis())
        )
        for (i in 0 until old.length().coerceAtMost(1999)) old.optJSONObject(i)?.let(out::put)
        prefs.edit().putString("usage_events", out.toString()).apply()
    }

    fun usageEvents(): List<UsageEvent> {
        val arr = usageEventArray()
        return buildList {
            for (i in 0 until arr.length()) {
                val item = arr.optJSONObject(i) ?: continue
                add(
                    UsageEvent(
                        type = item.optString("type"), topicId = item.optLong("topicId"),
                        title = item.optString("title"), value = item.optInt("value"),
                        at = item.optLong("at"),
                    )
                )
            }
        }
    }

    var usageAiSummary: String
        get() = prefs.getString("usage_ai_summary", "") ?: ""
        set(v) = prefs.edit().putString("usage_ai_summary", v).apply()

    var sourceUsageJson: String
        get() = prefs.getString("source_usage", "{}") ?: "{}"
        set(value) = prefs.edit().putString("source_usage", value).apply()
    var builtInImageHostProvider: String
        get() = prefs.getString("builtin_image_provider", "pixhost") ?: "pixhost"
        set(value) = prefs.edit().putString("builtin_image_provider", value).apply()

    var confirmGachaPull: Boolean
        get() = prefs.getBoolean("confirm_gacha_pull", true)
        set(value) = prefs.edit().putBoolean("confirm_gacha_pull", value).apply()

    fun saveSourcePoints(rows: List<PointRow>) {
        val array = JSONArray()
        rows.forEach { row -> array.put(JSONObject().put("time", row.timeText).put("reason", row.reason)
            .put("change", row.change).put("positive", row.positive).put("timestamp", row.timestamp).put("topicId", row.topicId)) }
        prefs.edit().putString("source_points", array.toString()).putLong("source_points_synced_at", System.currentTimeMillis()).apply()
    }

    fun sourcePoints(): List<PointRow> = runCatching {
        val array = JSONArray(prefs.getString("source_points", "[]"))
        (0 until array.length()).map { array.getJSONObject(it) }.map {
            PointRow(it.optString("time"), it.optString("reason"), it.optString("change"), it.optBoolean("positive"), it.optLong("timestamp"), it.optLong("topicId"))
        }
    }.getOrDefault(emptyList())

    val sourcePointsSyncedAt: Long get() = prefs.getLong("source_points_synced_at", 0L)

    fun clearUsageStats() = prefs.edit().remove("usage_events").remove("usage_ai_summary")
        .remove("source_points").remove("source_points_synced_at").remove("source_usage").apply()

    // ---------------- 本地收藏内容（收藏时本地快照，不额外访问源站） ----------------
}
