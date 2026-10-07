package sb.linux.client.ui.feature.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import sb.linux.client.domain.settings.BrowseSettings
import sb.linux.client.domain.settings.BrowseSettingsRepository

data class BrowseSettingsUiState(
    val settings: BrowseSettings,
    val applyScope: String = "all",
    val topicsText: String = settings.topicsPerPage.toString(),
    val commentsText: String = settings.commentsPerPage.toString(),
) {
    val homeInfinite: Boolean get() = settings.scrollModeOverrides["home"] ?: settings.infiniteScroll
    val forumInfinite: Boolean get() = settings.scrollModeOverrides["forum"] ?: settings.infiniteScroll
    val topicInfinite: Boolean get() = settings.scrollModeOverrides["topic"] ?: settings.infiniteScroll
    val selectedInfinite: Boolean get() = when (applyScope) {
        "home" -> homeInfinite
        "forum" -> forumInfinite
        "topic" -> topicInfinite
        else -> settings.infiniteScroll
    }
    val anyPaged: Boolean get() = !(homeInfinite && forumInfinite && topicInfinite)
}

class BrowseSettingsViewModel(
    private val repository: BrowseSettingsRepository,
    private val onSettingsChanged: (BrowseSettings) -> Unit,
) : ViewModel() {
    var state by mutableStateOf(BrowseSettingsUiState(repository.current()))
        private set

    fun selectScope(scope: String) { state = state.copy(applyScope = scope) }

    fun applyScrollMode(infinite: Boolean) {
        when (state.applyScope) {
            "home", "forum", "topic" -> repository.saveScrollModeOverride(state.applyScope, infinite)
            else -> {
                repository.saveInfiniteScroll(infinite)
                repository.clearScrollModeOverrides()
            }
        }
        synchronize()
    }

    fun setTopicsText(value: String) {
        val text = value.filter(Char::isDigit).take(3)
        state = state.copy(topicsText = text)
        text.toIntOrNull()?.let(repository::saveTopicsPerPage)
        synchronize()
    }

    fun setCommentsText(value: String) {
        val text = value.filter(Char::isDigit).take(3)
        state = state.copy(commentsText = text)
        text.toIntOrNull()?.let(repository::saveCommentsPerPage)
        synchronize()
    }

    fun setHomeKeepCache(value: Boolean) = update(repository::saveHomeKeepCache, value)
    fun setCollapseNewTopics(value: Boolean) = update(repository::saveCollapseNewTopics, value)
    fun setSidebarTwoColumns(value: Boolean) = update(repository::saveSidebarTwoColumns, value)
    fun setSidebarShowOnlineUsers(value: Boolean) = update(repository::saveSidebarShowOnlineUsers, value)
    fun setDanmaku(value: Boolean) = update(repository::saveDanmaku, value)
    fun setTableDisplayMode(value: Int) = update(repository::saveTableDisplayMode, value)
    fun setShowUid(value: Boolean) = update(repository::saveShowUid, value)
    fun setSmartDecodeEnabled(value: Boolean) = update(repository::saveSmartDecodeEnabled, value)
    fun setThreadDialogEnabled(value: Boolean) = update(repository::saveThreadDialogEnabled, value)
    fun setUnfavoriteConfirm(value: Boolean) = update(repository::saveUnfavoriteConfirm, value)

    fun reset() {
        repository.reset()
        synchronize(resetInputs = true)
    }

    private fun <T> update(save: (T) -> Unit, value: T) {
        save(value)
        synchronize()
    }

    private fun synchronize(resetInputs: Boolean = false) {
        val settings = repository.current()
        onSettingsChanged(settings)
        state = state.copy(
            settings = settings,
            topicsText = if (resetInputs) settings.topicsPerPage.toString() else state.topicsText,
            commentsText = if (resetInputs) settings.commentsPerPage.toString() else state.commentsText,
        )
    }
}

class BrowseSettingsViewModelFactory(
    private val repository: BrowseSettingsRepository,
    private val onSettingsChanged: (BrowseSettings) -> Unit,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        BrowseSettingsViewModel(repository, onSettingsChanged) as T
}
