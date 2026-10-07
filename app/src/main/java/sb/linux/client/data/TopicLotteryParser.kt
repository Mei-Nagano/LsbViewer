package sb.linux.client.data
import org.jsoup.nodes.Document
import sb.linux.client.domain.topic.LotteryPanel
import sb.linux.client.domain.topic.LotteryWinner

internal object TopicLotteryParser {
    fun parse(document: org.jsoup.nodes.Document): LotteryPanel? {
        return document.selectFirst(".community-lottery-card, .community-lottery-topic-panel")?.let { p ->
            val joinForm = p.parents().firstOrNull { it.tagName() == "form" }
                ?: p.selectFirst("form[action]")
            val prizeLis = p.select(".community-lottery-prizes > li")
            val winnersLis = p.select(".community-lottery-winners li")
            LotteryPanel(
                status = p.selectFirst(".community-lottery-status-pill")?.text()?.trim()
                    ?: document.selectFirst(".community-lottery-title-status")?.text()?.trim() ?: "",
                note = p.selectFirst("header span")?.text()?.trim() ?: "",
                participants = p.selectFirst(".community-lottery-card-side em")?.text()?.trim() ?: "",
                prizes = if (prizeLis.isNotEmpty()) prizeLis.map { li ->
                    val name = li.selectFirst("strong")?.text()?.trim() ?: ""
                    val desc = li.selectFirst("span")?.text()?.trim() ?: ""
                    listOf(name, desc).filter { it.isNotBlank() }.joinToString(" · ")
                }.filter { it.isNotBlank() } else p.select(".community-lottery-readonly-prize, .community-lottery-prize-row")
                    .map { it.text().trim() }.filter { it.isNotBlank() },
                condition = p.selectFirst(".community-lottery-condition")?.text()?.trim() ?: "",
                result = p.selectFirst(".community-lottery-result")?.text()?.trim() ?: "",
                winners = if (winnersLis.isNotEmpty()) winnersLis.mapNotNull { li ->
                    val u = li.selectFirst("a")?.text()?.trim() ?: return@mapNotNull null
                    // 奖品名：span 文本去掉内嵌 <code>（本人条目形如 "13.14额度兑换码 · 兑换码：<code>…</code>"）
                    val prize = li.selectFirst("span")?.let { s ->
                        val clone = s.clone()
                        clone.select("code").remove()
                        clone.text().trim().replace(Regex("""\s*·?\s*兑换码[:：]?\s*$"""), "").trim()
                    } ?: ""
                    // 兑换码：code 文本形如 "奖品名<TAB>实际兑换码"；text() 会把 TAB 归一为空格，
                    // 用 wholeText() 保留原始分隔后取最后一个长 token
                    val codeRaw = li.selectFirst("code")?.wholeText()?.trim() ?: ""
                    val code = codeRaw.split(Regex("\\s+")).lastOrNull { it.length >= 8 } ?: codeRaw
                    LotteryWinner(user = u, prize = prize, code = code)
                } else emptyList(),
                rules = p.selectFirst(".community-lottery-rules, .community-lottery-rule-note")?.text()?.trim() ?: "",
                joinAction = joinForm?.attr("action") ?: "",
                joinFields = SourceHtmlSupport.hiddenFields(joinForm),
                options = p.select("input[type=radio], .community-lottery-topic-option").mapNotNull { el ->
                    if (el.tagName() == "input") {
                        val v = el.attr("value")
                        if (v.isNotBlank()) v to (el.parent()?.text()?.trim() ?: v) else null
                    } else {
                        val v = el.attr("data-value")
                        if (v.isNotBlank()) v to el.text().trim() else null
                    }
                },
                joinedText = p.selectFirst(".community-lottery-pending-notice, .community-lottery-pending-reply")?.text()?.trim() ?: "",
            )
        }
    }
}
