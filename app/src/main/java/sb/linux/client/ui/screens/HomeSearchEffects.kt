package sb.linux.client.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
    val listAtTop by remember(listState) {
        derivedStateOf { listState.firstVisibleItemIndex == 0 }
    }
    LaunchedEffect(listAtTop) { onTopBarVisibleChange(listAtTop) }
    LaunchedEffect(searchActive) {
        if (searchActive) searchFocus.requestFocus()
    }
    LaunchedEffect(topBarVisible) {
        if (!topBarVisible) onSearchClose()
    }
    BackHandler(enabled = searchActive, onBack = onSearchClose)
}
