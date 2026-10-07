package sb.linux.client.ui.session

import sb.linux.client.domain.topic.TopicCard

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CompletableDeferred

data class PendingVerification(val url: String, val html: String, val deferred: CompletableDeferred<Boolean>)

class HomeTabState {
    var topics by mutableStateOf<List<TopicCard>>(emptyList())
    var page by mutableIntStateOf(1)
    var totalPages by mutableIntStateOf(1)
    var sourcePage by mutableIntStateOf(1)
    var sourceTotalPages by mutableIntStateOf(1)
    var loaded by mutableStateOf(false)
    var scrollIndex by mutableIntStateOf(0)
    var scrollOffset by mutableIntStateOf(0)
}

/** Account-independent home browsing state retained while navigating into a topic and back. */
class HomeBrowseState {
    var refreshRequest by mutableIntStateOf(0)
    var homeTabIndex by mutableIntStateOf(0)
    var homeCombo by mutableIntStateOf(0)
    var homeSortDrawerOpen by mutableStateOf(true)
    val homeTabs = List(5) { HomeTabState() }

    fun requestRefresh() { refreshRequest++ }
}

enum class ThemeModePref { SYSTEM, LIGHT, DARK }
