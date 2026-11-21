package sb.linux.client.ui.screens

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import sb.linux.client.domain.topic.PostEntry
import sb.linux.client.ui.session.Session

@Composable
internal fun TopicScreenEffects(
    session: Session,
    topicId: Long,
    initialPage: Int,
    initialFloor: Int,
    initialReplyId: Long,
    restoreReading: Boolean,
    paging: TopicPagingState,
    ui: TopicScreenUiState,
    actions: TopicScreenActions,
    listState: LazyListState,
    reply: TopicReplyPresentation,
    sortedPosts: List<PostEntry>,
    mainPost: PostEntry?,
    topicInfinite: Boolean,
    repliesPerPage: Int,
    searchHits: List<TopicSearchHit>,
) {
    LaunchedEffect(session.homeBrowse.refreshRequest) {
        val request = session.homeBrowse.refreshRequest
        if (request != ui.consumedBlockRefresh) {
            ui.consumedBlockRefresh = request
            actions.load(paging.page.value?.page ?: initialPage, force = true)
        }
    }
    LaunchedEffect(paging.page.value?.topicId, paging.page.value?.favorited) {
        paging.page.value?.let { if (!ui.favoriteBusy) ui.isFav = it.favorited }
    }
    LaunchedEffect(ui.topicSearchQuery, searchHits.size) {
        ui.topicSearchIndex = ui.topicSearchIndex.coerceIn(0, (searchHits.size - 1).coerceAtLeast(0))
    }
    TopicFloorJumpEffect(
        pendingPost = ui.pendingSearchPost,
        flatTree = reply.flatTree,
        localPage = paging.localReplyPage.value,
        collapsedIds = ui.collapsedReplyIds,
        sortedPosts = sortedPosts,
        mainPost = mainPost,
        hasTree = reply.hasTree,
        repliesByFloor = reply.repliesByFloor,
        replyTree = reply.replyTree,
        infinite = topicInfinite,
        repliesPerPage = repliesPerPage,
        replies = reply.replies,
        pagedReplies = reply.pagedReplies,
        danmakuEnabled = ui.danmakuLocalOn,
        danmaku = ui.danmaku,
        page = paging.page.value,
        listState = listState,
        searchOpen = ui.topicSearchOpen,
        searchQuery = ui.topicSearchQuery,
        searchPositions = ui.searchPositions,
        contentTopInRoot = ui.contentTopInRoot,
        pendingRestoreOffset = ui.pendingRestoreOffset,
        onCollapsedIdsChange = { ui.collapsedReplyIds = it },
        onLocalPageChange = { paging.localReplyPage.value = it },
        onHighlight = { ui.highlightPostId = it },
        onPendingPostChange = { ui.pendingSearchPost = it },
        onRestoreOffsetConsumed = { ui.pendingRestoreOffset = null },
        sourceGeometry = ui.sourceGeometry,
    )
    TopicHighlightExpiryEffect(ui.highlightPostId) { ui.highlightPostId = null }
    TopicSearchAutoSelectEffect(
        query = ui.topicSearchQuery,
        open = ui.topicSearchOpen,
        firstHit = searchHits.firstOrNull()?.post,
        onSelect = { ui.pendingSearchPost = it },
    )
    TopicInitialLoadEffect(
        topicId = topicId,
        initialPage = initialPage,
        initialFloor = initialFloor,
        initialReplyId = initialReplyId,
        restoreReading = restoreReading,
        paging = paging,
        ui = ui,
        actions = actions,
    )
    LaunchedEffect(session.account.loginState.loggedIn) {
        if (session.account.loginState.loggedIn != ui.lastLoggedIn) {
            ui.lastLoggedIn = session.account.loginState.loggedIn
            actions.load(paging.page.value?.page ?: initialPage)
            actions.loadDanmaku()
        }
    }
    LaunchedEffect(paging.page.value) {
        if (paging.page.value == null) return@LaunchedEffect
        val cached = actions.cachedAiSummary()
        if (cached != null) ui.aiSummary = cached
        else if (actions.shouldAutoRunAiSummary()) actions.runAi()
    }
    val listLast by remember {
        derivedStateOf { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1 }
    }
    LaunchedEffect(
        listLast, paging.page.value?.posts?.size, paging.page.value?.page, paging.page.value?.totalPages,
        paging.loadingMore.value, paging.loading.value, ui.sourceGeometry.ready,
    ) {
        val page = paging.page.value
        val totalItems = listState.layoutInfo.totalItemsCount
        if (topicInfinite && page != null && ui.sourceGeometry.ready && listLast >= totalItems - 2 && page.posts.isNotEmpty() &&
            page.page < page.totalPages && !paging.loadingMore.value && !paging.loading.value
        ) actions.load(page.page + 1, append = true)
    }
}

@Composable
private fun TopicInitialLoadEffect(
    topicId: Long,
    initialPage: Int,
    initialFloor: Int,
    initialReplyId: Long,
    restoreReading: Boolean,
    paging: TopicPagingState,
    ui: TopicScreenUiState,
    actions: TopicScreenActions,
) {
    LaunchedEffect(topicId) {
        // 同一返回栈条目保留当前页；新入口才选择页码和阅读锚点。
        if (paging.page.value == null) {
            val rebuilt = if (paging.loadedMaxPage.value > 1) {
                actions.restoreCachedPages(paging.loadedMaxPage.value)
            } else null
            if (rebuilt != null) {
                paging.page.value = rebuilt
                paging.loading.value = false
            } else {
                val anchor = if (restoreReading) actions.readingAnchor() else null
                val floor = if (restoreReading) anchor?.floor ?: actions.readingPosition()?.floor ?: 0 else initialFloor
                when {
                    initialReplyId > 0 -> actions.jumpReply(initialReplyId)
                    anchor != null && anchor.floor > 0 && anchor.postId > 0 -> actions.jumpReply(anchor.postId)
                    floor > 0 -> actions.jumpFloor(floor)
                    else -> actions.load(initialPage)
                }
            }
            if (rebuilt != null && restoreReading) {
                val anchor = actions.readingAnchor()
                val target = rebuilt.posts.firstOrNull {
                    if (anchor != null && anchor.postId > 0) it.id == anchor.postId
                    else it.floor == (anchor?.floor ?: actions.readingPosition()?.floor) && it.floor > 0
                }
                if (target != null) ui.pendingSearchPost = target
            }
        }
        if (!ui.danmakuInitialized) {
            ui.danmakuInitialized = true
            actions.loadDanmaku()
        }
    }
}
