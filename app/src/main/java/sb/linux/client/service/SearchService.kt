package sb.linux.client.service

import sb.linux.client.data.SearchPage
import sb.linux.client.data.SearchQuery
import sb.linux.client.repository.SearchRepository

/** Coordinates validation and source search requests for the UI. */
class SearchService(private val repository: SearchRepository) {
    suspend fun search(query: SearchQuery): SearchPage = repository.search(query)
}
