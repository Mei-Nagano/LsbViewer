package sb.linux.client.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.focus.FocusRequester

@Composable
internal fun HomeSearchEffects(
    listState: LazyListState,
    searchFocus: FocusRequester,
    searchActive: Boolean,
    topBarVisible: Boolean,
    onTopBarVisibleChange: (Boolean) -> Unit,
    onSearchClose: () -> Unit,
) {
    val changeVisibility by rememberUpdatedState(onTopBarVisibleChange)
    LaunchedEffect(listState) {
        var previousIndex = listState.firstVisibleItemIndex
        var previousOffset = listState.firstVisibleItemScrollOffset
        snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
            .collect { (index, offset) ->
                when {
                    // Do not expose the initial header padding while it is still on screen.
                    !listState.canScrollBackward ||
                        (index == 0 && offset < -listState.layoutInfo.viewportStartOffset) -> changeVisibility(true)
                    index < previousIndex || (index == previousIndex && offset < previousOffset) -> changeVisibility(true)
                    index > previousIndex || (index == previousIndex && offset > previousOffset) -> changeVisibility(false)
                }
                previousIndex = index
                previousOffset = offset
            }
    }
    LaunchedEffect(searchActive) {
        if (searchActive) searchFocus.requestFocus()
    }
    LaunchedEffect(topBarVisible) {
        if (!topBarVisible) onSearchClose()
    }
    BackHandler(enabled = searchActive, onBack = onSearchClose)
}
