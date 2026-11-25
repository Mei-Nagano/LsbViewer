package sb.linux.client.data

import org.jsoup.nodes.Element

internal object TopicSourceTagsParser {
    private const val SELECTOR = ".topic-badge, .topic-stamp-badge, .topic-management-featured-badge, " +
        ".topic-management-detail-badge, .community-lottery-title-status, .virtual-card-title-status, " +
        ".virtual-card-list-price, .red-packet-title-status, .poll-vote-badge, .resource_vote-rate, " +
        ".jubaopen-title-badge, .hot-discussions-badge, .topic-management-locked-badge"
    fun styled(element: Element): List<sb.linux.client.domain.topic.SourceTopicTag> = element.select(SELECTOR)
        .filter { it.text().isNotBlank() }.map { sb.linux.client.domain.topic.SourceTopicTag(it.text().trim(), it.className(), it.attr("style")) }.distinctBy { it.label }
    fun parse(element: Element): List<String> = element.select(SELECTOR)
        .map { it.text().trim() }.filter(String::isNotBlank).distinct()
}
