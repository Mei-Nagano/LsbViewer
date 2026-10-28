package sb.linux.client.domain.topic

data class TopicCard(
    val topicId: Long,
    val title: String,
    val authorId: Long,
    val authorName: String,
    val forumId: Long,
    val forumName: String,
    val replies: Int,
    val lastReplier: String,
    val timeText: String,
    val pinned: Boolean = false,
    val hot: Boolean = false,
    val featured: Boolean = false,
    val unread: Boolean = false,
    val titleColor: String = "",
    val avatarUrl: String = "",
    val pages: List<Int> = emptyList(),
    val lotteryStatus: String = "",
    val cardStatus: String = "",
    val sourceTags: List<String> = emptyList(),
    val views: Int = -1,
    val online: Boolean = false,
    val titleBadge: TitleBadge? = null,
    /** 本地浏览时间（毫秒），仅浏览历史使用；0 表示未记录。 */
    val visitedAt: Long = 0L,
)
