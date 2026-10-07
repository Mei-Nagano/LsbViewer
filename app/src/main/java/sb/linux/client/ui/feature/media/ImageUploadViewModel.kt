package sb.linux.client.ui.feature.media

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import sb.linux.client.domain.media.ImageHostConfig
import sb.linux.client.domain.media.ImageHostSettings
import sb.linux.client.domain.media.ImageHostSettingsRepository
import sb.linux.client.domain.media.ImageUploadRepository

class ImageUploadViewModel(
    private val repository: ImageUploadRepository,
    private val settingsRepository: ImageHostSettingsRepository,
) : ViewModel() {
    fun hasUsableHost(): Boolean = settingsRepository.current().let { it.useBuiltIn || it.url.isNotBlank() }

    fun upload(uri: String, onComplete: (Result<String>) -> Unit) {
        viewModelScope.launch {
            try {
                onComplete(Result.success(repository.upload(uri, settingsRepository.current().toConfig())))
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                onComplete(Result.failure(error))
            }
        }
    }
}

class ImageUploadViewModelFactory(
    private val repository: ImageUploadRepository,
    private val settingsRepository: ImageHostSettingsRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        ImageUploadViewModel(repository, settingsRepository) as T
}

private fun ImageHostSettings.toConfig() = ImageHostConfig(
    useBuiltInHost = useBuiltIn,
    builtInProvider = provider,
    endpoint = url,
    fieldName = fileField,
    token = token,
)
