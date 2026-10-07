package sb.linux.client.ui.session

import androidx.compose.ui.graphics.Color
import sb.linux.client.common.filter.KeywordFilterSnapshot
import sb.linux.client.common.ui.SessionUiInvalidation
import sb.linux.client.ui.feature.topic.TopicPinnedSnapshot

/** Readable state surface shared by the app ViewModel and app-scoped state coordinators. */
internal interface SessionState {
    val homeBrowse: HomeBrowseState
    val homeRefreshRequest: Int
    fun requestHomeRefresh()

    var themeMode: ThemeModePref
    var dynamicColor: Boolean
    var themeColorKey: String
    var oledDark: Boolean
    var themeStyle: Int
    var themeContrast: Float
    var themePrimary: Color?
    var themeSecondary: Color?
    var themeTertiary: Color?
    var themeBackground: Color?
    var keepBackgroundColor: Boolean
    var backgroundImage: String
    var backgroundImageEnabled: Boolean
    var backgroundImageOpacity: Float
    var surfaceOpacity: Float
    var themeCustomColors: List<String>
    var fontKey: String
    var customFontName: String
    var fontScale: Float
    var fontWeightLevel: Int
    val cardColors: Map<String, Color>

    var toast: String?
    var uiInvalidation: SessionUiInvalidation?
    val pendingVerification: PendingVerification?

    var infiniteScroll: Boolean
    var scrollModeOverrides: Map<String, Boolean>
    var homeCacheEnabled: Boolean
    var collapseNewTopics: Boolean
    var danmakuOn: Boolean
    var sidebarTwoColumns: Boolean
    var showOnlineUsers: Boolean
    var threadDialogEnabled: Boolean
    var unfavoriteConfirm: Boolean
    var topicsPerPage: Int
    var commentSortOrder: Int
    var tableDisplayMode: Int
    var showUid: Boolean
    var smartDecodeEnabled: Boolean
    var tabletMode: Boolean
    var bottomBarStyle: Int
    var bottomBarItems: List<String>
    var replyBarStyle: Int
    var linkPreviewEnabled: Boolean

    var floatingTopicId: Long
    var floatingTopicTitle: String
    var floatingTopicSnapshot: TopicPinnedSnapshot?
    val checkinText: String
    val checkinCheckedToday: Boolean
    val keywordFilter: KeywordFilterSnapshot
    var blockedWords: Set<String>
    var blockedUsers: Set<String>

    fun updateCardColors(colors: Map<String, Color>)
    fun setPendingVerification(request: PendingVerification?)
    fun updateCheckinUi(text: String, checkedToday: Boolean)
    fun setKeywordFilterSnapshot(snapshot: KeywordFilterSnapshot)
    fun resetCheckinUi()
    fun resetKeywordFilterUi()
    fun showToast(message: String)
}
