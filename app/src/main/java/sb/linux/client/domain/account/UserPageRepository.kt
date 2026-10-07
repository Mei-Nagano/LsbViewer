package sb.linux.client.domain.account

import sb.linux.client.domain.community.NotificationItem
import sb.linux.client.domain.community.PointRow
import sb.linux.client.domain.community.ReplyItem
import sb.linux.client.domain.topic.TopicCard

data class UserPageContent(
    val profile: UserProfile?,
    val topics: List<TopicCard> = emptyList(),
    val replies: List<ReplyItem> = emptyList(),
    val points: List<PointRow> = emptyList(),
    val notifications: List<NotificationItem> = emptyList(),
)

interface UserPageRepository {
    suspend fun load(userId: Long, tab: String): UserPageContent
}
