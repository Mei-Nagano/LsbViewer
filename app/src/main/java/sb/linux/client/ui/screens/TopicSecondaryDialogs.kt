package sb.linux.client.ui.screens

import androidx.compose.runtime.Composable
import sb.linux.client.domain.topic.PostEntry
import sb.linux.client.ui.session.Session
import sb.linux.client.domain.topic.TopicPageData
import sb.linux.client.ui.feature.topic.TopicInteractionViewModel

@Composable
internal fun TopicSecondaryDialogs(
    session: Session,
    topicId: Long,
    interactionViewModel: TopicInteractionViewModel,
    page: TopicPageData?,
    copyPost: PostEntry?,
    onCopyDismiss: () -> Unit,
    onCopied: () -> Unit,
    onViewUserReplies: (PostEntry) -> Unit,
    userReplies: List<PostEntry>?,
    userRepliesTitle: String,
    onUserRepliesDismiss: () -> Unit,
    onUser: (Long) -> Unit,
    onJumpFloor: (Int) -> Unit,
    exportDialogOpen: Boolean,
    exportBusy: String?,
    loadingPosts: Boolean,
    allLoaded: Boolean,
    onLoadAll: () -> Unit,
    onExportDismiss: () -> Unit,
    onExport: (String, String, Int, Int, Boolean, Set<Long>) -> Unit,
    collectionOpen: Boolean,
    onCollectionDismiss: () -> Unit,
    donateOpen: Boolean,
    onDonateDismiss: () -> Unit,
    onDonateSuccess: () -> Unit,
) {
    copyPost?.let { target ->
        PostCopySheet(
            post = target,
            onDismiss = onCopyDismiss,
            onCopied = onCopied,
            onViewUserReplies = { onViewUserReplies(target) },
        )
    }

    if (userReplies != null) {
        ThreadDialog(
            posts = userReplies,
            loading = false,
            error = if (userReplies.isEmpty()) "TA 在本帖还没有回复" else null,
            currentPostId = -1L,
            onDismiss = onUserRepliesDismiss,
            onUser = onUser,
            title = userRepliesTitle,
            countText = { "共 $it 条回复" },
            onJumpFloor = onJumpFloor,
        )
    }

    if (exportDialogOpen) {
        ExportDialog(
            posts = page?.posts.orEmpty(),
            busy = exportBusy,
            loadingPosts = loadingPosts,
            onLoadAll = onLoadAll,
            allLoaded = allLoaded,
            onDismiss = onExportDismiss,
            onExport = onExport,
        )
    }

    if (collectionOpen) {
        TopicCollectionSheet(
            session = session,
            topicPath = "/topic/$topicId",
            onDismiss = onCollectionDismiss,
        )
    }

    if (donateOpen) {
        DonateSheet(
            viewModel = interactionViewModel,
            tid = topicId,
            onDismiss = onDonateDismiss,
            onSuccess = onDonateSuccess,
        )
    }
}
