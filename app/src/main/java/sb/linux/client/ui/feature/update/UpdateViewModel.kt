package sb.linux.client.ui.feature.update

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import sb.linux.client.domain.update.UpdateInfo
import sb.linux.client.domain.update.UpdateRepository
import sb.linux.client.domain.settings.GeneralSettingsRepository

class UpdateViewModel(
    private val repository: UpdateRepository,
    private val settings: GeneralSettingsRepository,
) : ViewModel() {
    fun beginAutomaticCheck(now: Long): Boolean {
        val current = settings.current()
        if (current.updateCheckMode == 2) return false
        val due = current.updateCheckMode == 0 ||
            now - current.lastUpdateCheck > current.updateCheckIntervalHours * 3600_000L
        if (due) settings.markUpdateCheck(now)
        return due
    }

    fun markChecked(atMillis: Long) = settings.markUpdateCheck(atMillis)

    fun isFirstRunCheckMode(): Boolean = settings.current().updateCheckMode == 0

    fun disableAutomaticChecks() = settings.setUpdateCheckMode(2)

    fun loadLatest(onComplete: (Result<UpdateInfo?>) -> Unit) {
        viewModelScope.launch {
            try {
                onComplete(Result.success(repository.latestRelease()))
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                onComplete(Result.failure(error))
            }
        }
    }
}

class UpdateViewModelFactory(
    private val repository: UpdateRepository,
    private val settings: GeneralSettingsRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = UpdateViewModel(repository, settings) as T
}
