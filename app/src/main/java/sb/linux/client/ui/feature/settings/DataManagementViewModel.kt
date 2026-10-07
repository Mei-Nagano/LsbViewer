package sb.linux.client.ui.feature.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import sb.linux.client.domain.settings.DataManagementRepository

class DataManagementViewModel(private val repository: DataManagementRepository) : ViewModel() {
    var autoClearItems by mutableStateOf(repository.autoClearItems())
        private set

    fun setAutoClearEnabled(item: String, enabled: Boolean) {
        val updated = if (enabled) autoClearItems + item else autoClearItems - item
        repository.saveAutoClearItems(updated)
        autoClearItems = updated
    }

    fun clear(items: Set<String>, onComplete: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            try {
                repository.clearDataItems(items)
                onComplete(Result.success(Unit))
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                onComplete(Result.failure(error))
            }
        }
    }
}

class DataManagementViewModelFactory(
    private val repository: DataManagementRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        DataManagementViewModel(repository) as T
}
