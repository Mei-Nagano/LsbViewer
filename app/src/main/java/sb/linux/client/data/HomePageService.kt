package sb.linux.client.data

import sb.linux.client.domain.community.HomeSessionObservation

import sb.linux.client.domain.community.HomeBrowseRepository
import sb.linux.client.domain.community.HomePage
import sb.linux.client.domain.community.FollowingView

internal class HomePageService(
    private val client: LsbClient,
    private val currentSessionVersion: () -> Long,
    private val observeSession: (HomeSessionObservation, Long) -> Unit,
    private val updateUnread: (Int) -> Unit,
) : HomeBrowseRepository {
    override suspend fun load(sort: String, page: Int): HomePage = loadPath(buildPath(sort, page))

    override suspend fun loadFollowing(view: FollowingView, page: Int, userId: Long): HomePage {
        check(userId > 0) { "登录后查看我的关注" }
        return loadPath(
            "/index.php?sort=following&follow_view=${view.sourceValue}&follow_uid=$userId&p=$page",
            followingView = view,
        )
    }

    private suspend fun loadPath(path: String, followingView: FollowingView? = null): HomePage {
        val responseVersion = currentSessionVersion()
        val response = client.get(path)
        observeSession(observeHomeSession(response.html, response.code), responseVersion)
        val document = SourceHtmlSupport.sourceDocument(response.html)
        if (followingView != null) {
            check(response.code in 200..299 && !response.url.contains("/login")) { "登录已失效，请重新登录" }
            check(document.selectFirst(".user-follow-subtabs") != null) { "未解析到关注分类，请重新加载" }
        }
        val (topics, pagination) = TopicListParser.parseTopicList(response.html)
        if (currentSessionVersion() == responseVersion) updateUnread(NotificationHtmlParser.parseNotifyBadgeCount(response.html))
        val sidebar = runCatching { HomeSidebarHtmlParser.parseHomeSidebar(response.html) }.getOrNull()
        return HomePage(
            topics, pagination.first, pagination.second, sidebar,
            followingUsers = if (followingView != null && followingView != FollowingView.TOPICS)
                HomeFollowingParser.parseUsers(document) else emptyList(),
            emptyText = document.selectFirst(".main-panel .post-list > .empty-state")?.text().orEmpty(),
        )
    }

    private fun buildPath(sort: String, page: Int): String = when {
        sort == "hot" -> "/hot_discussions?p=$page"
        sort == "footprint" -> "/unread_topic_notice_footprint?p=$page"
        sort == "essence" -> "/topic_essence_review_list?p=$page"
        sort == "featured" && page <= 1 -> "/topic_featured"
        sort == "featured" -> "/topic_featured?p=$page"
        page <= 1 -> "/index.php?sort=$sort"
        else -> "/index.php?sort=$sort&p=$page"
    }
}
