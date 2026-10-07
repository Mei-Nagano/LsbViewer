package sb.linux.client.data

import sb.linux.client.domain.community.DirectMessageContact
import sb.linux.client.domain.community.DirectMessageThread
import sb.linux.client.domain.community.PrivateMessageHistory
import sb.linux.client.domain.community.PrivateMessageRepository
import sb.linux.client.domain.community.PmMessage
import sb.linux.client.domain.community.NotificationItem

/** Owns the source forum's private-message pages and form protocol. */
internal class PrivateMessageService(
    private val client: LsbClient,
    private val settings: AppSettings,
) : PrivateMessageRepository, PrivateMessageHistory {
    override suspend fun loadContacts(): List<DirectMessageContact> =
        DirectMessageHtmlParser.parseDirectMessageContacts(client.get("/direct_messages").html)

    override suspend fun loadThread(partnerId: Long): DirectMessageThread {
        val response = client.get("/direct_messages/$partnerId")
        check(!response.url.contains("login")) { "登录已失效" }
        check(response.html.contains("direct-messages-thread")) { "未能识别源站会话，请刷新重试" }
        return DirectMessageHtmlParser.parseDirectMessageThread(response.html, partnerId)
    }

    override suspend fun sendMessage(partnerId: Long, body: String): String? {
        val csrf = client.csrf()
        val response = client.postForm(
            "/direct_messages/$partnerId",
            mapOf("_csrf" to csrf, "partner_id" to partnerId.toString(), "content" to body),
        )
        check(!response.url.contains("login")) { "登录已失效，消息未发送" }
        return if (response.url.contains("form_error")) {
            HtmlTextParser.extractError(response.html).ifBlank { "发送失败" }
        } else null
    }

    fun syncNotifications(items: List<NotificationItem>) {
        items.asReversed().forEach { item ->
            if (item.partnerId <= 0 || item.content.isBlank()) return@forEach
            val timestamp = ForumTimeParser.parseRelativeTime(item.timeText)
            if (item.quoteText.isNotBlank() &&
                !settings.pmHasContent(item.partnerId, item.quoteText)
            ) {
                settings.addPmMessage(
                    partnerId = item.partnerId,
                    partnerName = item.fromUser,
                    avatarUrl = item.partnerAvatar,
                    incoming = false,
                    content = item.quoteText,
                    ts = timestamp,
                    synthetic = true,
                )
            }
            settings.addPmMessage(
                partnerId = item.partnerId,
                partnerName = item.fromUser,
                avatarUrl = item.partnerAvatar,
                incoming = true,
                content = item.content,
                ts = timestamp,
                quoteText = item.quoteText,
            )
        }
    }

    override fun messages(partnerId: Long): List<PmMessage> = settings.pmThread(partnerId)

    override fun markRead(partnerId: Long) = settings.markPmRead(partnerId)

    override fun saveThread(thread: DirectMessageThread) = settings.savePmThread(thread)

    override fun addOutgoing(partnerId: Long, partnerName: String, avatarUrl: String, body: String) =
        settings.addPmMessage(partnerId, partnerName, avatarUrl, incoming = false, content = body)
}
