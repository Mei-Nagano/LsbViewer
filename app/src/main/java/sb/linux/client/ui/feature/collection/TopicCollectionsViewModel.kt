package sb.linux.client.ui.feature.collection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import sb.linux.client.common.collection.TopicCollectionListPage
import sb.linux.client.common.collection.TopicCollectionTab
import sb.linux.client.domain.collection.TopicCollectionPageRepository

data class TopicCollectionsUiState(
    val mine: Boolean,
    val rowsByTab: Map<TopicCollectionTab, TopicCollectionListPage> = emptyMap(),
    val requiresLoginByTab: Map<TopicCollectionTab, Boolean> = emptyMap(),
    val loading: Boolean = true,
    val error: String? = null,
) {
    val selectedTab: TopicCollectionTab get() = if (mine) TopicCollectionTab.MINE else TopicCollectionTab.EVERYONE
}

class TopicCollectionsViewModel(
    private val repository: TopicCollectionPageRepository,
    initialMine: Boolean,
) : ViewModel() {
    private val mutableState = MutableStateFlow(TopicCollectionsUiState(initialMine))
    val uiState = mutableState.asStateFlow()
    private var requestVersion = 0

    fun selectMine(mine: Boolean) {
        if (mutableState.value.mine == mine) return
        mutableState.update { it.copy(mine = mine) }
    }

    fun load(userId: Long, loggedIn: Boolean) {
        val version = ++requestVersion
        val tab = mutableState.value.selectedTab
        mutableState.update { it.copy(loading = true, error = null) }
        currentUserId = userId
        if (tab == TopicCollectionTab.MINE && !loggedIn) {
            mutableState.update {
                it.copy(requiresLoginByTab = it.requiresLoginByTab + (tab to true), loading = false)
            }
            return
        }
        viewModelScope.launch {
            try {
                val page = repository.list(tab)
                if (version == requestVersion && tab == mutableState.value.selectedTab && userId == currentUserId) {
                    mutableState.update {
                        it.copy(
                            rowsByTab = it.rowsByTab + (tab to page),
                            requiresLoginByTab = it.requiresLoginByTab + (tab to page.requiresLogin),
                        )
                    }
                }
            } catch (error: Exception) {
                if (version == requestVersion) mutableState.update { it.copy(error = error.message ?: "加载失败") }
            } finally {
                if (version == requestVersion) mutableState.update { it.copy(loading = false) }
            }
        }
    }

    private var currentUserId = 0L
}

class TopicCollectionsViewModelFactory(
    private val repository: TopicCollectionPageRepository,
    private val initialMine: Boolean,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        TopicCollectionsViewModel(repository, initialMine) as T
}
