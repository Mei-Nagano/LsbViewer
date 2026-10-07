package sb.linux.client.ui.feature.forum

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import sb.linux.client.domain.community.ForumInfo
import sb.linux.client.domain.community.ForumRepository
import sb.linux.client.domain.topic.TopicCard

data class ForumListUiState(
    val forums: List<ForumInfo> = emptyList(),
    val loading: Boolean = true,
    val error: String? = null,
)

class ForumListViewModel(private val repository: ForumRepository) : ViewModel() {
    private val mutableState = MutableStateFlow(ForumListUiState())
    val uiState = mutableState.asStateFlow()

    fun load() {
        viewModelScope.launch {
            mutableState.update { it.copy(loading = true, error = null) }
            try {
                val forums = repository.loadDirectory()
                mutableState.update { it.copy(forums = forums, loading = false) }
            } catch (error: Exception) {
                mutableState.update { it.copy(loading = false, error = error.message ?: "加载失败") }
            }
        }
    }
}

data class ForumTopicsUiState(
    val topics: List<TopicCard> = emptyList(),
    val page: Int = 1,
    val totalPages: Int = 1,
    val forumName: String = "板块",
    val sort: String = "comment",
    val loading: Boolean = true,
    val loadingMore: Boolean = false,
    val error: String? = null,
)

class ForumTopicsViewModel(
    private val repository: ForumRepository,
    private val forumId: Long,
) : ViewModel() {
    private val mutableState = MutableStateFlow(ForumTopicsUiState())
    val uiState = mutableState.asStateFlow()

    fun load(page: Int, append: Boolean = false) {
        val requestedSort = mutableState.value.sort
        mutableState.update {
            it.copy(
                loading = !append,
                loadingMore = append,
                error = null,
            )
        }
        viewModelScope.launch {
            try {
                val loaded = repository.loadTopics(forumId, requestedSort, page)
                mutableState.update { current ->
                    val items = if (append) {
                        (current.topics + loaded.topics).distinctBy { topic -> topic.topicId }
                    } else loaded.topics
                    current.copy(
                        topics = items,
                        page = loaded.currentPage,
                        totalPages = loaded.totalPages,
                        forumName = loaded.topics.firstOrNull()?.forumName ?: current.forumName,
                        loading = false,
                        loadingMore = false,
                    )
                }
            } catch (error: Exception) {
                mutableState.update {
                    it.copy(loading = false, loadingMore = false, error = error.message ?: "加载失败")
                }
            }
        }
    }

    fun selectSort(sort: String) {
        if (mutableState.value.sort == sort) return
        mutableState.update { it.copy(sort = sort, page = 1) }
        load(page = 1)
    }
}

class ForumListViewModelFactory(private val repository: ForumRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        ForumListViewModel(repository) as T
}

class ForumTopicsViewModelFactory(
    private val repository: ForumRepository,
    private val forumId: Long,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        ForumTopicsViewModel(repository, forumId) as T
}
