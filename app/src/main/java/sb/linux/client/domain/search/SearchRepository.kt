package sb.linux.client.domain.search

/** Source-independent search contract used by the presentation layer. */
interface SearchRepository {
    suspend fun loadCharge(scope: SearchScope): SearchCharge
    suspend fun search(query: SearchQuery, confirmedCost: Int? = null): SearchPage
}

data class SearchCharge(val cost: Int, val balance: Int, val notice: String = "")

class SearchConfirmationRequired(val charge: SearchCharge) : RuntimeException()
