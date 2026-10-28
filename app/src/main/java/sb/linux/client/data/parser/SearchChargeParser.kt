package sb.linux.client.data.parser

import org.jsoup.nodes.Document
import sb.linux.client.domain.search.SearchCharge

internal object SearchChargeParser {
    fun parse(document: Document): SearchCharge? {
        val form = document.selectFirst("form.meilisearch-search-form") ?: return null
        val cost = form.attr("data-search-cost").toIntOrNull() ?: return null
        val balance = form.attr("data-search-balance").toIntOrNull() ?: return null
        if (cost < 0) return null
        return SearchCharge(cost, balance, document.selectFirst(".meilisearch-search-cost-note")?.text().orEmpty())
    }
}
