package sb.linux.client.data

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

object HtmlParser {

    /** 从 form_error 页面提取错误提示文本 */
    fun extractError(html: String): String {
        // 优先取 form-error-panel 内的具体错误文案
        val panel = doc(html).selectFirst(".form-error-panel, .form_error, .error-message")
        if (panel != null) {
            val t = panel.select("h2, p, li").eachText().distinct().joinToString("；").trim()
            if (t.isNotBlank()) return t
        }
        val seg = Regex("""<main.*?</main>""", RegexOption.DOT_MATCHES_ALL).find(html)?.groupValues?.get(0) ?: html
        val txt = seg.replace(Regex("""<(svg|script|style)[\s\S]*?</\1>"""), " ")
            .replace(Regex("""<[^>]+>"""), " ")
        return Regex("""\s+""").replace(txt, " ").trim()
    }

    /** HTML 转纯文本（供 AI 总结使用） */
    fun htmlToText(html: String): String =
        Regex("""\s+""").replace(
            html.replace(Regex("""<(br|/p|/li|/h[1-6]|/div|/blockquote)[^>]*>"""), "\n")
                .replace(Regex("""<[^>]+>"""), " ")
                .replace("&nbsp;", " ").replace("&amp;", "&")
                .replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\""),
            " "
        ).trim()

    private fun doc(html: String): Document = Jsoup.parse(html, Endpoints.BASE)

    /**
     * 提取正文中的楼层引用：优先取 ?floor=N 链接（源站渲染为 "#<a href=..?floor=N>N</a>"，
     * htmlToText 后 "#" 与数字之间隔着空格，纯文本正则 #N 不可靠），兜底纯文本 #N。
     */
    fun floorRefs(html: String): List<Int> {
        val linked = Regex("""[?&]floor=(\d+)""").findAll(html)
            .mapNotNull { it.groupValues[1].toIntOrNull() }.toList()
        if (linked.isNotEmpty()) return linked
        return Regex("""#(\d+)""").findAll(htmlToText(html))
            .mapNotNull { it.groupValues[1].toIntOrNull() }.toList()
    }

    private fun idFrom(href: String?): Long =
        Regex("""/(\d+)""").find(href ?: "")?.groupValues?.get(1)?.toLongOrNull() ?: 0L

    fun absUrl(u: String): String = when {
        u.startsWith("//") -> "https:" + u
        u.startsWith("http") -> u
        else -> Endpoints.abs(u)
    }

    /** 兼容源站不同头像标签：优先 .avatar-img，其次带 avatar 的图片，最后任意 img */
    private fun avatarOf(el: Element): String =
        el.selectFirst("img.avatar-img")?.attr("src")
            ?: el.selectFirst("img[src*='avatar']")?.attr("src")
            ?: el.selectFirst("img")?.attr("src") ?: ""

    /** 元素内头像是否带源站在线标记：
     *  online-users-dot 是子元素（<span class="online-users-dot">），
     *  online-users-host 是直接加在头像链接 class 上（class="avatar-profile-link online-users-host"） */
    private fun onlineOf(el: Element): Boolean =
        el.selectFirst(".online-users-dot") != null ||
            el.selectFirst(".online-users-host") != null ||
            el.selectFirst("a.online-users-host") != null ||
            el.classNames().contains("online-users-host")

    /**
     * 源站在线用户 ID 集合。v8.6.5+ 源站不再把在线标记写进静态 HTML，
     * 而是通过 <span class="online-users-boot" data-online-users-ids="id,id,..."> 下发在线用户，
     * 由前端 JS 动态给头像加绿点。应用解析原始 HTML 需要自己读这个集合来判定某用户是否在线。
     */
    private fun onlineIdsOf(d: Document): Set<Long> =
        d.selectFirst("span.online-users-boot[data-online-users-ids]")?.attr("data-online-users-ids")
            ?.split(",")
            ?.mapNotNull { it.trim().toLongOrNull() }
            ?.toSet() ?: emptySet()

    /**
     * 解析元素（或其子节点）内首个源站称号徽章 a.gacha-title-badge：
     * icon（emoji）、name（称号名）、rarity（稀有度，取自类名 gacha-title-<x>，兜底文本）。
     * 无称号徽章返回 null。
     */
    private fun gachaTitleOf(el: Element): TitleBadge? {
        val badge = el.selectFirst("a.gacha-title-badge") ?: return null
        val rare = Regex("""gacha-title-(n|r|sr|ssr|ur|ur\+)""")
            .find(badge.className())?.groupValues?.get(1)?.uppercase()
        return TitleBadge(
            icon = badge.selectFirst(".gacha-title-icon")?.text()?.trim() ?: "",
            name = badge.selectFirst(".gacha-title-name")?.text()?.trim() ?: "",
            rarity = rare ?: badge.selectFirst(".gacha-title-rarity")?.text()?.trim() ?: "",
        ).takeIf { it.name.isNotBlank() || it.icon.isNotBlank() }
    }

    /** 收集表单隐藏字段（兑换/参与时随 _csrf 一起提交） */
    private fun hiddenFields(form: Element?): Map<String, String> =
        form?.select("input[type=hidden]")?.mapNotNull { i ->
            val n = i.attr("name")
            if (n.isNotBlank() && n != "_csrf") n to i.attr("value") else null
        }?.toMap() ?: emptyMap()

    // ---------------- 帖子卡片列表 ----------------

    fun parseTopicList(html: String): Pair<List<TopicCard>, Pair<Int, Int>> {
        val d = doc(html)
        val onlineIds = onlineIdsOf(d)
        val items = d.select("ul.post-list > li.post-item").mapNotNull { li ->
            val titleA = li.selectFirst("a.post-title") ?: return@mapNotNull null
            val topicId = idFrom(titleA.attr("href"))
            if (topicId == 0L) return@mapNotNull null
            val meta = li.selectFirst(".post-meta")
            val userLink = meta?.selectFirst("a[href^=/user/]")
            val forumLink = li.selectFirst("a.post-forum-badge") ?: meta?.selectFirst("a[href^=/forum/]")
            // 无 <a> 且非时间的 span：[回复数, 最后回复人]
            val plainSpans = meta?.select("span")
                ?.filter { !it.hasAttr("data-performance-time") && it.select("a").isEmpty() }
                ?.map { it.text().trim() }
                ?.filter { it.isNotBlank() } ?: emptyList()
            val replies = plainSpans.firstOrNull { it.all { c -> c.isDigit() } }?.toIntOrNull() ?: 0
            val lastReplier = plainSpans.lastOrNull { !it.all { c -> c.isDigit() } } ?: ""
            val timeSpan = meta?.selectFirst("span[data-performance-time]")
            val timeText = timeSpan?.attr("data-performance-time")?.toLongOrNull()
                ?.let { TimeFmt.rel(it) } ?: (timeSpan?.text() ?: "")
            val authorId = idFrom(userLink?.attr("href"))
            TopicCard(
                topicId = topicId,
                title = titleA.text(),
                authorId = authorId,
                authorName = userLink?.text() ?: "",
                forumId = idFrom(forumLink?.attr("href")),
                forumName = forumLink?.text() ?: "",
                replies = replies,
                lastReplier = lastReplier,
                timeText = timeText,
                pinned = li.classNames().contains("topic-pinned") || li.selectFirst(".topic-badge.pinned") != null,
                hot = li.selectFirst(".topic-stamp-hot") != null,
                featured = li.selectFirst(".topic-management-featured-badge") != null,
                unread = li.classNames().any { it.contains("unread", ignoreCase = true) },
                titleColor = Regex("""(?i)color\s*:\s*(#[0-9a-fA-F]{3,8})""").find(titleA.attr("style"))?.groupValues?.get(1) ?: "",
                avatarUrl = absUrl(avatarOf(li)),
                pages = li.select(".topic-pages a").mapNotNull { it.text().toIntOrNull() },
                lotteryStatus = li.selectFirst(".community-lottery-title-status")?.text()?.trim() ?: "",
                cardStatus = li.selectFirst(".virtual-card-title-status")?.text()?.trim() ?: "",
                online = authorId in onlineIds || onlineOf(li),
                titleBadge = gachaTitleOf(meta ?: li),
            )
        }
        return items to parsePagination(d)
    }

    /** 返回 (当前页, 总页数) */
    fun parsePagination(d: Document): Pair<Int, Int> {
        val bar = d.selectFirst(".pagination-bar") ?: return 1 to 1
        val links = bar.select("a").mapNotNull {
            Regex("""[?&]p=(\d+)""").find(it.attr("href"))?.groupValues?.get(1)?.toIntOrNull()
        }
        val active = bar.selectFirst("li.active a")
            ?.attr("href")?.let { Regex("""[?&]p=(\d+)""").find(it)?.groupValues?.get(1)?.toIntOrNull() } ?: 1
        val maxPage = (links + active).maxOrNull() ?: 1
        return active to maxPage.coerceAtLeast(1)
    }

    // ---------------- 帖子详情 ----------------

    fun parseTopicPage(html: String, fallbackId: Long): TopicPageData {
        val d = doc(html)
        val onlineIds = onlineIdsOf(d)
        val csrf = d.selectFirst("input[name=_csrf]")?.attr("value") ?: ""
        val realTitle = d.selectFirst("h1.post-content-title")?.text()
            ?: d.selectFirst("h1.topic-title")?.text()
            ?: d.selectFirst(".main-panel h1")?.text()
            ?: d.selectFirst("h1")?.text()
            ?: run {
                // 从 toolbar 或 title 标签恢复
                val t = d.title()
                if (t.contains(" - ")) t.substringBefore(" - ").trim() else t.trim()
            }

        // 抽奖帖面板（源站 .community-lottery-card）：必须先于楼层解析——
        // 下方解析楼层时会把 .community-lottery-card / .virtual-card-box 从 DOM 移除
        // （避免混入正文），若放在其后将永远解析不到面板
        val lottery = d.selectFirst(".community-lottery-card, .community-lottery-topic-panel")?.let { p ->
            val joinForm = p.parents().firstOrNull { it.tagName() == "form" }
                ?: p.selectFirst("form[action]")
            val prizeLis = p.select(".community-lottery-prizes > li")
            val winnersLis = p.select(".community-lottery-winners li")
            LotteryPanel(
                status = p.selectFirst(".community-lottery-status-pill")?.text()?.trim()
                    ?: d.selectFirst(".community-lottery-title-status")?.text()?.trim() ?: "",
                note = p.selectFirst("header span")?.text()?.trim() ?: "",
                participants = p.selectFirst(".community-lottery-card-side em")?.text()?.trim() ?: "",
                prizes = if (prizeLis.isNotEmpty()) prizeLis.map { li ->
                    val name = li.selectFirst("strong")?.text()?.trim() ?: ""
                    val desc = li.selectFirst("span")?.text()?.trim() ?: ""
                    listOf(name, desc).filter { it.isNotBlank() }.joinToString(" · ")
                }.filter { it.isNotBlank() } else p.select(".community-lottery-readonly-prize, .community-lottery-prize-row")
                    .map { it.text().trim() }.filter { it.isNotBlank() },
                condition = p.selectFirst(".community-lottery-condition")?.text()?.trim() ?: "",
                result = p.selectFirst(".community-lottery-result")?.text()?.trim() ?: "",
                winners = if (winnersLis.isNotEmpty()) winnersLis.mapNotNull { li ->
                    val u = li.selectFirst("a")?.text()?.trim() ?: return@mapNotNull null
                    // 奖品名：span 文本去掉内嵌 <code>（本人条目形如 "13.14额度兑换码 · 兑换码：<code>…</code>"）
                    val prize = li.selectFirst("span")?.let { s ->
                        val clone = s.clone()
                        clone.select("code").remove()
                        clone.text().trim().replace(Regex("""\s*·?\s*兑换码[:：]?\s*$"""), "").trim()
                    } ?: ""
                    // 兑换码：code 文本形如 "奖品名<TAB>实际兑换码"；text() 会把 TAB 归一为空格，
                    // 用 wholeText() 保留原始分隔后取最后一个长 token
                    val codeRaw = li.selectFirst("code")?.wholeText()?.trim() ?: ""
                    val code = codeRaw.split(Regex("\\s+")).lastOrNull { it.length >= 8 } ?: codeRaw
                    LotteryWinner(user = u, prize = prize, code = code)
                } else emptyList(),
                rules = p.selectFirst(".community-lottery-rules, .community-lottery-rule-note")?.text()?.trim() ?: "",
                joinAction = joinForm?.attr("action") ?: "",
                joinFields = hiddenFields(joinForm),
                options = p.select("input[type=radio], .community-lottery-topic-option").mapNotNull { el ->
                    if (el.tagName() == "input") {
                        val v = el.attr("value")
                        if (v.isNotBlank()) v to (el.parent()?.text()?.trim() ?: v) else null
                    } else {
                        val v = el.attr("data-value")
                        if (v.isNotBlank()) v to el.text().trim() else null
                    }
                },
                joinedText = p.selectFirst(".community-lottery-pending-notice, .community-lottery-pending-reply")?.text()?.trim() ?: "",
            )
        }

        // 发卡帖虚拟卡片：同理先于楼层解析（.virtual-card-box 会在楼层解析时被移除）
        val card = d.selectFirst(".virtual-card-box.virtual-card-card")?.let { c ->
            val status = c.selectFirst(".virtual-card-status")?.text() ?: ""
            val price = buildString {
                append(c.selectFirst(".virtual-card-price strong")?.text() ?: "")
                val unit = c.selectFirst(".virtual-card-price span")?.text() ?: ""
                if (unit.isNotBlank()) append(" $unit")
            }
            val buyForm = c.parents().firstOrNull { it.tagName() == "form" }
                ?: c.selectFirst("form[action]")
            VirtualCard(
                kicker = c.selectFirst(".virtual-card-kicker")?.text() ?: "积分兑换",
                title = c.selectFirst(".virtual-card-head strong")?.text() ?: "",
                status = status,
                priceText = price,
                tip = c.selectFirst(".virtual-card-tip")?.text() ?: "",
                footText = c.select(".virtual-card-foot span").eachText().joinToString(" · "),
                buyAction = buyForm?.attr("action") ?: "",
                buyFields = hiddenFields(buyForm),
                inStock = Regex("""(\d+)""").find(status)?.groupValues?.get(1)?.toIntOrNull()?.let { it > 0 }
                    ?: status.contains("有货"),
                // 我的购买记录（登录且兑换过才有）：li > code=兑换码，small span=时间/积分
                orders = c.select(".virtual-card-orders ul li").mapNotNull { li ->
                    val code = li.selectFirst("code")?.text()?.trim() ?: return@mapNotNull null
                    val meta = li.select("small span").eachText()
                    CardOrder(
                        code = code,
                        time = meta.getOrNull(0) ?: "",
                        price = meta.getOrNull(1) ?: "",
                    )
                },
            )
        }

        val posts = d.select("ul.topic-post-list > li.post-entry, ul.post-list.topic-post-list > li.post-entry").map { li ->
            val floorNum = li.attr("data-floor").toIntOrNull() ?: 0
            val authorA = li.selectFirst("a.post-author")
            val groups = li.select(".post-user-group").map { it.text().trim() }.filter { it.isNotBlank() }
            val uidBadge = groups.firstOrNull { it.startsWith("UID") }
            val group = groups.lastOrNull { !it.startsWith("UID") } ?: ""
            val uidNum = uidBadge?.removePrefix("UID")?.trim()?.toLongOrNull() ?: 0L
            val meta = li.selectFirst(".post-meta")
            val timeText = meta?.selectFirst("span[data-performance-time]")?.attr("data-performance-time")?.toLongOrNull()
                ?.let { TimeFmt.rel(it) } ?: meta?.selectFirst("span")?.text() ?: ""
            // 源站 v8.6+ 点赞改版：like-coin-* → donate-reaction-*（按钮 data-liked/data-coined/计数 span + 表单 donate-reaction-form）
            val likeBtn = li.selectFirst("[data-donate-reaction]")
            val likeForm = li.selectFirst("form.donate-reaction-form")
            // 正文：把"最后编辑"信息从正文拆出，单独用分割线展示
            val contentEl = li.selectFirst(".post-content")
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
                        ".donate-area, [data-donate-area], .quick-reply-main-action, .virtual-card-box"
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
                        editUserId = idFrom(editorA.attr("href"))
                        editUserName = editorA.text().trim()
                    }
                    editInfo = editEl.text().trim()
                    editEl.remove()
                }
            }
            PostEntry(
                id = (li.attr("id").removePrefix("post-").toLongOrNull() ?: 0L),
                floor = floorNum,
                authorId = idFrom(authorA?.attr("href")),
                authorName = authorA?.text() ?: "",
                avatarUrl = absUrl(avatarOf(li)),
                userGroup = group,
                authorUid = uidNum,
                timeText = timeText,
                ipLocation = meta?.selectFirst(".ip-history-location-subtitle")?.text() ?: "",
                contentHtml = contentEl?.html() ?: "",
                editInfo = editInfo,
                editUserId = editUserId,
                editUserName = editUserName,
                // 整楼 HTML 提取引用（?floor=N 链接 / 纯文本 #N）。
                // 必须过滤本楼楼层号：源站每楼自带 <a class="post-floor" href="?floor=N">#N</a> 徽标，
                // 不过滤会导致每条评论都出现"查看对话"按钮且指向自己
                referencedFloors = floorRefs(li.html()).filter { it > 0 && it != floorNum }.distinct(),
                likeCount = likeBtn?.selectFirst(".donate-reaction-count")?.text()?.filter { it.isDigit() }?.toIntOrNull() ?: 0,
                liked = likeBtn?.attr("data-liked") == "1",
                coined = likeBtn?.attr("data-coined") == "1",
                likeCoinType = "", // 举报类型由调用方按楼层默认（主楼 topic / 回复 reply）
                // 有表单或按钮即可互动：源站改版后表单类名可能变化/移出楼层，
                // 但 [data-donate-reaction] 按钮仍在楼内；仅以按钮判断会漏，
                // 两者任一存在即显示点赞投币（未登录时源站不渲染按钮与表单）
                canLike = likeForm != null || likeBtn != null,
                isOp = li.selectFirst(".quick-reply-main-action") != null || li.selectFirst("form.topic-favorites-action") != null,
                online = idFrom(authorA?.attr("href")) in onlineIds || onlineOf(li),
                titleBadge = gachaTitleOf(li),
            )
        }
        val favForm = d.selectFirst("form.topic-favorites-action")
        val (page, totalPages) = parsePagination(d)

        // 未登录时评论区隐藏
        val loginVisible = d.selectFirst(".replies-login-visible-count")?.text()?.toIntOrNull() ?: -1
        val quickAuthor = d.selectFirst("[data-quick-reply-topic-author]")?.attr("data-quick-reply-topic-author") ?: ""

        // 查看次数 / 回复数：.post-content-stats 第一个 span 是浏览数（眼睛），第二个是回复数
        val statsNums = d.selectFirst(".post-content-stats")?.select("span")
            ?.map { it.text().trim().filter { c -> c.isDigit() } } ?: emptyList()
        val viewsText = statsNums.getOrNull(0) ?: ""
        val repliesText = statsNums.getOrNull(1) ?: ""

        // 回复表单人机验证（抽奖帖等需要验证后才可回复）
        val replyCaptcha = d.selectFirst("[data-native-captcha]")?.let { w ->
            val q = w.selectFirst(".native-captcha-question")?.text()?.trim() ?: ""
            val token = w.selectFirst("input[name=native_captcha_token]")?.attr("value") ?: ""
            if (q.isNotBlank() && token.isNotBlank()) NativeCaptcha(
                question = q,
                token = token,
                powPrefix = w.attr("data-pow-prefix"),
                powZeros = w.attr("data-pow-zeroes").toIntOrNull() ?: 3,
            ) else null
        }

        // 正文标签：标题旁的状态徽章（如抽奖帖的 已开奖 / 抽奖中）
        val titleTag = d.selectFirst(".post-topic-title > span")?.text()?.trim()
            ?: d.selectFirst(".community-lottery-title-status")?.text()?.trim() ?: ""

        return TopicPageData(
            topicId = fallbackId,
            title = realTitle,
            titleTag = titleTag,
            posts = posts,
            page = page,
            totalPages = totalPages,
            csrf = csrf,
            canFavorite = favForm != null,
            favAction = favForm?.attr("action") ?: "/topic_favorite",
            donateUrl = d.selectFirst("a[data-donate-btn]")?.attr("href"),
            virtualCard = card,
            loginVisibleReplies = loginVisible,
            quickReplyAuthor = quickAuthor,
            viewsText = viewsText,
            repliesText = repliesText,
            lottery = lottery,
            replyCaptcha = replyCaptcha,
        )
    }

    // ---------------- 板块 ----------------

    fun parseForumList(html: String): List<ForumInfo> {
        val d = doc(html)
        val out = mutableListOf<ForumInfo>()
        d.select("a[href^=/forum/]").forEach { a ->
            val id = idFrom(a.attr("href"))
            if (id > 0 && a.text().isNotBlank()) {
                val name = a.selectFirst(
                    ".forum-enhancements-name, .forum-enhancements-sidebar-name, .forum-name"
                )?.text()?.trim() ?: a.text().trim()
                val count = a.selectFirst(
                    ".forum-enhancements-count, .forum-enhancements-sidebar-count"
                )?.text()?.trim() ?: ""
                val existing = out.find { it.id == id }
                if (existing == null) {
                    out.add(ForumInfo(id, name, a.attr("title") ?: "", count))
                }
            }
        }
        if (out.isEmpty()) {
            // 论坛列表页失效时，从首页顶部/移动菜单兜底
            d.select(".forum-more-panel a[href^=/forum/], .mobile-menu-link[href^=/forum/]").forEach { a ->
                val id = idFrom(a.attr("href"))
                if (id > 0 && a.text().isNotBlank() && out.none { it.id == id }) {
                    out.add(ForumInfo(id, a.text().trim(), a.attr("title") ?: ""))
                }
            }
        }
        return out.distinctBy { it.id }
    }

    fun parseForumHeader(html: String): Triple<String, String, Int> {
        val d = doc(html)
        val h = d.selectFirst(".main-panel h1, .main-panel h2, h1")?.text() ?: ""
        val desc = d.selectFirst(".forum-description, .main-panel p")?.text() ?: ""
        val topics = Regex("""(\d+)\s*个?主题""").find(d.selectFirst(".main-panel")?.text() ?: "")
            ?.groupValues?.get(1)?.toIntOrNull() ?: 0
        return Triple(h, desc, topics)
    }

    // ---------------- 用户 ----------------

    fun parseUserProfile(html: String): UserProfile {
        val d = doc(html)
        val onlineIds = onlineIdsOf(d)
        val name = d.selectFirst("a.user-name")?.text()
            ?: d.selectFirst(".user-name")?.text()
            ?: d.selectFirst(".profile-toolbar")?.previousElementSibling()?.text()
            ?: ""
        val uidHref = d.selectFirst("a.user-avatar-big[href^=/user/]")?.attr("href")
            ?: d.selectFirst("a[href^=/user/]")?.attr("href")
        val avatar = d.selectFirst("a.user-avatar-big img, .user-header img")?.attr("src")
            ?: d.selectFirst("img.avatar-img, img[src*='avatar']")?.attr("src") ?: ""
        val rank = d.selectFirst(".user-rank")?.text() ?: ""
        // rank 形如 "饼友 · 积分 1324"：拆出用户组与积分
        val group = rank.substringBefore("·").trim()
        val points = Regex("""积分\s*([\d.,kw万K W]+)""", RegexOption.IGNORE_CASE).find(rank)?.groupValues?.get(1) ?: ""
        // 简介保留源站格式：直接取原始 HTML（含 markdown 渲染后的 <strong>/<a>/<br>/<pre> 等），
        // 交由 HtmlContent 渲染，保留加粗、链接、换行与代码块等排版
        val bio = d.selectFirst(".sidebar-bio")?.html()?.trim() ?: ""
        val stats = d.select(".sidebar-card .stat-row, .sidebar .card").mapNotNull { c ->
            val t = c.selectFirst(".quick-title")?.text() ?: return@mapNotNull null
            val v = c.selectFirst(".quick-value, .sidebar-value")?.text() ?: ""
            t to v
        }
        return UserProfile(
            userId = idFrom(uidHref),
            username = name,
            avatarUrl = absUrl(avatar),
            rankText = rank,
            userGroup = group,
            points = points,
            bio = bio,
            stats = stats,
            // 在线：源站 data-online-users-ids 在线集合命中，兜底静态标记
            online = idFrom(uidHref) in onlineIds ||
                d.selectFirst("a.user-avatar-big")?.let { onlineOf(it) } == true,
            titleBadge = gachaTitleOf(d),
        )
    }

    fun parseMySidebar(html: String): LoginState {
        val d = doc(html)
        val card = d.selectFirst(".user-card")
        val nameEl = card?.selectFirst("a.user-name")
        val rank = card?.selectFirst(".user-rank")?.text() ?: ""
        val points = Regex("""积分\s*([\d.kw万]+)""").find(rank)?.groupValues?.get(1) ?: ""
        return LoginState(
            loggedIn = nameEl != null,
            userId = idFrom(nameEl?.attr("href")),
            username = nameEl?.text() ?: "",
            points = points,
            avatarUrl = absUrl(avatarOf(card ?: d)),
            userGroup = rank.substringBefore("·").trim(),
        )
    }

    fun parseNotifications(html: String): List<NotificationItem> {
        val d = doc(html)
        return d.select("li.notification-item").map { li ->
            val typeText = li.selectFirst(".notification-kind")?.text() ?: "通知"
            val content = li.selectFirst(".notification-content")?.text()?.replace(Regex("""\s+"""), " ")?.trim() ?: ""
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
            val partnerAvatar = li.selectFirst(".post-avatar img[src]")?.attr("src") ?: ""
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
                partnerAvatar = if (isPm) partnerAvatar else "",
            )
        }
    }

    /**
     * 首页顶栏未读通知数（.nav-mine 内的 .notify-badge）。
     * 无未读时源站不渲染该元素；关键词过滤计数（.home-keyword-filter-count）不算在内。
     */
    fun parseNotifyBadgeCount(html: String): Int =
        doc(html).selectFirst(".nav-mine .notify-badge:not(.home-keyword-filter-count)")
            ?.text()?.trim()?.toIntOrNull() ?: 0

    /** 单独解析页面里的人机验证组件（回复弹窗「换一题」用，无需解析整个帖子） */
    fun parseNativeCaptcha(html: String): NativeCaptcha? {
        val w = doc(html).selectFirst("[data-native-captcha]") ?: return null
        val q = w.selectFirst(".native-captcha-question")?.text()?.trim() ?: ""
        val token = w.selectFirst("input[name=native_captcha_token]")?.attr("value") ?: ""
        return if (q.isNotBlank() && token.isNotBlank()) NativeCaptcha(
            question = q,
            token = token,
            powPrefix = w.attr("data-pow-prefix"),
            powZeros = w.attr("data-pow-zeroes").toIntOrNull() ?: 3,
        ) else null
    }

    /** 源站相对时间 → 时间戳：支持「刚刚/N秒/N分钟/N小时/N天前」与常见日期格式；解析失败返回 0 */
    fun parseRelativeTime(text: String): Long {
        val t = text.trim()
        if (t.isBlank()) return 0L
        val now = System.currentTimeMillis()
        Regex("""(\d+)\s*秒前""").find(t)?.let { return now - it.groupValues[1].toLong() * 1000L }
        Regex("""(\d+)\s*分钟前""").find(t)?.let { return now - it.groupValues[1].toLong() * 60_000L }
        Regex("""(\d+)\s*小时前""").find(t)?.let { return now - it.groupValues[1].toLong() * 3_600_000L }
        Regex("""(\d+)\s*天前""").find(t)?.let { return now - it.groupValues[1].toLong() * 86_400_000L }
        if (t.contains("刚刚") || t.contains("刚才")) return now
        // 日期格式：yyyy-MM-dd( HH:mm) / MM-dd( HH:mm，跨年自动回退一年)
        runCatching {
            java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.CHINA).parse(t)?.time
        }.getOrNull()?.let { return it }
        runCatching {
            java.text.SimpleDateFormat("MM-dd HH:mm", java.util.Locale.CHINA).parse(t)
        }.getOrNull()?.let { p ->
            val cal = java.util.Calendar.getInstance()
            cal.time = p
            cal.set(java.util.Calendar.YEAR, java.util.Calendar.getInstance().get(java.util.Calendar.YEAR))
            if (cal.timeInMillis > now) cal.add(java.util.Calendar.YEAR, -1)
            return cal.timeInMillis
        }
        runCatching {
            java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.CHINA).parse(t.substringBefore(" "))?.time
        }.getOrNull()?.let { return it }
        runCatching {
            java.text.SimpleDateFormat("MM-dd", java.util.Locale.CHINA).parse(t.substringBefore(" "))
        }.getOrNull()?.let { p ->
            val cal = java.util.Calendar.getInstance()
            cal.time = p
            cal.set(java.util.Calendar.YEAR, java.util.Calendar.getInstance().get(java.util.Calendar.YEAR))
            if (cal.timeInMillis > now) cal.add(java.util.Calendar.YEAR, -1)
            return cal.timeInMillis
        }
        return 0L
    }

    // ---------------- 榜单 ----------------

    fun parseLeaderboard(html: String): List<LeaderRow> {
        val d = doc(html)
        val onlineIds = onlineIdsOf(d)
        val out = mutableListOf<LeaderRow>()
        // 前三名领奖台
        d.select("a.leaderboard-podium-col").forEach { a ->
            out.add(
                LeaderRow(
                    rank = a.selectFirst(".leaderboard-podium-step span")?.text()?.toIntOrNull() ?: 0,
                    userId = idFrom(a.attr("href")),
                    username = a.selectFirst(".leaderboard-podium-name")?.text() ?: "",
                    userGroup = a.selectFirst(".leaderboard-podium-group")?.text() ?: "",
                    valueText = a.selectFirst(".leaderboard-podium-count")?.text() ?: "",
                    avatarUrl = absUrl(a.selectFirst("img")?.attr("src") ?: ""),
                    online = idFrom(a.attr("href")) in onlineIds || onlineOf(a),
                    titleBadge = gachaTitleOf(a),
                )
            )
        }
        // 列表部分
        d.select("a.leaderboard-item").forEach { a ->
            out.add(
                LeaderRow(
                    rank = a.selectFirst(".leaderboard-rank-badge")?.text()?.toIntOrNull() ?: 0,
                    userId = idFrom(a.attr("href")),
                    username = a.selectFirst(".leaderboard-name")?.text() ?: "",
                    userGroup = a.selectFirst(".leaderboard-group")?.text() ?: "",
                    valueText = a.selectFirst(".leaderboard-count")?.text() ?: "",
                    avatarUrl = absUrl(a.selectFirst("img")?.attr("src") ?: ""),
                    online = idFrom(a.attr("href")) in onlineIds || onlineOf(a),
                    titleBadge = gachaTitleOf(a),
                )
            )
        }
        return out.sortedBy { if (it.rank == 0) 999 else it.rank }
    }

    // ---------------- 回帖列表（我的回帖 tab） ----------------

    fun parseReplyList(html: String): List<ReplyItem> {
        val d = doc(html)
        return d.select("li.post-item").mapNotNull { li ->
            val a = li.selectFirst("a.post-title") ?: li.selectFirst("a[href^=/topic/]") ?: return@mapNotNull null
            val href = a.attr("href")
            val tid = idFrom(href)
            if (tid == 0L) return@mapNotNull null
            val replyId = Regex("""[?&]replyid=(\d+)""").find(href)?.groupValues?.get(1)?.toLongOrNull() ?: 0L
            ReplyItem(
                topicId = tid,
                title = a.text(),
                excerpt = li.selectFirst(".profile-reply-excerpt, .post-content, .reply-excerpt")?.text() ?: "",
                timeText = li.selectFirst("span[data-performance-time]")?.attr("data-performance-time")?.toLongOrNull()
                    ?.let { TimeFmt.rel(it) } ?: "",
                replyId = replyId,
                forumName = li.selectFirst("a.post-forum-badge")?.text() ?: "",
            )
        }
    }

    // ---------------- 首页侧板（源站 aside.sidebar） ----------------

    fun parseHomeSidebar(html: String): HomeSidebar {
        val d = doc(html)
        val aside = d.selectFirst("aside.sidebar") ?: d

        // 版块列表：与底栏「板块导航」使用同一解析（全文档抓取 a[href^=/forum/]），保证两处板块一致
        val forums = parseForumList(html).map { ForumCount(id = it.id, name = it.name, count = it.topicCount) }

        // 每日热帖
        val hot = aside.select(".daily-hot-topics-list a[href^=/topic/]").mapNotNull { a ->
            val tid = idFrom(a.attr("href"))
            if (tid <= 0) null else HotTopic(
                topicId = tid,
                title = a.selectFirst(".daily-hot-topics-title")?.text() ?: a.text().trim(),
                meta = a.selectFirst(".daily-hot-topics-count")?.text()?.replace(Regex("""\s+"""), " ")?.trim() ?: "",
            )
        }.distinctBy { it.topicId }

        // 最近浏览版块
        val recent = aside.select(".quick-forum-links a[href^=/forum/]").mapNotNull { a ->
            val id = idFrom(a.attr("href"))
            if (id <= 0) null else ForumInfo(id, a.selectFirst(".quick-link-text")?.text() ?: a.text().trim())
        }.distinctBy { it.id }

        // 站点统计
        val stats = aside.selectFirst(".stats-sub")?.text()?.replace(Regex("""\s+"""), " ")?.trim() ?: ""

        // 最新用户
        val newUsers = aside.select("a.nu-item").mapNotNull { a ->
            val uid = idFrom(a.attr("href"))
            if (uid <= 0) null else NewUser(
                userId = uid,
                username = a.selectFirst(".nu-name")?.text() ?: a.text().trim(),
                avatarUrl = absUrl(avatarOf(a)),
            )
        }.distinctBy { it.userId }

        // 当前在线（源站 v8.6.5+ 不再渲染 .online-users-card，改为 <span.online-users-boot data-online-users-ids>
        // 由 JS 动态拼卡）。这里读取在线 ID 集：人数 = ID 数；在线用户项交叉匹配本页帖子作者
        // （拿名字/头像），保证侧栏能把「当前在线的作者」展示出来，无数据时为空卡片不渲染。
        val onlineIds = onlineIdsOf(d)
        val onlineUsers = if (onlineIds.isNotEmpty()) {
            val fromPage = d.select("ul.post-list > li.post-item").mapNotNull { li ->
                val userLink = li.selectFirst(".post-meta a[href^=/user/]") ?: return@mapNotNull null
                val uid = idFrom(userLink.attr("href"))
                if (uid > 0 && uid in onlineIds) OnlineUser(
                    userId = uid,
                    username = userLink.text().trim(),
                    avatarUrl = absUrl(avatarOf(li)),
                ) else null
            }.distinctBy { it.userId }
            OnlineUsers(
                count = "${onlineIds.size} 人",
                items = fromPage,
                moreText = "",
            )
        } else OnlineUsers()

        return HomeSidebar(
            forums = forums,
            recentForums = recent,
            hotTopics = hot,
            statsText = stats,
            newUsers = newUsers,
            onlineUsers = onlineUsers,
        )
    }

    // ---------------- 积分记录 ----------------

    fun parsePointRows(html: String): List<PointRow> {
        val d = doc(html)
        return d.select("li.points-rewards-detail").map { li ->
            val raw = li.selectFirst(".points-rewards-change-value b")?.text() ?: "0"
            val positive = li.selectFirst(".points-rewards-change-value")?.classNames()?.contains("positive") == true
                || raw.trimStart().startsWith("+")
            PointRow(
                timeText = li.selectFirst("time")?.attr("datetime")?.take(16)?.replace("T", " ")
                    ?: li.selectFirst(".points-rewards-time")?.text() ?: "",
                reason = li.selectFirst(".points-rewards-reason")?.text() ?: "",
                change = raw.trim().trimStart('+', '-'),
                positive = positive,
            )
        }
    }

    // ---------------- 打赏 ----------------

    data class DonateInfo(
        val csrf: String,
        val topicId: String,
        val requestKey: String,
        val quickMessages: List<String>,
    )

    fun parseDonate(html: String): DonateInfo {
        val d = doc(html)
        val form = d.selectFirst("form[action=/donate]") ?: d.selectFirst("main form")
        return DonateInfo(
            csrf = form?.selectFirst("input[name=_csrf]")?.attr("value") ?: "",
            topicId = form?.selectFirst("input[name=topic_id]")?.attr("value") ?: "",
            requestKey = form?.selectFirst("input[name=request_key]")?.attr("value") ?: "",
            quickMessages = d.select("button[data-quick-message]").map { it.text() },
        )
    }

    // ---------------- 举报 ----------------

    data class ReportInfo(
        val csrf: String,
        val targetType: String,
        val targetId: String,
        val reasons: List<Pair<String, String>>, // value to label
    )

    fun parseReport(html: String): ReportInfo {
        val d = doc(html)
        val form = d.selectFirst("main form")
        val reasons = d.select("input[name=reason_type]").mapNotNull { r ->
            val v = r.attr("value")
            val label = r.parent()?.text()?.trim() ?: v
            if (v.isBlank()) null else v to label
        }
        return ReportInfo(
            csrf = form?.selectFirst("input[name=_csrf]")?.attr("value") ?: "",
            targetType = form?.selectFirst("input[name=target_type]")?.attr("value")
                ?: Regex("""type=(\w+)""").find(d.location() ?: "")?.groupValues?.get(1) ?: "reply",
            targetId = form?.selectFirst("input[name=target_id]")?.attr("value")
                ?: Regex("""/content_report/(\d+)""").find(html)?.groupValues?.get(1) ?: "",
            reasons = reasons,
        )
    }

    // ---------------- 个人设置 ----------------

    data class ProfileField(
        val name: String,
        val type: String,        // text/password/email/number/file/hidden/checkbox/radio/select/textarea
        val label: String,
        val value: String,
        val required: Boolean = false,
        val options: List<Pair<String, String>> = emptyList(), // select 选项 value to label
    )

    data class ProfileFormData(
        val title: String,          // 表单标题（legend/标题/按钮文案）
        val action: String,
        val method: String,         // POST / GET
        val hasFile: Boolean,
        val fileFieldName: String,  // 文件字段名（头像上传）
        val fields: List<ProfileField>,  // 含 hidden 字段（提交时自动带上）
        val submitText: String,
    ) {
        val hidden: Map<String, String> get() = fields.filter { it.type == "hidden" }.associate { it.name to it.value }
        val passwords: List<ProfileField> get() = fields.filter { it.type == "password" }
        val visibles: List<ProfileField> get() = fields.filter { it.type != "hidden" && it.type != "password" && it.type != "file" }
    }

    /** 源站预置头像选择器（.avatar-picker）：风格/编号/基址全部以服务端 HTML 为准，不做公式推导 */
    data class AvatarPickerData(
        val styleField: String,                    // 风格字段名（avatar_style）
        val seedField: String,                     // 编号字段名（avatar_seed）
        val styles: List<Pair<String, String>>,    // 风格选项 value → 标签
        val currentStyle: String,
        val currentSeed: String,
        val base: String,                          // 头像基址（data-avatar-base，如 /app/avatars/）
    )

    data class ProfilePageData(
        val avatarUrl: String = "",
        val info: List<Pair<String, String>> = emptyList(),  // 用户名/UID/邮箱/注册时间/积分等
        val forms: List<ProfileFormData> = emptyList(),
        val avatarPicker: AvatarPickerData? = null,
    )

    /** label.grid > span / aria-label / placeholder / 字段名映射 */
    private fun labelOf(el: Element): String {
        el.parent()?.takeIf { it.tagName() == "label" }?.selectFirst("span")?.text()?.trim()
            ?.takeIf { it.isNotBlank() }?.let { return it }
        el.attr("aria-label").trim().takeIf { it.isNotBlank() }?.let { return it }
        el.attr("placeholder").trim().takeIf { it.isNotBlank() }?.let { return it }
        return FIELD_LABELS[el.attr("name")] ?: el.attr("name")
    }

    private val FIELD_LABELS = mapOf(
        "username" to "用户名", "user_name" to "用户名",
        "email" to "邮箱", "new_email" to "新邮箱",
        "password" to "新密码", "password2" to "确认新密码",
        "old_password" to "当前密码", "current_password" to "当前密码",
        "bio" to "个人简介", "signature" to "个人简介",
        "avatar" to "头像", "avatar_file" to "头像",
    )

    /**
     * 解析 /profile 个人设置页（通用表单驱动）：
     * 提取展示信息（用户名/UID/邮箱/注册时间/积分）与全部可提交表单，
     * 表单字段动态渲染，自动适配源站实际功能。
     */
    fun parseProfilePage(html: String): ProfilePageData {
        val d = doc(html)
        val main = d.selectFirst("main") ?: d

        // 头像
        val avatar = main.selectFirst(".user-avatar-big img, .profile-avatar img")?.attr("src")
            ?: main.selectFirst("img.avatar-img, img[src*='avatar']")?.attr("src") ?: ""

        // 预置头像选择器：风格下拉 + 编号输入 + 头像基址（与源站 index.js 的 .avatar-picker 结构对应）
        val pickerEl = main.selectFirst(".avatar-picker")
        val styleSel = pickerEl?.selectFirst("select[name=avatar_style]")
        val seedInput = pickerEl?.selectFirst("input[name=avatar_seed]")
        val avatarPicker = if (pickerEl != null && styleSel != null && seedInput != null) {
            AvatarPickerData(
                styleField = styleSel.attr("name").ifBlank { "avatar_style" },
                seedField = seedInput.attr("name").ifBlank { "avatar_seed" },
                styles = styleSel.select("option")
                    .map { it.attr("value") to it.text().trim() }
                    .filter { it.first.isNotBlank() },
                currentStyle = styleSel.selectFirst("option[selected]")?.attr("value")?.ifBlank { null }
                    ?: styleSel.selectFirst("option")?.attr("value") ?: "",
                currentSeed = seedInput.attr("value").trim(),
                base = pickerEl.attr("data-avatar-base").trim(),
            )
        } else null

        // 信息键值对：label.grid（span + 控件值）→ dl dt/dd → 正文正则兜底
        val info = linkedMapOf<String, String>()
        fun valueOf(el: Element): String = when (el.tagName()) {
            "select" -> el.selectFirst("option[selected]")?.text() ?: ""
            "textarea" -> el.text().trim()
            "input" -> if (el.attr("type").equals("checkbox", true))
                (if (el.hasAttr("checked")) "开" else "关") else el.attr("value").trim()
            else -> el.text().trim()
        }
        main.select("label.grid").forEach { l ->
            val k = l.selectFirst("span")?.text()?.trim() ?: return@forEach
            val v = l.selectFirst("input, select, textarea")?.let { valueOf(it) } ?: ""
            if (k.isNotBlank() && v.isNotBlank()) info[k] = v
        }
        main.select("dl").forEach { dl ->
            val dts = dl.select("dt"); val dds = dl.select("dd")
            dts.forEachIndexed { i, dt ->
                val v = dds.getOrNull(i)?.text()?.trim() ?: return@forEachIndexed
                val k = dt.text().trim()
                if (k.isNotBlank() && v.isNotBlank()) info[k] = v
            }
        }
        val text = main.text().replace(Regex("""\s+"""), " ")
        if (info.keys.none { it.contains("UID") })
            Regex("""UID\s*[:：]?\s*(\d+)""").find(text)?.let { info["UID"] = it.groupValues[1] }
        if (info.keys.none { it.contains("邮箱") })
            Regex("""[\w.+-]+@[\w-]+\.[\w.]+""").find(text)?.let { info["邮箱"] = it.value }
        if (info.keys.none { it.contains("积分") })
            Regex("""积分\s*[:：]?\s*([\d.,]+[万kK]?)""").find(text)?.let { info["积分"] = it.groupValues[1] }
        if (info.keys.none { it.contains("注册") })
            Regex("""(?:注册时间|注册日期|加入时间|加入于|注册于)\s*[:：]?\s*(\d{4}[-/年]\d{1,2}[-/月]\d{1,2}日?)""")
                .find(text)?.let { info["注册时间"] = it.groupValues[1] }

        // 表单（排除搜索表单与无提交按钮的表单）
        val forms = main.select("form").filter { f ->
            !f.classNames().contains("search-form") &&
                f.selectFirst("input[name=q]") == null &&
                f.selectFirst("button[type=submit], input[type=submit]") != null
        }.map { f ->
            val fields = mutableListOf<ProfileField>()
            f.select("input, select, textarea").forEach { el ->
                val name = el.attr("name")
                if (name.isBlank() || name == "_csrf") return@forEach
                when (el.tagName()) {
                    "textarea" -> fields += ProfileField(name, "textarea", labelOf(el), el.text().trim(), el.hasAttr("required"))
                    "select" -> fields += ProfileField(
                        name, "select", labelOf(el),
                        el.selectFirst("option[selected]")?.attr("value")
                            ?: el.selectFirst("option")?.attr("value") ?: "",
                        el.hasAttr("required"),
                        el.select("option").map { it.attr("value") to it.text().trim() },
                    )
                    else -> {
                        val type = el.attr("type").ifBlank { "text" }.lowercase()
                        if (type == "submit" || type == "button" || type == "reset") return@forEach
                        if (type == "checkbox")
                            fields += ProfileField(name, "checkbox", labelOf(el), if (el.hasAttr("checked")) "1" else "0", el.hasAttr("required"))
                        else if (type == "radio") {
                            if (el.hasAttr("checked"))
                                fields += ProfileField(name, "radio", labelOf(el), el.attr("value"), el.hasAttr("required"))
                        } else
                            fields += ProfileField(name, type, labelOf(el), el.attr("value").trim(), el.hasAttr("required"))
                    }
                }
            }
            val fileEl = f.selectFirst("input[type=file]")
            ProfileFormData(
                title = f.selectFirst("legend")?.text()?.trim()?.takeIf { it.isNotBlank() }
                    ?: f.parents().firstOrNull()?.selectFirst("> h2, > h3, > .quick-title")?.text()?.trim()?.takeIf { it.isNotBlank() }
                    ?: f.selectFirst("button[type=submit], input[type=submit]")?.text()?.trim()?.takeIf { it.isNotBlank() }
                    ?: "设置",
                action = f.attr("action").ifBlank { "/profile" },
                method = f.attr("method").ifBlank { "post" }.uppercase(),
                hasFile = fileEl != null && fileEl.attr("name").isNotBlank(),
                fileFieldName = fileEl?.attr("name") ?: "",
                fields = fields,
                submitText = f.selectFirst("button[type=submit], input[type=submit]")?.text()?.trim()?.takeIf { it.isNotBlank() } ?: "保存",
            )
        }
        return ProfilePageData(absUrl(avatar), info.toList(), forms, avatarPicker)
    }

    /** 签到页结构化信息 */
    data class CheckinInfo(
        val streak: Int = 0,           // 连续签到天数（0 = 未知）
        val total: Int = 0,            // 累计签到天数（0 = 未知）
        val canCheckin: Boolean,       // 今日可签到（存在签到表单）
        val checkedToday: Boolean,     // 今日已签到
        val message: String = "",      // 页面核心提示文案
    )

    /** 签到页解析：连续/累计天数 + 是否可签到 */
    fun parseCheckin(html: String): CheckinInfo {
        val d = doc(html)
        if (d.selectFirst("form[action=/login], input[name=username], input[name=password]") != null) {
            return CheckinInfo(canCheckin = false, checkedToday = false)
        }
        val full = (d.selectFirst("main")?.text() ?: d.body()?.text() ?: "")
            .replace(Regex("""\s+"""), " ").trim()
        val canCheckin = d.selectFirst("form[action=/daily_checkin], form[action*=/daily_checkin]") != null
        val streak = listOf(
            Regex("""连续签到\s*(\d+)\s*天"""),
            Regex("""已连续签到\s*(\d+)\s*天"""),
            Regex("""连续签到\s*[:：]?\s*(\d+)"""),
            Regex("""连续天数\s*[:：]?\s*(\d+)"""),
            Regex("""连续\s*(\d+)\s*天"""),
            Regex("""(\d+)\s*连续天数"""),
            Regex("""(\d+)\s*连续签到"""),
            Regex("""(\d+)\s*连续\s*天"""),
        ).mapNotNull { it.find(full)?.groupValues?.get(1)?.toIntOrNull() }.firstOrNull() ?: 0
        val total = listOf(
            Regex("""累计签到\s*[:：]?\s*(\d+)"""),
            Regex("""累计天数\s*[:：]?\s*(\d+)"""),
            Regex("""(?:累计|累积|总计|总共)签到\s*(\d+)\s*天"""),
            Regex("""累计\s*(\d+)\s*天"""),
            Regex("""(\d+)\s*累计签到"""),
            Regex("""(\d+)\s*累计天数"""),
            Regex("""(\d+)\s*累计\s*天"""),
        ).mapNotNull { it.find(full)?.groupValues?.get(1)?.toIntOrNull() }.firstOrNull() ?: 0
        val doneWords = listOf(
            "今日已签到", "今天已签到", "今日已经签到", "已经签到",
            "签到成功", "已完成", "明日再来", "明天再来",
        )
        val checkedToday = doneWords.any { full.contains(it) } || !canCheckin && full.contains("签到")
        val core = d.selectFirst(
            ".daily-checkin-message, .daily-checkin-result, .home-signin-tip, .main-panel .notice, .main-panel .alert"
        )?.text()?.replace(Regex("""\s+"""), " ")?.trim()
            ?: Regex("""(签到成功[^。！!]{0,40}|已连续签到\s*\d+\s*天[^。！!]{0,40}|今日已签到)""")
                .find(full)?.value ?: ""
        return CheckinInfo(
            streak = streak,
            total = total,
            canCheckin = canCheckin,
            checkedToday = checkedToday,
            message = core,
        )
    }

    /** 邀请中心（/invite_code）：兑换规则 + 可用邀请码 + 我邀请到的用户 */
    fun parseInviteCenter(html: String): InviteCenter {
        val d = doc(html)
        if (d.selectFirst("form[action=/login], input[name=username]") != null) {
            // 未登录
            return InviteCenter()
        }
        val csrf = d.selectFirst("input[name=_csrf]")?.attr("value") ?: ""
        val descEl = d.selectFirst(".invite-exchange-form p.muted")
        // 可用邀请码：每个 <li> 为一枚码（空态直接展示占位文本，不纳入列表）
        val codesEl = d.selectFirst("ul.invite-code-grid")
        val codes = codesEl?.select("> li")?.mapNotNull { li ->
            if (li.classNames().contains("empty-state")) {
                null
            } else {
                // 优先取外观上的码文本；剔除按钮/图标文字，避免把「复制」等混进码里
                val codeEl = li.selectFirst(".invite-code")
                val code = codeEl?.ownText()?.ifBlank { codeEl.text() }
                    ?: codeEl?.text()
                    ?: li.clone().let { c ->
                        c.select("button, [data-invite-copy], .invite-code-actions").remove()
                        c.text()
                    }
                        ?.trim()
                        ?: ""
                if (code.isBlank()) null else InviteCode(code = code)
            }
        } ?: emptyList()
        // 我邀请到的用户
        val invitedEl = d.selectFirst("ul.admin-manage-list")
        val invited = invitedEl?.select("> li")?.mapNotNull { li ->
            if (li.classNames().contains("empty-state")) null
            else {
                val ua = li.selectFirst("a[href^=/user/]")
                InvitedUser(
                    userId = idFrom(ua?.attr("href")),
                    username = ua?.text()?.trim() ?: li.ownText().trim(),
                    avatarUrl = absUrl(avatarOf(li)),
                    statusText = li.text().replace(Regex("""\s+"""), " ").trim(),
                )
            }
        } ?: emptyList()
        return InviteCenter(
            subtitle = d.selectFirst(".admin-plugin-summary span")?.text()?.trim() ?: "",
            description = descEl?.text()?.replace(Regex("""\s+"""), " ")?.trim() ?: "",
            canExchange = d.selectFirst("form.invite-exchange-form, form[action=/invite_code]") != null,
            csrf = csrf,
            codes = codes,
            invited = invited,
            emptyCodesText = codesEl?.selectFirst("> li.empty-state")?.text()?.trim() ?: "",
            emptyInvitedText = invitedEl?.selectFirst("> li.empty-state")?.text()?.trim() ?: "",
        )
    }

    /** 足迹/未读提醒页：解析成帖子卡片列表（结构与首页 post-list 一致时） */
    fun parseFootprintList(html: String): List<TopicCard> {
        runCatching { parseTopicList(html).first }.getOrDefault(emptyList()).takeIf { it.isNotEmpty() }?.let {
            return it
        }
        val d = doc(html)
        val seen = mutableSetOf<Long>()
        return d.select("a[href^=/topic/]").mapNotNull { a ->
            val tid = idFrom(a.attr("href"))
            if (tid == 0L || !seen.add(tid)) return@mapNotNull null
            // 跳过分页/翻页链接避免把 2、3 当成标题
            if (a.parent()?.selectFirst(".topic-pages") != null || a.text().trim().all { it.isDigit() }) return@mapNotNull null
            val item = a.parents().firstOrNull {
                it.tagName() == "li" || it.hasClass("post-item") || it.hasClass("notice-item") ||
                    it.hasClass("footprint-item") || it.hasClass("history-item")
            } ?: a.parent()
            val meta = item?.selectFirst(".post-meta")
            val user = meta?.selectFirst("a[href^=/user/]") ?: item?.selectFirst("a[href^=/user/]")
            val forum = meta?.selectFirst("a[href^=/forum/]")
                ?: item?.selectFirst(".forum-name, .post-forum-badge")
            val time = meta?.selectFirst("span[data-performance-time]")
            TopicCard(
                topicId = tid,
                title = a.text().trim(),
                authorId = idFrom(user?.attr("href")),
                authorName = user?.text() ?: "",
                forumId = idFrom(forum?.attr("href")),
                forumName = forum?.text() ?: "",
                replies = 0,
                lastReplier = "",
                timeText = time?.attr("data-performance-time")?.toLongOrNull()
                    ?.let { TimeFmt.rel(it) } ?: (time?.text() ?: ""),
            )
        }
    }

    /** 打赏弹幕：解析 /donate_feed 返回的 area_html（.donate-barrage-item） */
    fun parseDonateBarrage(areaHtml: String): List<DanmakuItem> = runCatching {
        doc(areaHtml).select(".donate-barrage-item").mapNotNull { el ->
            val user = el.selectFirst(".donate-barrage-user")?.text()?.trim() ?: ""
            if (user.isBlank()) null else DanmakuItem(
                user = user,
                amount = el.selectFirst(".donate-barrage-amount")?.text()?.trim() ?: "",
                action = el.selectFirst(".donate-barrage-action")?.text()?.trim() ?: "打赏",
                avatarUrl = absUrl(el.selectFirst(".donate-barrage-avatar")?.attr("src") ?: ""),
            )
        }
    }.getOrDefault(emptyList())
}
