package sb.linux.client.ui.common.content

import org.jsoup.Jsoup

internal data class TopicTocHeading(val text: String, val level: Int)

internal fun extractTopicTocHeadings(html: String?): List<TopicTocHeading> {
    if (html.isNullOrBlank()) return emptyList()
    return Jsoup.parseBodyFragment(html)
        .select("h1,h2,h3,h4,h5,h6")
        .mapNotNull { node ->
            node.text().trim().takeIf { it.isNotBlank() }?.let {
                TopicTocHeading(it, node.tagName().removePrefix("h").toIntOrNull() ?: 1)
            }
        }
}
