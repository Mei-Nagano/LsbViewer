package sb.linux.client.ui.feature.community

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import sb.linux.client.domain.community.DirectMessageContact
import sb.linux.client.domain.community.PrivateMessageRepository
import sb.linux.client.domain.community.PmMessage

data class DirectMessagesUiState(
    val contacts: List<DirectMessageContact> = emptyList(),
    val loading: Boolean = false,
    val loaded: Boolean = false,
    val error: String? = null,
    val query: String = "",
)

class DirectMessagesViewModel(private val repository: PrivateMessageRepository) : ViewModel() {
    private val mutableState = MutableStateFlow(DirectMessagesUiState())
    val uiState = mutableState.asStateFlow()

    private var loadJob: kotlinx.coroutines.Job? = null

    fun setQuery(value: String) = mutableState.update { it.copy(query = value) }

    fun load() {
        loadJob?.cancel()
        val query = mutableState.value.query
        loadJob = viewModelScope.launch {
            mutableState.update { it.copy(loading = true, error = null) }
            try {
                val contacts = repository.loadContacts(query)
                mutableState.update { it.copy(contacts = contacts, loading = false, loaded = true) }
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                mutableState.update {
                    it.copy(loading = false, loaded = true, error = error.message ?: "加载失败")
                }
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
    val canSend: Boolean = false,
    val blockLabel: String = "",
    val blocking: Boolean = false,
)

class ChatViewModel(
    private val repository: PrivateMessageRepository,
    private val partnerId: Long,
    name: String,
    avatarUrl: String,
    private val showToast: (String) -> Unit,
) : ViewModel() {
    private val mutableState = MutableStateFlow(
        ChatUiState(name, avatarUrl, emptyList()),
    )
    val uiState = mutableState.asStateFlow()

    fun setDraft(value: String) = mutableState.update { it.copy(draft = value) }

    private var syncJob: kotlinx.coroutines.Job? = null

    fun sync() {
        if (partnerId <= 0) {
            mutableState.update { it.copy(loading = false) }
            return
        }
        syncJob?.cancel()
        syncJob = viewModelScope.launch {
            mutableState.update { it.copy(loading = true, syncError = null) }
            try {
                val thread = repository.loadThread(partnerId)
                mutableState.update { current ->
                    current.copy(
                        name = thread.partnerName.ifBlank { current.name },
                        avatarUrl = thread.avatarUrl.ifBlank { current.avatarUrl },
                        messages = thread.messages,
                        canSend = thread.canSend,
                        blockLabel = thread.blockLabel,
                        loading = false,
                    )
                }
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                mutableState.update {
                    it.copy(loading = false, syncError = "加载原站会话失败：${error.message.orEmpty()}")
                }
            }
        }
    }

    fun toggleBlock() {
        if (mutableState.value.blocking || partnerId <= 0) return
        mutableState.update { it.copy(blocking = true) }
        viewModelScope.launch {
            try {
                val error = repository.toggleBlock(partnerId)
                if (error != null) showToast(error) else sync()
            } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
            catch (error: Exception) { showToast(error.message ?: "操作失败") }
            finally { mutableState.update { it.copy(blocking = false) } }
        }
    }

    fun send() {
        val current = mutableState.value
        val body = current.draft.trim()
        if (body.isBlank() || partnerId <= 0 || current.sending || !current.canSend) return
        if (body.codePointCount(0, body.length) > 500) { showToast("私信最多 500 字"); return }
        mutableState.update { it.copy(sending = true) }
        viewModelScope.launch {
            try {
                val rejection = repository.sendMessage(partnerId, body)
                if (rejection != null) {
                    showToast(rejection)
                } else {
                    mutableState.update { it.copy(draft = "") }
                    sync()
                    showToast("已发送")
                }
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
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
    private val partnerId: Long,
    private val name: String,
    private val avatarUrl: String,
    private val showToast: (String) -> Unit,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        ChatViewModel(repository, partnerId, name, avatarUrl, showToast) as T
}
