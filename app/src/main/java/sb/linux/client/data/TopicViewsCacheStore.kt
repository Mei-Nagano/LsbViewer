package sb.linux.client.data

import android.content.Context
import sb.linux.client.domain.topic.TopicViewsRepository

/** Persists view counts while mutating the supplied observable map used by home cards. */
internal class TopicViewsCacheStore(
    context: Context,
    private val values: MutableMap<Long, Int>,
) : TopicViewsRepository {
    private val prefs = context.getSharedPreferences("lsb_views", Context.MODE_PRIVATE)

    override fun reload() {
        runCatching {
            val stored = org.json.JSONObject(prefs.getString("views", "{}") ?: "{}")
            stored.keys().forEach { key -> key.toLongOrNull()?.let { values[it] = stored.optInt(key, -1) } }
        }
    }

    override fun save(topicId: Long, views: Int) {
        if (topicId <= 0 || views < 0 || values[topicId] == views) return
        values[topicId] = views
        persist()
    }

    override fun get(topicId: Long): Int = values[topicId] ?: -1

    override fun clear() = values.clear()

    private fun persist() {
        runCatching {
            val stored = org.json.JSONObject()
            values.entries.sortedByDescending { it.key }.take(500)
                .forEach { entry -> stored.put(entry.key.toString(), entry.value) }
            prefs.edit().putString("views", stored.toString()).apply()
        }
    }
}
