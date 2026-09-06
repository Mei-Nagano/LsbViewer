package sb.linux.client.model.local

/** 本地收藏的评论快照。 */
data class CommentFavorite(
    val replyId: Long,
    val topicId: Long,
    val topicTitle: String,
    val floor: Int,
    val authorId: Long,
    val authorName: String,
    val avatarUrl: String,
    val content: String,
    val at: Long,
    val timeText: String = "",
)
