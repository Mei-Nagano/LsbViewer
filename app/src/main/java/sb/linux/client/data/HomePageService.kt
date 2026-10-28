package sb.linux.client.data

import sb.linux.client.domain.community.HomeSessionObservation

import sb.linux.client.domain.community.HomeBrowseRepository
import sb.linux.client.domain.community.HomePage

internal class HomePageService(
    private val client: LsbClient,
    private val currentSessionVersion: () -> Long,
    private val observeSession: (HomeSessionObservation, Long) -> Unit,
    private val updateUnread: (Int) -> Unit,
) : HomeBrowseRepository {
    override suspend fun load(sort: String, page: Int): HomePage {
        val responseVersion = currentSessionVersion()
        val response = client.get(buildPath(sort, page))
        observeSession(observeHomeSession(response.html, response.code), responseVersion)
        val (topics, pagination) = TopicListParser.parseTopicList(response.html)
        updateUnread(NotificationHtmlParser.parseNotifyBadgeCount(response.html))
        val sidebar = runCatching { HomeSidebarHtmlParser.parseHomeSidebar(response.html) }.getOrNull()
        return HomePage(topics, pagination.first, pagination.second, sidebar)
    }

    private fun buildPath(sort: String, page: Int): String = when {
        sort == "essence" -> "/topic_essence_review_list?p=$page"
        sort == "featured" && page <= 1 -> "/topic_featured"
        sort == "featured" -> "/topic_featured?p=$page"
        page <= 1 -> "/index.php?sort=$sort"
        else -> "/index.php?sort=$sort&p=$page"
    }
}
