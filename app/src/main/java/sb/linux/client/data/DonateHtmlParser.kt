package sb.linux.client.data

import sb.linux.client.domain.topic.DonationForm

object DonateHtmlParser {
    internal fun parseDonate(html: String): DonateFormParseResult {
        val d = SourceHtmlSupport.sourceDocument(html)
        val form = d.selectFirst("form[action=/donate]")
        return DonateFormParseResult(
            form = DonationForm(
                quickMessages = d.select("button[data-quick-message]").map { it.text() },
                unavailableReason = if (form == null) d.selectFirst(".forum-main")?.text()
                    ?.removePrefix("消息")?.trim()?.takeIf { it.isNotBlank() } ?: HtmlTextParser.extractError(html) else "",
            ),
            csrf = form?.selectFirst("input[name=_csrf]")?.attr("value") ?: "",
            topicId = form?.selectFirst("input[name=topic_id]")?.attr("value") ?: "",
            requestKey = form?.selectFirst("input[name=request_key]")?.attr("value") ?: "",
        )
    }

    // ---------------- 举报 ----------------

}
