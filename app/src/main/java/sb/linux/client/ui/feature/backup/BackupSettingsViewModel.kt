package sb.linux.client.ui.feature.backup

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import sb.linux.client.domain.backup.BackupSettings
import sb.linux.client.domain.backup.BackupSettingsRepository

data class BackupSettingsUiState(
    val settings: BackupSettings,
    val directoryText: String = settings.webDavDirectory.ifBlank { "/linuxsb/" },
)

class BackupSettingsViewModel(
    private val repository: BackupSettingsRepository,
) : ViewModel() {
    var state by mutableStateOf(BackupSettingsUiState(repository.current()))
        private set

    fun setWebDavUrl(value: String) = update(repository::setWebDavUrl, value)
    fun setWebDavUsername(value: String) = update(repository::setWebDavUsername, value)
    fun setWebDavPassword(value: String) = update(repository::setWebDavPassword, value)

    fun setWebDavDirectory(value: String) {
        repository.setWebDavDirectory(value)
        state = state.copy(settings = repository.current(), directoryText = value)
    }

    fun setItemEnabled(item: String, enabled: Boolean) {
        val current = state.settings.items
        val next = if (enabled) current + item else current - item
        if (next.isNotEmpty()) update(repository::setItems, next)
    }

    fun exportJson(): String = repository.exportJson(state.settings.items)
    fun importJson(json: String): String? = repository.importJson(json)

    private fun <T> update(save: (T) -> Unit, value: T) {
        save(value)
        state = state.copy(settings = repository.current())
    }
}

class BackupSettingsViewModelFactory(
    private val repository: BackupSettingsRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        BackupSettingsViewModel(repository) as T
}
