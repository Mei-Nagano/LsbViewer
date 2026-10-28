package sb.linux.client.data
import sb.linux.client.common.network.Endpoints
import sb.linux.client.domain.gacha.SourceFormProtocol
import org.jsoup.Jsoup
import org.jsoup.nodes.TextNode
import sb.linux.client.data.parser.LoginVerificationParser
import sb.linux.client.util.TimeFormat
import sb.linux.client.domain.topic.PostEntry
import sb.linux.client.domain.topic.TopicPageData
internal object TopicPageParser {
    fun parse(html: String, fallbackId: Long): TopicPageData {
        val d = Jsoup.parse(html, Endpoints.BASE)
        val onlineIds = SourceHtmlSupport.onlineIdsOf(d)
        val csrf = d.selectFirst("input[name=_csrf]")?.attr("value") ?: ""
        val replyCaptcha = LoginVerificationParser.parse(html, Endpoints.abs("/topic/$fallbackId"))
        val realTitle = TopicTitleParser.parse(d)
        // Read topic panels before reply parsing; reply cleanup removes these nodes.
        val lottery = TopicLotteryParser.parse(d)
        val card = TopicVirtualCardParser.parse(d)
        val essencePanel = d.selectFirst(".topic-essence-review-panel")
        val essenceApplication = TopicEssenceReviewParser.parseApplication(essencePanel)
        val essenceReview = TopicEssenceReviewParser.parseReview(essencePanel)
        val poll = TopicPollParser.parse(d, essencePanel, essenceApplication)

        val postElements = d.select("ul.topic-post-list li.post-entry, ul.post-list.topic-post-list li.post-entry")
        val replyIdToFloor = postElements.mapNotNull { li ->
            val replyId = li.id().removePrefix("post-").toLongOrNull() ?: return@mapNotNull null
            val floor = li.attr("data-floor").toIntOrNull() ?: return@mapNotNull null
            if (floor > 0) replyId to floor else null
        }.toMap()
        val posts = postElements.map { li ->
            val block = BlockedPostParser.parse(li)
            val floorNum = li.attr("data-floor").toIntOrNull() ?: 0
            // 网页脚本会补 data-quote-threads-parent-floor，但 OkHttp 拿到的是执行脚本前 HTML。
            // 因此还需从正文第一段开头的 ?replyid=父回复ID 反查父楼层，这是原生客户端的权威兜底。
            val ownContent = li.select(".post-content").firstOrNull { it.closest("li.post-entry") == li }
            val mentionAnchor = ownContent?.selectFirst("p")
                ?.takeIf { it.closest("blockquote") == null }
                ?.selectFirst("a[href*=replyid]")
                ?.takeIf { anchor ->
                    anchor.parent()?.tagName() == "p" &&
                    Regex("""^(?:https://linux\.sb)?/topic/$fallbackId(?:[?#]|$)""").containsMatchIn(anchor.attr("href")) &&
                    generateSequence(anchor.previousSibling()) { it.previousSibling() }.all { sibling ->
                        sibling is TextNode && sibling.isBlank
                    }
                }
            val mentionHref = mentionAnchor?.attr("href").orEmpty()
            val parentReplyId = Regex("""[?&]replyid=(\d+)""").find(mentionHref)
                ?.groupValues?.get(1)?.toLongOrNull()
            val mentionedFloor = mentionAnchor?.text()?.let { text ->
                Regex("""#\s*(\d+)\s*$""").find(text)?.groupValues?.get(1)?.toIntOrNull()
            } ?: (mentionAnchor?.nextSibling() as? TextNode)?.text()?.let {
                Regex("""^\s*#\s*(\d+)(?:\s|$)""").find(it)?.groupValues?.get(1)?.toIntOrNull()
            }
            val parentFloorNum = li.attr("data-quote-threads-parent-floor").toIntOrNull()
                ?.takeIf { it > 0 } ?: parentReplyId?.let { replyIdToFloor[it] } ?: mentionedFloor ?: 0
            val authorA = li.selectFirst("a.post-author")
            val groups = li.select(".post-user-group").map { group ->
                SourceHtmlSupport.textWithoutGachaTitle(group).substringBefore("积分").trim(' ', '·')
            }.filter { it.isNotBlank() }.distinct()
            val uidBadge = groups.firstOrNull { it.startsWith("UID") }
            val group = groups.lastOrNull { !it.startsWith("UID") } ?: ""
            val uidNum = uidBadge?.removePrefix("UID")?.trim()?.toLongOrNull() ?: 0L
            val meta = li.selectFirst(".post-meta")
            val timeText = meta?.selectFirst("span[data-performance-time]")?.attr("data-performance-time")?.toLongOrNull()
                // 兜底取首个 span 时须跳过树形回复的「回复 #N」标识，否则时间缺失时会显示成楼层引用
                ?.let { TimeFormat.relative(it) } ?: meta?.selectFirst("span:not(.quote-threads-reference)")?.text() ?: ""
            // 源站 v8.6+ 点赞改版：like-coin-* → donate-reaction-*（按钮 data-liked/data-coined/计数 span + 表单 donate-reaction-form）
            val likeBtn = li.selectFirst("[data-donate-reaction]")
            val likeForm = li.selectFirst("form.donate-reaction-form")
            val essenceVoteEl = li.selectFirst(".topic-essence-review-reply-label")
            val essenceVoteLabel = essenceVoteEl?.text()?.trim().orEmpty()
            val essenceVoteSupport = essenceVoteEl?.let {
                when {
                    it.hasClass("is-support") -> true
                    it.hasClass("is-oppose") -> false
                    else -> null
                }
            }
            // 正文：把"最后编辑"信息从正文拆出，单独用分割线展示
            val signatureHtml = PostSignatureParser.parse(li)
            val contentEl = ownContent?.clone()
            contentEl?.select(PostSignatureParser.SELECTOR)?.remove()
            var editInfo = ""
            var editUserId = 0L
            var editUserName = ""
            if (contentEl != null) {
                // 源站「展开全文」折叠长内容（针对长图/超长帖占位修复）：本应用直接展示全文，
                // 忽略折叠。移除展开按钮（long-content-fold-actions）并解包折叠容器
                // （long-content-fold-content），避免正文里混入「展开全文」字样，也让编辑标注
                // 恢复为正文的直接子节点，便于下方提取。
                contentEl.select(".long-content-fold-actions").remove()
                contentEl.select(".long-content-fold-content").forEach { it.unwrap() }
                // 非正文元素剥离：抽奖面板已单独解析到帖子顶部卡片（community-lottery-card
                // 会渲染在 post-content 内）、发卡兑换卡同理；打赏区（donate-area，源站 JS 会
                // 把它移入 post-content，登录态下可能直接渲染在正文里）与快速回复区改为
                // 操作行按钮（分割线下方），不再混入正文重复展示
                contentEl.select(
                    ".community-lottery-card, .community-lottery-topic-panel, " +
                        ".community-lottery-readonly-prize, .community-lottery-prize-row, " +
                        ".red-packet-card, .donate-area, [data-donate-area], .quick-reply-main-action, .virtual-card-box"
                        + ", .topic-poll, .poll-card, form[class*=poll]"
                        // 淘帖插件会把所属专辑、收录/移除表单插进 post-content；
                        // 应用已有独立淘帖入口和帖子菜单，这些不属于 Markdown 正文。
                        + ", [class*=topic-collection], [data-topic-collection], "
                        + "form[action*=topic_collection], form[action*=topic_collections]"
                        + ", .topic-essence-review-reply-label"
                ).remove()
                // 编辑信息节点：专用类名优先（含源站新类 sb-limit-edit-time-note）；
                // 兜底查找正文内任意含「最后编辑」的尾部元素
                // （包括深层嵌套，如 blockquote 内的编辑标注），避免残留在正文中显示在分隔线上方
                val editEl = contentEl.selectFirst(".post-edit-info, .edit-info, .post-last-edit, .sb-limit-edit-time-note")
                    ?: contentEl.select("p, div, span, small, em, footer")
                        .lastOrNull { el ->
                            val t = el.ownText().ifBlank { el.text() }
                            t.contains("最后编辑") && el.children().none { c ->
                                c.text().contains("最后编辑")
                            }
                        }
                if (editEl != null) {
                    val editorA = editEl.selectFirst("a[href^=/user/]")
                    if (editorA != null) {
                        editUserId = SourceHtmlSupport.idFrom(editorA.attr("href"))
                        editUserName = editorA.text().trim()
                    }
                    editInfo = editEl.text().trim()
                    editEl.remove()
                }
                // 树形回复引用锚点剥离：源站在正文首段开头渲染「@用户 #N」锚点
                //（href 为 ?replyid=父楼id）。层级关系已由 data-quote-threads-parent-floor
                // 表达、UI 以「回复 #N」胶囊展示，正文里去掉避免重复。
                // 长用户名时源站会把锚点结构拆散（#楼层号 落在锚点外、或锚点外有空白文本），
                // 旧逻辑只认「首段第一个子节点恰为完整锚点」导致剥离失败——正文残留
                //「@用户 #N」文本且层级展示错乱。放宽：找首段开头第一个 replyid 锚点删除，
                // 并连同其后紧邻的「#N」文本节点一并剥离
                if (parentFloorNum > 0) {
                    contentEl.selectFirst("p")?.let { p ->
                        val anchor = p.select("a[href*=replyid=]").firstOrNull()
                        if (anchor != null) {
                            // 锚点须位于段落开头（前面的兄弟节点仅允许空白文本）
                            val leading = mentionAnchor != null && anchor.attr("href") == mentionHref &&
                                generateSequence(anchor.previousSibling()) { it.previousSibling() }.all {
                                    it is TextNode && it.isBlank
                                }
                            if (leading) {
                                val next = anchor.nextSibling()
                                anchor.remove()
                                // 「#N」被拆到锚点外时，紧邻的纯楼层号文本一并删除
                                if (next is TextNode) {
                                    next.text(next.wholeText.replaceFirst(Regex("""^\s*#\s*$parentFloorNum(?:\s+|$)"""), ""))
                                }
                            }
                        }
                    }
                    // 剥离后首段可能只剩空白（纯引用回复），删除避免顶部空行
                    contentEl.selectFirst("p")?.takeIf { it.text().isBlank() && it.children().isEmpty() }?.remove()
                }
            }
            PostEntry(
                id = (li.attr("id").removePrefix("post-").toLongOrNull() ?: 0L),
                floor = floorNum,
                parentFloor = parentFloorNum.takeIf { it > 0 && it != floorNum } ?: 0,
                authorId = SourceHtmlSupport.idFrom(authorA?.attr("href")),
                // wholeText 保留用户名内部的空格；只折叠 HTML 排版产生的连续空白，
                // 不再使用按空格切分用户名的启发式逻辑。
                authorName = authorA?.clone()?.also { it.select(".gacha-title-badge, .gacha-title-post-badge").remove() }
                    ?.wholeText()?.trim()?.replace(Regex("\\s+"), " ") ?: "",
                avatarUrl = SourceHtmlSupport.absUrl(SourceHtmlSupport.avatarOf(li)),
                userGroup = group,
                authorUid = uidNum,
                timeText = timeText,
                ipLocation = meta?.selectFirst(".ip-history-location-subtitle")?.text() ?: "",
                contentHtml = if (block.blocked) "" else contentEl?.html().orEmpty(),
                userBlocked = block.blocked,
                blockedPlaceholder = block.placeholder,
                editInfo = editInfo,
                editUserId = editUserId,
                editUserName = editUserName,
                // 整楼 HTML 提取引用（?floor=N 链接 / 纯文本 #N）。
                // 必须过滤本楼楼层号：源站每楼自带 <a class="post-floor" href="?floor=N">#N</a> 徽标，
                // 不过滤会导致每条评论都出现"查看对话"按钮且指向自己
                referencedFloors = SourceHtmlSupport.floorRefs(li.html()).filter { it > 0 && it != floorNum }.distinct(),
                likeCount = likeBtn?.selectFirst(".donate-reaction-count")?.text()?.filter { it.isDigit() }?.toIntOrNull() ?: 0,
                liked = likeBtn?.attr("data-liked") == "1",
                coined = likeBtn?.attr("data-coined") == "1",
                likeCoinType = "", // 举报类型由调用方按楼层默认（主楼 topic / 回复 reply）
                // 有表单或按钮即可互动：源站改版后表单类名可能变化/移出楼层，
                // 但 [data-donate-reaction] 按钮仍在楼内；仅以按钮判断会漏，
                // 两者任一存在即显示点赞投币（未登录时源站不渲染按钮与表单）
                canLike = !block.blocked && (likeForm != null || likeBtn != null),
                coinTiers = SourceFormProtocol.reactionTiers(likeBtn?.attr("data-tiers").orEmpty()),
                isOp = li.selectFirst(".quick-reply-main-action") != null || li.selectFirst("form.topic-favorites-action") != null,
                online = SourceHtmlSupport.idFrom(authorA?.attr("href")) in onlineIds || SourceHtmlSupport.onlineOf(li),
                titleBadge = SourceHtmlSupport.gachaTitleOf(li),
                signatureHtml = if (block.blocked) "" else signatureHtml,
                redPacketReward = li.selectFirst(".red-packet-reply-reward")?.attr("aria-label").orEmpty(),
                essenceVoteLabel = essenceVoteLabel,
                essenceVoteSupport = essenceVoteSupport,
            )
        }
        val favForm = d.selectFirst("form.topic-favorites-action")
        val (page, totalPages) = TopicListParser.parsePagination(d)

        // 未登录时评论区隐藏
        val loginVisible = d.selectFirst(".replies-login-visible-count")?.text()?.toIntOrNull() ?: -1
        val quickAuthor = d.selectFirst("[data-quick-reply-topic-author]")?.attr("data-quick-reply-topic-author") ?: ""

        // 查看次数 / 回复数：.post-content-stats 第一个 span 是浏览数（眼睛），第二个是回复数
        val statsNums = d.selectFirst(".post-content-stats")?.select("span")
            ?.map { it.text().trim().filter { c -> c.isDigit() } } ?: emptyList()
        val viewsText = statsNums.getOrNull(0) ?: ""
        val repliesText = statsNums.getOrNull(1) ?: ""

        // 正文标签：标题旁的状态徽章（如抽奖帖的 已开奖 / 抽奖中）
        val titleTag = d.selectFirst(".post-topic-title, h1.post-content-title, h1.topic-title")
            ?.let(TopicSourceTagsParser::parse)?.joinToString(" · ").orEmpty()

        return TopicPageData(
            topicId = fallbackId,
            title = realTitle,
            titleTag = titleTag,
            posts = posts,
            page = page,
            totalPages = totalPages,
            csrf = csrf,
            canFavorite = favForm != null,
            // 已收藏判定：源站收藏表单按钮文案为「取消收藏」，未收藏为「收藏」；
            // 兜底 data-favorited="1" 属性（同点赞按钮 data-liked 的写法）
            favorited = favForm != null &&
                (favForm.attr("data-favorited") == "1" || favForm.text().contains("取消")),
            favAction = favForm?.attr("action") ?: "/topic_favorite",
            donateUrl = d.selectFirst("a[data-donate-btn]")?.attr("href"),
            virtualCard = card,
            loginVisibleReplies = loginVisible,
            quickReplyAuthor = quickAuthor,
            viewsText = viewsText,
            repliesText = repliesText,
            redPacket = RedPacketParser.parse(d),
            lottery = lottery,
            poll = poll,
            replyCaptcha = replyCaptcha,
            essenceApplication = essenceApplication,
            essenceReview = essenceReview,
        )
    }

    // ---------------- 板块 ----------------

}

