package sb.linux.client.data

import org.jsoup.nodes.Element
import sb.linux.client.domain.account.*

/** 保留截图中卡片、概率、徽章和材料的关系，提交继续使用页面原始表单。 */
internal object GachaAccountNodesParser {
    fun parse(e: Element, base: String, children: (Element) -> List<AccountNode>): List<AccountNode>? {
        fun isClass(vararg names: String) = names.any(e::hasClass)
        if (isClass("gacha-profile-item")) {
            val badge = SourceHtmlSupport.gachaTitleOf(e) ?: return null
            val giftButton = e.select("button[onclick]").firstOrNull { it.attr("onclick").contains("gachaGiftModalOpen(") }
            return listOf(AccountNode.OwnedTitle(badge,
                e.select(".gacha-profile-meta,.gacha-ur-instance-list").map { it.text() }.filter(String::isNotBlank),
                e.selectFirst(".gacha-profile-desc,p")?.text().orEmpty(),
                e.select("form").filterNot { it.selectFirst("[name=recipient]") != null }.map { AccountManagementParser.parseForm(it, base) },
                giftButton?.takeUnless { it.hasAttr("disabled") }?.let { GachaAccountFormParser.gift(it, base) }, e.hasClass("is-equipped")))
        }
        if (isClass("gacha-actions")) {
            val forms = e.select("form").map { AccountManagementParser.parseForm(it, base) }
            if (forms.size == 3 && forms.all { it.fields.isEmpty() }) return listOf(AccountNode.DrawActions(forms))
        }
        if (isClass("gacha-title-badge", "gacha-title-post-badge"))
            return SourceHtmlSupport.gachaTitleOf(e)?.let { listOf(AccountNode.Badge(it)) }.orEmpty()
        if (isClass("gacha-good-news")) return listOf(AccountNode.News(e.select(".gacha-good-news-item").map { it.text() }.distinct()))
        if (isClass("gacha-market-note")) return listOf(AccountNode.Notice(e.text()))
        if (isClass("gacha-recipe-materials")) return listOf(AccountNode.Pills(e.children().map { it.text() to !it.hasClass("is-missing") }))
        if (isClass("gacha-ur-instance-list")) return listOf(AccountNode.Pills(e.children().map { it.text() to true }))
        if (isClass("gacha-profile-meta")) return listOfNotNull(e.ownText().takeIf(String::isNotBlank)?.let { AccountNode.Text(it) },
            e.children().takeIf { it.isNotEmpty() }?.let { AccountNode.Pills(it.map { child -> child.text() to !child.hasClass("gacha-status-expired") }) })
        if (isClass("gacha-pool-rarity") && e.selectFirst(".gacha-pool-rarity-label")?.text()?.trim().equals("UR", true)) return emptyList()
        if (isClass("gacha-pool-rarity")) return listOf(AccountNode.RarityStat(
            e.selectFirst(".gacha-pool-rarity-label")?.text().orEmpty(),
            e.selectFirst(".gacha-pool-rarity-count")?.text().orEmpty(),
            e.selectFirst(".gacha-pool-rarity-rate")?.text().orEmpty(), e.attr("style")))
        if (isClass("gacha-all-grid")) return listOf(AccountNode.Badges(e.select(".gacha-title-badge").mapNotNull(SourceHtmlSupport::gachaTitleOf)))
        if (isClass("gacha-profile-titles", "gacha-profile-grid", "gacha-pool-grid", "gacha-recipe-grid", "gacha-market-grid", "gacha-operation-grid", "gacha-pull-10-grid", "gacha-pull-100-grid"))
            return listOf(AccountNode.Grid(children(e)))
        if (isClass("gacha-result-card", "gacha-pull-10-item")) {
            val title = sb.linux.client.domain.topic.TitleBadge(
                name = e.selectFirst(".gacha-result-name,.gacha-pull-10-name")?.text().orEmpty(),
                icon = e.selectFirst(".gacha-result-icon,.gacha-pull-10-icon")?.text().orEmpty(),
                rarity = e.selectFirst(".gacha-result-rarity,.gacha-pull-10-rarity")?.text().orEmpty(), sourceStyle = e.attr("style"))
            return listOf(AccountNode.TitleCard(listOf(AccountNode.Badge(title)) +
                e.select(".gacha-result-desc,.gacha-result-new,.gacha-pull-10-new,.gacha-pull-10-quantity")
                    .map { AccountNode.Text(it.text()) }))
        }
        if (isClass("gacha-actions", "gacha-market-nav", "gacha-profile-action-main", "gacha-profile-action"))
            return listOf(AccountNode.Actions(children(e).flatMap { if (it is AccountNode.Actions) it.children else listOf(it) }))
        if (isClass("gacha-pool-section", "gacha-all-titles")) {
            val copy = e.clone().apply { select("h2,h3").remove() }
            return listOf(AccountNode.Section(e.selectFirst("h2,h3")?.text().orEmpty(), "", children(copy)))
        }
        if (isClass("gacha-profile-item", "gacha-recipe-card", "gacha-market-card", "gacha-operation-card"))
            return listOf(AccountNode.TitleCard(children(e), e.hasClass("is-equipped")))
        if (isClass("gacha-profile-collection-head", "gacha-section-head", "gacha-market-head", "gacha-recipe-head"))
            return listOf(AccountNode.Text(e.selectFirst("strong,h2,h3")?.text().orEmpty(), 3),
                AccountNode.Text(e.selectFirst("span")?.text().orEmpty())).filterNot { it.value.isBlank() }
        if (isClass("gacha-sub-stats", "gacha-market-meta")) return listOf(AccountNode.Text(e.text()))
        return null
    }

    fun materials(root: Element, base: String): AccountNode.TitleMaterials? {
        val buttons = root.select("[data-gacha-forge-button], [data-gacha-recycle-button]")
        if (buttons.isEmpty()) return null
        val actions = buttons.mapNotNull { button ->
            val form = button.closest("form") ?: return@mapNotNull null
            TitleMaterialAction(AccountManagementParser.parseForm(form, base), button.attr("data-gacha-forge-source"),
                button.attr("data-gacha-forge-target"), button.attr("data-gacha-forge-cost").toIntOrNull() ?: 1,
                button.attr("data-gacha-recycle-price").toLongOrNull()
                    ?: Regex("\\d+").find(button.selectFirst("small")?.text().orEmpty())?.value?.toLongOrNull() ?: 0L,
                form.selectFirst("[data-gacha-forge-count]")?.attr("name").orEmpty())
        }
        val items = root.select(".gacha-operation-card").mapNotNull { card ->
            val badge = SourceHtmlSupport.gachaTitleOf(card) ?: return@mapNotNull null
            val check = card.selectFirst("input[type=checkbox][name]") ?: return@mapNotNull null
            val quantity = card.selectFirst("input[type=number][name]") ?: return@mapNotNull null
            val formId = check.attr("form").ifBlank { quantity.attr("form") }
            val action = actions.firstOrNull { it.form.id == formId || it.source.equals(badge.rarity, true) }
                ?: return@mapNotNull null
            val selector = action.form.fields.firstOrNull { it.name == check.attr("name") && it.value == check.attr("value") }
                ?: return@mapNotNull null
            val amount = action.form.fields.firstOrNull { it.name == quantity.attr("name") } ?: return@mapNotNull null
            TitleMaterial(badge, card.selectFirst(".gacha-operation-desc,p")?.text().orEmpty(),
                card.selectFirst(".gacha-operation-meta")?.text().orEmpty(), selector, amount, action.form.id)
        }
        return AccountNode.TitleMaterials(actions, items)
    }
}
