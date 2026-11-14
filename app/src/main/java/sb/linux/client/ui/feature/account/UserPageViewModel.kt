package sb.linux.client.ui.feature.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import sb.linux.client.domain.account.UserPageContent
import sb.linux.client.domain.account.UserPageRepository

data class UserPageUiState(
    val tab: String,
    val content: UserPageContent? = null,
    val loading: Boolean = true,
    val error: String? = null,
)

class UserPageViewModel(
    private val repository: UserPageRepository,
    private val userId: Long,
    initialTab: String,
) : ViewModel() {
    private val mutableState = MutableStateFlow(UserPageUiState(initialTab, repository.cached(userId, initialTab),
        loading = repository.cached(userId, initialTab) == null))
    val uiState = mutableState.asStateFlow()
    private var requestVersion = 0

    fun selectTab(tab: String) {
        if (mutableState.value.tab == tab) return
        mutableState.update { it.copy(tab = tab, content = repository.cached(userId, tab)
            ?: it.content?.let { content -> UserPageContent(content.profile) }) }
        load(forceRefresh = false)
    }

    fun load(forceRefresh: Boolean = true) {
        val version = ++requestVersion
        val tab = mutableState.value.tab
        mutableState.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            try {
                val content = repository.load(userId, tab, forceRefresh)
                if (version == requestVersion) {
                    mutableState.update { current ->
                        val profile = content.profile ?: current.content?.profile
                        current.copy(content = content.copy(profile = profile), loading = false)
                    }
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) {
                if (version == requestVersion) mutableState.update {
                    it.copy(loading = false, error = error.message ?: "加载失败")
                }
            }
        }
    }
}

class UserPageViewModelFactory(
    private val repository: UserPageRepository,
    private val userId: Long,
    private val initialTab: String,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        UserPageViewModel(repository, userId, initialTab) as T
}
