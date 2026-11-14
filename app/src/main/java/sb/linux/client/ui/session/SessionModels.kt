package sb.linux.client.ui.session

import sb.linux.client.domain.topic.TopicCard
import sb.linux.client.domain.community.FollowingUser
import sb.linux.client.domain.community.FollowingView

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CompletableDeferred

data class PendingVerification(val url: String, val html: String, val deferred: CompletableDeferred<Boolean>)

class HomeTabState {
    var generation: Int = 0
    var topics by mutableStateOf<List<TopicCard>>(emptyList())
    var followingUsers by mutableStateOf<List<FollowingUser>>(emptyList())
    var emptyText by mutableStateOf("")
    var page by mutableIntStateOf(1)
    var totalPages by mutableIntStateOf(1)
    var sourcePage by mutableIntStateOf(1)
    var sourceTotalPages by mutableIntStateOf(1)
    var loaded by mutableStateOf(false)
    var scrollIndex by mutableIntStateOf(0)
    var scrollOffset by mutableIntStateOf(0)

    fun reset() {
        generation++
        topics = emptyList(); loaded = false
        followingUsers = emptyList(); emptyText = ""
        page = 1; totalPages = 1; sourcePage = 1; sourceTotalPages = 1
        scrollIndex = 0; scrollOffset = 0
    }
}

/** Account-independent home browsing state retained while navigating into a topic and back. */
class HomeBrowseState {
    var refreshRequest by mutableIntStateOf(0)
    var homeTabIndex by mutableIntStateOf(0)
    var homeCombo by mutableIntStateOf(0)
    var homeSortDrawerOpen by mutableStateOf(true)
    var followingView by mutableStateOf(FollowingView.TOPICS)
    // 旧七项索引保留，7/8/9 为热议、关注动态、足迹；10/11 为关注用户、粉丝。
    val homeTabs = List(12) { HomeTabState() }

    fun requestRefresh() { refreshRequest++ }
}

enum class ThemeModePref { SYSTEM, LIGHT, DARK }
