package sb.linux.client.ui.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import sb.linux.client.domain.search.SearchRepository
import sb.linux.client.domain.search.SearchResultCache
import sb.linux.client.domain.search.SearchScope
import sb.linux.client.domain.search.SearchSort

class SearchViewModelFactory(
    private val repository: SearchRepository,
    private val initialQuery: String,
    private val initialScope: SearchScope,
    private val initialSort: SearchSort,
    private val cached: SearchResultCache?,
    private val onCacheSaved: (SearchResultCache) -> Unit,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(SearchViewModel::class.java))
        @Suppress("UNCHECKED_CAST")
        return SearchViewModel(
            repository = repository,
            initialQuery = initialQuery,
            initialScope = initialScope,
            initialSort = initialSort,
            cached = cached,
            onCacheSaved = onCacheSaved,
        ) as T
    }
}
