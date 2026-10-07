package sb.linux.client.data
import org.jsoup.nodes.Document
import sb.linux.client.domain.topic.CardOrder
import sb.linux.client.domain.topic.VirtualCard

internal object TopicVirtualCardParser {
    fun parse(document: org.jsoup.nodes.Document): VirtualCard? {
        return document.selectFirst(".virtual-card-box.virtual-card-card")?.let { c ->
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
                buyFields = SourceHtmlSupport.hiddenFields(buyForm),
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
    }
}
