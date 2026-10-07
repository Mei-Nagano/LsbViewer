package sb.linux.client.data

import sb.linux.client.domain.community.ReportForm

object ReportHtmlParser {
    internal fun parseReport(html: String): ReportFormParseResult {
        val d = SourceHtmlSupport.sourceDocument(html)
        val form = d.selectFirst("main form")
        val reasons = d.select("input[name=reason_type]").mapNotNull { r ->
            val v = r.attr("value")
            val label = r.parent()?.text()?.trim() ?: v
            if (v.isBlank()) null else v to label
        }
        return ReportFormParseResult(
            form = ReportForm(reasons),
            targetType = form?.selectFirst("input[name=target_type]")?.attr("value")
                ?: Regex("""type=(\w+)""").find(d.location() ?: "")?.groupValues?.get(1) ?: "reply",
            targetId = form?.selectFirst("input[name=target_id]")?.attr("value")
                ?: Regex("""/content_report/(\d+)""").find(html)?.groupValues?.get(1) ?: "",
        )
    }

}
