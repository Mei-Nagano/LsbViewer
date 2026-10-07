package sb.linux.client.domain.community

data class NotificationItem(
    val fromUser: String,
    val typeText: String,
    val timeText: String,
    val content: String,
    val link: String,
    val unread: Boolean = false,
    val isTopic: Boolean = true,
    val partnerId: Long = 0,
    val partnerAvatar: String = "",
    val quoteText: String = "",
)

interface NotificationRepository {
    suspend fun loadNotifications(userId: Long): List<NotificationItem>
}
