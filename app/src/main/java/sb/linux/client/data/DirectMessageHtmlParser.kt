package sb.linux.client.data
import sb.linux.client.domain.community.DirectMessageContact
import sb.linux.client.domain.community.DirectMessageThread
import sb.linux.client.domain.community.PmMessage

object DirectMessageHtmlParser {
    fun parseDirectMessageContacts(html: String): List<DirectMessageContact> {
        val d = SourceHtmlSupport.sourceDocument(html)
        return d.select("a.direct-messages-search-result[href^=/direct_messages/], a.direct-messages-conversation[href^=/direct_messages/]").mapNotNull { a ->
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
            val contentEl = article.selectFirst(".direct-messages-content")
            val imageEl = article.selectFirst(".direct-messages-image img[src]")
            if (contentEl == null && imageEl == null) return@mapIndexedNotNull null
            val clonedContent = contentEl?.clone()
            val quoteText = clonedContent?.selectFirst(".direct-messages-quote")?.let { quote ->
                val text = quote.text().trim()
                quote.remove()
                text
            }.orEmpty()
            val content = clonedContent?.wholeText()?.trim().orEmpty()
            val contentHtml = clonedContent?.html()?.trim().orEmpty()
            val imageUrl = imageEl?.attr("src")?.trim()?.takeIf { it.isNotBlank() }?.let(SourceHtmlSupport::absUrl).orEmpty()
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
                contentHtml = contentHtml,
                imageUrl = imageUrl,
                timeText = rawTime,
                ts = ts,
                seq = index.toLong(),
                quoteText = quoteText,
            )
        }
        val composeForm = d.selectFirst("form.direct-messages-compose")
            ?: d.select("form").firstOrNull { it.selectFirst("textarea[name=content]") != null }
        val composeTextarea = composeForm?.selectFirst("textarea[name=content]")
        val maxLength = composeTextarea?.attr("maxlength")?.toIntOrNull()?.takeIf { it > 0 } ?: 500
        val csrf = composeForm?.selectFirst("input[name=_csrf]")?.attr("value").orEmpty()
            .ifBlank { d.selectFirst("input[name=_csrf]")?.attr("value").orEmpty() }
        val blockForm = d.select("form").firstOrNull { it.selectFirst("input[name=direct_messages_block_action]") != null }
        return DirectMessageThread(
            partnerId = partnerId,
            partnerName = partnerName,
            avatarUrl = partnerAvatar,
            messages = messages,
            canSend = d.selectFirst("textarea[name=content]:not([disabled])") != null,
            blockLabel = blockForm?.selectFirst("button[type=submit]")?.text().orEmpty(),
            maxLength = maxLength,
            stickerPicker = StickerPackHtmlParser.parsePicker(composeForm, csrf),
            stickerCollect = StickerPackHtmlParser.parseCollect(d),
        )
    }

    /** 源站 meta 文本可能自带前导分隔符（· / | 等），显示层自己排版，这里统一剥掉。 */
    private fun trimMetaSeparator(text: String): String =
        text.trim().trimStart('·', '•', '|', '/', '-', '–', '—', ' ', ' ').trim()


    /** 足迹/未读提醒页：解析成帖子卡片列表（结构与首页 post-list 一致时） */
}
