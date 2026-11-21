package sb.linux.client.domain.community

import sb.linux.client.domain.topic.TopicCard

data class NodeLink(val title: String, val url: String, val selected: Boolean = false)
data class NodeSummary(
    val url: String,
    val name: String,
    val description: String,
    val meta: String,
    val badges: String,
    val iconUrl: String,
)

data class NodePage(
    val url: String,
    val title: String,
    val description: String,
    val meta: String,
    val tabs: List<NodeLink>,
    val nodes: List<NodeSummary>,
    val topics: List<TopicCard>,
    val page: Int,
    val totalPages: Int,
    val emptyMessage: String,
    val loginRequired: Boolean,
)

interface NodeRepository {
    suspend fun load(url: String): NodePage
}
