package sb.linux.client.ui.screens

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import sb.linux.client.ui.feature.topic.TopicPinnedSnapshot
import sb.linux.client.domain.topic.PostEntry
import sb.linux.client.ui.session.Session

@Composable
internal fun TopicScreenEffects(
    session: Session,
    topicId: Long,
    initialPage: Int,
    initialFloor: Int,
    pinnedSnapshot: TopicPinnedSnapshot?,
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
    LaunchedEffect(topicId) { session.preferences.clearFloatingTopic(topicId) }
    LaunchedEffect(paging.page.value?.topicId, paging.page.value?.favorited) {
        paging.page.value?.let { ui.isFav = it.favorited }
    }
    LaunchedEffect(ui.topicSearchQuery, searchHits.size) {
        ui.topicSearchIndex = ui.topicSearchIndex.coerceIn(0, (searchHits.size - 1).coerceAtLeast(0))
    }
    LaunchedEffect(ui.sortOrder, paging.page.value?.page, paging.page.value?.totalPages, paging.loading.value, paging.loadingMore.value) {
        val page = paging.page.value
        if (ui.sortOrder == 2 && page != null && !paging.loading.value && !paging.loadingMore.value &&
            page.page < page.totalPages && paging.reverseStalledAtPage.value != page.page
        ) actions.loadAllRemaining()
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
    )
    TopicHighlightExpiryEffect(ui.highlightPostId) { ui.highlightPostId = null }
    TopicSearchAutoSelectEffect(
        query = ui.topicSearchQuery,
        open = ui.topicSearchOpen,
        firstHit = searchHits.firstOrNull()?.post,
        onSelect = { ui.pendingSearchPost = it },
    )
    LaunchedEffect(paging.loading.value, paging.page.value) {
        val page = paging.page.value
        if (!ui.floorJumped && !paging.loading.value && page != null && initialFloor > 0) {
            ui.floorJumped = true
            if (page.posts.any { it.floor == initialFloor }) actions.jumpToFloor(initialFloor, sortedPosts)
            else actions.jumpFloor(initialFloor)
        }
    }
    LaunchedEffect(paging.loading.value, paging.page.value, initialFloor) {
        if (ui.readingPositionRestored || paging.loading.value || paging.page.value == null) return@LaunchedEffect
        ui.readingPositionRestored = true
        pinnedSnapshot?.let {
            withFrameNanos { }
            listState.scrollToItem(it.listIndex, it.offset)
            ui.returnPos = null
        }
    }
    TopicInitialLoadEffect(
        session = session,
        topicId = topicId,
        initialPage = initialPage,
        pinnedSnapshot = pinnedSnapshot,
        paging = paging,
        ui = ui,
        actions = actions,
    )
    var lastLoggedIn by remember { mutableStateOf(session.account.loginState.loggedIn) }
    LaunchedEffect(session.account.loginState.loggedIn) {
        if (session.account.loginState.loggedIn != lastLoggedIn) {
            lastLoggedIn = session.account.loginState.loggedIn
            actions.load(initialPage)
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
        paging.loadingMore.value, paging.loading.value,
    ) {
        val page = paging.page.value
        val totalItems = listState.layoutInfo.totalItemsCount
        if (topicInfinite && page != null && listLast >= totalItems - 3 && page.posts.isNotEmpty() &&
            page.page < page.totalPages && !paging.loadingMore.value && !paging.loading.value
        ) actions.load(page.page + 1, append = true)
    }
}

@Composable
private fun TopicInitialLoadEffect(
    session: Session,
    topicId: Long,
    initialPage: Int,
    pinnedSnapshot: TopicPinnedSnapshot?,
    paging: TopicPagingState,
    ui: TopicScreenUiState,
    actions: TopicScreenActions,
) {
    LaunchedEffect(topicId) {
        if (pinnedSnapshot != null) {
            ui.sortOrder = pinnedSnapshot.sort
            paging.localReplyPage.value = pinnedSnapshot.localPage
            ui.collapsedReplyIds = pinnedSnapshot.collapsed
            paging.loadedMaxPage.value = pinnedSnapshot.topic.page
            paging.page.value = pinnedSnapshot.topic
            ui.danmaku = pinnedSnapshot.danmaku
            ui.danmakuLocalOn = pinnedSnapshot.danmakuEnabled
            paging.loading.value = false
            return@LaunchedEffect
        }
        val rebuilt = if (paging.loadedMaxPage.value > 1) {
            actions.restoreCachedPages(paging.loadedMaxPage.value)
        } else null
        if (rebuilt != null) {
            paging.loading.value = false
            paging.page.value = rebuilt
        } else actions.load(initialPage)
        actions.loadDanmaku()
    }
}
