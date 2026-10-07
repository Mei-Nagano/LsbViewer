package sb.linux.client.domain.community

import sb.linux.client.domain.topic.TopicCard

data class HomePage(
    val topics: List<TopicCard>,
    val page: Int,
    val totalPages: Int,
    val sidebar: HomeSidebar?,
)

interface HomeBrowseRepository {
    suspend fun load(sort: String, page: Int): HomePage
}

interface HomeBrowseCache {
    fun saveHomeCache(sort: String, items: List<TopicCard>)
    fun loadHomeCache(sort: String): List<TopicCard>
    fun clearHomeCache()
    fun saveHomeSidebar(sidebar: HomeSidebar?)
    fun loadHomeSidebar(): HomeSidebar?
}
