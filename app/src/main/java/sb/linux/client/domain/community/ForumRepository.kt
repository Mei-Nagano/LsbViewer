package sb.linux.client.domain.community

import sb.linux.client.domain.topic.TopicCard

data class ForumTopicsPage(
    val topics: List<TopicCard>,
    val currentPage: Int,
    val totalPages: Int,
)

interface ForumRepository {
    suspend fun loadDirectory(): List<ForumInfo>
    suspend fun loadTopics(forumId: Long, sort: String, page: Int): ForumTopicsPage
}
