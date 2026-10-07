package sb.linux.client.domain.topic

interface TopicPageCacheRepository {
    fun get(topicId: Long, page: Int): TopicPageData?
    fun save(topicId: Long, page: Int, data: TopicPageData)
    fun clearTopic(topicId: Long)
    fun clearAll()
}
