package sb.linux.client.ui.session

import androidx.compose.ui.graphics.Color
import sb.linux.client.common.filter.KeywordFilterSnapshot
import sb.linux.client.common.ui.SessionUiInvalidation
import sb.linux.client.ui.feature.topic.TopicPinnedSnapshot

/** Compose-observable state owner projected through the SessionState contract. */
internal class SessionUiState : SessionState {
    private val themeState = SessionThemeState()
    private val preferenceState = SessionPreferenceState()
    private val feedbackState = SessionFeedbackState()
    private val accountUiState = SessionAccountUiState()
    private val topicPinState = SessionTopicPinState()

    override val homeBrowse = HomeBrowseState()
    override val homeRefreshRequest: Int get() = homeBrowse.refreshRequest
    override fun requestHomeRefresh() = homeBrowse.requestRefresh()

    override var themeMode by themeState::themeMode
    override var dynamicColor by themeState::dynamicColor
    override var themeColorKey by themeState::themeColorKey
    override var oledDark by themeState::oledDark
    override var themeStyle by themeState::themeStyle
    override var themeContrast by themeState::themeContrast
    override var themePrimary by themeState::themePrimary
    override var themeSecondary by themeState::themeSecondary
    override var themeTertiary by themeState::themeTertiary
    override var themeBackground by themeState::themeBackground
    override var keepBackgroundColor by themeState::keepBackgroundColor
    override var backgroundImage by themeState::backgroundImage
    override var backgroundImageEnabled by themeState::backgroundImageEnabled
    override var backgroundImageOpacity by themeState::backgroundImageOpacity
    override var surfaceOpacity by themeState::surfaceOpacity
    override var themeCustomColors by themeState::themeCustomColors
    override var fontKey by themeState::fontKey
    override var customFontName by themeState::customFontName
    override var fontScale by themeState::fontScale
    override var fontWeightLevel by themeState::fontWeightLevel
    override val cardColors: Map<String, Color> get() = themeState.cardColors

    override var toast by feedbackState::toast
    override var uiInvalidation: SessionUiInvalidation?
        get() = feedbackState.uiInvalidation
        set(value) { feedbackState.uiInvalidation = value }
    override val pendingVerification: PendingVerification?
        get() = feedbackState.pendingVerification

    override var infiniteScroll by preferenceState::infiniteScroll
    override var scrollModeOverrides by preferenceState::scrollModeOverrides
    override var homeCacheEnabled by preferenceState::homeCacheEnabled
    override var collapseNewTopics by preferenceState::collapseNewTopics
    override var danmakuOn by preferenceState::danmakuOn
    override var sidebarTwoColumns by preferenceState::sidebarTwoColumns
    override var showOnlineUsers by preferenceState::showOnlineUsers
    override var threadDialogEnabled by preferenceState::threadDialogEnabled
    override var unfavoriteConfirm by preferenceState::unfavoriteConfirm
    override var topicsPerPage by preferenceState::topicsPerPage
    override var commentSortOrder by preferenceState::commentSortOrder
    override var tableDisplayMode by preferenceState::tableDisplayMode
    override var showUid by preferenceState::showUid
    override var smartDecodeEnabled by preferenceState::smartDecodeEnabled
    override var tabletMode by preferenceState::tabletMode
    override var singleContentColumn by preferenceState::singleContentColumn
    override var bottomBarStyle by preferenceState::bottomBarStyle
    override var bottomBarItems by preferenceState::bottomBarItems
    override var replyBarStyle by preferenceState::replyBarStyle
    override var linkPreviewEnabled by preferenceState::linkPreviewEnabled

    override var floatingTopicId by topicPinState::topicId
    override var floatingTopicTitle by topicPinState::title
    override var floatingTopicSnapshot: TopicPinnedSnapshot?
        get() = topicPinState.snapshot
        set(value) { topicPinState.snapshot = value }

    override val checkinText: String get() = accountUiState.checkinText
    override val checkinCheckedToday: Boolean get() = accountUiState.checkinCheckedToday
    override val keywordFilter: KeywordFilterSnapshot get() = accountUiState.keywordFilter
    override var blockedWords by accountUiState::blockedWords
    override var blockedUsers by accountUiState::blockedUsers

    override fun updateCardColors(colors: Map<String, Color>) = themeState.updateCardColors(colors)
    override fun setPendingVerification(request: PendingVerification?) {
        feedbackState.pendingVerification = request
    }
    override fun updateCheckinUi(text: String, checkedToday: Boolean) {
        accountUiState.checkinText = text
        accountUiState.checkinCheckedToday = checkedToday
    }
    override fun setKeywordFilterSnapshot(snapshot: KeywordFilterSnapshot) {
        accountUiState.keywordFilter = snapshot
    }
    override fun resetCheckinUi() = updateCheckinUi("", false)
    override fun resetKeywordFilterUi() {
        blockedWords = emptySet()
        blockedUsers = emptySet()
        setKeywordFilterSnapshot(KeywordFilterSnapshot())
    }
    override fun showToast(message: String) {
        toast = message
    }
}
