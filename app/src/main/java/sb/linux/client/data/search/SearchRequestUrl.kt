package sb.linux.client.data.search

import sb.linux.client.domain.search.SearchQuery
import sb.linux.client.domain.search.SearchScope
import sb.linux.client.domain.search.SearchSort
import java.net.URLEncoder

/** Encodes the current server-rendered search URL contract. */
internal object SearchRequestUrl {
    fun pathFor(query: SearchQuery): String {
        val request = query.normalized()
        return buildString {
            append("/search?q=")
            append(URLEncoder.encode(request.normalizedText, Charsets.UTF_8.name()))
            if (request.access.isNotBlank()) {
                append("&access=").append(URLEncoder.encode(request.access, Charsets.UTF_8.name()))
            }
            if (request.scope != SearchScope.ALL) append("&scope=").append(request.scope.value)
            if (request.scope != SearchScope.USER && request.normalizedSort != SearchSort.RELEVANCE) {
                append("&sort=").append(request.normalizedSort.value)
            }
            if (request.normalizedPage > 1) append("&p=").append(request.normalizedPage)
        }
    }
}
