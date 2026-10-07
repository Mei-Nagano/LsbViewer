package sb.linux.client.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.foundation.layout.BoxScope
import androidx.navigation.NavHostController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import sb.linux.client.common.network.Endpoints
import sb.linux.client.ui.feature.topic.TopicPinnedSnapshot
import sb.linux.client.ui.session.Session
import sb.linux.client.domain.topic.TopicPageData

@Composable
internal fun BoxScope.TopicScreenTopBarOverlay(
    session: Session,
    nav: NavHostController,
    topicId: Long,
    scope: CoroutineScope,
    listState: LazyListState,
    paging: TopicPagingState,
    ui: TopicScreenUiState,
    actions: TopicScreenActions,
    view: TopicScreenViewState,
    context: Context,
    clipboard: ClipboardManager,
) {
    TopicTopBar(
        modifier = Modifier.align(Alignment.TopCenter),
        title = view.page?.title.orEmpty(),
        showTitle = view.showTitleInTopBar,
        searchOpen = ui.topicSearchOpen,
        menuExpanded = ui.menuOpen,
        danmakuVisible = ui.danmakuLocalOn,
        exporting = ui.exportBusy != null,
        infiniteScroll = view.infinite,
        onMenuExpandedChange = { ui.menuOpen = it },
        onBack = { nav.popBackStack() },
        onComments = { actions.scrollToComments(view.mainPost != null) },
        onJumpFloor = { ui.showFloorDialog = true },
        onSearch = { ui.topicSearchOpen = true },
        onOpenBrowser = {
            runCatching {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse(Endpoints.abs("/topic/$topicId")))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            }.onFailure { session.showToast("打开失败：${it.message}") }
        },
        onCopyLink = {
            clipboard.setText(AnnotatedString(Endpoints.abs("/topic/$topicId")))
            session.showToast("链接已复制")
        },
        onToggleDanmaku = {
            ui.danmakuLocalOn = !ui.danmakuLocalOn
            session.preferences.saveDanmaku(ui.danmakuLocalOn)
        },
        onExport = { ui.showExportDialog = true },
        onCollections = { ui.showCollectionSheet = true },
        onRefresh = { actions.load(paging.page.value?.page ?: 1, force = true); actions.loadDanmaku() },
        onToggleScrollMode = {
            session.preferences.saveScrollModeOverride("topic", !view.infinite)
            paging.localReplyPage.value = 1
            ui.replyModeTick++
            scope.launch { listState.scrollToItem(0) }
            session.showToast(if (!view.infinite) "已切换为无限滚动" else "已切换为翻页模式")
        },
    )
}

internal fun TopicPageData.pinSnapshot(
    session: Session,
    listState: LazyListState,
    paging: TopicPagingState,
    ui: TopicScreenUiState,
    topicId: Long,
) {
    session.floatingTopicSnapshot = TopicPinnedSnapshot(
        this,
        listState.firstVisibleItemIndex,
        listState.firstVisibleItemScrollOffset,
        paging.localReplyPage.value,
        ui.sortOrder,
        ui.collapsedReplyIds,
        ui.danmaku,
        ui.danmakuLocalOn,
    )
    session.preferences.pinFloatingTopic(topicId, title)
}

