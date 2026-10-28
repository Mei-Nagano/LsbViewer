package sb.linux.client.ui.screens

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import sb.linux.client.domain.account.LoginVerification
import sb.linux.client.domain.topic.DEFAULT_COMMENT_REWARD_TIERS
import sb.linux.client.domain.topic.PostEntry
import sb.linux.client.ui.session.Session
import sb.linux.client.domain.topic.TopicPageData
import sb.linux.client.ui.feature.topic.TopicReplyViewModel

@Composable
internal fun TopicPrimaryDialogs(
    session: Session,
    topicId: Long,
    page: TopicPageData?,
    replyViewModel: TopicReplyViewModel,
    captchaOverride: MutableState<LoginVerification?>,
    replyOpen: Boolean,
    replyText: String,
    onReplyDismiss: () -> Unit,
    onLoadPage: (Int) -> Unit,
    favoriteDialogOpen: Boolean,
    onFavoriteDialogDismiss: () -> Unit,
    onUnfavorite: () -> Unit,
    floorDialogOpen: Boolean,
    currentFloor: Int,
    onFloorDialogDismiss: () -> Unit,
    onJumpFloor: (Int) -> Unit,
    threadLoading: Boolean,
    threadError: String?,
    threadPosts: List<PostEntry>?,
    onThreadDismiss: () -> Unit,
    onUser: (Long) -> Unit,
    coinTarget: PostEntry?,
    onCoinDismiss: () -> Unit,
    onCoin: (PostEntry, Int, String) -> Unit,
) {
    if (favoriteDialogOpen) {
        sb.linux.client.ui.components.feedback.AppConfirmDialog(
            title = "取消收藏", message = "确定取消收藏吗？",
            onDismiss = onFavoriteDialogDismiss, onConfirm = onUnfavorite, confirmText = "确定",
        )
    }

    TopicReplyDialogHost(
        isOpen = replyOpen,
        session = session,
        topicId = topicId,
        page = page,
        initial = replyText,
        captchaOverride = captchaOverride,
        viewModel = replyViewModel,
        onDismiss = onReplyDismiss,
        onLoad = onLoadPage,
    )

    if (floorDialogOpen) {
        FloorJumpDialog(
            maxFloor = page?.repliesText?.toIntOrNull() ?: ((page?.totalPages ?: 1) * 20),
            initial = currentFloor,
            onDismiss = onFloorDialogDismiss,
            onJump = onJumpFloor,
        )
    }

    if (threadLoading || threadError != null || threadPosts != null) {
        ThreadDialog(
            posts = threadPosts,
            loading = threadLoading,
            error = threadError,
            currentPostId = -1L,
            onDismiss = onThreadDismiss,
            onUser = onUser,
            onJumpFloor = { onJumpFloor(it) },
        )
    }

    coinTarget?.let { target ->
        CoinDialog(
            onDismiss = onCoinDismiss,
            tiers = target.coinTiers.ifEmpty { DEFAULT_COMMENT_REWARD_TIERS },
            onConfirm = { points, reason -> onCoin(target, points, reason) },
        )
    }
}
