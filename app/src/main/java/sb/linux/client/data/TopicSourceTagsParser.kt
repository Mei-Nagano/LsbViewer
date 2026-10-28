package sb.linux.client.data

import org.jsoup.nodes.Element

internal object TopicSourceTagsParser {
    private const val SELECTOR = ".topic-badge, .topic-stamp-badge, .topic-management-featured-badge, " +
        ".topic-management-detail-badge, .community-lottery-title-status, .virtual-card-title-status, " +
        ".virtual-card-list-price, [class*=red-packet-title]"
    fun parse(element: Element): List<String> = element.select(SELECTOR)
        .map { it.text().trim() }.filter(String::isNotBlank).distinct()
}
