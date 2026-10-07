package sb.linux.client.domain.settings

data class BrowseSettings(
    val infiniteScroll: Boolean,
    val scrollModeOverrides: Map<String, Boolean>,
    val homeKeepCache: Boolean,
    val collapseNewTopics: Boolean,
    val danmakuOn: Boolean,
    val homeSortDrawerOpen: Boolean,
    val sidebarTwoColumns: Boolean,
    val sidebarShowOnlineUsers: Boolean,
    val threadDialogEnabled: Boolean,
    val unfavoriteConfirm: Boolean,
    val topicsPerPage: Int,
    val commentsPerPage: Int,
    val commentSortOrder: Int,
    val tableDisplayMode: Int,
    val showUid: Boolean,
    val smartDecodeEnabled: Boolean,
)

interface BrowseSettingsRepository {
    fun current(): BrowseSettings
    fun reset()
    fun saveInfiniteScroll(value: Boolean)
    fun saveScrollModeOverride(scope: String, value: Boolean)
    fun clearScrollModeOverrides()
    fun saveHomeSortDrawerOpen(value: Boolean)
    fun saveHomeKeepCache(value: Boolean)
    fun saveCollapseNewTopics(value: Boolean)
    fun saveSidebarTwoColumns(value: Boolean)
    fun saveSidebarShowOnlineUsers(value: Boolean)
    fun saveDanmaku(value: Boolean)
    fun saveTableDisplayMode(value: Int)
    fun saveShowUid(value: Boolean)
    fun saveSmartDecodeEnabled(value: Boolean)
    fun saveThreadDialogEnabled(value: Boolean)
    fun saveUnfavoriteConfirm(value: Boolean)
    fun saveCommentSortOrder(value: Int)
    fun saveTopicsPerPage(value: Int)
    fun saveCommentsPerPage(value: Int)
}
