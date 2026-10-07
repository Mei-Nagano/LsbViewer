package sb.linux.client.ui.screens

import androidx.compose.foundation.lazy.LazyListState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import sb.linux.client.domain.topic.PostEntry
import sb.linux.client.ui.session.Session
import sb.linux.client.ui.feature.topic.TopicInteractionViewModel

/** Page loading, floor jumps, thread display, and list scrolling for a topic screen. */
internal class TopicScreenPageActions(
    private val session: Session,
    private val topicId: Long,
    private val scope: CoroutineScope,
    private val state: TopicPagingState,
    private val ui: TopicScreenUiState,
    private val pageCoordinator: TopicPageCoordinator,
    private val interactionViewModel: TopicInteractionViewModel,
    private val listState: LazyListState,
) {
    fun load(page: Int, append: Boolean = false, force: Boolean = false) =
        pageCoordinator.load(page, append, force)

    fun loadAllRemaining() = pageCoordinator.loadAllRemaining()
    fun loadAllForSearch() = pageCoordinator.loadAllForSearch()
    fun restoreCachedPages(maxPage: Int) = pageCoordinator.restoreCachedPages(maxPage)

    fun loadDanmaku() = interactionViewModel.loadDonateFeedCached(topicId) { ui.danmaku = it }

    fun goLocalReplyPage(target: Int, hasMainPost: Boolean) {
        scope.launch {
            val perPage = pageCoordinator.commentsPerPage()
            state.error.value = null
            pageCoordinator.loadUntil(target * perPage + 1)
            state.loadingMore.value = false
            state.localReplyPage.value = target.coerceAtLeast(1)
            listState.scrollToItem(if (hasMainPost) 2 else 1)
        }
    }

    fun jumpFloor(floor: Int) {
        state.loading.value = true
        state.error.value = null
        pageCoordinator.loadFloor(floor) { result ->
            result.onSuccess { loaded ->
                state.page.value = loaded.page
                ui.sortOrder = 1
                state.localReplyPage.value = 1
                state.loadedMaxPage.value = loaded.pageNumber
                ui.collapsedReplyIds = emptySet()
                ui.pendingSearchPost = loaded.target
                if (loaded.target == null) session.showToast("未找到该楼层")
            }.onFailure { state.error.value = it.message }
            state.loading.value = false
        }
    }

    fun jumpToFloor(floor: Int, sortedPosts: List<PostEntry>) {
        ui.returnPos = listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset
        val target = sortedPosts.firstOrNull { it.floor == floor }
        if (target == null) jumpFloor(floor) else ui.pendingSearchPost = target
    }

    fun scrollToComments(hasMainPost: Boolean) {
        scope.launch { listState.animateScrollToItem(if (hasMainPost) 2 else 1) }
    }

    fun openThread(startPost: PostEntry) {
        ui.threadLoading = true
        ui.threadError = null
        ui.threadPosts = null
        pageCoordinator.loadThread(startPost, state.page.value?.totalPages ?: 1) { result ->
            result.onSuccess { posts ->
                if (posts.isEmpty()) ui.threadError = "未找到相关楼层，可能已被删除"
                ui.threadPosts = posts
            }.onFailure { ui.threadError = it.message ?: "加载失败" }
            ui.threadLoading = false
        }
    }
}
