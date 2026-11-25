package sb.linux.client.ui.feature.community

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import sb.linux.client.domain.community.NotificationItem
import sb.linux.client.domain.community.NotificationRepository

data class NotificationsUiState(
    val items: List<NotificationItem> = emptyList(),
    val loading: Boolean = false,
    val loaded: Boolean = false,
    val error: String? = null,
)

class NotificationsViewModel(
    private val repository: NotificationRepository,
    private val userId: Long,
    private val updateUnread: (Int) -> Unit,
    private val showToast: (String) -> Unit,
) : ViewModel() {
    private val mutableState = MutableStateFlow(NotificationsUiState(items = repository.cachedNotifications(userId).orEmpty()))
    val uiState = mutableState.asStateFlow()
    private var requestVersion = 0
    private var requestJob: Job? = null

    fun load() = request(forceRefresh = true)
    fun loadCached() = request(forceRefresh = false)

    /**
     * 逐页收结果：源站通知历史可能有几十页，整份读完再上屏会让用户一直看加载圈。
     * 新的一次请求会取消上一次，避免「进入页面」和「手动刷新」各跑一遍完整翻页。
     */
    private fun request(forceRefresh: Boolean) {
        val version = ++requestVersion
        requestJob?.cancel()
        mutableState.update { it.copy(loading = true, error = null) }
        requestJob = viewModelScope.launch {
            try {
                repository.loadNotificationsIncrementally(userId, forceRefresh).collect { items ->
                    if (version == requestVersion) mutableState.update { it.copy(items = items) }
                }
                if (version == requestVersion) mutableState.update { it.copy(loading = false, loaded = true) }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) {
                if (version == requestVersion) {
                    mutableState.update { it.copy(loading = false, loaded = true, error = error.message) }
                }
            }
        }
    }

    fun markRead() {
        val items = mutableState.value.items
        if (items.none(NotificationItem::unread)) return
        mutableState.update { state -> state.copy(items = state.items.map { it.copy(unread = false) }) }
        updateUnread(0)
        showToast("已标记为已读")
    }
}

class NotificationsViewModelFactory(
    private val repository: NotificationRepository,
    private val userId: Long,
    private val updateUnread: (Int) -> Unit,
    private val showToast: (String) -> Unit,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        NotificationsViewModel(repository, userId, updateUnread, showToast) as T
}
