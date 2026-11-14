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
import sb.linux.client.domain.media.ImageUploadMethod

class ImageUploadViewModel(
    private val repository: ImageUploadRepository,
    private val settingsRepository: ImageHostSettingsRepository,
) : ViewModel() {
    fun hasUsableHost(method: ImageUploadMethod = settingsRepository.current().uploadMethod): Boolean =
        method != ImageUploadMethod.CUSTOM || settingsRepository.current().url.isNotBlank()

    fun upload(uri: String, method: ImageUploadMethod = settingsRepository.current().uploadMethod,
        onComplete: (Result<String>) -> Unit) {
        viewModelScope.launch {
            try {
                onComplete(Result.success(repository.upload(uri, settingsRepository.current().toConfig(method))))
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

private fun ImageHostSettings.toConfig(method: ImageUploadMethod) = ImageHostConfig(
    useBuiltInHost = method == ImageUploadMethod.BUILT_IN,
    builtInProvider = provider,
    endpoint = url,
    fieldName = fileField,
    token = token,
    method = method,
)
