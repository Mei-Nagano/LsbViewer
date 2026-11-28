package sb.linux.client.data

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.TextNode
import sb.linux.client.common.network.Endpoints
import sb.linux.client.domain.topic.*

internal object InviteRelayHtmlParser {

    fun parsePanel(root: Element, fallbackTopicId: Long = 0L): InviteRelayPanel? {
        val section = if (root.hasClass("invite-relay-panel") || root.hasAttr("data-invite-relay-panel")) {
            root
        } else {
            root.selectFirst("section.invite-relay-panel, [data-invite-relay-panel]")
        } ?: return null

        val topicId = section.attr("data-invite-relay-panel").toLongOrNull() ?: fallbackTopicId
        val head = section.selectFirst(".invite-relay-panel-head")
        val title = head?.selectFirst("> strong, strong")?.text()?.trim().orEmpty().ifBlank { "邀请码接力" }
        val stats = section.selectFirst(".invite-relay-stats")
        val stockText = stats?.selectFirst(".invite-relay-stock")?.text()?.trim().orEmpty()
        val totalText = stats?.selectFirst(".invite-relay-total")?.text()?.trim().orEmpty()
        val quotaText = stats?.selectFirst(".invite-relay-quota")?.text()?.trim().orEmpty()
        val pendingBadge = section.selectFirst(".invite-relay-pending-badge")?.text()?.trim().orEmpty()
        val forceNotice = section.selectFirst(".invite-relay-force-notice")
        val forceNoticeTitle = forceNotice?.selectFirst("> strong")?.text()?.trim().orEmpty()
        val forceNoticeBody = forceNotice?.selectFirst("> span")?.text()?.trim().orEmpty()
            .ifBlank { forceNotice?.text()?.removePrefix(forceNoticeTitle)?.trim().orEmpty() }
        val deadlineText = section.selectFirst(".invite-relay-deadline")?.text()?.trim().orEmpty()

        val statusNotes = section.children().mapNotNull { child ->
            if (child.hasAttr("hidden")) return@mapNotNull null
            if (child.hasClass("invite-relay-panel-head") ||
                child.hasClass("invite-relay-force-notice") ||
                child.hasClass("invite-relay-actions") ||
                child.hasClass("invite-relay-deadline") ||
                child.hasClass("invite-relay-pending-badge")
            ) return@mapNotNull null
            child.text().trim().takeIf { it.isNotBlank() }
        }

        val actions = section.select(".invite-relay-actions > button, .invite-relay-actions > a").mapNotNull { el ->
            parseAction(el, Endpoints.BASE)
        }

        return InviteRelayPanel(
            topicId = topicId,
            title = title,
            awaitingHandoff = section.hasClass("invite-relay-awaiting-handoff"),
            pendingBadge = pendingBadge,
            stockText = stockText,
            totalText = totalText,
            quotaText = quotaText,
            forceNoticeTitle = forceNoticeTitle,
            forceNoticeBody = forceNoticeBody,
            deadlineText = deadlineText,
            statusNotes = statusNotes,
            actions = actions,
        )
    }

    fun parsePanelHtml(panelHtml: String, fallbackTopicId: Long): InviteRelayPanel? {
        if (panelHtml.isBlank()) return null
        val doc = Jsoup.parseBodyFragment(panelHtml, Endpoints.BASE).body()
        return parsePanel(doc, fallbackTopicId)
    }

    fun parseViewHtml(url: String, title: String, html: String): InviteRelayViewData {
        val base = Endpoints.abs(url)
        val doc = Jsoup.parseBodyFragment(html, base).body()
        doc.select("script, style").remove()
        val nodes = parseChildren(doc, base)
        return InviteRelayViewData(
            url = base,
            title = title.ifBlank { "邀请码接力" },
            nodes = nodes,
        )
    }

    private fun parseChildren(container: Element, base: String): List<InviteRelayNode> =
        container.childNodes().flatMap { node ->
            when (node) {
                is Element -> parseElement(node, base)
                is TextNode -> node.text().trim().takeIf { it.isNotBlank() }
                    ?.let { listOf(InviteRelayNode.Text(it)) }.orEmpty()
                else -> emptyList()
            }
        }

    private fun parseElement(element: Element, base: String): List<InviteRelayNode> {
        if (element.hasAttr("hidden") && !element.hasAttr("data-invite-relay-copy")) return emptyList()
        if (element.attr("aria-hidden") == "true") return emptyList()
        val tag = element.normalName()
        if (tag in setOf("script", "style", "svg", "template")) return emptyList()

        // 邀请码展示与复制块
        if (element.hasClass("invite-relay-code")) {
            val input = element.selectFirst("[data-invite-relay-code], input")
            val code = input?.attr("value")?.trim().orEmpty().ifBlank { element.selectFirst("code")?.text()?.trim().orEmpty() }
            if (code.isNotBlank()) {
                val label = element.selectFirst("label")?.clone()?.apply { select("input,button").remove() }?.text()?.trim().orEmpty()
                    .ifBlank { "邀请码" }
                val copyLabel = element.selectFirst("[data-invite-relay-copy]")?.text()?.trim().orEmpty().ifBlank { "复制" }
                return listOf(InviteRelayNode.CodeBox(label = label, code = code, copyLabel = copyLabel))
            }
        }

        // 操作按钮条
        if (element.hasClass("invite-relay-actions") && element.select("form").isEmpty()) {
            val actions = element.select("button, a").mapNotNull { parseAction(it, base) }
            if (actions.isNotEmpty()) return listOf(InviteRelayNode.ActionBar(actions))
        }

        // 独立弹窗触发按钮
        if (element.hasAttr("data-invite-relay-open")) {
            val action = parseAction(element, base)
            if (action != null) return listOf(InviteRelayNode.ActionBar(listOf(action)))
        }

        // 表单（含文件上传、人机验证、确认协议、批量库存勾选）
        if (tag == "form") {
            return listOf(InviteRelayNode.Form(parseRelayForm(element, base)))
        }

        // 折叠区（如举报无效邀请码）
        if (tag == "details") {
            val summary = element.children().firstOrNull { it.normalName() == "summary" }?.text()?.trim().orEmpty().ifBlank { "更多操作" }
            val bodyClone = element.clone().apply { children().firstOrNull { it.normalName() == "summary" }?.remove() }
            return listOf(InviteRelayNode.Details(summary, element.hasAttr("open"), parseChildren(bodyClone, base)))
        }

        // 图片举证
        if (tag == "img") {
            val src = AccountManagementParser.resolve(base, element.attr("src"))
            return if (src.isNotBlank()) listOf(InviteRelayNode.EvidenceImage(src, element.attr("alt"))) else emptyList()
        }

        // 超链接
        if (tag == "a" && element.hasAttr("href")) {
            val resolved = AccountManagementParser.resolve(base, element.attr("href"))
            if (resolved.isBlank()) return emptyList()
            val label = element.text().trim().ifBlank { element.selectFirst("img")?.attr("alt").orEmpty() }.ifBlank { "查看" }
            return listOf(InviteRelayNode.Link(label, resolved))
        }

        // 举报列表项 / 独立卡片条目
        if (tag == "li" && (element.parent()?.hasClass("invite-relay-reports") == true || element.select("form, [data-invite-relay-open], img, code").isNotEmpty())) {
            val title = element.children().firstOrNull { it.normalName() == "strong" }?.text()?.trim().orEmpty()
            val bodyClone = element.clone().apply {
                if (title.isNotBlank()) children().firstOrNull { it.normalName() == "strong" }?.remove()
            }
            val inner = parseChildren(bodyClone, base)
            return if (title.isNotBlank() || inner.isNotEmpty()) listOf(InviteRelayNode.Card(title, inner)) else emptyList()
        }

        // 标题
        if (tag.matches(Regex("h[1-6]"))) {
            val text = element.text().trim()
            return if (text.isNotBlank()) listOf(InviteRelayNode.Text(text, heading = tag.drop(1).toInt())) else emptyList()
        }

        // 特殊语义提示块
        if (element.hasClass("invite-relay-deadline")) {
            val text = element.text().trim()
            return if (text.isNotBlank()) listOf(InviteRelayNode.Text(text, style = InviteRelayNode.TextStyleKind.DEADLINE)) else emptyList()
        }
        if (element.hasClass("invite-relay-claim-warning")) {
            val text = element.text().trim()
            return if (text.isNotBlank()) listOf(InviteRelayNode.Text(text, style = InviteRelayNode.TextStyleKind.WARNING)) else emptyList()
        }
        if (element.hasClass("invite-relay-pending-summary") || element.hasClass("invite-relay-pending-badge")) {
            val text = element.text().trim()
            return if (text.isNotBlank()) listOf(InviteRelayNode.Text(text, style = InviteRelayNode.TextStyleKind.PENDING)) else emptyList()
        }

        // 代码展示块（如库存里的邀请码）
        if (tag == "code" && element.parent()?.normalName() != "p") {
            val code = element.text().trim()
            if (code.isNotBlank()) return listOf(InviteRelayNode.CodeBox(label = "邀请码", code = code))
        }

        // 纯文本段落
        if (tag in setOf("p", "small", "span", "strong", "div") &&
            element.select("form, button, a[href], img, details, ul, ol, input, [data-invite-relay-open], .invite-relay-code").isEmpty()
        ) {
            val text = element.text().trim()
            if (text.isBlank()) return emptyList()
            val kind = if (tag == "small" || element.hasClass("muted")) InviteRelayNode.TextStyleKind.MUTED else InviteRelayNode.TextStyleKind.BODY
            return listOf(InviteRelayNode.Text(text, style = kind))
        }

        return parseChildren(element, base)
    }

    private fun parseRelayForm(form: Element, base: String): InviteRelayForm {
        val parsed = AccountManagementParser.parseForm(form, base)
        val fileFields = form.select("input[type=file][name]").mapNotNull { input ->
            if (input.hasAttr("disabled") || input.parents().any { it !== form && it.hasAttr("hidden") }) return@mapNotNull null
            val labelEl = input.closest("label")
                ?: input.id().takeIf { it.isNotBlank() }?.let { id -> form.select("label").firstOrNull { it.attr("for") == id } }
            val label = labelEl?.clone()?.apply { select("input,small").remove() }?.text()?.trim().orEmpty()
                .ifBlank { input.attr("aria-label").trim() }
                .ifBlank { "上传图片" }
            InviteRelayFileField(
                name = input.attr("name").trim(),
                label = label,
                accept = input.attr("accept").trim().ifBlank { "image/*" },
                required = input.hasAttr("required"),
            )
        }

        // 与 AccountManagementParser.parseForm 相同的 submit 顺序提取按钮级确认属性
        val submitElements = form.select("input,select,textarea,button").filter { e ->
            if (e.parents().any { it !== form && it.hasAttr("hidden") }) return@filter false
            val tag = e.normalName()
            val type = when (tag) {
                "button" -> e.attr("type").ifBlank { "submit" }
                "input" -> e.attr("type").ifBlank { "text" }
                else -> ""
            }
            type == "submit"
        }
        val submitMeta = buildMap {
            submitElements.forEachIndexed { index, el ->
                val confirmMsg = el.attr("data-invite-relay-confirm").trim()
                val claimConfirm = el.attr("data-invite-relay-claim-confirm") == "1"
                if (confirmMsg.isNotBlank() || claimConfirm) {
                    put(index, InviteRelaySubmitMeta(confirmMsg, claimConfirm))
                }
            }
        }

        return InviteRelayForm(
            form = parsed,
            fileFields = fileFields,
            confirmMessage = form.attr("data-invite-relay-confirm").trim().ifBlank { parsed.confirmation },
            publishConfirm = form.attr("data-invite-relay-publish-confirm") == "1",
            submitMeta = submitMeta,
            isInventoryBatch = form.hasClass("invite-relay-inventory-batch"),
        )
    }

    private fun parseAction(el: Element, base: String): InviteRelayAction? {
        if (el.hasAttr("hidden")) return null
        val openUrl = el.attr("data-invite-relay-open").trim()
        val href = el.attr("href").trim()
        val rawUrl = openUrl.ifBlank { href }
        if (rawUrl.isBlank()) return null
        val resolved = AccountManagementParser.resolve(base, rawUrl).ifBlank { Endpoints.abs(rawUrl) }
        val label = el.text().trim()
        if (label.isBlank()) return null
        val style = when {
            el.hasClass("invite-relay-handoff-primary") -> InviteRelayActionStyle.HANDOFF_PRIMARY
            el.hasClass("invite-relay-primary") -> InviteRelayActionStyle.PRIMARY
            el.hasClass("invite-relay-manage") -> InviteRelayActionStyle.MANAGE
            else -> InviteRelayActionStyle.DEFAULT
        }
        return InviteRelayAction(
            label = label,
            url = resolved,
            style = style,
            opensDialog = openUrl.isNotBlank(),
        )
    }
}
