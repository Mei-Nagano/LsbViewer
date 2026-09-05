package sb.linux.client.data

/** 一次称号抽取返回的结果页。 */
data class GachaPullResult(
    val title: String = "抽取结果",
    val items: List<GachaPullResultItem> = emptyList(),
)

/** 抽取结果中的单种称号及数量。 */
data class GachaPullResultItem(
    val badge: TitleBadge,
    val quantity: Int = 1,
    val isNew: Boolean = false,
    val description: String = "",
)
