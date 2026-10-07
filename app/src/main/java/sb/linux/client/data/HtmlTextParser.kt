package sb.linux.client.data

import sb.linux.client.common.content.HtmlTextConversion

internal object HtmlTextParser {
    /** Extract source validation errors while keeping transport parsing in the data layer. */
    fun extractError(html: String): String {
        val panel = SourceHtmlSupport.sourceDocument(html)
            .selectFirst(".form-error-panel, .form_error, .error-message")
        if (panel != null) {
            val text = panel.select("h2, p, li").eachText().distinct().joinToString("；").trim()
            if (text.isNotBlank()) return text
        }
        val segment = Regex("""<main.*?</main>""", RegexOption.DOT_MATCHES_ALL)
            .find(html)?.groupValues?.get(0) ?: html
        val text = segment.replace(Regex("""<(svg|script|style)[\s\S]*?</\1>"""), " ")
            .replace(Regex("""<[^>]+>"""), " ")
        return Regex("""\s+""").replace(text, " ").trim()
    }

    fun htmlToText(html: String): String = HtmlTextConversion.htmlToText(html)

    fun htmlToSearchText(html: String): String = HtmlTextConversion.htmlToSearchText(html)

    fun markdownToHtml(markdown: String): String = HtmlTextConversion.markdownToHtml(markdown)
}
