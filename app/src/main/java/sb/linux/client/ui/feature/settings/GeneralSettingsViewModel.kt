package sb.linux.client.ui.feature.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import sb.linux.client.domain.settings.GeneralSettings
import sb.linux.client.domain.settings.GeneralSettingsRepository

data class GeneralSettingsUiState(
    val settings: GeneralSettings,
    val intervalText: String = settings.updateCheckIntervalHours.toString(),
)

class GeneralSettingsViewModel(
    private val repository: GeneralSettingsRepository,
) : ViewModel() {
    var state by mutableStateOf(GeneralSettingsUiState(repository.current()))
        private set

    fun linkOpenMode(): Int = repository.current().linkOpenMode

    fun setLinkOpenMode(mode: Int) {
        repository.setLinkOpenMode(mode)
        state = state.copy(settings = repository.current())
    }

    fun setUpdateCheckMode(mode: Int) {
        repository.setUpdateCheckMode(mode)
        state = state.copy(settings = repository.current())
    }

    fun setIntervalText(value: String) {
        val text = value.filter(Char::isDigit).take(4)
        state = state.copy(intervalText = text)
        text.toIntOrNull()?.let(repository::setUpdateCheckIntervalHours)
    }

    fun markUpdateCheck(atMillis: Long) = repository.markUpdateCheck(atMillis)

    fun reset() {
        repository.reset()
        state = GeneralSettingsUiState(repository.current())
    }
}

class GeneralSettingsViewModelFactory(
    private val repository: GeneralSettingsRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        GeneralSettingsViewModel(repository) as T
}
