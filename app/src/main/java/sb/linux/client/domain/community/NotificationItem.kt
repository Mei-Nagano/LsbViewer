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
    val isSystem: Boolean = false,
    val lotteryPrize: String = "",
    val redemptionCode: String = "",
    val cardRedemptionTitle: String = "",
    val cardRedemptionCost: String = "",
)

interface NotificationRepository {
    suspend fun loadNotifications(userId: Long, forceRefresh: Boolean = false): List<NotificationItem>
    fun cachedNotifications(userId: Long): List<NotificationItem>? = null

    /** 逐页上屏：先给出已经有的一页，再继续读完剩余历史。 */
    fun loadNotificationsIncrementally(userId: Long, forceRefresh: Boolean = false): kotlinx.coroutines.flow.Flow<List<NotificationItem>> =
        kotlinx.coroutines.flow.flow { emit(loadNotifications(userId, forceRefresh)) }
}
