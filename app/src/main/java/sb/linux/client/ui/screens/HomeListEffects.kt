package sb.linux.client.ui.screens

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import sb.linux.client.ui.session.HomeTabState
import sb.linux.client.ui.session.Session
import sb.linux.client.domain.topic.TopicCard
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
internal fun HomeListEffects(
    session: Session,
    current: HomeTabState,
    tabIndex: Int,
    combo: Int,
    homeInfinite: Boolean,
    visibleTopics: List<TopicCard>,
    loading: Boolean,
    loadingMore: Boolean,
    listState: LazyListState,
    backTopPosition: Pair<Int, Int>?,
    onBackTopPositionChange: (Pair<Int, Int>?) -> Unit,
    loadCachedHome: () -> List<TopicCard>,
    onLoad: (Int, Boolean, Boolean) -> Unit,
    onLocalPage: (Int) -> Unit,
) {
    val perPage = session.topicsPerPage.coerceAtLeast(1)
    LaunchedEffect(visibleTopics.size, homeInfinite, current.page, current.sourceTotalPages) {
        if (!homeInfinite && current.page > 1) {
            val full = ((visibleTopics.size + perPage - 1) / perPage).coerceAtLeast(1)
            if (current.page > full && current.sourcePage >= current.sourceTotalPages) current.page = full
        }
    }

    var lastRetInfinite by rememberSaveable { mutableStateOf(homeInfinite) }
    LaunchedEffect(homeInfinite) {
        val changed = homeInfinite != lastRetInfinite
        lastRetInfinite = homeInfinite
        if (changed && !homeInfinite) {
            withFrameNanos { }
            onLocalPage(1)
        }
    }

    LaunchedEffect(current) {
        if (!current.loaded) {
            val cached = loadCachedHome()
            if (cached.isNotEmpty()) {
                current.topics = cached
                current.loaded = true
                current.page = 1
                current.sourcePage = 1
                current.sourceTotalPages = 999
            } else {
                onLoad(1, false, false)
            }
        } else if (current.scrollIndex > 0 && listState.firstVisibleItemIndex == 0) {
            listState.scrollToItem(
                current.scrollIndex.coerceAtMost(current.topics.size - 1).coerceAtLeast(0),
                current.scrollOffset,
            )
        }
    }

    DisposableEffect(current) {
        onDispose {
            current.scrollIndex = listState.firstVisibleItemIndex
            current.scrollOffset = listState.firstVisibleItemScrollOffset
        }
    }

    LaunchedEffect(current, listState, homeInfinite, visibleTopics.size, current.sourceTotalPages, loadingMore, loading) {
        if (!homeInfinite || loadingMore || loading || current.topics.isEmpty()) return@LaunchedEffect
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1 }
            .distinctUntilChanged()
            .collect { lastIndex ->
                if (!loadingMore && !loading && lastIndex >= visibleTopics.size - 2 &&
                    current.sourcePage < current.sourceTotalPages
                ) onLoad(current.sourcePage + 1, true, false)
            }
    }

    var lastCombo by remember { mutableStateOf(combo) }
    var lastTabIndex by remember { mutableStateOf(tabIndex) }
    LaunchedEffect(combo, tabIndex) {
        if (combo != lastCombo || tabIndex != lastTabIndex) {
            lastCombo = combo
            lastTabIndex = tabIndex
            onBackTopPositionChange(null)
            withFrameNanos { }
            listState.scrollToItem(0)
        }
    }
}
