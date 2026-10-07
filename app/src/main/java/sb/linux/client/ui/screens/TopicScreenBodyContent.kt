package sb.linux.client.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import kotlinx.coroutines.CoroutineScope
import sb.linux.client.ui.session.Session
import sb.linux.client.ui.*

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
            paging.loading.value -> LoadingBox()
            paging.error.value != null -> ErrorBox(paging.error.value!!) { actions.load(initialPage) }
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
            replyGlass = view.replyGlass,
            currentFloor = view.currentFloor,
            onFloorJump = { ui.showFloorDialog = true },
        )
    }
}
