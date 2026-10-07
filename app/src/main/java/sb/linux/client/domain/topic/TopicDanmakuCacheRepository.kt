package sb.linux.client.domain.topic

interface TopicDanmakuCacheRepository {
    fun get(topicId: Long): List<DanmakuItem>?
    fun save(topicId: Long, items: List<DanmakuItem>)
    fun clear()
}
