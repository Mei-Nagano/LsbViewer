package sb.linux.client.domain.topic

interface TopicViewsRepository {
    fun reload()
    fun save(topicId: Long, views: Int)
    fun get(topicId: Long): Int
    fun clear()
}
