package sb.linux.client.ui.feature.media

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import sb.linux.client.domain.media.ImageHostSettings
import sb.linux.client.domain.media.ImageHostSettingsRepository

data class ImageHostSettingsUiState(
    val settings: ImageHostSettings,
    val urlDraft: String = settings.url,
    val tokenDraft: String = settings.token,
    val fileFieldDraft: String = settings.fileField,
    val feedback: String? = null,
)

class ImageHostSettingsViewModel(
    private val repository: ImageHostSettingsRepository,
) : ViewModel() {
    var state by mutableStateOf(ImageHostSettingsUiState(repository.current()))
        private set

    fun refresh() {
        state = ImageHostSettingsUiState(repository.current())
    }

    fun setUrl(value: String) { state = state.copy(urlDraft = value) }
    fun setToken(value: String) { state = state.copy(tokenDraft = value) }
    fun setFileField(value: String) { state = state.copy(fileFieldDraft = value) }
    fun consumeFeedback() { state = state.copy(feedback = null) }

    fun setBuiltInEnabled(enabled: Boolean) {
        repository.setBuiltInEnabled(enabled)
        state = state.copy(settings = repository.current())
    }

    fun setProvider(provider: String) {
        repository.setProvider(provider)
        state = state.copy(settings = repository.current())
    }

    fun saveCustom() {
        repository.saveCustom(
            state.urlDraft.trim(),
            state.tokenDraft.trim(),
            state.fileFieldDraft.trim().ifBlank { "file" },
        )
        state = state.copy(settings = repository.current(), feedback = "图床配置已保存")
    }
}

class ImageHostSettingsViewModelFactory(
    private val repository: ImageHostSettingsRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        ImageHostSettingsViewModel(repository) as T
}
