package sb.linux.client.data

import sb.linux.client.domain.topic.DanmakuItem
import sb.linux.client.domain.topic.TopicDanmakuCacheRepository

internal class TopicDanmakuCacheStore : TopicDanmakuCacheRepository {
    private val items = mutableMapOf<Long, List<DanmakuItem>>()

    override fun get(topicId: Long): List<DanmakuItem>? = items[topicId]

    override fun save(topicId: Long, items: List<DanmakuItem>) {
        this.items[topicId] = items
    }

    override fun clear() = items.clear()
}
