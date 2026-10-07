package sb.linux.client.data.topic

import sb.linux.client.data.LsbClient
import sb.linux.client.data.TopicPageParser
import sb.linux.client.domain.topic.TopicPageCacheRepository
import sb.linux.client.domain.topic.TopicPageData
import sb.linux.client.domain.topic.TopicPageRepository

/** Resolves a topic page from the account-scoped cache or the existing forum HTML endpoint. */
internal class TopicPageDataSource(
    private val client: LsbClient,
    private val cache: TopicPageCacheRepository,
) : TopicPageRepository {
    override suspend fun load(topicId: Long, page: Int): TopicPageData {
        cache.get(topicId, page)?.let { return it }
        val path = if (page <= 1) "/topic/$topicId" else "/topic/$topicId?p=$page"
        val response = client.get(path)
        return TopicPageParser.parse(response.html, topicId).also {
            cache.save(topicId, page, it)
        }
    }
}
