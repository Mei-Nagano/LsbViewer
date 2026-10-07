package sb.linux.client.ui.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import sb.linux.client.domain.profile.ProfileSettingsPage
import sb.linux.client.domain.profile.ProfileSettingsForm
import sb.linux.client.domain.profile.ProfileSettingsRepository
import sb.linux.client.domain.profile.ProfileSettingsUpload

data class ProfileSettingsUiState(
    val page: ProfileSettingsPage? = null,
    val loading: Boolean = true,
    val submitting: Boolean = false,
    val error: String? = null,
    val message: String? = null,
)

class ProfileSettingsViewModel(
    private val repository: ProfileSettingsRepository,
    private val refreshSession: () -> Unit,
) : ViewModel() {
    private val mutableState = MutableStateFlow(ProfileSettingsUiState())
    val uiState = mutableState.asStateFlow()

    fun load() {
        viewModelScope.launch {
            mutableState.update { it.copy(loading = true, error = null) }
            try {
                val result = repository.load()
                mutableState.update { it.copy(page = result.page, error = result.error, loading = false) }
            } catch (error: Exception) {
                mutableState.update { it.copy(error = error.message, loading = false) }
            }
        }
    }

    fun submit(
        form: ProfileSettingsForm,
        fields: Map<String, String>,
        file: File?,
        mime: String,
        onComplete: () -> Unit,
    ) {
        if (mutableState.value.submitting) return
        viewModelScope.launch {
            mutableState.update { it.copy(submitting = true, message = null) }
            try {
                val upload = file?.let { ProfileSettingsUpload(it.absolutePath, mime) }
                val result = repository.submit(form, fields, upload)
                mutableState.update { it.copy(message = result.message) }
                if (result.shouldRefresh) {
                    refreshSession()
                    load()
                }
            } catch (error: Exception) {
                mutableState.update { it.copy(message = error.message ?: "提交失败") }
            } finally {
                mutableState.update { it.copy(submitting = false) }
                onComplete()
            }
        }
    }

    fun clearMessage() = mutableState.update { it.copy(message = null) }
}

class ProfileSettingsViewModelFactory(
    private val repository: ProfileSettingsRepository,
    private val refreshSession: () -> Unit,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        ProfileSettingsViewModel(repository, refreshSession) as T
}
