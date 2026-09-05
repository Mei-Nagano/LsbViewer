package sb.linux.client.data

/** 解析单抽、十连抽和百连抽的源站结果页。 */
internal object GachaPullResultParser {
    private const val RESULT_CARDS =
        ".gacha-result-card, .gacha-pull-10-item, .gacha-pull-100-item"

    fun parse(html: String): GachaPullResult {
        val document = HtmlParser.sourceDocument(html)
        val root = document.selectFirst(".gacha-result") ?: return GachaPullResult()
        val items = root.select(RESULT_CARDS).mapNotNull { card ->
            val name = card.selectFirst(".gacha-result-name, .gacha-pull-10-name")
                ?.text()?.trim().orEmpty()
            if (name.isBlank()) return@mapNotNull null
            val rarity = card.selectFirst(".gacha-result-rarity, .gacha-pull-10-rarity")
                ?.text()?.trim().orEmpty()
            val ownedCountText = card.selectFirst(".gacha-result-quantity, .gacha-pull-10-quantity")
                ?.text().orEmpty()
            GachaPullResultItem(
                badge = TitleBadge(
                    icon = card.selectFirst(".gacha-result-icon, .gacha-pull-10-icon")
                        ?.text()?.trim().orEmpty(),
                    name = name,
                    rarity = rarity,
                    serial = card.selectFirst(".gacha-title-serial, .gacha-result-serial")
                        ?.text()?.trim().orEmpty(),
                ),
                // quantity 节点展示抽取后的当前持有量，不代表本次获得了多枚。
                ownedCount = SourceFormProtocol.parseInteger(ownedCountText)?.coerceAtLeast(0) ?: 0,
                isNew = card.selectFirst(".gacha-result-new, .gacha-pull-10-new") != null,
                description = card.selectFirst(".gacha-result-desc")?.text()?.trim().orEmpty(),
            )
        }
        return GachaPullResult(
            title = root.selectFirst("h1, h2, h3")?.text()?.trim().orEmpty().ifBlank { "抽取结果" },
            items = items,
        )
    }
}
