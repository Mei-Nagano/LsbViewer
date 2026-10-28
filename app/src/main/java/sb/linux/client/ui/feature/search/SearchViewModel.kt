package sb.linux.client.ui.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
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
import sb.linux.client.domain.search.SearchCharge
import sb.linux.client.domain.search.SearchConfirmationRequired

data class PendingSearch(val query: SearchQuery, val charge: SearchCharge)

data class SearchUiState(
    val query: String,
    val scope: SearchScope,
    val sort: SearchSort,
    val page: Int,
    val resultPage: SearchPage?,
    val loading: Boolean = false,
    val error: String? = null,
    val pendingSearch: PendingSearch? = null,
    val charge: SearchCharge? = null,
    val chargeError: String? = null,
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
    private var chargeJob: Job? = null

    fun loadCharge() {
        val scope = _uiState.value.scope
        chargeJob?.cancel()
        _uiState.update { it.copy(charge = null, chargeError = null) }
        chargeJob = viewModelScope.launch {
            try {
                val charge = repository.loadCharge(scope)
                _uiState.update { it.copy(charge = charge, chargeError = null) }
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                _uiState.update { it.copy(charge = null, chargeError = error.message ?: "无法读取搜索费用") }
            }
        }
    }

    fun updateQuery(query: String) = _uiState.update { it.copy(query = query) }

    fun selectScope(scope: SearchScope) = _uiState.update {
        it.copy(scope = scope, sort = if (scope == SearchScope.USER) SearchSort.RELEVANCE else it.sort)
    }

    fun selectSort(sort: SearchSort) = _uiState.update { it.copy(sort = sort) }

    fun search(targetPage: Int = 1) {
        val current = _uiState.value
        val access = current.resultPage?.query
            ?.takeIf { it.normalizedText == current.query.trim() && current.error == null }?.access.orEmpty()
        performSearch(SearchQuery(current.query, current.scope, current.sort, targetPage, access))
    }

    fun dismissConfirmation() = _uiState.update { it.copy(pendingSearch = null) }

    fun confirmSearch() {
        val pending = _uiState.value.pendingSearch ?: return
        performSearch(pending.query, pending.charge.cost)
    }

    private fun performSearch(request: SearchQuery, confirmedCost: Int? = null) {
        if (_uiState.value.loading) return
        if (request.normalizedText.length !in 2..120) {
            _uiState.update { it.copy(error = "关键词需要 2 至 120 个字符") }
            return
        }
        _uiState.update { it.copy(loading = true, error = null, pendingSearch = null) }
        viewModelScope.launch {
            try {
                val pageData = repository.search(request, confirmedCost)
                chargeJob?.cancel()
                _uiState.update { it.copy(resultPage = pageData, page = pageData.query.normalizedPage,
                    charge = pageData.charge ?: it.charge, chargeError = null) }
                onCacheSaved(
                    SearchResultCache(
                        request = pageData.query,
                        page = pageData,
                        time = System.currentTimeMillis(),
                    ),
                )
            } catch (confirmation: SearchConfirmationRequired) {
                chargeJob?.cancel()
                _uiState.update { it.copy(pendingSearch = PendingSearch(request, confirmation.charge),
                    charge = confirmation.charge, chargeError = null) }
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                _uiState.update { it.copy(error = error.message ?: "搜索失败") }
            } finally {
                _uiState.update { it.copy(loading = false) }
            }
        }
    }
}
