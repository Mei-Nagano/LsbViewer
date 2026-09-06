package sb.linux.client.repository

import sb.linux.client.data.SearchPage
import sb.linux.client.data.SearchQuery

/** Source-independent search contract used by the presentation layer. */
interface SearchRepository {
    suspend fun search(query: SearchQuery): SearchPage
}
