package sb.linux.client.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavHostController
import sb.linux.client.ui.session.Session

@Composable
internal fun TopicScreenDialogs(
    session: Session,
    topicId: Long,
    nav: NavHostController,
    paging: TopicPagingState,
    ui: TopicScreenUiState,
    view: TopicScreenViewState,
    actions: TopicScreenActions,
    exportColors: androidx.compose.material3.ColorScheme,
) {
    TopicPrimaryDialogs(
        session = session,
        topicId = topicId,
        page = paging.page.value,
        replyViewModel = actions.replyViewModel,
        captchaOverride = ui.captchaOverride,
        replyOpen = ui.showReply,
        replyText = ui.quoteText,
        onReplyDismiss = { ui.showReply = false },
        onLoadPage = { actions.load(it, append = true, force = true) },
        favoriteDialogOpen = ui.showUnfavDialog,
        onFavoriteDialogDismiss = { ui.showUnfavDialog = false },
        onUnfavorite = { ui.showUnfavDialog = false; actions.toggleFavorite() },
        floorDialogOpen = ui.showFloorDialog,
        currentFloor = view.currentFloor,
        onFloorDialogDismiss = { ui.showFloorDialog = false },
        onJumpFloor = {
            ui.showFloorDialog = false
            actions.jumpToFloor(it, view.sortedPosts)
        },
        threadLoading = ui.threadLoading,
        threadError = ui.threadError,
        threadPosts = ui.threadPosts,
        onThreadDismiss = { ui.threadPosts = null; ui.threadError = null },
        onUser = { nav.navigate("user/$it") },
        coinTarget = ui.coinTarget,
        onCoinDismiss = { ui.coinTarget = null },
        onCoin = { target, points, reason ->
            ui.coinTarget = null
            actions.react(target, points, reason)
        },
    )
    TopicSecondaryDialogs(
        session = session,
        topicId = topicId,
        interactionViewModel = actions.interactionViewModel,
        page = paging.page.value,
        copyPost = ui.copyPost,
        onCopyDismiss = { ui.copyPost = null },
        onCopied = { session.showToast("已复制到剪贴板") },
        onViewUserReplies = { target ->
            val current = paging.page.value
            if (current != null) {
                ui.userRepliesTitle = "${target.authorName} 在本帖的回复"
                ui.userReplies = current.posts.filter { it.authorId == target.authorId && it.floor > 0 }
            }
        },
        userReplies = ui.userReplies,
        userRepliesTitle = ui.userRepliesTitle,
        onUserRepliesDismiss = { ui.userReplies = null },
        onUser = { nav.navigate("user/$it") },
        onJumpFloor = {
            ui.userReplies = null
            actions.jumpToFloor(it, view.sortedPosts)
        },
        exportDialogOpen = ui.showExportDialog,
        exportBusy = ui.exportBusy,
        loadingPosts = paging.loadingMore.value,
        allLoaded = paging.allSearchPagesLoaded.value,
        onLoadAll = actions::loadAllForSearch,
        onExportDismiss = { if (ui.exportBusy == null) ui.showExportDialog = false },
        onExport = { kind, scope, from, to, multiPage, selectedIds ->
            ui.showExportDialog = false
            actions.export(kind, scope, from, to, multiPage, selectedIds, exportColors)
        },
        collectionOpen = ui.showCollectionSheet,
        onCollectionDismiss = { ui.showCollectionSheet = false },
        donateOpen = ui.showDonate,
        onDonateDismiss = { ui.showDonate = false },
        onDonateSuccess = {
            ui.showDonate = false
            session.showToast("打赏成功")
            actions.loadDanmaku()
        },
    )
}
