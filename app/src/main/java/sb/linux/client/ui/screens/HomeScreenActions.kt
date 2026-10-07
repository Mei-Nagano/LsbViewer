package sb.linux.client.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.tween
import androidx.compose.material3.DrawerState
import androidx.compose.foundation.lazy.LazyListState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import sb.linux.client.ui.session.HomeTabState
import sb.linux.client.ui.session.Session
import sb.linux.client.ui.feature.home.HomePageUiState
import sb.linux.client.ui.feature.home.HomePageViewModel

internal class HomeScreenActions(
    private val session: Session,
    private val scope: CoroutineScope,
    private val listState: LazyListState,
    private val drawerState: DrawerState,
    private val current: HomeTabState,
    private val categoryMotion: Animatable<Float, AnimationVector1D>,
    private val pageViewModel: HomePageViewModel,
    private val pageState: HomePageUiState,
) {
    fun selectCombo(target: Int, direction: Int = if (target > session.homeBrowse.homeCombo) 1 else -1) {
        val next = target.coerceIn(0, 3)
        if (next == session.homeBrowse.homeCombo) return
        scope.launch {
            categoryMotion.snapTo(direction * 72f)
            session.homeBrowse.homeCombo = next
            categoryMotion.animateTo(0f, tween(190))
        }
    }

    fun openDrawer() {
        scope.launch { drawerState.open() }
    }

    fun closeDrawer(action: () -> Unit = {}) {
        if (drawerState.isClosed) action() else scope.launch {
            drawerState.close()
            action()
        }
    }

    fun refresh() {
        if (pageState.loading || pageState.loadingMore) return
        val foldNew = session.collapseNewTopics && current.topics.isNotEmpty()
        current.scrollIndex = 0
        current.scrollOffset = 0
        if (!foldNew) pageViewModel.setPendingTopics(emptyList())
        pageViewModel.clearHomeCaches()
        scope.launch { listState.scrollToItem(0) }
        pageViewModel.load(1, foldNew = foldNew)
    }
}

@androidx.compose.runtime.Composable
internal fun HomeRefreshRequestEffect(session: Session, onRefresh: () -> Unit) {
    val consumedRequest = androidx.compose.runtime.remember {
        androidx.compose.runtime.mutableIntStateOf(session.homeRefreshRequest)
    }
    androidx.compose.runtime.LaunchedEffect(session.homeRefreshRequest) {
        if (session.homeRefreshRequest != consumedRequest.intValue) {
            consumedRequest.intValue = session.homeRefreshRequest
            onRefresh()
        }
    }
}

internal fun submitHomeSearch(
    query: String,
    field: String,
    nav: androidx.navigation.NavHostController,
    onClose: () -> Unit,
) {
    if (query.isBlank()) {
        onClose()
        return
    }
    val encoded = java.net.URLEncoder.encode(query.trim(), "UTF-8")
    nav.navigate("search?q=$encoded&scope=$field&sort=relevance")
    onClose()
}
