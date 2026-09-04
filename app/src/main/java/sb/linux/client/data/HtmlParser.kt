package sb.linux.client.data

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.nodes.TextNode

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

    /** 帖内搜索专用纯文本：解码实体，并保留 pre/code 的真实内容与换行。 */
    fun htmlToSearchText(html: String): String {
        val body = Jsoup.parseBodyFragment(html).body()
        body.select("script, style, noscript").remove()
        return body.wholeText().replace('\u00A0', ' ').trim()
    }

    // ---------------- Markdown → HTML（发帖预览用） ----------------

    /** HTML 特殊字符转义 */
    private fun esc(s: String): String = s
        .replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
        .replace("\"", "&quot;")

    /**
     * 轻量 Markdown → HTML：覆盖编辑器工具栏与源站支持的语法——
     * 标题/粗斜体/删除线/行内代码/代码块/引用/有序无序列表/链接/图片/分隔线/表格。
     * 产物直接交给 HtmlContent 渲染，预览效果与帖子实际展示一致。
     */
    fun markdownToHtml(md: String): String {
        val out = StringBuilder()
        val lines = md.replace("\r\n", "\n").replace("\r", "\n").split("\n")
        var i = 0

        // 行内语法：先转义再替换标记（代码 span 优先保护，避免内部被二次处理）
        fun inline(s: String): String {
            val codeSpans = mutableListOf<String>()
            var t = esc(s).replace(Regex("`([^`\n]+)`")) {
                codeSpans += "<code>${it.groupValues[1]}</code>"; "\u0000${codeSpans.size - 1}\u0000"
            }
            t = t
                .replace(Regex("""!\[([^\]\n]*)\]\(([^)\s]+)[^)]*\)"""), """<img src="$2" alt="$1">""")
                .replace(Regex("""\[([^\]\n]+)\]\(([^)\s]+)[^)]*\)"""), """<a href="$2">$1</a>""")
                .replace(Regex("""\*\*([^*\n]+)\*\*"""), "<strong>$1</strong>")
                .replace(Regex("""\*([^*\n]+)\*"""), "<em>$1</em>")
                .replace(Regex("""~~([^~\n]+)~~"""), "<del>$1</del>")
                .replace(Regex("""==([^=\n]+)=="""), "<mark>$1</mark>")
                .replace(Regex("""\^([^\^\n]+)\^"""), "<sup>$1</sup>")
                .replace(Regex("""(?<!~)~([^~\n]+)~(?!~)"""), "<sub>$1</sub>")
            // 还原代码 span
            t = Regex("\u0000(\\d+)\u0000").replace(t) { codeSpans[it.groupValues[1].toInt()] }
            return t
        }

        while (i < lines.size) {
            val line = lines[i]

            // 代码块 ```
            if (line.trimStart().startsWith("```")) {
                val sb = StringBuilder()
                i++
                while (i < lines.size && !lines[i].trimStart().startsWith("```")) { sb.appendLine(lines[i]); i++ }
                i++ // 跳过结尾 ```
                out.append("<pre><code>").append(esc(sb.toString().trimEnd('\n'))).append("</code></pre>\n")
                continue
            }
            // 空行
            if (line.isBlank()) { i++; continue }
            // 分隔线
            if (Regex("^\\s*(---+|\\*\\*\\*+)\\s*$").matches(line)) {
                out.append("<hr>\n"); i++; continue
            }
            // 标题
            val h = Regex("^(#{1,6})\\s+(.*)$").find(line)
            if (h != null) {
                val lvl = h.groupValues[1].length
                out.append("<h$lvl>").append(inline(h.groupValues[2])).append("</h$lvl>\n")
                i++; continue
            }
            // 引用：按每行 > 的数量生成真正嵌套的 blockquote。
            if (line.trimStart().startsWith(">")) {
                var depth = 0
                while (i < lines.size && lines[i].trimStart().startsWith(">")) {
                    val current = lines[i].trimStart()
                    var pos = 0
                    var targetDepth = 0
                    while (pos < current.length && current[pos] == '>') {
                        targetDepth++
                        pos++
                        while (pos < current.length && current[pos] == ' ') pos++
                    }
                    while (depth < targetDepth) { out.append("<blockquote>"); depth++ }
                    while (depth > targetDepth) { out.append("</blockquote>"); depth-- }
                    out.append("<p>").append(inline(current.substring(pos))).append("</p>")
                    i++
                }
                while (depth > 0) { out.append("</blockquote>"); depth-- }
                out.append('\n')
                continue
            }
            // 表格：| a | b | 且下一行是 |---|---|
            if (line.trim().startsWith("|") && i + 1 < lines.size &&
                Regex("^\\s*\\|?[\\s:|-]+\\|?\\s*$").matches(lines[i + 1]) &&
                lines[i + 1].contains("-")
            ) {
                val header = line.trim().trim('|').split("|").map { it.trim() }
                out.append("<table><thead><tr>")
                header.forEach { out.append("<th>").append(inline(it)).append("</th>") }
                out.append("</tr></thead><tbody>")
                i += 2
                while (i < lines.size && lines[i].trim().startsWith("|")) {
                    val cells = lines[i].trim().trim('|').split("|").map { it.trim() }
                    out.append("<tr>")
                    header.indices.forEach { c -> out.append("<td>").append(inline(cells.getOrElse(c) { "" })).append("</td>") }
                    out.append("</tr>")
                    i++
                }
                out.append("</tbody></table>\n")
                continue
            }
            // 无序列表
            if (Regex("^\\s*[-*+]\\s+").containsMatchIn(line)) {
                out.append("<ul>")
                while (i < lines.size && Regex("^\\s*[-*+]\\s+").containsMatchIn(lines[i])) {
                    out.append("<li>").append(inline(Regex("^\\s*[-*+]\\s+").replace(lines[i], ""))).append("</li>")
                    i++
                }
                out.append("</ul>\n")
                continue
            }
            // 有序列表
            if (Regex("^\\s*\\d+\\.\\s+").containsMatchIn(line)) {
                out.append("<ol>")
                while (i < lines.size && Regex("^\\s*\\d+\\.\\s+").containsMatchIn(lines[i])) {
                    out.append("<li>").append(inline(Regex("^\\s*\\d+\\.\\s+").replace(lines[i], ""))).append("</li>")
                    i++
                }
                out.append("</ol>\n")
                continue
            }
            // 段落（连续非空行合并）。
            // 首行无条件消费：上面的表格分支要求下一行是 |---| 分隔行，未命中时
            // （分隔行还没敲、或表头就是最后一行）以 | 开头的行会被下面的循环条件
            // 排除，若不先消费首行则 i 不推进 → 外层 while 死循环。
            val sb = StringBuilder()
            sb.append(lines[i]).append('\n'); i++
            while (i < lines.size && lines[i].isNotBlank() &&
                !lines[i].trimStart().startsWith(">") && !lines[i].trimStart().startsWith("```") &&
                !Regex("^\\s*[-*+]\\s+").containsMatchIn(lines[i]) && !Regex("^\\s*\\d+\\.\\s+").containsMatchIn(lines[i]) &&
                !Regex("^(#{1,6})\\s+").containsMatchIn(lines[i]) && !lines[i].trim().startsWith("|")
            ) { sb.append(lines[i]).append('\n'); i++ }
            out.append("<p>").append(inline(sb.toString().trim())).append("</p>\n")
        }
        return out.toString()
    }

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
    /** 称号既可能是容器内的子节点，也可能就是用户组节点本身。 */
    private fun textWithoutGachaTitle(el: Element?): String {
        if (el == null || el.hasClass("gacha-title-badge") || el.hasClass("gacha-title-post-badge")) return ""
        return el.clone().also { it.select(".gacha-title-badge, .gacha-title-post-badge").remove() }.text()
    }

    private fun gachaTitleOf(el: Element): TitleBadge? {
        val badge = el.selectFirst(".gacha-title-badge, .gacha-title-post-badge") ?: return null
        val rare = Regex("""gacha-title-(ur\+|ssr|sr|ur|r|n)(?:\s|$)""")
            .find(badge.className())?.groupValues?.get(1)?.uppercase()
        val rarityText = badge.selectFirst(".gacha-title-rarity")?.text()?.trim().orEmpty()
        return TitleBadge(
            icon = badge.selectFirst(".gacha-title-icon")?.text()?.trim() ?: "",
            name = badge.selectFirst(".gacha-title-name")?.text()?.trim() ?: "",
            rarity = rare ?: rarityText,
            // 个人主页把 UR 编号放在 rarity 节点，帖子中则使用 serial 节点。
            serial = badge.selectFirst(".gacha-title-serial")?.text()?.trim()?.takeIf { it.isNotBlank() }
                ?: rarityText.takeIf { rare?.startsWith("UR") == true && it.matches(Regex("[0-9]+")) }.orEmpty(),
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
                authorName = textWithoutGachaTitle(userLink),
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
        // 标题清洗：抽奖/发卡等特殊帖的源站标题开头常带不可见字符
        // （零宽空格、BOM、不间断/全角空格等），Jsoup .text() 不会折叠它们，
        // 渲染出来就是标题前的空位
        fun cleanTitle(s: String) = s
            .replace(Regex("[\\u200B\\u200C\\u200D\\uFEFF\\u00A0\\u3000\\u2060]"), " ")
            .trim()
        fun titleText(selector: String): String? = d.selectFirst(selector)?.clone()?.also {
            // 精华/抽奖/发卡状态徽章是 h1 的子节点。直接 h1.text() 会把“精华”等
            // 状态重复拼进标题；状态由 titleTag 单独展示，标题只保留文本本体。
            it.select(
                ".topic-management-featured-badge, .topic-management-detail-badge, " +
                    ".community-lottery-title-status, .virtual-card-title-status"
            ).remove()
        }?.text()
        val realTitle = (titleText("h1.post-content-title")
            ?: titleText("h1.topic-title")
            ?: titleText(".main-panel h1")
            ?: titleText("h1")
            ?: run {
                // 从 toolbar 或 title 标签恢复
                val t = d.title()
                if (t.contains(" - ")) t.substringBefore(" - ") else t
            }).let(::cleanTitle)

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

        val essencePanel = d.selectFirst(".topic-essence-review-panel")
        val essenceApplication = essencePanel?.takeIf {
            it.attr("data-status") == "not_applied" || it.selectFirst("form.topic-essence-review-apply-form") != null
        }?.let { panel ->
            val form = panel.selectFirst("form.topic-essence-review-apply-form[action]")
            val button = form?.selectFirst("button[type=submit], button")
            EssenceApplication(
                title = panel.selectFirst(".topic-essence-review-head strong")?.text().orEmpty().ifBlank { "申请帖子加精" },
                note = panel.clone().apply { select("form, .topic-essence-review-head").remove() }.text(),
                action = form?.attr("action").orEmpty(),
                fields = hiddenFields(form),
                label = button?.text().orEmpty().ifBlank { "申请加精" },
                enabled = form != null && button != null && !button.hasAttr("disabled"),
            )
        }
        // 投票组件：申请表单与评议投票分开，不能把“可以申请”误判为已结束投票。
        val pollForm = d.selectFirst(
            ".topic-essence-review-vote-form[action], .topic-poll form[action], .poll-card form[action], form[class*=poll][action], " +
                "form[action]:has(input[type=radio][name*=poll]), form[action]:has(input[type=checkbox][name*=poll])"
        )
        val poll = pollForm?.let { form ->
            val root = form.parents().firstOrNull { el ->
                el.classNames().any { it.contains("poll", ignoreCase = true) || it == "topic-essence-review-panel" }
            } ?: form
            val inputs = form.select("input[type=radio][name], input[type=checkbox][name]")
            val panelText = root.text().replace(Regex("""\s+"""), " ").trim()
            val unavailableReason = when {
                panelText.contains("不能给自己", true) || panelText.contains("不能为自己", true) ||
                    panelText.contains("作者不能", true) -> "作者不能参与自己的申精投票"
                else -> ""
            }
            val options = inputs.mapNotNull { input ->
                val name = input.attr("name"); val value = input.attr("value")
                if (name.isBlank() || value.isBlank()) return@mapNotNull null
                val id = input.id()
                val label = (if (id.isNotBlank()) form.select("label[for]").firstOrNull { it.attr("for") == id } else null)
                    ?.text()?.trim()
                    ?: input.closest("label")?.text()?.trim()
                    ?: input.parent()?.text()?.trim()
                    ?: value
                TopicPollOption(name, value, label.ifBlank { value }, input.hasAttr("checked"), input.hasAttr("disabled"))
            }
            TopicPoll(
                title = if (form.hasClass("topic-essence-review-vote-form")) "精华申请 · 社区投票" else root.selectFirst("h2, h3, h4, .poll-title")?.text()?.trim().orEmpty().ifBlank { "投票" },
                note = if (form.hasClass("topic-essence-review-vote-form")) root.clone().apply { select("form").remove() }.text()
                    else root.selectFirst(".poll-note, .poll-description, small, p")?.text()?.trim().orEmpty(),
                action = form.attr("action"),
                fields = hiddenFields(form),
                options = options,
                multiple = inputs.any { it.attr("type").equals("checkbox", true) },
                submitLabel = form.selectFirst("button[type=submit], input[type=submit]")?.let { it.text().ifBlank { it.attr("value") } }?.trim().orEmpty().ifBlank { "提交投票" },
                closed = unavailableReason.isBlank() && (options.isEmpty() || inputs.all { it.hasAttr("disabled") }),
                unavailableReason = unavailableReason,
                reasonName = form.selectFirst("textarea[name]")?.attr("name").orEmpty(),
                reasonRequired = form.selectFirst("textarea[name]")?.hasAttr("required") == true,
                reasonHint = form.selectFirst("textarea[name]")?.attr("placeholder").orEmpty(),
                reasonMaxLength = form.selectFirst("textarea[name]")?.attr("maxlength")?.toIntOrNull()?.coerceAtLeast(1) ?: 300,
            )
        } ?: essencePanel?.takeIf { essenceApplication == null }?.let { panel ->
            val text = panel.text().replace(Regex("""\s+"""), " ").trim()
            val status = panel.attr("data-status").lowercase()
            val authorBlocked = text.contains("不能给自己", true) || text.contains("不能为自己", true) ||
                text.contains("作者不能", true) || status in setOf("author", "self", "owner")
            val ended = status in setOf("closed", "expired", "approved", "rejected", "finished") ||
                listOf("投票已结束", "评议已结束", "已过期").any { text.contains(it) }
            TopicPoll(
                title = "精华申请 · 社区投票",
                note = text,
                closed = ended,
                unavailableReason = if (authorBlocked) "作者不能参与自己的申精投票"
                    else if (!ended) "当前账号不能参与此投票" else "",
            )
        }

        val postElements = d.select("ul.topic-post-list li.post-entry, ul.post-list.topic-post-list li.post-entry")
        val replyIdToFloor = postElements.mapNotNull { li ->
            val replyId = li.id().removePrefix("post-").toLongOrNull() ?: return@mapNotNull null
            val floor = li.attr("data-floor").toIntOrNull() ?: return@mapNotNull null
            if (floor > 0) replyId to floor else null
        }.toMap()
        val posts = postElements.map { li ->
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
                textWithoutGachaTitle(group).substringBefore("积分").trim(' ', '·')
            }.filter { it.isNotBlank() }.distinct()
            val uidBadge = groups.firstOrNull { it.startsWith("UID") }
            val group = groups.lastOrNull { !it.startsWith("UID") } ?: ""
            val uidNum = uidBadge?.removePrefix("UID")?.trim()?.toLongOrNull() ?: 0L
            val meta = li.selectFirst(".post-meta")
            val timeText = meta?.selectFirst("span[data-performance-time]")?.attr("data-performance-time")?.toLongOrNull()
                // 兜底取首个 span 时须跳过树形回复的「回复 #N」标识，否则时间缺失时会显示成楼层引用
                ?.let { TimeFmt.rel(it) } ?: meta?.selectFirst("span:not(.quote-threads-reference)")?.text() ?: ""
            // 源站 v8.6+ 点赞改版：like-coin-* → donate-reaction-*（按钮 data-liked/data-coined/计数 span + 表单 donate-reaction-form）
            val likeBtn = li.selectFirst("[data-donate-reaction]")
            val likeForm = li.selectFirst("form.donate-reaction-form")
            // 正文：把"最后编辑"信息从正文拆出，单独用分割线展示
            val contentEl = ownContent?.clone()
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
                        + ", .topic-poll, .poll-card, form[class*=poll]"
                        // 淘帖插件会把所属专辑、收录/移除表单插进 post-content；
                        // 应用已有独立淘帖入口和帖子菜单，这些不属于 Markdown 正文。
                        + ", [class*=topic-collection], [data-topic-collection], "
                        + "form[action*=topic_collection], form[action*=topic_collections]"
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
                authorId = idFrom(authorA?.attr("href")),
                // wholeText 保留用户名内部的空格；只折叠 HTML 排版产生的连续空白，
                // 不再使用按空格切分用户名的启发式逻辑。
                authorName = authorA?.clone()?.also { it.select(".gacha-title-badge, .gacha-title-post-badge").remove() }
                    ?.wholeText()?.trim()?.replace(Regex("\\s+"), " ") ?: "",
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
            lottery = lottery,
            poll = poll,
            replyCaptcha = replyCaptcha,
            essenceApplication = essenceApplication,
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
        val rank = textWithoutGachaTitle(d.selectFirst(".user-rank"))
        // rank 形如 "饼友 · 积分 1324"：拆出用户组与积分
        val group = rank.substringBefore("积分").trim(' ', '·')
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
            titleBadge = d.selectFirst(".user-header, .user-card")?.let(::gachaTitleOf),
        )
    }

    fun parseMySidebar(html: String): LoginState {
        val d = doc(html)
        val card = d.selectFirst(".user-card")
        val nameEl = card?.selectFirst("a.user-name")
        val rank = textWithoutGachaTitle(card?.selectFirst(".user-rank"))
        val points = Regex("""积分\s*([\d.kw万]+)""").find(rank)?.groupValues?.get(1) ?: ""
        return LoginState(
            loggedIn = nameEl != null,
            userId = idFrom(nameEl?.attr("href")),
            username = nameEl?.text() ?: "",
            points = points,
            avatarUrl = absUrl(avatarOf(card ?: d)),
            userGroup = rank.substringBefore("积分").trim(' ', '·'),
            titleBadge = card?.let(::gachaTitleOf),
        )
    }

    fun parseNotifications(html: String): List<NotificationItem> {
        val d = doc(html)
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
            val partnerAvatar = absUrl(li.selectFirst(".post-avatar img[src]")?.attr("src") ?: "")
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

    /** 源站相对时间 → 时间戳：支持「刚刚/N秒/N分钟/N小时/N天/周/月/年前、昨天/前天」与常见日期格式；解析失败返回 0 */
    fun parseRelativeTime(text: String): Long {
        val t = text.trim()
        if (t.isBlank()) return 0L
        val now = System.currentTimeMillis()
        Regex("""(\d+)\s*秒前""").find(t)?.let { return now - it.groupValues[1].toLong() * 1000L }
        Regex("""(\d+)\s*分钟前""").find(t)?.let { return now - it.groupValues[1].toLong() * 60_000L }
        Regex("""(\d+)\s*小时前""").find(t)?.let { return now - it.groupValues[1].toLong() * 3_600_000L }
        Regex("""(\d+)\s*天前""").find(t)?.let { return now - it.groupValues[1].toLong() * 86_400_000L }
        Regex("""(\d+)\s*周前""").find(t)?.let { return now - it.groupValues[1].toLong() * 7L * 86_400_000L }
        Regex("""(\d+)\s*月前""").find(t)?.let { return now - it.groupValues[1].toLong() * 30L * 86_400_000L }
        Regex("""(\d+)\s*年前""").find(t)?.let { return now - it.groupValues[1].toLong() * 365L * 86_400_000L }
        // 「昨天/前天」不带具体时刻：按当前时刻回退整/两个自然日（时刻精度源站未提供）
        if (t.contains("前天")) return now - 2L * 86_400_000L
        if (t.contains("昨天")) return now - 86_400_000L
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
                    username = textWithoutGachaTitle(a.selectFirst(".leaderboard-podium-name")),
                    userGroup = textWithoutGachaTitle(a.selectFirst(".leaderboard-podium-group")),
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
                    username = textWithoutGachaTitle(a.selectFirst(".leaderboard-name")),
                    userGroup = textWithoutGachaTitle(a.selectFirst(".leaderboard-group")),
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

        // 当前在线：源站现已服务端渲染 .online-users-card（总数 + 头像网格），直接解析；
        // 卡片缺失（旧版/异常页）时回退：读 online-users-boot 的 ID 集，人数 = ID 数，
        // 在线用户项交叉匹配本页帖子作者（拿名字/头像）。
        val card = d.selectFirst(".online-users-card")
        val onlineUsers = if (card != null) {
            OnlineUsers(
                count = card.selectFirst(".online-users-count")?.text()?.trim() ?: "",
                items = card.select(".online-users-grid a.online-users-item[href]").mapNotNull { a ->
                    val uid = idFrom(a.attr("href"))
                    if (uid <= 0) null else OnlineUser(
                        userId = uid,
                        username = a.attr("title").ifBlank {
                            a.selectFirst(".online-users-name")?.text() ?: ""
                        }.trim(),
                        avatarUrl = absUrl(a.selectFirst("img[src]")?.attr("src") ?: ""),
                    )
                }.distinctBy { it.userId },
                moreText = "",
            )
        } else {
            val onlineIds = onlineIdsOf(d)
            if (onlineIds.isNotEmpty()) {
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
        }

        return HomeSidebar(
            forums = forums,
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
                timestamp = runCatching { java.time.OffsetDateTime.parse(li.selectFirst("time")?.attr("datetime")).toInstant().toEpochMilli() }.getOrDefault(0L),
                topicId = idFrom(li.selectFirst("a[href^=/topic/]")?.attr("href")),
            )
        }
    }

    // ---------------- 打赏 ----------------

    data class DonateInfo(
        val csrf: String,
        val topicId: String,
        val requestKey: String,
        val quickMessages: List<String>,
        val unavailableReason: String = "",
    )

    fun parseDonate(html: String): DonateInfo {
        val d = doc(html)
        val form = d.selectFirst("form[action=/donate]")
        return DonateInfo(
            csrf = form?.selectFirst("input[name=_csrf]")?.attr("value") ?: "",
            topicId = form?.selectFirst("input[name=topic_id]")?.attr("value") ?: "",
            requestKey = form?.selectFirst("input[name=request_key]")?.attr("value") ?: "",
            quickMessages = d.select("button[data-quick-message]").map { it.text() },
            unavailableReason = if (form == null) d.selectFirst(".forum-main")?.text()
                ?.removePrefix("消息")?.trim()?.takeIf { it.isNotBlank() } ?: extractError(html) else "",
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

    // ---------------- 认证中心 / 邀请中心 / 我的称号 ----------------

    /** 创作者认证中心（/identity_center）：权益、条件、邮箱状态与历史申请。 */
    fun parseIdentityCenter(html: String): IdentityCenterData {
        val d = doc(html)
        fun heading(text: String) = d.select("h2,h3,h4").firstOrNull { it.text().contains(text) }
        fun nextElement(start: Element?, selector: String): Element? {
            var node = start?.nextElementSibling()
            while (node != null && node.tagName() !in setOf("h2", "h3", "h4")) {
                if (node.`is`(selector)) return node
                node.selectFirst(selector)?.let { return it }
                node = node.nextElementSibling()
            }
            return null
        }
        fun requirements(title: String): List<IdentityRequirement> {
            val table = nextElement(heading(title), "table") ?: return emptyList()
            return table.select("tbody tr, tr").mapNotNull { row ->
                val cells = row.select("td")
                if (cells.size < 2) return@mapNotNull null
                val label = cells[0].text().trim()
                val state = cells[1].text().trim()
                if (label.isBlank()) null else IdentityRequirement(label, state.contains("符合") && !state.contains("不符合"))
            }.distinctBy { it.label }
        }
        val benefits = nextElement(heading("创作者福利"), "ul")?.select("li")?.mapNotNull { item ->
            val name = item.selectFirst("strong")?.text()?.trim().orEmpty()
            if (name.isBlank()) return@mapNotNull null
            val description = item.text().removePrefix(name).trim()
            name to description
        }.orEmpty()
        val account = requirements("账号核验条件")
        val mainText = d.selectFirst("main")?.text().orEmpty()
        val email = Regex("当前邮箱[：:]\\s*(\\S+)").find(mainText)?.groupValues?.get(1).orEmpty()
        val records = nextElement(heading("我的申请"), "ul")?.select("li")?.map { it.text().trim() }
            ?.filter { it.isNotBlank() && !it.contains("暂无申请") }.orEmpty()
        val applyButton = d.selectFirst(".identity-center-next, form.identity-center-apply button[type=submit]")
        return IdentityCenterData(
            benefits = benefits,
            creatorRequirements = requirements("创作者认证条件"),
            accountRequirements = account,
            email = email,
            emailVerified = account.firstOrNull { it.label.contains("邮箱验证") }?.met == true,
            canApply = applyButton != null && !applyButton.hasAttr("disabled"),
            applicationRecords = records,
            notice = d.selectFirst(".identity-center-notice, .identity-center-rules, details")?.text()?.trim().orEmpty(),
        )
    }

    /**
     * 邀请中心（/invite_center，登录后可见）。
     * 结构（v8.7.5 插件）：.invite-center-link（说明 + 分享链接 input[data-invite-center-link]）、
     * .invite-center-stats（三个 strong+span 统计）、.invite-center-list ul li（被邀请用户）。
     */
    fun parseInviteCenter(html: String): InviteCenter {
        val d = doc(html)
        fun statsAt(i: Int): String =
            d.select(".invite-center-stats > div")[i].selectFirst("strong")?.text()?.trim() ?: "0"
        val users = d.select(".invite-center-list ul li").mapNotNull { li ->
            // 空态 li.empty-state（"暂时还没有用户…"）不是用户条目
            if (li.hasClass("empty-state") || li.selectFirst("a[href^=/user/]") == null) return@mapNotNull null
            val a = li.selectFirst("a[href^=/user/]")
            val name = a?.text()?.trim() ?: return@mapNotNull null
            // 状态文本：条目全文去掉用户名后的剩余文本（注册时间/奖励发放等）
            val status = li.text().replace(name, "").replace(Regex("""\s+"""), " ").trim()
            InviteUser(
                name = name,
                userId = a?.attr("href")?.let { idFrom(it) } ?: 0,
                statusText = status,
            )
        }
        return InviteCenter(
            link = d.selectFirst("input[data-invite-center-link]")?.attr("value")?.trim() ?: "",
            desc = d.selectFirst(".invite-center-link p.muted")?.text()?.trim() ?: "",
            rule = d.selectFirst(".invite-center-rule")?.text()?.trim() ?: "",
            invitedCount = statsAt(0),
            firstReward = d.select(".invite-center-stats > div").getOrNull(1)
                ?.selectFirst("span")?.text()?.trim() ?: "",
            secondReward = d.select(".invite-center-stats > div").getOrNull(2)
                ?.selectFirst("span")?.text()?.trim() ?: "",
            users = users,
        )
    }

    /**
     * 我的称号（/gacha_profile，登录后可见）。
     * 结构：.gacha-center-stat（"N 种称号"）+ .gacha-profile-titles 内称号条目。
     * 条目 DOM 未见于空态页面（测试账号无称号），按源站惯例宽松解析：
     * 容器直接子元素逐个取 .gacha-title-badge（icon/name/rarity），
     * 其余表单（装备/赠送等）按 action + 隐藏字段 + 按钮文本通用收集，不硬编码接口。
     */
    fun parseGachaProfile(html: String): GachaProfile {
        val d = doc(html)
        val candidates = d.select(".gacha-profile-titles .gacha-title-badge, .gacha-profile-titles .gacha-title-post-badge, .gacha-profile-grid .gacha-title-badge")
            .map { badge ->
                val collection = badge.parents().firstOrNull {
                    it.hasClass("gacha-profile-titles") || it.hasClass("gacha-profile-grid")
                }
                // 取集合以内完整的单称号条目，不能只取装备按钮附近的内层容器，
                // 否则放在同级的持有数量与说明会丢失。
                badge.parents().takeWhile { it !== collection }
                    .lastOrNull { it.select(".gacha-title-badge, .gacha-title-post-badge").size == 1 }
                    ?: badge
            }.distinct()
        val titles = candidates.mapNotNull { el ->
            if (el.hasClass("empty-state")) return@mapNotNull null
            val badge = gachaTitleOf(el) ?: el.selectFirst(".gacha-title-badge")?.let { b ->
                TitleBadge(
                    icon = b.selectFirst(".gacha-title-icon")?.text()?.trim() ?: "",
                    name = b.selectFirst(".gacha-title-name")?.text()?.trim() ?: "",
                    rarity = b.selectFirst(".gacha-title-rarity")?.text()?.trim() ?: "",
                    serial = b.selectFirst(".gacha-title-serial")?.text()?.trim() ?: "",
                )
            }
            if (badge == null || badge.name.isBlank()) return@mapNotNull null
            val full = el.text().replace(Regex("""\s+"""), " ").trim()
            // 数量：条目里「×N」或「x N」形式的持有数（无则 1）
            val count = el.selectFirst("[data-count]")?.attr("data-count")?.toIntOrNull()
                ?: Regex("""(?:[×x]|数量\s*[:：]?|持有\s*[:：]?)\s*(\d+)""").find(full)?.groupValues?.get(1)?.toIntOrNull()
                ?: Regex("""(\d+)\s*[个枚份]""").find(full)?.groupValues?.get(1)?.toIntOrNull() ?: 1
            // 装备状态：类标记 is-equipped / 文本含「已装备」
            val equipped = el.hasClass("is-equipped") || el.selectFirst(".is-equipped, [class*=equipped]") != null || full.contains("已装备") || full.contains("已佩戴")
            val actions = el.select("form[action]").mapNotNull { form ->
                val btn = form.selectFirst("button") ?: return@mapNotNull null
                GachaAction(
                    label = btn.text().trim(),
                    action = form.attr("action"),
                    fields = hiddenFields(form),
                    enabled = !btn.hasAttr("disabled"),
                )
            }.filter {
                it.label.isNotBlank() && it.action.isNotBlank() &&
                    !it.label.contains("赠送", ignoreCase = true) && !it.label.contains("gift", ignoreCase = true)
            }
            GachaTitleItem(badge = badge, equipped = equipped, count = count, actions = actions)
        }
        return GachaProfile(
            stat = d.selectFirst(".gacha-center-stat")?.text()?.trim() ?: "",
            titles = titles.distinctBy { Triple(it.badge.name, it.badge.rarity, it.badge.serial) },
        )
    }

    /** 称号抽取页：操作地址和池概率均以源站 DOM 为准。 */
    fun parseGachaCenter(html: String): GachaCenter {
        val d = doc(html)
        val actions = d.select(".gacha-actions form[action]").mapNotNull { form ->
            val button = form.selectFirst("button[type=submit], button") ?: return@mapNotNull null
            GachaAction(button.text().trim(), form.attr("action"), hiddenFields(form), !button.hasAttr("disabled"))
        }
        val pool = d.select(".gacha-pool-rarity").mapNotNull { row ->
            val rarity = row.selectFirst(".gacha-pool-rarity-label")?.text()?.trim().orEmpty()
            if (rarity.isBlank() || rarity.uppercase().startsWith("UR")) return@mapNotNull null
            GachaPoolRow(
                rarity = rarity,
                countText = row.selectFirst(".gacha-pool-rarity-count")?.text()?.trim().orEmpty(),
                rateText = row.selectFirst(".gacha-pool-rarity-rate")?.text()?.trim().orEmpty(),
            )
        }
        return GachaCenter(
            pointsText = d.selectFirst(".gacha-center-header .gacha-center-stat")?.text()?.trim().orEmpty(),
            statsText = d.selectFirst(".gacha-sub-stats")?.text()?.trim().orEmpty(),
            pullActions = actions,
            pool = pool,
            allTitles = d.select(".gacha-all-item").mapNotNull(::gachaTitleOf),
            // 跑马灯会复制一组节点实现循环，按文本去重。
            news = d.select(".gacha-good-news-item").eachText().map { it.trim() }
                .filter { it.isNotBlank() }.distinct().take(10),
        )
    }

    /** 称号市场：保留每个购买表单的服务端 listing_id 及返回状态。 */
    fun parseGachaMarket(html: String): GachaMarketPage {
        val d = doc(html)
        val listings = d.select("article.gacha-market-card").mapNotNull { card ->
            val badge = gachaTitleOf(card) ?: return@mapNotNull null
            val form = card.selectFirst("form.gacha-market-buy[action]") ?: return@mapNotNull null
            val text = card.selectFirst(".gacha-market-meta")?.text()?.trim().orEmpty()
            val price = Regex("""单价\s*(\d+)""").find(text)?.groupValues?.get(1)?.toIntOrNull() ?: 0
            val available = form.selectFirst("input[name=quantity]")?.attr("max")?.toIntOrNull()
                ?: Regex("""剩余\s*(\d+)\s*个""").find(text)?.groupValues?.get(1)?.toIntOrNull() ?: 1
            val timeLeft = Regex("""剩余时间\s*(.+)$""").find(text)?.groupValues?.get(1)?.trim().orEmpty()
            GachaMarketListing(
                badge = badge,
                price = price,
                available = available,
                timeLeft = timeLeft,
                action = GachaAction("购买", form.attr("action"), hiddenFields(form)),
            )
        }
        val pageLinks = d.select(".pagination a[href*=p]").mapNotNull { a ->
            Regex("""[?&]p=(\d+)""").find(a.attr("href"))?.groupValues?.get(1)?.toIntOrNull()
        }
        val summary = d.selectFirst(".gacha-market-head")?.text()?.trim().orEmpty()
        val current = Regex("""当前\s*(\d+)-""").find(summary)?.groupValues?.get(1)?.toIntOrNull()
            ?.let { start -> ((start - 1) / 24) + 1 } ?: 1
        return GachaMarketPage(
            summary = summary,
            note = d.selectFirst(".gacha-market-note")?.text()?.trim().orEmpty(),
            listings = listings,
            currentPage = current,
            lastPage = pageLinks.maxOrNull() ?: current,
        )
    }

    /**
     * 选项控件所属的选项容器，由内向外最多四层。容器里一旦出现第二个 checkbox/radio，
     * 说明已经越过本行，再往上取就会串到别人的称号，到此为止。
     */
    private fun optionHolders(control: Element): List<Element> {
        val holders = mutableListOf<Element>()
        var node = control.parent()
        while (node != null && holders.size < 4) {
            if (node.select("input[type=checkbox], input[type=radio]").size > 1) break
            holders += node
            node = node.parent()
        }
        return holders
    }

    /**
     * 熔炼/回收页里 checkbox、radio 的称号名。源站把称号渲染成 .gacha-title-badge 卡片，
     * 控件本身既没有 label 也没有文字，直接取字段名的话界面上只会看到 title_ids，
     * 分不清勾的是哪个称号。没有称号徽章时返回空，交给后面的 label 兜底。
     */
    private fun gachaOptionLabel(control: Element): String {
        for (holder in optionHolders(control)) {
            val badge = gachaTitleOf(holder) ?: continue
            val head = listOf(badge.icon, badge.name).filter { it.isNotBlank() }.joinToString(" ")
            return listOf(head, badge.rarity, badge.serial, optionSideText(holder))
                .filter { it.isNotBlank() }.joinToString(" · ")
        }
        return ""
    }

    /** 同一种称号的多枚实例共用聚合标题；序号和持有数量留在实例标签中。 */
    private fun gachaOptionGroupLabel(control: Element): String {
        for (holder in optionHolders(control)) {
            val badge = gachaTitleOf(holder) ?: continue
            return listOf(badge.icon, badge.name, badge.rarity)
                .filter { it.isNotBlank() }.joinToString(" · ")
        }
        return ""
    }

    /** 既没有称号徽章又没有 label 的选项：退回选项行的可见文字，至少比字段名可读。 */
    private fun gachaOptionText(control: Element): String =
        optionHolders(control).firstNotNullOfOrNull { optionSideText(it).takeIf(String::isNotBlank) }.orEmpty()

    /** 选项容器里除徽章和控件外的可见文字，例如持有数量「×2」；成段的说明文字不取。 */
    private fun optionSideText(el: Element): String = el.clone()
        .also { it.select(".gacha-title-badge, .gacha-title-post-badge, input, select, textarea, button").remove() }
        .text().replace(Regex("""\s+"""), " ").trim()
        .takeIf { it.length in 1..40 }.orEmpty()

    /**
     * 解析称号系统的动态表单页。这里只限定 /gacha* 的提交表单，避免把页头搜索、登录等
     * 无关表单带进客户端；字段名称和值保持原样，提交时可完整复现重复 checkbox 参数。
     */
    fun parseGachaOperationPage(html: String, collectionsOnly: Boolean = false): GachaOperationPage {
        val d = doc(html)
        val main = d.selectFirst("main") ?: d.body()
        val forms = main.select("form[action]").flatMap { form ->
            val action = form.attr("action").trim()
            val isSupportedOperation = if (collectionsOnly) action == "/topic_collections_action"
                else Regex("^/(gacha|identity|verify|creator)[a-zA-Z0-9_/?=&%-]*$").matches(action)
            // 只在源站显式写了 method=get 时跳过。缺省 method 的表单（源站熔炼/回收页就是这样）
            // 一律按可提交处理：之前连同缺省一起判成 GET 会让整页解析不出任何表单，
            // 界面上就是「按钮在但点不动」或压根没有按钮。
            if (!isSupportedOperation || form.attr("method").trim().equals("get", true)) return@flatMap emptyList()
            val hidden = form.select("input[type=hidden][name]:not([disabled])").map { it.attr("name") to it.attr("value") }
            val externalControls = main.select("input[form], select[form], textarea[form]").filter { it.attr("form") == form.id() && form.id().isNotBlank() }
            val controls = (form.select("input[name]:not([type=hidden]):not([type=submit]):not([type=button]), select[name], textarea[name]") + externalControls).distinct()
            val fields = controls
                .mapNotNull { control ->
                    val name = control.attr("name").trim()
                    if (name.isBlank() || control.hasAttr("disabled") || control.closest("fieldset[disabled]") != null) return@mapNotNull null
                    val tag = control.tagName()
                    val type = when (tag) {
                        "select" -> "select"
                        "textarea" -> "textarea"
                        else -> control.attr("type").lowercase().ifBlank { "text" }
                    }
                    val id = control.id()
                    // label 可能写在 form 外面（源站把选项卡片和表单拆成两块），所以整页找
                    val explicitLabel = id.takeIf { it.isNotBlank() }
                        ?.let { target -> main.select("label[for]").firstOrNull { it.attr("for") == target }?.text()?.trim() }
                    val wrappingLabel = control.closest("label")?.text()?.trim()
                    val fallbackLabel = control.attr("aria-label").trim()
                        .ifBlank { control.attr("placeholder").trim() }
                        .ifBlank { name }
                    // 选项控件优先取称号名：熔炼/回收的 checkbox 只带 id，文字在旁边的徽章里
                    val isOption = type == "checkbox" || type == "radio"
                    val label = (if (isOption) gachaOptionLabel(control) else "")
                        .ifBlank { explicitLabel.orEmpty() }
                        .ifBlank { wrappingLabel.orEmpty() }
                        .ifBlank { if (isOption) gachaOptionText(control) else "" }
                        .ifBlank { fallbackLabel }
                        .replace(Regex("""\s+"""), " ").trim()
                    GachaFormField(
                        name = name,
                        label = label,
                        groupLabel = if (isOption) gachaOptionGroupLabel(control) else "",
                        type = type,
                        value = if (tag == "textarea") control.text() else control.attr("value"),
                        placeholder = control.attr("placeholder"),
                        min = control.attr("min"),
                        max = control.attr("max"),
                        required = control.hasAttr("required"),
                        checked = control.hasAttr("checked"),
                        options = if (tag == "select") control.select("option").map { option ->
                            // optgroup 的分组名（源站按稀有度分组）拼在前面，空文字的选项退回 value
                            val group = option.closest("optgroup")?.attr("label")?.trim().orEmpty()
                            val text = option.text().trim().ifBlank { option.attr("value").trim() }
                            GachaFormOption(
                                if (option.hasAttr("value")) option.attr("value") else option.text(),
                                listOf(group, text).filter { it.isNotBlank() }.joinToString(" · "),
                                option.hasAttr("selected"),
                                option.hasAttr("disabled") || option.closest("optgroup[disabled]") != null,
                            )
                        } else emptyList(),
                        maxLength = control.attr("maxlength").toIntOrNull()?.coerceAtLeast(0) ?: Int.MAX_VALUE,
                        multiple = control.hasAttr("multiple"),
                    )
                }
            val externalButtons = main.select("button[form], input[type=submit][form]").filter {
                form.id().isNotBlank() && it.attr("form") == form.id()
            }
            val buttons = (form.select("button[type=submit], input[type=submit], button:not([type])") + externalButtons).distinct()
            buttons.map { button ->
            val submitFields = hidden.toMutableList()
            button.attr("name").takeIf { it.isNotBlank() }?.let { submitName ->
                submitFields += submitName to button.attr("value")
            }
            val label = button.clone().also { it.select("small").remove() }.text().trim()
                .ifBlank { button?.attr("value")?.trim().orEmpty() }
                .ifBlank { form.selectFirst("h2, h3, legend")?.text()?.trim().orEmpty() }
                .ifBlank { "提交" }
            val submitAction = button.attr("formaction").ifBlank { action }
            // A submit override must stay within the same supported operation family.
            val choiceDrivenForge = (action.contains("forge", ignoreCase = true) ||
                action.contains("recycle", ignoreCase = true)) &&
                fields.any { it.type == "checkbox" || it.type == "radio" }
            val minSelections = if (choiceDrivenForge) {
                listOf(button, form).firstNotNullOfOrNull { node ->
                    listOf("data-min-selected", "data-min-selections", "data-required-count", "data-min-count")
                        .firstNotNullOfOrNull { attr -> node.attr(attr).toIntOrNull() }
                } ?: Regex("(?:至少选择|选择至少|需要选择|请选择)\\s*(\\d+)\\s*(?:个|枚|项)")
                    .find(form.text())?.groupValues?.get(1)?.toIntOrNull() ?: 1
            } else 0
            GachaOperationForm(label, if (submitAction == action) action else submitAction.takeIf {
                if (collectionsOnly) it == "/topic_collections_action" else Regex("^/(gacha|identity|verify|creator)[a-zA-Z0-9_/?=&%-]*$").matches(it)
            } ?: action, submitFields, fields, choiceDrivenForge ||
                (!button.hasAttr("disabled") && button.closest("fieldset[disabled]") == null), minSelections)
            }
        }
        val notes = main.select(".gacha-market-note, .gacha-note, .notice, .alert, .tips, .help-text")
            .eachText().map { it.replace(Regex("""\s+"""), " ").trim() }
            .filter { it.isNotBlank() }.distinct().take(8)
        val records = main.select("table tbody tr, .empty-state, .gacha-record, .order-item")
            .eachText().map { it.replace(Regex("""\s+"""), " ").trim() }
            .filter { it.isNotBlank() && it.length <= 500 }.distinct().take(100)
        return GachaOperationPage(
            title = main.selectFirst("h1")?.text()?.trim().orEmpty()
                .ifBlank { d.title().substringBefore("-").trim() },
            notes = notes,
            forms = forms,
            records = records,
            links = main.select(".pagination a[href]").map { it.text().trim() to it.attr("href") }
                .filter { it.second.startsWith("/") && !it.second.startsWith("//") }.distinct(),
        )
    }

    /** 新版私信联系人列表（/direct_messages）。 */
    fun parseDirectMessageContacts(html: String): List<DirectMessageContact> {
        val d = doc(html)
        return d.select("a.direct-messages-conversation[href^=/direct_messages/]").mapNotNull { a ->
            val uid = idFrom(a.attr("href"))
            if (uid <= 0) return@mapNotNull null
            DirectMessageContact(
                userId = uid,
                username = a.selectFirst("strong")?.text()?.trim() ?: "",
                avatarUrl = absUrl(avatarOf(a)),
                timeText = a.selectFirst("time")?.text()?.trim() ?: "",
                preview = a.selectFirst("small")?.text()?.trim() ?: "",
            )
        }.distinctBy { it.userId }
    }

    /** 新版服务端私信会话（/direct_messages/{uid}），服务端消息为权威数据源。 */
    fun parseDirectMessageThread(html: String, partnerId: Long): DirectMessageThread {
        val d = doc(html)
        val header = d.selectFirst(".direct-messages-thread-header a[href^=/user/]")
            ?: d.selectFirst("main a[href=/user/$partnerId]")
        val partnerName = header?.selectFirst("strong")?.text()?.trim()
            ?: header?.attr("aria-label")?.substringBefore(" 私信")?.trim().orEmpty()
        val partnerAvatar = header?.let(::avatarOf).orEmpty().let(::absUrl)
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
                avatarUrl = if (incoming) absUrl(avatarOf(article)) else partnerAvatar,
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

    /** 淘帖中心专辑卡片（/topic_collections?tab=mine|everyone）。 */
    fun parseTopicCollections(html: String): List<TopicCollectionCard> {
        val d = doc(html)
        // 源站列表容器：优先 forum-main 下的 li，其次裸 li，再其次任何含 topic_collection 链接的容器。
        val rows = d.select(".forum-main li.topic-collections-collection-row").ifEmpty {
            d.select("li.topic-collections-collection-row")
        }.ifEmpty {
            // 兜底：源站可能改了类名，只要列表项里有指向 /topic_collection/{id} 的链接就尝试解析。
            d.select("li:has(a[href^=/topic_collection/])")
        }
        return rows.mapNotNull { li ->
            val title = li.selectFirst("a[href^=/topic_collection/]") ?: return@mapNotNull null
            val author = li.selectFirst("a[href^=/user/]")
            // meta 文本：取 .post-meta 下的 span，兜底取所有非链接的 span
            val meta = li.select(".post-meta > span").eachText().ifEmpty {
                li.select("span:not(:has(a))").eachText()
            }
            val collectionId = idFrom(title.attr("href"))
            if (collectionId <= 0) return@mapNotNull null
            TopicCollectionCard(
                collectionId = collectionId,
                title = title.text().trim(),
                authorId = idFrom(author?.attr("href")),
                authorName = author?.text()?.trim() ?: "",
                avatarUrl = absUrl(avatarOf(li)),
                visibility = li.selectFirst(".topic-collections-tag")?.text()?.trim() ?: "",
                articleCount = meta.firstOrNull { it.contains("篇") || it.contains("文章") }?.let(::trimMetaSeparator) ?: "",
                updatedText = meta.firstOrNull { it.startsWith("更新") || it.contains("前") }?.let(::trimMetaSeparator) ?: "",
                description = (li.selectFirst(".topic-collections-card-desc") ?: li.selectFirst("p, .desc, .description"))?.text()?.trim() ?: "",
                subscribed = li.selectFirst(".topic-collections-tag-subscribed, [class*=subscribed]") != null,
            )
        }.distinctBy { it.collectionId }
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
