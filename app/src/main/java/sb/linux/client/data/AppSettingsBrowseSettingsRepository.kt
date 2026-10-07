package sb.linux.client.data

import sb.linux.client.domain.settings.BrowseSettings
import sb.linux.client.domain.settings.BrowseSettingsRepository

internal class AppSettingsBrowseSettingsRepository(
    private val settings: AppSettings,
) : BrowseSettingsRepository {
    override fun current() = BrowseSettings(
        settings.infiniteScroll,
        settings.scrollModeOverrides,
        settings.homeKeepCache,
        settings.collapseNewTopics,
        settings.danmakuOn,
        settings.homeSortDrawerOpen,
        settings.sidebarTwoColumns,
        settings.sidebarShowOnlineUsers,
        settings.threadDialogEnabled,
        settings.unfavoriteConfirm,
        settings.topicsPerPage,
        settings.commentsPerPage,
        settings.commentSortOrder,
        settings.tableDisplayMode,
        settings.showUid,
        settings.smartDecodeEnabled,
    )

    override fun reset() = settings.resetBrowse()
    override fun saveInfiniteScroll(value: Boolean) { settings.infiniteScroll = value }
    override fun saveScrollModeOverride(scope: String, value: Boolean) {
        if (scope !in setOf("home", "forum", "topic")) return
        settings.scrollModeOverrides = settings.scrollModeOverrides + (scope to value)
    }
    override fun clearScrollModeOverrides() { settings.scrollModeOverrides = emptyMap() }
    override fun saveHomeSortDrawerOpen(value: Boolean) { settings.homeSortDrawerOpen = value }
    override fun saveHomeKeepCache(value: Boolean) { settings.homeKeepCache = value }
    override fun saveCollapseNewTopics(value: Boolean) { settings.collapseNewTopics = value }
    override fun saveSidebarTwoColumns(value: Boolean) { settings.sidebarTwoColumns = value }
    override fun saveSidebarShowOnlineUsers(value: Boolean) { settings.sidebarShowOnlineUsers = value }
    override fun saveDanmaku(value: Boolean) { settings.danmakuOn = value }
    override fun saveTableDisplayMode(value: Int) { settings.tableDisplayMode = value }
    override fun saveShowUid(value: Boolean) { settings.showUid = value }
    override fun saveSmartDecodeEnabled(value: Boolean) { settings.smartDecodeEnabled = value }
    override fun saveThreadDialogEnabled(value: Boolean) { settings.threadDialogEnabled = value }
    override fun saveUnfavoriteConfirm(value: Boolean) { settings.unfavoriteConfirm = value }
    override fun saveCommentSortOrder(value: Int) { settings.commentSortOrder = value }
    override fun saveTopicsPerPage(value: Int) { settings.topicsPerPage = value }
    override fun saveCommentsPerPage(value: Int) { settings.commentsPerPage = value }
}
