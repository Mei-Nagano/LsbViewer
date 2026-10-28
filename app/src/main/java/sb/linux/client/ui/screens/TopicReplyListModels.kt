package sb.linux.client.ui.screens

import sb.linux.client.domain.topic.PostEntry
import sb.linux.client.domain.topic.TopicPageData

internal data class TopicReplyListState(
    val page: TopicPageData,
    val replies: List<PostEntry>,
    val hasTree: Boolean,
    val flatTree: List<Pair<PostEntry, Int>>,
    val pagedReplies: List<PostEntry>,
    val repliesByFloor: Map<Int, PostEntry>,
    val treeChildCounts: Map<Long, Int>,
    val collapsedIds: Set<Long>,
    val highlightPostId: Long?,
    val threadEnabled: Boolean,
    val threadReplyCount: Map<Int, Int>,
    val favoriteCommentIds: Set<Long>,
    val searchOpen: Boolean,
    val searchQuery: String,
    val loggedIn: Boolean,
    val sortOrder: Int,
    val localPage: Int,
    val localPageTotal: Int,
    val infinite: Boolean,
    val loadingMore: Boolean,
    val replyModeTick: Int,
    val replyCountText: String,
)

internal data class TopicReplyListActions(
    val onSortChange: (Int) -> Unit,
    val onToggleCollapsed: (Long) -> Unit,
    val onUser: (Long) -> Unit,
    val onQuote: (PostEntry) -> Unit,
    val onLike: (PostEntry) -> Unit,
    val onCoin: (PostEntry) -> Unit,
    val onReport: (PostEntry) -> Unit,
    val onFloor: (Int) -> Unit,
    val onThread: (PostEntry) -> Unit,
    val onCopy: (PostEntry) -> Unit,
    val onEditUser: (Long) -> Unit,
    val onSearchPosition: (Long, Float) -> Unit,
    val onFavorite: (PostEntry) -> Unit,
    val onLogin: () -> Unit,
    val onLoadMore: () -> Unit,
    val onLocalPage: (Int) -> Unit,
)