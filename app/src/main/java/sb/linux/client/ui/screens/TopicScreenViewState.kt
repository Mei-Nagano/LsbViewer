package sb.linux.client.ui.screens

import sb.linux.client.common.content.HtmlTextConversion
import sb.linux.client.domain.ai.AiAssistantSettings

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import sb.linux.client.ui.common.content.TopicTocHeading
import sb.linux.client.ui.common.content.extractTopicTocHeadings
import sb.linux.client.ui.session.Session
import sb.linux.client.domain.topic.TopicPageData
import sb.linux.client.domain.topic.PostEntry

internal data class TopicScreenViewState(
    val page: TopicPageData?,
    val sortedPosts: List<PostEntry>,
    val mainPost: PostEntry?,
    val searchHits: List<TopicSearchHit>,
    val readingStats: Pair<Int, Int>?,
    val tocHeadings: List<TopicTocHeading>,
    val reply: TopicReplyPresentation,
    val currentFloor: Int,
    val bodyVisible: Boolean,
    val readingProgress: Float,
    val topBarPx: Float,
    val showTitleInTopBar: Boolean,
    val infinite: Boolean,
    val repliesPerPage: Int,
    val threadEnabled: Boolean,
    val aiSettings: AiAssistantSettings,
)

@Composable
internal fun rememberTopicScreenViewState(
    session: Session,
    topicId: Long,
    paging: TopicPagingState,
    pageCoordinator: TopicPageCoordinator,
    ui: TopicScreenUiState,
    listState: LazyListState,
    aiSettings: AiAssistantSettings,
): TopicScreenViewState {
    val page = paging.page.value
    val density = LocalDensity.current
    val topBarPx = with(density) { 64.dp.toPx() }
    val idToFloor = remember(page) { page?.posts.orEmpty().associate { it.id to it.floor } }
    val latestIdToFloor by rememberUpdatedState(idToFloor)
    DisposableEffect(topicId) {
        onDispose {
            val first = listState.layoutInfo.visibleItemsInfo.firstOrNull()
            val key = first?.key as? String
            val replyId = key?.removePrefix("r-")?.toLongOrNull()
            val mainId = key?.removePrefix("main-")?.toLongOrNull()
            pageCoordinator.saveReadingPosition(
                floor = replyId?.let { latestIdToFloor[it] } ?: 0,
                listIndex = listState.firstVisibleItemIndex,
                scrollOffset = listState.firstVisibleItemScrollOffset,
                // 帖子标识用于版本化锚点；取不到时只写旧四段记录
                postId = replyId ?: mainId ?: 0L,
            )
        }
    }
    val currentFloor by remember(idToFloor) {
        derivedStateOf {
            listState.layoutInfo.visibleItemsInfo
                .firstOrNull { (it.key as? String)?.startsWith("r-") == true }
                ?.let { info -> (info.key as String).removePrefix("r-").toLongOrNull()?.let { idToFloor[it] } }
                ?: 0
        }
    }
    val sortedPosts = remember(page, ui.sortOrder) { sortTopicPosts(page?.posts.orEmpty(), ui.sortOrder) }
    val mainPost = sortedPosts.firstOrNull { it.floor == 0 }
    val searchHits = remember(page?.posts, ui.topicSearchQuery) {
        findTopicSearchHits(page?.posts.orEmpty(), ui.topicSearchQuery)
    }
    val readingStats = remember(mainPost?.contentHtml) {
        mainPost?.contentHtml?.let { estimateReading(HtmlTextConversion.htmlToText(it)) }
    }
    val tocHeadings = remember(mainPost?.contentHtml) { extractTopicTocHeadings(mainPost?.contentHtml) }
    val infinite = pageCoordinator.topicInfiniteScrollEnabled()
    val repliesPerPage = pageCoordinator.commentsPerPage()
    val reply by remember(
        page, sortedPosts, ui.sortOrder, paging.localReplyPage.value, infinite,
        repliesPerPage, ui.collapsedReplyIds,
    ) {
        derivedStateOf {
            buildTopicReplyPresentation(
                sortedPosts = sortedPosts,
                data = page,
                sortOrder = ui.sortOrder,
                localReplyPage = paging.localReplyPage.value,
                infinite = infinite,
                repliesPerPage = repliesPerPage,
                collapsedIds = ui.collapsedReplyIds,
            )
        }
    }
    val bodyVisible by remember {
        derivedStateOf {
            listState.layoutInfo.visibleItemsInfo.any { (it.key as? String)?.startsWith("main-") == true } &&
                ui.bodyHeight > 0 && ui.bodyTop + ui.bodyHeight > ui.contentTopInRoot + topBarPx &&
                ui.bodyTop < ui.contentTopInRoot + listState.layoutInfo.viewportEndOffset
        }
    }
    val readingProgress by remember {
        derivedStateOf {
            val viewportHeight = (listState.layoutInfo.viewportEndOffset - topBarPx).coerceAtLeast(1f)
            ((ui.contentTopInRoot + topBarPx - ui.bodyTop) /
                (ui.bodyHeight - viewportHeight).coerceAtLeast(1f)).coerceIn(0f, 1f)
        }
    }
    val showTitleInTopBar by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || ui.titleBottomInRoot - ui.contentTopInRoot <= topBarPx
        }
    }
    return TopicScreenViewState(
        page, sortedPosts, mainPost, searchHits, readingStats, tocHeadings, reply,
        currentFloor, bodyVisible, readingProgress, topBarPx, showTitleInTopBar,
        infinite, repliesPerPage, session.threadDialogEnabled, aiSettings,
    )
}
