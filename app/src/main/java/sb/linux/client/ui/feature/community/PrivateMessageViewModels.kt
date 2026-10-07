package sb.linux.client.ui.feature.community

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import sb.linux.client.domain.community.DirectMessageContact
import sb.linux.client.domain.community.PrivateMessageHistory
import sb.linux.client.domain.community.PrivateMessageRepository
import sb.linux.client.domain.community.PmMessage

data class DirectMessagesUiState(
    val contacts: List<DirectMessageContact> = emptyList(),
    val loading: Boolean = true,
    val error: String? = null,
)

class DirectMessagesViewModel(private val repository: PrivateMessageRepository) : ViewModel() {
    private val mutableState = MutableStateFlow(DirectMessagesUiState())
    val uiState = mutableState.asStateFlow()

    fun load() {
        viewModelScope.launch {
            mutableState.update { it.copy(loading = true, error = null) }
            try {
                val contacts = repository.loadContacts()
                mutableState.update { it.copy(contacts = contacts, loading = false) }
            } catch (error: Exception) {
                mutableState.update { it.copy(loading = false, error = error.message ?: "加载失败") }
            }
        }
    }
}

class DirectMessagesViewModelFactory(
    private val repository: PrivateMessageRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        DirectMessagesViewModel(repository) as T
}

data class ChatUiState(
    val name: String,
    val avatarUrl: String,
    val messages: List<PmMessage>,
    val draft: String = "",
    val loading: Boolean = true,
    val sending: Boolean = false,
    val syncError: String? = null,
)

class ChatViewModel(
    private val repository: PrivateMessageRepository,
    private val history: PrivateMessageHistory,
    private val partnerId: Long,
    name: String,
    avatarUrl: String,
    private val showToast: (String) -> Unit,
) : ViewModel() {
    private val mutableState = MutableStateFlow(
        ChatUiState(name, avatarUrl, history.messages(partnerId)),
    )
    val uiState = mutableState.asStateFlow()

    fun setDraft(value: String) = mutableState.update { it.copy(draft = value) }

    fun sync() {
        if (partnerId <= 0) {
            mutableState.update { it.copy(loading = false) }
            return
        }
        history.markRead(partnerId)
        viewModelScope.launch {
            mutableState.update { it.copy(loading = true, syncError = null) }
            try {
                val thread = repository.loadThread(partnerId)
                history.saveThread(thread)
                mutableState.update { current ->
                    current.copy(
                        name = thread.partnerName.ifBlank { current.name },
                        avatarUrl = thread.avatarUrl.ifBlank { current.avatarUrl },
                        messages = thread.messages,
                        loading = false,
                    )
                }
            } catch (error: Exception) {
                mutableState.update {
                    it.copy(loading = false, syncError = "同步失败，显示本地消息：${error.message.orEmpty()}")
                }
            }
        }
    }

    fun send() {
        val current = mutableState.value
        val body = current.draft.trim()
        if (body.isBlank() || partnerId <= 0 || current.sending) return
        viewModelScope.launch {
            mutableState.update { it.copy(sending = true) }
            try {
                val rejection = repository.sendMessage(partnerId, body)
                if (rejection != null) {
                    showToast(rejection)
                } else {
                    history.addOutgoing(partnerId, current.name, current.avatarUrl, body)
                    mutableState.update { it.copy(draft = "", messages = history.messages(partnerId)) }
                    sync()
                    showToast("已发送")
                }
            } catch (error: Exception) {
                showToast(error.message ?: "发送失败")
            } finally {
                mutableState.update { it.copy(sending = false) }
            }
        }
    }
}

class ChatViewModelFactory(
    private val repository: PrivateMessageRepository,
    private val history: PrivateMessageHistory,
    private val partnerId: Long,
    private val name: String,
    private val avatarUrl: String,
    private val showToast: (String) -> Unit,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        ChatViewModel(repository, history, partnerId, name, avatarUrl, showToast) as T
}
