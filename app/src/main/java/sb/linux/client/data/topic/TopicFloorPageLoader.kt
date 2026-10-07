package sb.linux.client.data.topic

import sb.linux.client.domain.topic.PostEntry
import sb.linux.client.data.LsbClient
import sb.linux.client.domain.topic.TopicPageData
import sb.linux.client.data.TopicPageParser
import sb.linux.client.domain.topic.TopicFloorPage
import sb.linux.client.domain.topic.TopicFloorPageRepository
import sb.linux.client.domain.topic.TopicPageRepository

/** Resolves the server's floor redirect and fills preceding pages for stable local indexing. */
internal class TopicFloorPageLoader(
    private val client: LsbClient,
    private val pages: TopicPageRepository,
) : TopicFloorPageRepository {
    override suspend fun load(topicId: Long, floor: Int): TopicFloorPage {
        val response = client.get("/topic/$topicId?floor=$floor")
        val pageNumber = Regex("""[?&]p=(\d+)""").find(response.url)
            ?.groupValues?.get(1)?.toIntOrNull() ?: 1
        val targetPage = TopicPageParser.parse(response.html, topicId)
        val posts = buildList {
            for (page in 1 until pageNumber) addAll(pages.load(topicId, page).posts)
            addAll(targetPage.posts)
        }.distinctBy(PostEntry::id)
        val page = targetPage.copy(posts = posts)
        return TopicFloorPage(page, pageNumber.coerceAtLeast(1), posts.firstOrNull { it.floor == floor })
    }
}
