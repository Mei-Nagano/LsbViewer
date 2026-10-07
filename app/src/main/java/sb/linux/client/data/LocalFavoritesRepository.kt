package sb.linux.client.data

import sb.linux.client.domain.community.CommentFavorite
import sb.linux.client.domain.community.FavoritesRepository
import sb.linux.client.domain.topic.TopicCard

internal class LocalFavoritesRepository(
    private val settings: AppSettings,
    private val source: FavoriteSourceService,
) : FavoritesRepository {
    override fun topics(): List<TopicCard> = settings.favoriteList()
    override fun comments(): List<CommentFavorite> = settings.commentFavoriteList()
    override suspend fun syncTopics(userId: Long) = source.syncTopics(userId)
    override fun removeTopic(topicId: Long) = settings.removeFavorite(topicId)
    override fun removeComment(replyId: Long) = settings.removeCommentFavorite(replyId)
    override fun clearTopics() = settings.clearFavorites()
    override fun clearComments() = settings.clearCommentFavorites()
}
