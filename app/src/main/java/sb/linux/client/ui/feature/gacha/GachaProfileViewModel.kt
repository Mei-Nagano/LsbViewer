package sb.linux.client.ui.feature.gacha

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import sb.linux.client.domain.gacha.GachaAction
import sb.linux.client.domain.gacha.GachaProfile
import sb.linux.client.domain.gacha.GachaProfileRepository

data class GachaProfileUiState(
    val profile: GachaProfile? = null,
    val loading: Boolean = false,
    val error: String? = null,
    val acting: Boolean = false,
)

class GachaProfileViewModel(
    private val repository: GachaProfileRepository,
    private val showToast: (String) -> Unit,
    private val refreshSession: () -> Unit,
) : ViewModel() {
    private val mutableState = MutableStateFlow(GachaProfileUiState())
    val uiState = mutableState.asStateFlow()

    fun load() {
        viewModelScope.launch {
            mutableState.update { it.copy(loading = true, error = null) }
            try {
                val profile = repository.load()
                mutableState.update { it.copy(profile = profile, loading = false) }
            } catch (error: Exception) {
                mutableState.update { it.copy(loading = false, error = error.message ?: "加载失败") }
            }
        }
    }

    fun submit(action: GachaAction) {
        if (mutableState.value.acting) return
        viewModelScope.launch {
            mutableState.update { it.copy(acting = true) }
            try {
                val rejection = repository.submit(action)
                if (rejection != null) showToast(rejection) else {
                    showToast("${action.label}成功")
                    load()
                    refreshSession()
                }
            } catch (error: Exception) {
                showToast(error.message ?: "操作失败")
            } finally {
                mutableState.update { it.copy(acting = false) }
            }
        }
    }
}

class GachaProfileViewModelFactory(
    private val repository: GachaProfileRepository,
    private val showToast: (String) -> Unit,
    private val refreshSession: () -> Unit,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        GachaProfileViewModel(repository, showToast, refreshSession) as T
}
