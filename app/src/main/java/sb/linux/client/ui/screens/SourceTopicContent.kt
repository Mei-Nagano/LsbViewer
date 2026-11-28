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
import sb.linux.client.ui.content.ViewerImage
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
    var image by remember(page.topicId) { mutableStateOf<ViewerImage?>(null) }
    var user by remember(page.topicId, session.avatarInfoCardEnabled) { mutableStateOf<Long?>(null) }
    var sourceActions by remember(page.topicId) { mutableStateOf<PostEntry?>(null) }
    var displayedInviteRelay by remember(page) { mutableStateOf(page.inviteRelay) }
    var inviteRelayDialogUrl by remember(page.topicId) { mutableStateOf<String?>(null) }
    fun scroll(top: Float) {
        scope.launch { listState.animateScrollToItem(topicMainItemIndex(page), top.toInt().coerceAtLeast(0)) }
    }
    fun login(action: () -> Unit) {
        if (session.account.loginState.loggedIn) action() else nav.navigate("login")
    }
    SourceTopicWebView(page, view, ui, session.account.loginState.loggedIn, session.sourceTopicResourceRepository, onAction = { raw ->
        val uri = Uri.parse(raw)
        if (uri.scheme == "lsb-action") {
            val id = uri.lastPathSegment?.toLongOrNull()
            val post = page.posts.firstOrNull { it.id == id }
            when (uri.host) {
                "image" -> uri.getQueryParameter("url")?.takeIf { it.startsWith("https://") || it.startsWith("http://") }?.let { url ->
                    val eligible = uri.getQueryParameter("eligible") == "1" &&
                        sb.linux.client.data.StickerPackHtmlParser.isCollectableImageUrl(url)
                    image = ViewerImage(
                        url = url,
                        caption = uri.getQueryParameter("alt").orEmpty().trim(),
                        collectable = eligible,
                    )
                }
                "collect-sticker" -> {
                    val url = uri.getQueryParameter("url")?.takeIf { it.startsWith("https://") || it.startsWith("http://") }
                    val alt = uri.getQueryParameter("alt").orEmpty().trim()
                    val collectConfig = page.stickerCollect?.takeIf { it.enabled }
                    if (url != null && collectConfig != null) {
                        scope.launch {
                            val result = session.stickerPackRepository.collectImage(collectConfig, url, alt)
                            session.showToast(result.message)
                            ui.sourceBrowser?.evaluateJavascript(
                                "window.lsbContent&&window.lsbContent.stickerCollectResult(${org.json.JSONObject.quote(url)},${result.ok})",
                                null,
                            )
                        }
                    }
                }
                "anchor" -> uri.getQueryParameter("top")?.toFloatOrNull()?.let { scroll(it * ui.sourceGeometry.pixelsPerCss) }
                "user" -> id?.takeIf { it > 0 }?.let {
                    if (session.avatarInfoCardEnabled) user = it else nav.navigate("user/$it")
                }
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
            displayedInviteRelay?.let { relay ->
                InviteRelayCard(
                    panel = relay,
                    loggedIn = session.account.loginState.loggedIn,
                    onLogin = { nav.navigate("login") },
                    onOpenDialog = { url -> inviteRelayDialogUrl = url },
                    onOpenLink = { url -> linkHandler?.invoke(Endpoints.abs(url)) },
                )
            }
            if (page.poll != null || page.essenceReview != null || page.essenceApplication != null || page.resourceVote != null || page.footerPanels.isNotEmpty()) {
                TopicInteractivePanels(page, session, actions, ui, onLogin = { nav.navigate("login") },
                    onRefresh = { actions.load(page.page, force = true) })
            }
            if (ui.danmakuLocalOn && ui.danmaku.isNotEmpty()) DanmakuBar(ui.danmaku)
        }
    }
    inviteRelayDialogUrl?.let { url ->
        InviteRelayDialog(
            session = session,
            initialUrl = url,
            onDismiss = { inviteRelayDialogUrl = null },
            onPanelUpdated = { updated ->
                displayedInviteRelay = updated
                session.topicPageCacheRepository.clearTopic(page.topicId)
            },
        )
    }
    image?.let { target ->
        val collectConfig = page.stickerCollect?.takeIf { it.enabled }
        ZoomImageViewer(
            images = listOf(target),
            initialIndex = 0,
            onCollectSticker = collectConfig?.let { config ->
                { item -> session.stickerPackRepository.collectImage(config, item.url, item.caption) }
            },
            onDismiss = { image = null },
        )
    }
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
