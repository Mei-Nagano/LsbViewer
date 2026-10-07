package sb.linux.client.ui.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import sb.linux.client.domain.search.SearchPage
import sb.linux.client.domain.search.SearchQuery
import sb.linux.client.domain.search.SearchRepository
import sb.linux.client.domain.search.SearchResultCache
import sb.linux.client.domain.search.SearchScope
import sb.linux.client.domain.search.SearchSort

data class SearchUiState(
    val query: String,
    val scope: SearchScope,
    val sort: SearchSort,
    val page: Int,
    val resultPage: SearchPage?,
    val loading: Boolean = false,
    val error: String? = null,
)

class SearchViewModel(
    private val repository: SearchRepository,
    initialQuery: String,
    initialScope: SearchScope,
    initialSort: SearchSort,
    cached: SearchResultCache?,
    private val onCacheSaved: (SearchResultCache) -> Unit,
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        SearchUiState(
            query = initialQuery,
            scope = cached?.request?.scope ?: initialScope,
            sort = cached?.request?.sort ?: initialSort,
            page = cached?.page?.query?.normalizedPage ?: 1,
            resultPage = cached?.page,
        ),
    )
    val uiState = _uiState.asStateFlow()

    fun updateQuery(query: String) = _uiState.update { it.copy(query = query) }

    fun selectScope(scope: SearchScope) = _uiState.update {
        it.copy(scope = scope, sort = if (scope == SearchScope.USER) SearchSort.RELEVANCE else it.sort)
    }

    fun selectSort(sort: SearchSort) = _uiState.update { it.copy(sort = sort) }

    fun clearResult() = _uiState.update { it.copy(resultPage = null) }

    fun search(targetPage: Int = 1) {
        val current = _uiState.value
        val request = SearchQuery(current.query, current.scope, current.sort, targetPage)
        if (request.normalizedText.length < 2) {
            _uiState.update { it.copy(error = "关键词至少 2 个字符") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
            try {
                val pageData = repository.search(request)
                _uiState.update { it.copy(resultPage = pageData, page = pageData.query.normalizedPage) }
                onCacheSaved(
                    SearchResultCache(
                        request = pageData.query,
                        page = pageData,
                        time = System.currentTimeMillis(),
                    ),
                )
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                _uiState.update { it.copy(error = error.message ?: "搜索失败") }
            } finally {
                _uiState.update { it.copy(loading = false) }
            }
        }
    }
}
