package sb.linux.client.domain.community

import sb.linux.client.domain.topic.TopicCard

data class HomePage(
    val topics: List<TopicCard>,
    val page: Int,
    val totalPages: Int,
    val sidebar: HomeSidebar?,
    val followingUsers: List<FollowingUser> = emptyList(),
    val emptyText: String = "",
)

interface HomeBrowseRepository {
    suspend fun load(sort: String, page: Int): HomePage
    suspend fun loadFollowing(view: FollowingView, page: Int, userId: Long): HomePage {
        check(view == FollowingView.TOPICS) { "当前数据源不支持关注用户列表" }
        return load("following", page)
    }
}

interface HomeBrowseCache {
    fun saveHomeCache(sort: String, items: List<TopicCard>)
    fun loadHomeCache(sort: String): List<TopicCard>
    fun clearHomeCache()
    fun saveHomeSidebar(sidebar: HomeSidebar?)
    fun loadHomeSidebar(): HomeSidebar?
}
