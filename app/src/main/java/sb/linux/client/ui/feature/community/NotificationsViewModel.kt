package sb.linux.client.ui.feature.community

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import sb.linux.client.domain.community.NotificationItem
import sb.linux.client.domain.community.NotificationRepository

data class NotificationsUiState(
    val items: List<NotificationItem> = emptyList(),
    val loading: Boolean = true,
    val error: String? = null,
)

class NotificationsViewModel(
    private val repository: NotificationRepository,
    private val userId: Long,
    private val updateUnread: (Int) -> Unit,
    private val showToast: (String) -> Unit,
) : ViewModel() {
    private val mutableState = MutableStateFlow(NotificationsUiState())
    val uiState = mutableState.asStateFlow()

    fun load() {
        viewModelScope.launch {
            mutableState.update { it.copy(loading = true, error = null) }
            try {
                val items = repository.loadNotifications(userId)
                mutableState.update { it.copy(items = items, loading = false) }
            } catch (error: Exception) {
                mutableState.update { it.copy(loading = false, error = error.message) }
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
