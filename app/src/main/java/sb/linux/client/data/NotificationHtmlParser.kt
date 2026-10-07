package sb.linux.client.data
import sb.linux.client.domain.community.NotificationItem


object NotificationHtmlParser {
    fun parseNotifications(html: String): List<NotificationItem> {
        val d = SourceHtmlSupport.sourceDocument(html)
        return d.select("li.notification-item").map { li ->
            val typeText = li.selectFirst(".notification-kind")?.text() ?: "通知"
            // 私信回复结构：<blockquote>对方引用的我方消息</blockquote><p>正文</p>。
            // 引用单独取出（供会话还原我方消息、展示回复上下文），正文剔除引用部分。
            val contentEl = li.selectFirst(".notification-content")
            val quoteText = contentEl?.selectFirst("blockquote")?.text()
                ?.replace(Regex("""\s+"""), " ")?.trim() ?: ""
            val content = run {
                val el = contentEl?.clone()
                el?.select("blockquote")?.remove()
                el?.text() ?: ""
            }.replace(Regex("""\s+"""), " ").trim()
            // 源站私信（站内信）必带「回复TA」→ /notify/{uid}，帖子/系统通知没有此入口。
            // 用它作为私信强标志：即便私信正文里贴了 /topic/ 链接（旧逻辑会被骗去当帖子）也绝不串帖。
            val replyAction = li.selectFirst(".notification-reply-action")?.attr("href") ?: ""
            val isPm = replyAction.contains("/notify/") || Regex(
                """私信|私聊|站内信|短消息|聊天|私讯|回信|pm|conversation"""
            , RegexOption.IGNORE_CASE).containsMatchIn(typeText)
            // 主目标链接：私信用「回复TA」进对话，帖子/系统通知取正文里的「查看主题」链接
            val link = replyAction.ifBlank { li.selectFirst(".notification-content a[href]")?.attr("href") ?: "" }
            // 确为帖子类：非私信，且主链接确为 /topic/{id}
            val isTopic = !isPm && Regex("""/topic/\d+""").containsMatchIn(link)
            // 发送者/来源：私信是发送者名；系统/帖子用 .post-title（可能为 <span>系统</span> 或帖子标题链接）
            val sender = li.selectFirst(".post-title")?.text()
                ?: li.selectFirst(".notification-content img[alt]")?.attr("alt")
                ?: li.selectFirst(".notification-content a[href*=\"/user/\"]")?.text()
            // 私信对方 uid 与头像：优先 /notify/{uid} 回复入口，其次头像链接 /user/{uid} 与头像 src
            val partnerId = Regex("""/notify/(\d+)""").find(replyAction)
                ?.groupValues?.get(1)?.toLongOrNull()
                ?: (li.selectFirst(".post-avatar a[href*=\"/user/\"]")?.attr("href")
                    ?.let { Regex("""/user/(\d+)""").find(it)?.groupValues?.get(1)?.toLongOrNull() } ?: 0)
            // 头像：源站 .post-avatar img 的 src 为相对路径（/app/upload/... 或 /app/avatars/dylan_48.svg），
            // 必须转绝对 URL，否则 OkHttp 请求图片时按非法 URL 失败、界面退回占位图标
            val partnerAvatar = SourceHtmlSupport.absUrl(li.selectFirst(".post-avatar img[src]")?.attr("src") ?: "")
            NotificationItem(
                fromUser = if (isTopic) li.selectFirst(".post-title")?.text() ?: (sender ?: "系统")
                           else sender ?: typeText,
                typeText = typeText,
                timeText = li.selectFirst(".post-meta span")?.text() ?: "",
                content = content,
                link = link,
                unread = li.classNames().contains("unread") || li.selectFirst(".notification-unread") != null,
                isTopic = isTopic,
                partnerId = if (isPm) partnerId else 0,
                partnerAvatar = partnerAvatar,
                quoteText = if (isPm) quoteText else "",
            )
        }
    }

    /**
     * 首页顶栏未读通知数（.nav-mine 内的 .notify-badge）。
     * 无未读时源站不渲染该元素；关键词过滤计数（.home-keyword-filter-count）不算在内。
     */
    fun parseNotifyBadgeCount(html: String): Int =
        SourceHtmlSupport.sourceDocument(html).selectFirst(".nav-mine .notify-badge:not(.home-keyword-filter-count)")
            ?.text()?.trim()?.toIntOrNull() ?: 0

}
