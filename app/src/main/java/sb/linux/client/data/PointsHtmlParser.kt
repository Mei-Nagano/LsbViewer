package sb.linux.client.data
import sb.linux.client.domain.community.PointRow

object PointsHtmlParser {
    fun parsePointRows(html: String): List<PointRow> {
        val d = SourceHtmlSupport.sourceDocument(html)
        return d.select("li.points-rewards-detail").map { li ->
            val raw = li.selectFirst(".points-rewards-change-value b")?.text() ?: "0"
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
            )
        }
    }

}
