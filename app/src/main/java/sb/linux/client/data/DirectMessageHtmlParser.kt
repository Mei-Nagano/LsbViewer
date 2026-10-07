package sb.linux.client.data
import sb.linux.client.domain.community.DirectMessageContact
import sb.linux.client.domain.community.DirectMessageThread
import sb.linux.client.domain.community.PmMessage

object DirectMessageHtmlParser {
    fun parseDirectMessageContacts(html: String): List<DirectMessageContact> {
        val d = SourceHtmlSupport.sourceDocument(html)
        return d.select("a.direct-messages-conversation[href^=/direct_messages/]").mapNotNull { a ->
            val uid = SourceHtmlSupport.idFrom(a.attr("href"))
            if (uid <= 0) return@mapNotNull null
            DirectMessageContact(
                userId = uid,
                username = a.selectFirst("strong")?.text()?.trim() ?: "",
                avatarUrl = SourceHtmlSupport.absUrl(SourceHtmlSupport.avatarOf(a)),
                timeText = a.selectFirst("time")?.text()?.trim() ?: "",
                preview = a.selectFirst("small")?.text()?.trim() ?: "",
            )
        }.distinctBy { it.userId }
    }

    /** 新版服务端私信会话（/direct_messages/{uid}），服务端消息为权威数据源。 */
    fun parseDirectMessageThread(html: String, partnerId: Long): DirectMessageThread {
        val d = SourceHtmlSupport.sourceDocument(html)
        val header = d.selectFirst(".direct-messages-thread-header a[href^=/user/]")
            ?: d.selectFirst("main a[href=/user/$partnerId]")
        val partnerName = header?.selectFirst("strong")?.text()?.trim()
            ?: header?.attr("aria-label")?.substringBefore(" 私信")?.trim().orEmpty()
        val partnerAvatar = header?.let(SourceHtmlSupport::avatarOf).orEmpty().let(SourceHtmlSupport::absUrl)
        val messages = d.select("article.direct-messages-message").mapIndexedNotNull { index, article ->
            val content = article.selectFirst(".direct-messages-content")?.text()?.trim() ?: return@mapIndexedNotNull null
            val incoming = !article.hasClass("is-mine")
            val time = article.selectFirst("time")
            val rawTime = time?.text()?.trim().orEmpty()
            val ts = runCatching {
                java.time.OffsetDateTime.parse(time?.attr("datetime")).toInstant().toEpochMilli()
            }.getOrElse {
                runCatching {
                    java.time.LocalDateTime.parse(rawTime, java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                        .atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                }.getOrDefault(0L)
            }
            PmMessage(
                partnerId = partnerId,
                partnerName = partnerName,
                avatarUrl = if (incoming) SourceHtmlSupport.absUrl(SourceHtmlSupport.avatarOf(article)) else partnerAvatar,
                incoming = incoming,
                content = content,
                timeText = rawTime,
                ts = ts,
                seq = index.toLong(),
            )
        }
        return DirectMessageThread(partnerId, partnerName, partnerAvatar, messages)
    }

    /** 源站 meta 文本可能自带前导分隔符（· / | 等），显示层自己排版，这里统一剥掉。 */
    private fun trimMetaSeparator(text: String): String =
        text.trim().trimStart('·', '•', '|', '/', '-', '–', '—', ' ', ' ').trim()


    /** 足迹/未读提醒页：解析成帖子卡片列表（结构与首页 post-list 一致时） */
}
