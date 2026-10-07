package sb.linux.client.ui.feature.gacha

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import sb.linux.client.domain.gacha.GachaAction
import sb.linux.client.domain.gacha.GachaCenter
import sb.linux.client.domain.gacha.GachaCenterRepository
import sb.linux.client.domain.gacha.GachaPullReceipt

data class GachaCenterUiState(
    val center: GachaCenter? = null,
    val confirmPullEnabled: Boolean = true,
    val loading: Boolean = true,
    val error: String? = null,
    val submitting: Boolean = false,
    val receipt: GachaPullReceipt? = null,
)

class GachaCenterViewModel(
    private val repository: GachaCenterRepository,
    private val showToast: (String) -> Unit,
) : ViewModel() {
    private val mutableState = MutableStateFlow(
        GachaCenterUiState(confirmPullEnabled = repository.confirmPullEnabled()),
    )
    val uiState = mutableState.asStateFlow()

    fun load() {
        viewModelScope.launch {
            mutableState.update { it.copy(loading = true, error = null) }
            try {
                val center = repository.load()
                mutableState.update { it.copy(center = center, loading = false) }
            } catch (error: Exception) {
                mutableState.update { it.copy(loading = false, error = error.message ?: "加载失败") }
            }
        }
    }

    fun pull(action: GachaAction) {
        if (mutableState.value.submitting) return
        viewModelScope.launch {
            mutableState.update { it.copy(submitting = true) }
            try {
                val receipt = repository.pull(action)
                if (receipt.result.items.isEmpty()) {
                    showToast("抽取完成，结果暂时无法解析，请在我的称号中查看")
                } else {
                    mutableState.update { it.copy(receipt = receipt) }
                }
                load()
            } catch (error: Exception) {
                showToast(error.message ?: "抽取失败")
            } finally {
                mutableState.update { it.copy(submitting = false) }
            }
        }
    }

    fun dismissReceipt() = mutableState.update { it.copy(receipt = null) }

    fun setConfirmPullEnabled(enabled: Boolean) {
        repository.setConfirmPullEnabled(enabled)
        mutableState.update { it.copy(confirmPullEnabled = enabled) }
    }
}

class GachaCenterViewModelFactory(
    private val repository: GachaCenterRepository,
    private val showToast: (String) -> Unit,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        GachaCenterViewModel(repository, showToast) as T
}
