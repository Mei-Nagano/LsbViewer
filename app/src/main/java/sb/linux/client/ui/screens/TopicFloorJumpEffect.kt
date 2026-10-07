package sb.linux.client.ui.screens

import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import kotlinx.coroutines.delay
import sb.linux.client.domain.topic.DanmakuItem
import sb.linux.client.domain.topic.PostEntry
import sb.linux.client.domain.topic.TopicPageData
import sb.linux.client.domain.topic.reply.ReplyNode

@Composable
internal fun TopicFloorJumpEffect(
    pendingPost: PostEntry?,
    flatTree: List<Pair<PostEntry, Int>>,
    localPage: Int,
    collapsedIds: Set<Long>,
    sortedPosts: List<PostEntry>,
    mainPost: PostEntry?,
    hasTree: Boolean,
    repliesByFloor: Map<Int, PostEntry>,
    replyTree: List<ReplyNode>,
    infinite: Boolean,
    repliesPerPage: Int,
    replies: List<PostEntry>,
    pagedReplies: List<PostEntry>,
    danmakuEnabled: Boolean,
    danmaku: List<DanmakuItem>,
    page: TopicPageData?,
    listState: LazyListState,
    searchOpen: Boolean,
    searchQuery: String,
    searchPositions: Map<Long, Pair<String, Float>>,
    contentTopInRoot: Float,
    pendingRestoreOffset: Int?,
    onCollapsedIdsChange: (Set<Long>) -> Unit,
    onLocalPageChange: (Int) -> Unit,
    onHighlight: (Long?) -> Unit,
    onPendingPostChange: (PostEntry?) -> Unit,
    onRestoreOffsetConsumed: () -> Unit,
) {
    LaunchedEffect(pendingPost, flatTree, localPage, collapsedIds, sortedPosts) {
        val target = pendingPost ?: return@LaunchedEffect
        val isMain = target.id == mainPost?.id
        if (!isMain && hasTree) {
            var root = target
            val ancestors = mutableSetOf<Long>()
            val visited = mutableSetOf(target.floor)
            while (true) {
                val parent = repliesByFloor[root.parentFloor] ?: break
                if (!visited.add(parent.floor)) break
                ancestors.add(parent.id)
                root = parent
            }
            val topIndex = replyTree.indexOfFirst { it.post.id == root.id }
            val targetPage = if (infinite || topIndex < 0) localPage else topIndex / repliesPerPage + 1
            val expanded = collapsedIds - ancestors
            if (expanded != collapsedIds || targetPage != localPage) {
                onCollapsedIdsChange(expanded)
                onLocalPageChange(targetPage)
                return@LaunchedEffect
            }
        } else if (!isMain && !infinite) {
            val index = replies.indexOfFirst { it.id == target.id }
            if (index < 0) {
                onPendingPostChange(null)
                return@LaunchedEffect
            }
            val targetPage = index / repliesPerPage + 1
            if (targetPage != localPage) {
                onLocalPageChange(targetPage)
                return@LaunchedEffect
            }
        }
        val visible = if (hasTree) flatTree.map { it.first } else pagedReplies
        val replyIndex = visible.indexOfFirst { it.id == target.id }
        if (!isMain && replyIndex < 0) {
            onPendingPostChange(null)
            return@LaunchedEffect
        }
        val replyStart = 1 + (if (mainPost != null) 1 else 0) +
            (if (danmakuEnabled && danmaku.isNotEmpty()) 1 else 0) +
            (if (replies.isNotEmpty() || (page?.loginVisibleReplies ?: -1) >= 0) 1 else 0)
        withFrameNanos { }
        onHighlight(target.id)
        listState.animateScrollToItem(
            if (isMain) 1 else replyStart + replyIndex,
            pendingRestoreOffset ?: 0,
        )
        onRestoreOffsetConsumed()
        if (searchOpen && searchQuery.isNotBlank()) {
            withFrameNanos { }
            withFrameNanos { }
            searchPositions[target.id]?.takeIf { it.first == searchQuery }?.let { (_, y) ->
                listState.scrollBy(y - contentTopInRoot - 12f)
            }
        }
        onPendingPostChange(null)
    }
}

@Composable
internal fun TopicHighlightExpiryEffect(highlightedPostId: Long?, onClear: () -> Unit) {
    LaunchedEffect(highlightedPostId) {
        if (highlightedPostId != null) {
            delay(1800)
            onClear()
        }
    }
}

@Composable
internal fun TopicSearchAutoSelectEffect(
    query: String,
    open: Boolean,
    firstHit: PostEntry?,
    onSelect: (PostEntry) -> Unit,
) {
    LaunchedEffect(query, open) {
        if (open && query.isNotBlank()) {
            delay(300)
            firstHit?.let(onSelect)
        }
    }
}
