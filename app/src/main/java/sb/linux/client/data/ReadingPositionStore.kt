package sb.linux.client.data


import android.content.Context
import sb.linux.client.domain.topic.TopicReadingPosition
import sb.linux.client.domain.topic.TopicReadingPositionRepository

internal class ReadingPositionStore(context: Context) : TopicReadingPositionRepository {
    private val prefs = context.getSharedPreferences("lsb_reading_positions", Context.MODE_PRIVATE)
    private val ttlMs = 2L * 60L * 60L * 1000L
    override fun save(topicId: Long, floor: Int, listIndex: Int, scrollOffset: Int) {
        if (topicId <= 0) return
        prefs.edit().putString(
            topicId.toString(), "$floor,$listIndex,$scrollOffset,${System.currentTimeMillis()}"
        ).apply()
    }

    override fun get(topicId: Long): TopicReadingPosition? {
        val values = prefs.getString(topicId.toString(), null)
            ?.split(',') ?: return null
        val savedAt = values.getOrNull(3)?.toLongOrNull()
        if (values.size != 4 || savedAt == null || System.currentTimeMillis() - savedAt > ttlMs) {
            prefs.edit().remove(topicId.toString()).apply()
            return null
        }
        return TopicReadingPosition(
            floor = values[0].toIntOrNull() ?: return null,
            listIndex = values[1].toIntOrNull() ?: return null,
            scrollOffset = values[2].toIntOrNull() ?: return null,
        )
    }

    override fun clear() = prefs.edit().clear().apply()

}
