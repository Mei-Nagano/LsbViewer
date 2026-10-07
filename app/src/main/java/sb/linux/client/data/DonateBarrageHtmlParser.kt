package sb.linux.client.data
import sb.linux.client.domain.topic.DanmakuItem

object DonateBarrageHtmlParser {
    fun parseDonateBarrage(areaHtml: String): List<DanmakuItem> = runCatching {
        SourceHtmlSupport.sourceDocument(areaHtml).select(".donate-barrage-item").mapNotNull { el ->
            val user = el.selectFirst(".donate-barrage-user")?.text()?.trim() ?: ""
            if (user.isBlank()) null else DanmakuItem(
                user = user,
                amount = el.selectFirst(".donate-barrage-amount")?.text()?.trim() ?: "",
                action = el.selectFirst(".donate-barrage-action")?.text()?.trim() ?: "打赏",
                avatarUrl = SourceHtmlSupport.absUrl(el.selectFirst(".donate-barrage-avatar")?.attr("src") ?: ""),
            )
        }
    }.getOrDefault(emptyList())}
