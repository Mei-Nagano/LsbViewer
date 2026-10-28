package sb.linux.client.data

import org.jsoup.nodes.Document
import sb.linux.client.domain.topic.RedPacketPanel

internal object RedPacketParser {
    fun parse(document: Document): RedPacketPanel? {
        val card = document.selectFirst("section.red-packet-card") ?: return null
        return RedPacketPanel(
            title = card.selectFirst("header strong")?.text().orEmpty(),
            status = card.selectFirst("header span")?.text().orEmpty(),
            remaining = card.selectFirst("header b")?.text().orEmpty(),
            details = card.select(".red-packet-card-grid > div").map { field ->
                field.children().map { it.text() }.filter(String::isNotBlank).joinToString(" · ")
            },
            rules = card.select("p").map { it.text() }.filter(String::isNotBlank),
        )
    }
}
