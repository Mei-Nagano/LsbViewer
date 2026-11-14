package sb.linux.client.ui.screens

import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import sb.linux.client.common.network.Endpoints
import sb.linux.client.domain.topic.PostEntry
import sb.linux.client.domain.topic.TopicPageData
import sb.linux.client.ui.LocalLinkHandler
import sb.linux.client.ui.content.ZoomImageViewer
import sb.linux.client.ui.patterns.user.UserSummaryContent
import sb.linux.client.ui.session.Session
import androidx.compose.foundation.lazy.LazyListState

/** 所有服务器操作继续由现有原生状态和仓库执行，网页只发送选择意图。 */
@Composable
internal fun SourceTopicContent(page: TopicPageData, session: Session, nav: NavHostController,
    listState: LazyListState, view: TopicScreenViewState, ui: TopicScreenUiState,
    actions: TopicScreenActions) {
    val scope = rememberCoroutineScope()
    val linkHandler = LocalLinkHandler.current
    var image by remember(page.topicId) { mutableStateOf<String?>(null) }
    var user by remember(page.topicId) { mutableStateOf<Long?>(null) }
    var sourceActions by remember(page.topicId) { mutableStateOf<PostEntry?>(null) }
    fun scroll(top: Float) {
        scope.launch { listState.animateScrollToItem(topicMainItemIndex(page), top.toInt().coerceAtLeast(0)) }
    }
    fun login(action: () -> Unit) {
        if (session.account.loginState.loggedIn) action() else nav.navigate("login")
    }
    SourceTopicWebView(page, view, ui, session.account.loginState.loggedIn, onAction = { raw ->
        val uri = Uri.parse(raw)
        if (uri.scheme == "lsb-action") {
            val id = uri.lastPathSegment?.toLongOrNull()
            val post = page.posts.firstOrNull { it.id == id }
            when (uri.host) {
                "image" -> uri.getQueryParameter("url")?.takeIf { it.startsWith("https://") || it.startsWith("http://") }?.let { image = it }
                "anchor" -> uri.getQueryParameter("top")?.toFloatOrNull()?.let { scroll(it * ui.sourceGeometry.pixelsPerCss) }
                "user" -> id?.takeIf { it > 0 }?.let { user = it }
                "floor" -> id?.let { actions.jumpToFloor(it.toInt(), view.sortedPosts) }
                "like" -> post?.let { login { actions.react(it, 0) } }
                "coin" -> post?.let { login { ui.coinTarget = it } }
                "donate" -> login { ui.showDonate = true }
                "quote" -> post?.let { login { ui.quoteText = "@${it.authorName}" + if (it.floor > 0) " #${it.floor} " else " "; ui.showReply = true } }
                "report" -> post?.let { nav.navigate("report/${it.likeCoinType.ifBlank { if (it.floor == 0) "topic" else "reply" }}/${it.id}") }
                "copy" -> post?.let { ui.copyPost = it }
                "favorite" -> post?.let(actions::toggleCommentFavorite)
                "favorite-topic" -> login { actions.requestFavorite() }
                "thread" -> post?.let(actions::openThread)
                "collapse", "expand" -> post?.let { ui.collapsedReplyIds = if (it.id in ui.collapsedReplyIds) ui.collapsedReplyIds - it.id else ui.collapsedReplyIds + it.id }
                "source-actions" -> post?.let { sourceActions = it }
            }
        } else if (Endpoints.isForumHost(uri.host) && uri.path == "/topic/${page.topicId}" &&
            (uri.getQueryParameter("floor") != null || uri.getQueryParameter("replyid") != null)) {
            val replyId = uri.getQueryParameter("replyid")?.toLongOrNull()?.takeIf { it > 0 }
            val floor = uri.getQueryParameter("floor")?.toIntOrNull()?.takeIf { it > 0 }
            if (replyId != null) actions.jumpReply(replyId)
            else if (floor != null) actions.jumpToFloor(floor, view.sortedPosts)
            else linkHandler?.invoke(raw)
        } else linkHandler?.invoke(Endpoints.abs(raw))
    }) {
        Column {
            page.redPacket?.let { RedPacketCard(it) }
            page.treasurePool?.let { TreasurePoolCard(it) }
            if (page.poll != null || page.essenceReview != null || page.essenceApplication != null || page.resourceVote != null || page.footerPanels.isNotEmpty()) {
                TopicInteractivePanels(page, session, actions, ui, onLogin = { nav.navigate("login") },
                    onRefresh = { actions.load(page.page, force = true) })
            }
            if (ui.danmakuLocalOn && ui.danmaku.isNotEmpty()) DanmakuBar(ui.danmaku)
        }
    }
    image?.let { ZoomImageViewer(listOf(it), 0) { image = null } }
    user?.let { id -> Dialog(onDismissRequest = { user = null }) {
        Surface(shape = MaterialTheme.shapes.extraLarge) {
            UserSummaryContent(id, session.userSummaryRepository, { user = null }) { raw ->
                user = null
                linkHandler?.invoke(Endpoints.abs(raw))
            }
        }
    } }
    sourceActions?.let { post -> AlertDialog(
        onDismissRequest = { sourceActions = null }, title = { Text("帖子操作") },
        text = { Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
            if (post.floor == 0) TopicForumMoveAction()
            TopicPostSourceActions(post)
        } }, confirmButton = { TextButton(onClick = { sourceActions = null }) { Text("关闭") } }) }
}
