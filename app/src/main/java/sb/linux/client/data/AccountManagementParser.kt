package sb.linux.client.data

import org.jsoup.nodes.Element
import org.jsoup.nodes.TextNode
import sb.linux.client.domain.account.*
import java.net.URI

/** 保留源站内容顺序，业务边界由页面主体选择器决定。 */
internal object AccountManagementParser {
    fun parse(response: LsbClient.Resp): AccountManagementPage {
        check(!response.url.contains("/login")) { "登录已失效，请重新登录" }
        val doc = SourceHtmlSupport.sourceDocument(response.html)
        val root = doc.selectFirst(
            ".user-privacy-panel, .community-wallet-page, .community-activity-page, " +
                ".sidebar-promotions-page, .gacha-profile-page, .user-block-page, " +
                ".gacha-center-page, .gacha-page, .gacha-operation-page, .gacha-market-page, .gacha-result, .invite-center-page",
        ) ?: doc.selectFirst(".forum-main > .main-panel, .main-panel")
            ?: error("未解析到账号管理内容，源站结构可能已变化")
        val copy = root.clone()
        val header = root.selectFirst(".gacha-center-header")?.let {
            AccountTitleHeader(it.selectFirst(".gacha-header-title")?.text().orEmpty(),
                it.selectFirst("p")?.text().orEmpty(), it.selectFirst(".gacha-center-stat")?.text().orEmpty())
        }
        val sections = root.select(".gacha-internal-nav a[href]").map {
            AccountNode.Link(it.text(), resolve(response.url, it.attr("href")))
        }
        val ownerCard = doc.selectFirst(".user-card:has(a.user-name)")
        val titleCollection = if (root.hasClass("gacha-profile-page")) TitleCollectionSummary(
            total = root.selectFirst(".gacha-center-stat, .gacha-profile-collection-head > span")?.text().orEmpty(),
            equipped = ownerCard?.let { SourceHtmlSupport.gachaTitleOf(it)?.name ?: "未佩戴" }.orEmpty(),
        ) else null
        if (sections.isNotEmpty()) copy.select(".gacha-internal-nav").remove()
        if (header != null) copy.select(".gacha-center-header").remove()
        if (titleCollection != null) copy.select(".gacha-profile-collection-head").remove()
        if (copy.selectFirst("#gachaGiftModal") == null) doc.selectFirst("#gachaGiftModal")?.let { copy.appendChild(it.clone()) }
        copy.select("script,style,svg,.breadcrumb,.profile-toolbar,.sidebar,.mobile-forum-strip,.topic-toolbar,[data-promotion-preview]").remove()
        val materials = GachaAccountNodesParser.materials(copy, response.url)
        if (materials != null) copy.select(".gacha-forge-panel,.gacha-recycle-panel,.gacha-operation-section").remove()
        val nodes = AccountLayoutParser.page(root, response.url) { children(it, response.url) }
            ?: (children(copy, response.url) + listOfNotNull(materials))
        check(nodes.isNotEmpty()) { "源站未提供可显示的内容" }
        return AccountManagementPage(response.url, nodes, titleCollection, sections, header)
    }

    private fun children(element: Element, base: String): List<AccountNode> = element.childNodes().flatMap { node ->
        when (node) {
            is Element -> parseNode(node, base)
            is TextNode -> node.text().trim().takeIf(String::isNotBlank)?.let { listOf(AccountNode.Text(it)) }.orEmpty()
            else -> emptyList()
        }
    }

    private fun parseNode(element: Element, base: String): List<AccountNode> {
        val tag = element.normalName()
        if (element.hasAttr("hidden") || element.attr("aria-hidden") == "true" ||
            Regex("display\\s*:\\s*none", RegexOption.IGNORE_CASE).containsMatchIn(element.attr("style"))) return emptyList()
        GachaAccountNodesParser.parse(element, base) { children(it, base) }?.let { return it }
        if (tag == "form") return listOf(AccountNode.Form(parseForm(element, base)))
        if (tag == "button" && element.attr("onclick").contains("gachaGiftModalOpen(")) {
            if (element.hasAttr("disabled")) return listOf(AccountNode.Text("${element.text()}（当前不可用）"))
            return listOf(GachaAccountFormParser.gift(element, base)?.let { AccountNode.Gift(it) }
                ?: AccountNode.Text("暂时无法加载赠送表单，请刷新后重试"))
        }
        if (tag == "a" && element.hasAttr("href")) {
            val url = resolve(base, element.attr("href"))
            if (url.isBlank()) return emptyList()
            val label = element.text().ifBlank { element.selectFirst("img")?.attr("alt").orEmpty() }
            return listOf(AccountNode.Link(label.ifBlank { "查看" }, url))
        }
        if (tag == "img") {
            val url = resolve(base, element.attr("src"))
            return if (url.isBlank()) emptyList() else listOf(AccountNode.Image(url, element.attr("alt")))
        }
        if (tag in setOf("input", "select", "textarea", "button", "canvas", "svg", "script", "style")) return emptyList()
        if (tag == "progress" || element.attr("role") == "progressbar") {
            val max = element.attr(if (tag == "progress") "max" else "aria-valuemax").toFloatOrNull() ?: 1f
            val value = element.attr(if (tag == "progress") "value" else "aria-valuenow").toFloatOrNull() ?: 0f
            return listOf(AccountNode.Progress((value / max.coerceAtLeast(1f)).coerceIn(0f, 1f), element.attr("aria-label")))
        }
        if (element.hasClass("community-activity-card")) {
            return listOf(CommunityActivityParser.parseCard(element))
        }
        if (element.hasClass("community-activity-guide")) {
            val title = element.selectFirst("h2")?.text().orEmpty().ifBlank { "活跃度提升指南" }
            val content = element.clone().apply { select("h2").remove() }
            return listOf(AccountNode.Details(title, children(content, base)))
        }
        if (tag == "h1") return emptyList() // 大标题由公共设置页 Header 绘制。
        if (tag.matches(Regex("h[2-6]"))) return listOf(AccountNode.Text(element.text(), tag.drop(1).toInt()))
        if (tag == "tr") {
            return listOf(AccountNode.Group(element.children().flatMap { cell -> children(cell, base) }))
        }
        if (tag in setOf("p", "dt", "dd", "small", "summary") && element.select("a,form,progress,img,.gacha-title-badge").isEmpty()) {
            return element.text().takeIf(String::isNotBlank)?.let { listOf(AccountNode.Text(it, if (tag == "dt") 4 else 0)) }.orEmpty()
        }
        if (element.children().isEmpty()) return element.text().takeIf(String::isNotBlank)?.let { listOf(AccountNode.Text(it)) }.orEmpty()
        if (element.children().all { it.normalName() in setOf("span", "strong", "b", "em", "i", "br") } &&
            element.select("a,form,input,button,progress,[role=progressbar],.gacha-title-badge").isEmpty()) {
            return element.text().takeIf(String::isNotBlank)?.let { listOf(AccountNode.Text(it)) }.orEmpty()
        }
        val content = children(element, base)
        if (content.isEmpty()) return emptyList()
        val group = tag in setOf("section", "li", "details", "fieldset") || element.hasClass("card") ||
            element.hasClass("sidebar-promotions-order") || element.hasClass("user-block-item")
        return if (group) listOf(AccountNode.Group(content)) else content
    }

    internal fun parseForm(form: Element, base: String): AccountForm {
        val fields = mutableListOf<AccountField>()
        val hidden = mutableListOf<Pair<String, String>>()
        val buttons = mutableListOf<AccountSubmit>()
        val controls = form.select("input,select,textarea,button").toMutableList()
        if (form.id().isNotBlank()) {
            var root = form
            while (root.parent() != null) root = root.parent()!!
            root.select("input,select,textarea,button").filter { it.attr("form") == form.id() }.forEach { if (it !in controls) controls += it }
        }
        controls.forEach { e ->
            if (e.parents().any { it !== form && it.hasAttr("hidden") }) return@forEach
            val tag = e.normalName()
            val type = when (tag) { "textarea" -> "textarea"; "select" -> "select"; "button" -> e.attr("type").ifBlank { "submit" }; else -> e.attr("type").ifBlank { "text" } }
            val name = e.attr("name")
            val value = if (tag == "textarea") e.wholeText() else e.attr("value")
            val disabled = e.hasAttr("disabled") || e.parents().any { it.normalName() == "fieldset" && it.hasAttr("disabled") }
            if (type == "hidden") { if (name.isNotBlank() && !disabled) hidden += name to value; return@forEach }
            if (type == "submit") {
                buttons += AccountSubmit(e.text().ifBlank { value }.ifBlank { "提交" }, name, value, !disabled,
                    action = resolve(base, e.attr("formaction")), method = e.attr("formmethod").lowercase())
                return@forEach
            }
            if (type in setOf("button", "reset", "range") || (name.isBlank() && !e.hasAttr("data-sidebar-promotions-image-input"))) return@forEach
            val labelElement = e.closest("label") ?: e.id().takeIf(String::isNotBlank)?.let { id -> form.select("label").firstOrNull { it.attr("for") == id } }
            val label = labelElement?.clone()?.apply { select("input,select,textarea,button,small,span.help").remove() }?.text()
                ?.takeIf(String::isNotBlank) ?: e.attr("aria-label").ifBlank { e.attr("placeholder") }.ifBlank { name }
            fields += AccountField(
                name = name, type = type, label = label.ifBlank { "广告图片" }, value = value,
                checked = e.hasAttr("checked"), disabled = disabled, required = e.hasAttr("required"),
                maxLength = e.attr("maxlength").toIntOrNull()?.takeIf { it > 0 } ?: Int.MAX_VALUE,
                min = e.attr("min"), max = e.attr("max"), placeholder = e.attr("placeholder"),
                options = e.select("option").map { AccountOption(it.attr("value").ifBlank { it.text() }, it.text(), it.hasAttr("selected"), it.hasAttr("disabled")) },
                multiple = e.hasAttr("multiple"),
                visibilityControl = labelElement?.hasClass("user-privacy-item") == true,
            )
        }
        val notes = form.select("p,small,.help,.muted,.invite-center-help,[data-gacha-gift-recipient-tip]")
            .filter { !it.hasClass("user-privacy-state") && !it.hasAttr("hidden") && it.parents().none { p -> p !== form && p.hasAttr("hidden") } }
            .map(Element::text).filter(String::isNotBlank).distinct()
        return AccountForm(
            id = form.id().ifBlank { "${form.siblingIndex()}:${form.attr("action")}:${hidden.hashCode()}" },
            action = resolve(base, form.attr("action").ifBlank { base }), method = form.attr("method").ifBlank { "get" }.lowercase(),
            hidden = hidden, fields = fields, buttons = buttons, notes = notes,
            recipientLookup = resolve(base, form.attr("data-gacha-recipient-url")),
            imageUpload = resolve(base, form.selectFirst("[data-sidebar-promotions-image-tools]")?.attr("data-upload-url").orEmpty()),
            confirmation = form.attr("data-confirm"),
            verification = sb.linux.client.data.parser.LoginVerificationParser.parse(form.outerHtml(), base),
        )
    }

    internal fun resolve(base: String, raw: String): String = runCatching {
        if (raw.isBlank() || raw.startsWith("#")) return ""
        URI(base).resolve(raw).toString().takeIf { it.startsWith("https://") || it.startsWith("http://") }.orEmpty()
    }.getOrDefault("")
}
