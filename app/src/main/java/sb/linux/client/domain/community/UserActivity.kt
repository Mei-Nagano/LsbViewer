package sb.linux.client.domain.community

data class PointRow(
    val timeText: String,
    val reason: String,
    val change: String,
    val positive: Boolean,
    val timestamp: Long = 0,
    val topicId: Long = 0,
)

data class ReplyItem(
    val topicId: Long,
    val title: String,
    val excerpt: String,
    val timeText: String,
    val replyId: Long = 0,
    val forumName: String = "",
)
