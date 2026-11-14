package sb.linux.client.data

import org.jsoup.nodes.Document
import sb.linux.client.domain.topic.*

/** 聚宝盆和福利投票是独立源站面板，不通过正文文字猜测帖子类型。 */
internal object TopicSpecialPanelParser {
    fun treasure(document: Document): TreasurePoolPanel? {
        val card = document.selectFirst(".jubaopen-card") ?: return null
        return TreasurePoolPanel(
            title = card.selectFirst(".jubaopen-head strong")?.text().orEmpty().removePrefix("🪙").trim().ifBlank { "聚宝盆" },
            status = card.selectFirst(".jubaopen-head > span")?.text().orEmpty(),
            statistics = card.select(".jubaopen-stats > div").map {
                PanelStatistic(it.selectFirst("small")?.text().orEmpty(), it.selectFirst("b")?.text().orEmpty())
            },
            rules = (card.select("p") + card.children().filter { it.normalName() == "small" })
                .map { it.text() }.filter(String::isNotBlank),
        )
    }

    fun resource(document: Document): ResourceVotePanel? {
        val card = document.selectFirst(".resource_vote-card") ?: return null
        val form = card.selectFirst("form.resource_vote-form")
        val inputs = form?.select("input[type=radio][name]").orEmpty()
        val note = card.selectFirst(".resource_vote-status")?.text().orEmpty()
        val options = inputs.map { input ->
            TopicPollOption(input.attr("name"), input.attr("value"), input.closest("label")?.text().orEmpty(),
                input.hasAttr("checked"), input.hasAttr("disabled"))
        }
        val voted = options.any { it.selected } || listOf("已投票", "已投过", "你已投", "您已投").any { note.contains(it) }
        val submit = form?.selectFirst("button[type=submit]")
        val tallies = card.select(".resource_vote-tally")
        val available = tallies.firstOrNull()?.selectFirst("strong")?.text().orEmpty()
        val unavailable = tallies.lastOrNull()?.selectFirst("strong")?.text().orEmpty()
        val width = card.selectFirst(".resource_vote-bar > span")?.attr("style").orEmpty()
        val fraction = Regex("width\\s*:\\s*([\\d.]+)%").find(width)?.groupValues?.get(1)?.toFloatOrNull()?.div(100f)
            ?: 0f
        return ResourceVotePanel(
            poll = TopicPoll(
                title = card.selectFirst(".resource_vote-head strong")?.text().orEmpty().ifBlank { "资源 / 福利可用性" },
                note = note, action = form?.attr("action").orEmpty(), fields = form?.let(SourceHtmlSupport::hiddenFields).orEmpty(),
                options = options, maxChoices = 1, submitLabel = submit?.text().orEmpty().ifBlank { "提交投票" },
                submitName = submit?.attr("name").orEmpty(), submitValue = submit?.attr("value").orEmpty(),
                voted = voted, unavailableReason = if (form == null && !voted) note else "",
                participants = card.selectFirst(".resource_vote-head > span")?.text().orEmpty(),
                statusLabel = if (voted) "已投票" else "你的实际体验",
            ),
            available = available, unavailable = unavailable, fraction = fraction.coerceIn(0f, 1f),
            summary = card.selectFirst(".resource_vote-labels > strong")?.text().orEmpty(),
            recent = card.select(".resource_vote-recent-item").map {
                ResourceVoteRecord(it.selectFirst(".resource_vote-recent-name")?.text().orEmpty(),
                    it.selectFirst("time")?.text().orEmpty(), it.selectFirst("strong")?.text().orEmpty())
            },
        )
    }
}
