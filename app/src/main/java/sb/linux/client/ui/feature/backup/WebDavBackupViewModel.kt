package sb.linux.client.ui.feature.backup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import sb.linux.client.domain.backup.WebDavBackupRepository

class WebDavBackupViewModel(private val repository: WebDavBackupRepository) : ViewModel() {
    fun upload(url: String, user: String, password: String, content: String, onComplete: (Result<Unit>) -> Unit) =
        runRequest({ repository.upload(url, user, password, content) }, onComplete)

    fun download(url: String, user: String, password: String, onComplete: (Result<String>) -> Unit) =
        runRequest({ repository.download(url, user, password) }, onComplete)

    private fun <T> runRequest(request: suspend () -> T, onComplete: (Result<T>) -> Unit) {
        viewModelScope.launch {
            try {
                onComplete(Result.success(request()))
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                onComplete(Result.failure(error))
            }
        }
    }
}

class WebDavBackupViewModelFactory(
    private val repository: WebDavBackupRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = WebDavBackupViewModel(repository) as T
}
