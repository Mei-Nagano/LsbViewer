package sb.linux.client.data

/** 源站称号系统徽章（gacha-title-badge：图标 + 名称 + 稀有度）。 */
data class TitleBadge(
    val icon: String = "",     // emoji 图标，如 🐉
    val name: String = "",     // 称号名，如 传说之龙
    val rarity: String = "",   // 稀有度，如 SSR（取自类名 gacha-title-<x>）
    val serial: String = "",   // UR 限定序列号，如 058
)

/** 我的称号（/gacha_profile）：称号收藏列表。 */
data class GachaProfile(
    val stat: String,
    val titles: List<GachaTitleItem> = emptyList(),
)

/** 收藏的单个称号条目：徽章 + 装备状态 + 可执行操作（装备/卸下/赠送等表单）。 */
data class GachaTitleItem(
    val badge: TitleBadge,
    val equipped: Boolean = false,
    val count: Int = 1,
    val actions: List<GachaAction> = emptyList(),
)

/** 称号操作：源站表单（action + 隐藏字段 + 按钮文本），提交后由页面反馈结果。 */
data class GachaAction(
    val label: String,
    val action: String,
    val fields: Map<String, String>,
    val enabled: Boolean = true,
    val cost: Int = 0,
)

/** 称号抽取中心（/gacha）。 */
data class GachaCenter(
    val pointsText: String = "",
    val statsText: String = "",
    val pullActions: List<GachaAction> = emptyList(),
    val pool: List<GachaPoolRow> = emptyList(),
    val allTitles: List<TitleBadge> = emptyList(),
    val news: List<String> = emptyList(),
)

data class GachaPoolRow(
    val rarity: String,
    val countText: String,
    val rateText: String,
)

/** 称号交易市场当前页（/gacha_market）。 */
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

/** 称号系统的动态操作页。字段直接来自源站表单，兼容熔炼、回收、UR 合成和市场发布。 */
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
