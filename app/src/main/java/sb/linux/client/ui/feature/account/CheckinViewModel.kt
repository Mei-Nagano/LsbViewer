package sb.linux.client.ui.feature.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import sb.linux.client.domain.account.CheckinInfo
import sb.linux.client.domain.account.CheckinRepository

data class CheckinUiState(
    val info: CheckinInfo? = null,
    val loading: Boolean = true,
    val submitting: Boolean = false,
)

class CheckinViewModel(
    private val repository: CheckinRepository,
    private val showToast: (String) -> Unit,
    private val onSuccess: () -> Unit,
) : ViewModel() {
    private val mutableState = MutableStateFlow(CheckinUiState())
    val uiState = mutableState.asStateFlow()

    fun load() {
        viewModelScope.launch {
            mutableState.update { it.copy(loading = true) }
            val info = runCatching { repository.load() }.getOrNull()
            mutableState.update { it.copy(info = info, loading = false) }
        }
    }

    fun submit() {
        if (mutableState.value.submitting) return
        viewModelScope.launch {
            mutableState.update { it.copy(submitting = true) }
            try {
                val result = repository.submit()
                showToast(result.message)
                if (result.succeeded) onSuccess()
            } catch (error: Exception) {
                showToast(error.message ?: "签到失败")
            } finally {
                mutableState.update { it.copy(submitting = false) }
                load()
            }
        }
    }
}

class CheckinViewModelFactory(
    private val repository: CheckinRepository,
    private val showToast: (String) -> Unit,
    private val onSuccess: () -> Unit,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        CheckinViewModel(repository, showToast, onSuccess) as T
}
