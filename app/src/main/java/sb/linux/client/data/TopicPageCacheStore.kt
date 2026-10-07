package sb.linux.client.data

import sb.linux.client.domain.topic.TopicPageCacheRepository
import sb.linux.client.domain.topic.TopicPageData

/** In-memory topic-page cache with the existing ten-minute freshness window. */
internal class TopicPageCacheStore : TopicPageCacheRepository {
    private val pages = mutableMapOf<String, TopicPageData>()
    private val savedAt = mutableMapOf<String, Long>()

    override fun save(topicId: Long, page: Int, data: TopicPageData) {
        if (data.page != page) return
        val key = "$topicId-$page"
        pages[key] = data
        savedAt[key] = System.currentTimeMillis()
    }

    override fun get(topicId: Long, page: Int): TopicPageData? {
        val key = "$topicId-$page"
        val time = savedAt[key] ?: return null
        if (System.currentTimeMillis() - time > CACHE_TTL_MS) {
            pages.remove(key)
            savedAt.remove(key)
            return null
        }
        return pages[key]?.takeIf { it.page == page }
    }

    override fun clearTopic(topicId: Long) {
        val keys = pages.keys.filter { it.substringBefore("-") == topicId.toString() }
        keys.forEach { key ->
            pages.remove(key)
            savedAt.remove(key)
        }
    }

    override fun clearAll() {
        pages.clear()
        savedAt.clear()
    }

    private companion object {
        const val CACHE_TTL_MS = 10 * 60 * 1000L
    }
}
