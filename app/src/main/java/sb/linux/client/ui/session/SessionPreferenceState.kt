package sb.linux.client.ui.session

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Observable app-preference mirror used by pages for immediate layout and behavior updates. */
internal class SessionPreferenceState {
    var infiniteScroll by mutableStateOf(true)
    var scrollModeOverrides by mutableStateOf<Map<String, Boolean>>(emptyMap())
    var homeCacheEnabled by mutableStateOf(false)
    var collapseNewTopics by mutableStateOf(false)
    var danmakuOn by mutableStateOf(true)
    var sidebarTwoColumns by mutableStateOf(true)
    var showOnlineUsers by mutableStateOf(true)
    var threadDialogEnabled by mutableStateOf(false)
    var unfavoriteConfirm by mutableStateOf(true)
    var topicsPerPage by mutableIntStateOf(15)
    var commentSortOrder by mutableIntStateOf(0)
    var tableDisplayMode by mutableIntStateOf(1)
    var showUid by mutableStateOf(true)
    var smartDecodeEnabled by mutableStateOf(false)
    var tabletMode by mutableStateOf(false)
    var bottomBarStyle by mutableIntStateOf(1)
    var bottomBarItems by mutableStateOf(listOf("home", "me"))
    var replyBarStyle by mutableIntStateOf(1)
    var linkPreviewEnabled by mutableStateOf(true)
}
