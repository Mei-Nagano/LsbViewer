package sb.linux.client.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import sb.linux.client.ui.session.Session
import sb.linux.client.ui.*
import sb.linux.client.ui.theme.*
import sb.linux.client.ui.content.*
import sb.linux.client.ui.patterns.topic.*
import sb.linux.client.ui.patterns.user.*
import sb.linux.client.ui.components.feedback.*
import sb.linux.client.ui.components.layout.*
import sb.linux.client.ui.components.navigation.*


@Composable
internal fun TopicScreenBodyContent(
    padding: PaddingValues,
    session: Session,
    nav: NavHostController,
    topicId: Long,
    initialPage: Int,
    scope: CoroutineScope,
    listState: LazyListState,
    ui: TopicScreenUiState,
    paging: TopicPagingState,
    actions: TopicScreenActions,
    view: TopicScreenViewState,
    listModifier: Modifier,
) {
    Box(
        Modifier.fillMaxSize().padding(padding)
            .onGloballyPositioned { ui.contentTopInRoot = it.boundsInRoot().top },
    ) {
        when {
            paging.loading.value && view.page == null -> LoadingBox()
            paging.error.value != null && view.page == null -> ErrorBox(paging.error.value!!) { actions.load(initialPage) }
            view.page == null -> EmptyBox()
            else -> TopicScreenPostList(
                page = view.page,
                session = session,
                nav = nav,
                listState = listState,
                modifier = listModifier,
                view = view,
                ui = ui,
                paging = paging,
                actions = actions,
            )
        }
        if (paging.loading.value && view.page != null) LinearProgressIndicator(
            Modifier.fillMaxWidth().align(Alignment.TopCenter).padding(top = 56.dp))
        TopicScrollControls(
            headings = view.tocHeadings,
            bodyVisible = view.bodyVisible,
            readingProgress = view.readingProgress,
            tocOpen = ui.tocOpen,
            onTocOpenChange = { ui.tocOpen = it },
            headingPositions = ui.headingPositions,
            bodyTop = ui.bodyTop,
            contentTopInRoot = ui.contentTopInRoot,
            topBarPx = view.topBarPx,
            listState = listState,
            scope = scope,
            returnPosition = ui.returnPos,
            onReturnPositionChange = { ui.returnPos = it },
            currentFloor = view.currentFloor,
            onFloorJump = { ui.showFloorDialog = true },
            onHeading = { index -> ui.sourceGeometry.headings[index]?.let { y ->
                scope.launch { listState.animateScrollToItem(topicMainItemIndex(view.page), (y - 8f).toInt().coerceAtLeast(0)) }
            } },
        )
    }
}
