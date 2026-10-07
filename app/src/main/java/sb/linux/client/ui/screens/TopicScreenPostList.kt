package sb.linux.client.ui.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import sb.linux.client.ui.session.Session
import sb.linux.client.domain.topic.TopicPageData

@Composable
internal fun TopicScreenPostList(
    page: TopicPageData,
    session: Session,
    nav: NavHostController,
    listState: LazyListState,
    modifier: Modifier,
    view: TopicScreenViewState,
    ui: TopicScreenUiState,
    paging: TopicPagingState,
    actions: TopicScreenActions,
) {
    val reply = view.reply
    val mainPost = view.mainPost
    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = if (ui.topicSearchOpen) 8.dp else 72.dp, bottom = 8.dp),
    ) {
        topicHeaderItem(
            d = page,
            session = session,
            interactionViewModel = actions.interactionViewModel,
            aiSettings = view.aiSettings,
            readingStats = view.readingStats,
            aiSummary = ui.aiSummary,
            aiBusy = ui.aiBusy,
            aiError = ui.aiError,
            onRunAi = { actions.runAi() },
            onRegenerateAi = { actions.runAi(force = true) },
            onDismissAi = { ui.aiSummary = null; ui.aiError = null },
            onJoinReply = { ui.quoteText = ""; ui.showReply = true },
            pollBusy = ui.pollBusy,
            essenceBusy = ui.essenceBusy,
            essenceTopUpBusy = ui.essenceTopUpBusy,
            onEssenceBusyChange = { ui.essenceBusy = it },
            onEssenceTopUpBusyChange = { ui.essenceTopUpBusy = it },
            onPollBusyChange = { ui.pollBusy = it },
            onLogin = { nav.navigate("login") },
            onRefresh = { actions.load(page.page, force = true) },
            onTitleBottomInRoot = { ui.titleBottomInRoot = it },
        )
        mainPost?.let { main ->
            item(key = "main-${main.id}") {
                PostCard(
                    post = main,
                    loggedIn = session.account.loginState.loggedIn,
                    isOpAuthor = main.authorName == page.posts.firstOrNull()?.authorName,
                    isMainPost = true,
                    onUser = { nav.navigate("user/${main.authorId}") },
                    onQuote = { ui.quoteText = "@${main.authorName} "; ui.showReply = true },
                    onLike = { actions.react(main, 0) },
                    onCoin = { ui.coinTarget = main },
                    onReport = { nav.navigate("report/${main.likeCoinType.ifBlank { "topic" }}/${main.id}") },
                    onFloor = { actions.jumpToFloor(it, view.sortedPosts) },
                    onThread = { actions.openThread(main) },
                    threadEnabled = view.threadEnabled,
                    threadReplyCount = reply.threadReplyCount[main.floor] ?: 0,
                    onCopy = { ui.copyPost = main },
                    onHeadingPosition = { index, y -> ui.headingPositions[index] = y },
                    onBodyLayout = { top, height -> ui.bodyTop = top; ui.bodyHeight = height },
                    onSearchPosition = { y -> ui.searchPositions[main.id] = ui.topicSearchQuery to y },
                    searchHighlight = ui.topicSearchQuery.takeIf { ui.topicSearchOpen }.orEmpty(),
                    onDonate = if (page.donateUrl != null && session.account.loginState.loggedIn) {
                        { ui.showDonate = true }
                    } else null,
                    onEditUser = { nav.navigate("user/$it") },
                )
            }
        }
        if (ui.danmakuLocalOn && ui.danmaku.isNotEmpty()) {
            item(key = "danmaku") { DanmakuBar(ui.danmaku) }
        }
        topicReplyListItems(
            state = TopicReplyListState(
                page = page,
                replies = reply.replies,
                hasTree = reply.hasTree,
                flatTree = reply.flatTree,
                pagedReplies = reply.pagedReplies,
                repliesByFloor = reply.repliesByFloor,
                treeChildCounts = reply.treeChildCounts,
                collapsedIds = ui.collapsedReplyIds,
                highlightPostId = ui.highlightPostId,
                threadEnabled = view.threadEnabled,
                threadReplyCount = reply.threadReplyCount,
                favoriteCommentIds = ui.favoriteCommentIds,
                searchOpen = ui.topicSearchOpen,
                searchQuery = ui.topicSearchQuery,
                loggedIn = session.account.loginState.loggedIn,
                sortOrder = ui.sortOrder,
                localPage = paging.localReplyPage.value,
                localPageTotal = reply.localReplyTotal,
                infinite = view.infinite,
                loadingMore = paging.loadingMore.value,
                replyModeTick = ui.replyModeTick,
                replyGlass = view.replyGlass,
                replyCountText = page.repliesText.ifBlank { reply.replies.size.toString() },
            ),
            actions = TopicReplyListActions(
                onSortChange = { ui.sortOrder = it; session.preferences.saveCommentSortOrder(it) },
                onToggleCollapsed = { postId ->
                    ui.collapsedReplyIds = if (postId in ui.collapsedReplyIds) {
                        ui.collapsedReplyIds - postId
                    } else ui.collapsedReplyIds + postId
                },
                onUser = { nav.navigate("user/$it") },
                onQuote = { post ->
                    ui.quoteText = if (post.floor > 0) "@${post.authorName} #${post.floor} " else "@${post.authorName} "
                    ui.showReply = true
                },
                onLike = { actions.react(it, 0) },
                onCoin = { ui.coinTarget = it },
                onReport = { post -> nav.navigate("report/${post.likeCoinType.ifBlank { "reply" }}/${post.id}") },
                onFloor = { actions.jumpToFloor(it, view.sortedPosts) },
                onThread = { actions.openThread(it) },
                onCopy = { ui.copyPost = it },
                onEditUser = { nav.navigate("user/$it") },
                onSearchPosition = { postId, y -> ui.searchPositions[postId] = ui.topicSearchQuery to y },
                onFavorite = actions::toggleCommentFavorite,
                onLogin = { nav.navigate("login") },
                onLoadMore = { actions.load(page.page + 1, append = true) },
                onLocalPage = { actions.goLocalReplyPage(it, mainPost != null) },
            ),
        )
    }
}
