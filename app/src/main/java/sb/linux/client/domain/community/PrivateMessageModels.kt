package sb.linux.client.domain.community

data class PmMessage(
    val partnerId: Long,
    val partnerName: String,
    val avatarUrl: String = "",
    val incoming: Boolean,
    val content: String,
    val timeText: String,
    val ts: Long = 0L,
    val seq: Long = 0L,
    val timeExact: Boolean = true,
    val quoteText: String = "",
    val synthetic: Boolean = false,
)

data class DirectMessageContact(
    val userId: Long,
    val username: String,
    val avatarUrl: String = "",
    val timeText: String = "",
    val preview: String = "",
)

data class DirectMessageThread(
    val partnerId: Long,
    val partnerName: String,
    val avatarUrl: String = "",
    val messages: List<PmMessage> = emptyList(),
)

interface PrivateMessageRepository {
    suspend fun loadContacts(): List<DirectMessageContact>
    suspend fun loadThread(partnerId: Long): DirectMessageThread
    suspend fun sendMessage(partnerId: Long, body: String): String?
}

interface PrivateMessageHistory {
    fun messages(partnerId: Long): List<PmMessage>
    fun markRead(partnerId: Long)
    fun saveThread(thread: DirectMessageThread)
    fun addOutgoing(partnerId: Long, partnerName: String, avatarUrl: String, body: String)
}
