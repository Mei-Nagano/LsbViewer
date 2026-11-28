package sb.linux.client.ui.screens

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.json.JSONObject
import sb.linux.client.common.network.Endpoints
import sb.linux.client.domain.topic.PostEntry
import sb.linux.client.domain.topic.TopicPageData
import sb.linux.client.ui.content.parseEmbeddedVideo

internal val SOURCE_TOPIC_ASSET: String get() = "${Endpoints.BASE}/__lsb_content__/"

/** 浏览器消费服务端 HTML，不再把段落、图片和内联公式拆成原生块。 */
internal object SourceTopicDocument {
    fun shell(styles: List<String>, dark: Boolean, theme: String): String {
        val links = styles.ifEmpty { listOf("${Endpoints.BASE}/app/assets/index.css", "${Endpoints.BASE}/app/assets/plugins.css") }
            .joinToString("") { "<link rel=\"stylesheet\" href=\"${escape(Endpoints.abs(it))}\">" }
        val mode = if (dark) "dark" else "light"
        return """<!doctype html><html data-dark-mode-theme="$mode" data-themes-color-mode="$mode" data-color-scheme-dark-mode-theme="$mode"><head>
            <meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
            <link rel="stylesheet" href="${SOURCE_TOPIC_ASSET}fallback.css">$links
            <link rel="stylesheet" href="${SOURCE_TOPIC_ASSET}host.css">
            <link rel="stylesheet" href="${SOURCE_TOPIC_ASSET}katex/katex.min.css">
            <style>:root{$theme}</style><script src="${SOURCE_TOPIC_ASSET}katex/katex.min.js"></script>
            <script src="${SOURCE_TOPIC_ASSET}highlight.min.js"></script></head>
            <body class="lsb-content"><ul class="post-list topic-post-list" id="lsb-posts"></ul>
            <script src="${SOURCE_TOPIC_ASSET}runtime.js"></script></body></html>""".trimIndent()
    }

    fun update(page: TopicPageData, view: TopicScreenViewState, ui: TopicScreenUiState,
        loggedIn: Boolean, hideAll: Boolean, hidden: Set<Long>, slotHeight: Float, showUid: Boolean): String {
        val posts = (view.mainPost?.let { listOf(it to 0) }.orEmpty() +
            (if (view.reply.hasTree) view.reply.flatTree else view.reply.pagedReplies.map { it to 0 }))
            .distinctBy { it.first.id }
        val rows = org.json.JSONArray()
        posts.forEach { (post, depth) -> rows.put(JSONObject().put("id", post.id.toString())
            .put("html", post(post, depth, page, view, ui, loggedIn, hideAll || post.id in hidden, showUid))) }
        val options = JSONObject().put("rows", rows).put("mainId", view.mainPost?.id?.toString().orEmpty())
            .put("comments", view.reply.replies.isNotEmpty() || page.loginVisibleReplies >= 0)
            .put("count", page.repliesText.ifBlank { view.reply.replies.size.toString() })
            .put("slotHeight", slotHeight).put("query", ui.topicSearchQuery.takeIf { ui.topicSearchOpen }.orEmpty())
            .put("canCollectSticker", page.stickerCollect?.enabled == true)
        return "window.lsbContent&&window.lsbContent.update(${options.toString().replace("<", "\\u003c")});"
    }

    private fun post(post: PostEntry, depth: Int, page: TopicPageData, view: TopicScreenViewState,
        ui: TopicScreenUiState, loggedIn: Boolean, signatureHidden: Boolean, showUid: Boolean): String {
        val row = Element("li").attr("id", "lsb-post-${post.id}").attr("data-post-id", post.id.toString())
            .attr("data-floor", post.floor.toString()).addClass("post-item").addClass("post-entry")
        if (depth > 0) row.addClass("lsb-thread-child").attr("style", "--lsb-thread-indent:${depth.coerceAtMost(2) * 16}px")
        val header = post.authorHtml.ifBlank {
            "<div class=\"post-avatar\"><a href=\"${action("user", post.authorId)}\"><img class=\"avatar-img\" src=\"${escape(post.avatarUrl)}\"></a></div>" +
                "<div class=\"post-body\"><div class=\"post-head\"><a class=\"post-author\" href=\"/user/${post.authorId}\">${escape(post.authorName)}</a></div><div class=\"post-meta\">${escape(post.timeText)}</div></div>"
        }
        row.append(safeHtml(header))
        if (!showUid) row.select(".user-uid-badge").remove()
        row.select(".post-avatar a").forEach { it.attr("href", action("user", post.authorId)) }
        if (post.online) row.selectFirst(".post-avatar")?.addClass("lsb-avatar-online")
        if (post.floor > 0) {
            row.addClass("lsb-has-floor")
            row.appendElement("a").addClass("lsb-post-floor")
                .attr("href", action("floor", post.floor.toLong())).attr("aria-label", "第 ${post.floor} 楼")
                .text("#${post.floor}")
        }
        if (post.userBlocked) {
            row.addClass("lsb-blocked-post")
            row.selectFirst(".post-avatar")?.empty()?.text("×")
            if (post.authorName.isBlank()) row.selectFirst(".post-author")?.text("已屏蔽用户")
            val notice = row.appendElement("div").addClass("lsb-blocked")
            if (post.blockedPlaceholderHtml.isNotBlank()) notice.append(safeHtml(post.blockedPlaceholderHtml))
            else notice.text(post.blockedPlaceholder.ifBlank { "已隐藏屏蔽用户的内容" })
            if (notice.selectFirst(".user-block-hidden-icon") != null) notice.addClass("lsb-blocked-with-icon")
            if (notice.select("a[href]").isEmpty()) notice.appendElement("a")
                .attr("href", "/user_block").text("解除屏蔽")
            return row.outerHtml()
        }
        if (post.pinned) row.selectFirst(".post-info")?.appendElement("span")?.addClass("reply-pin-badge")?.text("置顶")
        if (post.essenceVoteLabel.isNotBlank()) row.selectFirst(".post-info")?.appendElement("span")
            ?.addClass("topic-essence-review-reply-label")?.addClass(if (post.essenceVoteSupport == true) "is-support" else "is-oppose")?.text(post.essenceVoteLabel)
        val body = row.appendElement("div").addClass("post-content").attr("data-body", "")
        body.append(safeHtml(post.contentHtml))
        if (post.editInfo.isNotBlank()) {
            val note = body.appendElement("div").addClass("sb-limit-edit-time-note")
            val index = post.editUserName.takeIf { it.isNotBlank() && post.editUserId > 0 }?.let { post.editInfo.indexOf(it) } ?: -1
            if (index < 0) note.text(post.editInfo) else {
                note.appendText(post.editInfo.substring(0, index))
                note.appendElement("a").attr("href", "/user/${post.editUserId}").text(post.editUserName)
                note.appendText(post.editInfo.substring(index + post.editUserName.length))
            }
        }
        if (post.signatureHtml.isNotBlank() && !signatureHidden) {
            val signature = body.appendElement("footer").addClass("post-signature-content")
            signature.append(safeHtml(post.signatureHtml))
            signature.appendElement("a").addClass("lsb-signature-toggle")
                .attr("href", action("hide-signature", post.id)).text("隐藏签名")
        }
        val ops = row.appendElement("div").addClass("lsb-post-actions")
        fun button(kind: String, label: String, active: Boolean = false, count: Int? = null) {
            val link = ops.appendElement("a").attr("href", action(kind, post.id))
                .attr("aria-label", label).attr("title", label).append(SourcePostIcons.svg(kind))
            link.appendElement("span").addClass("lsb-action-label").attr("aria-hidden", "true")
                .text(if (kind == "like") "赞" else label)
            if (count != null) link.appendElement("span").attr("aria-hidden", "true").text(count.toString())
            if (active) link.addClass("is-active").attr("aria-pressed", "true")
        }
        if (post.canLike) { button("like", "赞 ${post.likeCount}", post.liked, post.likeCount); button("coin", if (post.coined) "已投币" else "投币", post.coined) }
        if (post.floor == 0 && page.donateUrl != null) button("donate", "点赞打赏", count = post.likeCount)
        button("quote", "回复")
        button("report", "举报")
        button("copy", "复制")
        if (post.floor == 0 && page.canFavorite) button("favorite-topic", if (ui.isFav) "已收藏" else "收藏文章", ui.isFav)
        if (post.floor > 0) button("favorite", if (post.id in ui.favoriteCommentIds) "已收藏" else "收藏", post.id in ui.favoriteCommentIds)
        if (view.threadEnabled && (post.referencedFloors.isNotEmpty() || (view.reply.threadReplyCount[post.floor] ?: 0) > 0)) button("thread", "查看对话")
        val children = view.reply.treeChildCounts[post.id] ?: 0
        if (children > 0) button(if (post.id in ui.collapsedReplyIds) "expand" else "collapse", if (post.id in ui.collapsedReplyIds) "展开 $children 条回复" else "折叠 $children 条回复")
        if (loggedIn && (post.sourceActions.any { it.buttons.any { button -> button.enabled } } || post.floor == 0 && page.forumMove != null)) button("source-actions", "帖子操作")
        return row.outerHtml()
    }

    private fun safeHtml(html: String): String = Jsoup.parseBodyFragment(html, Endpoints.BASE).body().apply {
        select("script, style, link, meta, base, form, input, button, object, embed").remove()
        getAllElements().forEach { node ->
            node.attributes().asList().filter { it.key.startsWith("on", true) || it.key in setOf("srcdoc", "autofocus") }
                .forEach { node.removeAttr(it.key) }
            if (node.normalName() == "iframe") {
                val video = parseEmbeddedVideo(node)
                if (video == null) { node.remove(); return@forEach }
                node.attr("src", video.playerUrl)
                node.attr("sandbox", "allow-scripts allow-same-origin allow-presentation allow-modals allow-popups allow-popups-to-escape-sandbox")
                node.attr("allow", "fullscreen; encrypted-media").attr("loading", "lazy")
            }
            listOf("href", "src", "poster").forEach { key ->
                val raw = node.attr(key).trim()
                if (raw.startsWith("javascript:", true) || raw.startsWith("vbscript:", true) || raw.startsWith("file:", true) || raw.startsWith("lsb-action:", true)) node.removeAttr(key)
                else if (raw.startsWith("https://", true) || raw.startsWith("http://", true) || raw.startsWith("//")) node.attr(key, Endpoints.abs(raw))
            }
        }
    }.html()

    private fun action(kind: String, id: Long) = "lsb-action://$kind/$id"
    private fun escape(value: String) = org.jsoup.nodes.TextNode(value).outerHtml().replace("\"", "&quot;")
}
