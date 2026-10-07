package sb.linux.client.data
import sb.linux.client.domain.topic.TopicCard

internal class FavoriteSourceService(
    private val client: LsbClient,
    private val mergeFavorites: (List<TopicCard>) -> Unit,
) {
    suspend fun syncTopics(userId: Long): List<TopicCard> {
        val topics = TopicListParser.parseTopicList(
            client.get("/user/$userId?tab=favorites").html,
        ).first
        mergeFavorites(topics)
        return topics
    }
}
