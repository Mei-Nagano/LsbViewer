package sb.linux.client.domain.community

import sb.linux.client.domain.topic.TopicCard

interface FavoritesRepository {
    fun topics(): List<TopicCard>
    fun comments(): List<CommentFavorite>
    suspend fun syncTopics(userId: Long): List<TopicCard>
    fun removeTopic(topicId: Long)
    fun removeComment(replyId: Long)
    fun clearTopics()
    fun clearComments()
}
