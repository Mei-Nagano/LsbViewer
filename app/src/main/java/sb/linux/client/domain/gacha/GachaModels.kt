package sb.linux.client.domain.gacha

import sb.linux.client.domain.topic.TitleBadge

data class GachaProfile(val stat: String, val titles: List<GachaTitleItem> = emptyList())

data class GachaTitleItem(
    val badge: TitleBadge,
    val equipped: Boolean = false,
    val count: Int = 1,
    val actions: List<GachaAction> = emptyList(),
)

data class GachaAction(
    val label: String,
    val action: String,
    val fields: Map<String, String>,
    val enabled: Boolean = true,
    val cost: Int = 0,
)

data class GachaCenter(
    val pointsText: String = "",
    val statsText: String = "",
    val pullActions: List<GachaAction> = emptyList(),
    val pool: List<GachaPoolRow> = emptyList(),
    val allTitles: List<TitleBadge> = emptyList(),
    val news: List<String> = emptyList(),
)

data class GachaPoolRow(val rarity: String, val countText: String, val rateText: String)

data class GachaMarketPage(
    val summary: String = "",
    val note: String = "",
    val listings: List<GachaMarketListing> = emptyList(),
    val currentPage: Int = 1,
    val lastPage: Int = 1,
)

data class GachaMarketListing(
    val badge: TitleBadge,
    val price: Int = 0,
    val available: Int = 1,
    val timeLeft: String = "",
    val action: GachaAction,
)

data class GachaOperationPage(
    val title: String = "",
    val notes: List<String> = emptyList(),
    val forms: List<GachaOperationForm> = emptyList(),
    val records: List<String> = emptyList(),
    val links: List<Pair<String, String>> = emptyList(),
)

data class GachaOperationForm(
    val label: String,
    val action: String,
    val hiddenFields: List<Pair<String, String>> = emptyList(),
    val fields: List<GachaFormField> = emptyList(),
    val enabled: Boolean = true,
    val minSelections: Int = 0,
)

data class GachaFormField(
    val name: String,
    val label: String,
    val groupLabel: String = "",
    val type: String = "text",
    val value: String = "",
    val placeholder: String = "",
    val min: String = "",
    val max: String = "",
    val required: Boolean = false,
    val checked: Boolean = false,
    val options: List<GachaFormOption> = emptyList(),
    val maxLength: Int = Int.MAX_VALUE,
    val multiple: Boolean = false,
)

data class GachaFormOption(
    val value: String,
    val label: String,
    val selected: Boolean = false,
    val disabled: Boolean = false,
)

data class GachaPullResult(
    val title: String = "抽取结果",
    val items: List<GachaPullResultItem> = emptyList(),
)

data class GachaPullResultItem(
    val badge: TitleBadge,
    val ownedCount: Int = 0,
    val isNew: Boolean = false,
    val description: String = "",
)

data class GachaPullReceipt(val result: GachaPullResult, val cost: Int)
