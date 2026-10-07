package sb.linux.client.domain.search

/** Source-independent search contract used by the presentation layer. */
interface SearchRepository {
    suspend fun search(query: SearchQuery): SearchPage
}
