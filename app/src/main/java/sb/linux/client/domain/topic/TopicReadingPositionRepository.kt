package sb.linux.client.domain.topic

data class TopicReadingPosition(val floor: Int, val listIndex: Int, val scrollOffset: Int)

interface TopicReadingPositionRepository {
    fun save(topicId: Long, floor: Int, listIndex: Int, scrollOffset: Int)
    fun get(topicId: Long): TopicReadingPosition?
    fun clear()
}
