package sb.linux.client.domain.topic

data class TopicFloorPage(
    val page: TopicPageData,
    val pageNumber: Int,
    val target: PostEntry?,
)

interface TopicPageRepository {
    suspend fun load(topicId: Long, page: Int): TopicPageData
}

interface TopicFloorPageRepository {
    suspend fun load(topicId: Long, floor: Int): TopicFloorPage
}
