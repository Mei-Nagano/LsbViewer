package sb.linux.client.ui.session

import sb.linux.client.domain.settings.BrowseSettings
import sb.linux.client.domain.settings.BrowseSettingsRepository

internal class SessionBrowsePreferenceController(
    private val session: SessionState,
    private val repository: BrowseSettingsRepository,
) {
    fun saveInfiniteScroll(value: Boolean) {
        repository.saveInfiniteScroll(value)
        applyCurrent()
    }

    fun saveHomeSortDrawerOpen(value: Boolean) {
        repository.saveHomeSortDrawerOpen(value)
        applyCurrent()
    }

    fun saveCollapseNewTopics(value: Boolean) {
        repository.saveCollapseNewTopics(value)
        applyCurrent()
    }

    fun effectiveInfiniteScroll(scope: String): Boolean =
        session.scrollModeOverrides[scope] ?: session.infiniteScroll

    fun saveScrollModeOverride(scope: String, infinite: Boolean) {
        repository.saveScrollModeOverride(scope, infinite)
        applyCurrent()
    }

    fun clearScrollModeOverrides() {
        repository.clearScrollModeOverrides()
        applyCurrent()
    }

    fun resetBrowseSettings() {
        repository.reset()
        applyCurrent()
    }

    fun apply(settings: BrowseSettings) {
        session.infiniteScroll = settings.infiniteScroll
        session.scrollModeOverrides = settings.scrollModeOverrides
        session.homeCacheEnabled = settings.homeKeepCache
        session.collapseNewTopics = settings.collapseNewTopics
        session.danmakuOn = settings.danmakuOn
        session.homeBrowse.homeSortDrawerOpen = settings.homeSortDrawerOpen
        session.sidebarTwoColumns = settings.sidebarTwoColumns
        session.threadDialogEnabled = settings.threadDialogEnabled
        session.unfavoriteConfirm = settings.unfavoriteConfirm
        session.topicsPerPage = settings.topicsPerPage
        session.commentSortOrder = settings.commentSortOrder
        session.tableDisplayMode = settings.tableDisplayMode
        session.showUid = settings.showUid
        session.smartDecodeEnabled = settings.smartDecodeEnabled
        session.showOnlineUsers = settings.sidebarShowOnlineUsers
    }

    private fun applyCurrent() {
        apply(repository.current())
    }
}
