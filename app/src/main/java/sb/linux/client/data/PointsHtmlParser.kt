package sb.linux.client.data
import sb.linux.client.domain.community.PointRow

object PointsHtmlParser {
    fun parsePointRows(html: String): List<PointRow> =
        parsePointRows(SourceHtmlSupport.sourceDocument(html))

    /** 分页遍历已经解析过同一份文档，直接复用以免整页 HTML 再解一次。 */
    fun parsePointRows(d: org.jsoup.nodes.Document): List<PointRow> {
        return d.select("li.points-rewards-detail").map { li ->
            val raw = li.selectFirst(".points-rewards-change-value b")?.text().orEmpty()
                .replace(",", "").replace("−", "-").trim()
            check(raw.trimStart('+', '-').toLongOrNull() != null) { "积分金额无法解析，请刷新后重试" }
            val positive = li.selectFirst(".points-rewards-change-value")?.classNames()?.contains("positive") == true
                || raw.trimStart().startsWith("+")
            PointRow(
                timeText = li.selectFirst("time")?.attr("datetime")?.take(16)?.replace("T", " ")
                    ?: li.selectFirst(".points-rewards-time")?.text() ?: "",
                reason = li.selectFirst(".points-rewards-reason")?.text() ?: "",
                change = raw.trim().trimStart('+', '-'),
                positive = positive,
                timestamp = runCatching { java.time.OffsetDateTime.parse(li.selectFirst("time")?.attr("datetime")).toInstant().toEpochMilli() }.getOrDefault(0L),
                topicId = SourceHtmlSupport.idFrom(li.selectFirst("a[href^=/topic/]")?.attr("href")),
                eventReason = li.selectFirst(".points-rewards-reason")?.clone()?.also { it.select("a[href*=/topic/]").remove() }?.text().orEmpty(),
            )
        }
    }

}
