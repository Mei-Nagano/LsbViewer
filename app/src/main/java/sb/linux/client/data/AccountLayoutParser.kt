package sb.linux.client.data

import org.jsoup.nodes.Element
import sb.linux.client.domain.account.*

/** 来源字段先成组，再交给 M3 卡片；不从文字推断提交地址或动作值。 */
internal object AccountLayoutParser {
    fun page(root: Element, base: String, children: (Element) -> List<AccountNode>): List<AccountNode>? = when {
        root.selectFirst(".attachment-upload-storage") != null -> attachments(root, base)
        root.hasClass("sidebar-promotions-page") -> promotions(root, base, children)
        root.selectFirst(".invite-center-link") != null -> invitation(root, base, children)
        else -> null
    }

    private fun links(element: Element?, base: String): List<AccountNode.Link> = element?.select("a[href]")?.map {
        AccountNode.Link(it.text(), AccountManagementParser.resolve(base, it.attr("href")))
    }.orEmpty().filter { it.url.isNotBlank() }

    private fun attachments(root: Element, base: String): List<AccountNode> {
        val storage = root.selectFirst(".attachment-upload-storage")!!
        val expand = storage.selectFirst("form")?.let { AccountManagementParser.parseForm(it, base) }
        val meter = storage.selectFirst(".attachment-upload-storage-meter span")?.attr("style").orEmpty()
        val fraction = Regex("width\\s*:\\s*([\\d.]+)%").find(meter)?.groupValues?.get(1)?.toFloatOrNull()?.div(100) ?: 0f
        val summary = AccountNode.Storage(storage.selectFirst("h2")?.text().orEmpty(),
            storage.selectFirst(".attachment-upload-storage-head p")?.text().orEmpty(),
            storage.selectFirst(".attachment-upload-storage-head > strong")?.text().orEmpty(), fraction.coerceIn(0f, 1f),
            storage.select(".attachment-upload-storage-stats > div").map { it.selectFirst("span")?.text().orEmpty() to it.selectFirst("strong")?.text().orEmpty() },
            expand, links(storage.selectFirst("form"), base))
        val batch = root.selectFirst(".attachment-upload-batch-form")?.let { AccountManagementParser.parseForm(it, base) }
        val files = root.select("li.attachment-upload-detail").map { item ->
            val file = item.selectFirst(".attachment-upload-detail-file")
            val check = item.selectFirst("input[type=checkbox][name]")
            ManagedAttachment(
                item.selectFirst(".attachment-upload-detail-name")?.text().orEmpty().ifBlank { file?.text().orEmpty() },
                AccountManagementParser.resolve(base, file?.attr("href").orEmpty()),
                AccountManagementParser.resolve(base, item.selectFirst("img.attachment-upload-preview")?.attr("src").orEmpty()),
                item.selectFirst(".attachment-upload-detail-meta")?.text().orEmpty(),
                links(item.selectFirst(".attachment-upload-detail-references"), base),
                item.selectFirst(".attachment-upload-detail-references")?.clone()?.apply { select("a").remove() }?.text().orEmpty(),
                check?.let { AccountField(it.attr("name"), "checkbox", "选择", it.attr("value"), disabled = it.hasAttr("disabled")) },
                item.select("button,input[type=submit]").mapNotNull { button ->
                    if (button.attr("type") in setOf("button", "reset")) return@mapNotNull null
                    val sourceForm = if (button.attr("form").isNotBlank()) root.select("form[id]")
                        .firstOrNull { it.id() == button.attr("form") } else button.closest("form")
                    val parsed = sourceForm?.let { AccountManagementParser.parseForm(it, base) } ?: return@mapNotNull null
                    parsed.buttons.firstOrNull { it.name == button.attr("name") && it.value == button.attr("value") }
                        ?.let { AttachmentAction(parsed, it) }
                })
        }
        val filter = root.selectFirst(".attachment-upload-filter")
        return listOf(summary, AccountNode.Attachments(links(filter, base),
            AccountManagementParser.resolve(base, filter?.selectFirst("a.active")?.attr("href").orEmpty()), files, batch)) +
            root.select(".pagination-bar").flatMap { links(it, base) }
    }

    private fun promotions(root: Element, base: String, children: (Element) -> List<AccountNode>): List<AccountNode> {
        val hero = root.selectFirst(".sidebar-promotions-hero")
        val header = listOfNotNull(hero?.let { AccountNode.Section("广告位管理", it.selectFirst("p")?.text().orEmpty(), emptyList()) })
        val sections = root.select(".sidebar-promotions-orders").map { section ->
            val cards = section.select("ul > li").map { item ->
                if (item.hasClass("empty-state")) AccountNode.Text(item.text())
                else {
                    val content = item.selectFirst(".sidebar-promotions-placement-content")
                        ?: item.children().firstOrNull { it.normalName() == "div" && !it.hasClass("sidebar-promotions-order-actions") }
                    val preview = content?.selectFirst("a.sidebar-promotions-slot")
                    if (content == null) AccountNode.Group(children(item)) else AccountNode.Promotion(
                        content.children().firstOrNull { it.normalName() == "strong" }?.text().orEmpty(),
                        content.children().filter { it.normalName() == "span" }.joinToString(" · ") { it.text() },
                        AccountManagementParser.resolve(base, preview?.selectFirst("img")?.attr("src").orEmpty()),
                        AccountManagementParser.resolve(base, preview?.attr("href").orEmpty()),
                        links(item.selectFirst(".sidebar-promotions-order-actions"), base) +
                            item.select("form").map { AccountNode.Form(AccountManagementParser.parseForm(it, base)) },
                        preview?.text().orEmpty())
                }
            }
            AccountNode.Section(section.selectFirst("h2")?.text().orEmpty(),
                section.children().firstOrNull { it.normalName() == "p" }?.text().orEmpty(), cards)
        }
        // placement 详情中的真实购买／编辑表单继续进入原生表单。
        val remainder = root.clone().apply { select(".sidebar-promotions-hero,.sidebar-promotions-orders").remove() }
        return header + sections + children(remainder)
    }

    private fun invitation(root: Element, base: String, children: (Element) -> List<AccountNode>): List<AccountNode> {
        val link = root.selectFirst(".invite-center-link")!!
        val form = link.selectFirst(".invite-center-escrow-form")
        val card = AccountNode.Invitation(link.selectFirst("input[data-invite-center-link]")?.attr("value").orEmpty(),
            link.selectFirst("p.muted")?.text().orEmpty(), link.selectFirst(".invite-center-status-value")?.text().orEmpty(),
            link.select(".invite-center-link-status strong").getOrNull(1)?.text().orEmpty(),
            form?.attr("data-invite-center-escrow").orEmpty(), form?.attr("data-invite-center-cost").orEmpty(),
            form?.let { AccountManagementParser.parseForm(it, base).copy(confirmation = "确认调整邀请数量与托管积分？设为 0 会关闭邀请并退回未使用的积分。") },
            link.selectFirst(".invite-center-rule")?.text().orEmpty())
        val stats = root.select(".invite-center-stats > div").map { it.selectFirst("span")?.text().orEmpty() to it.selectFirst("strong")?.text().orEmpty() }
        val users = root.selectFirst(".invite-center-list")
        return listOf(card, AccountNode.Statistics(stats)) + listOfNotNull(users?.let {
            val body = it.clone().apply { select("h2").remove() }
            AccountNode.Section(it.selectFirst("h2")?.text().orEmpty(), "", children(body))
        })
    }
}
