package sb.linux.client.data


import android.content.Context
import sb.linux.client.domain.topic.TopicReadingAnchor
import sb.linux.client.domain.topic.TopicReadingPosition
import sb.linux.client.domain.topic.TopicReadingPositionRepository

/**
 * 阅读位置存储。
 *
 * 兼容契约（IMPLEMENTATION_AND_ACCEPTANCE §9）：
 * - 数字 topicId 键保持严格四段 CSV：floor、listIndex、scrollOffset、保存时间，TTL 两小时；
 * - 新锚点写在独立的 `v2:<topicId>` 键里，不改原字符串；旧版本忽略该键，`clear()` 仍能清掉全部；
 * - 锚点只在对应旧记录存在且未过期时使用，否则回退旧记录或楼层。
 */
internal class ReadingPositionStore(context: Context) : TopicReadingPositionRepository {
    private val prefs = context.getSharedPreferences("lsb_reading_positions", Context.MODE_PRIVATE)
    private val ttlMs = 2L * 60L * 60L * 1000L

    private fun anchorKey(topicId: Long) = "v2:$topicId"

    override fun save(topicId: Long, floor: Int, listIndex: Int, scrollOffset: Int, postId: Long) {
        if (topicId <= 0) return
        val now = System.currentTimeMillis()
        val editor = prefs.edit().putString(
            topicId.toString(), "$floor,$listIndex,$scrollOffset,$now"
        )
        // 只有拿得到楼层/帖子标识时才写锚点，避免把无意义的 0 变成“可恢复位置”。
        if (postId > 0 || floor > 0) {
            editor.putString(anchorKey(topicId), "$floor,$postId,$listIndex,$scrollOffset,$now")
        }
        editor.apply()
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

    override fun getAnchor(topicId: Long): TopicReadingAnchor? {
        val values = prefs.getString(anchorKey(topicId), null)
            ?.split(',') ?: return null
        val savedAt = values.getOrNull(4)?.toLongOrNull()
        if (values.size != 5 || savedAt == null || System.currentTimeMillis() - savedAt > ttlMs) {
            prefs.edit().remove(anchorKey(topicId)).apply()
            return null
        }
        // 旧记录是权威来源：它被清空或过期时，锚点也不再生效。
        if (get(topicId) == null) return null
        return TopicReadingAnchor(
            floor = values[0].toIntOrNull() ?: return null,
            postId = values[1].toLongOrNull() ?: return null,
            listIndex = values[2].toIntOrNull() ?: return null,
            scrollOffset = values[3].toIntOrNull() ?: return null,
        )
    }

    override fun clear() = prefs.edit().clear().apply()

}
