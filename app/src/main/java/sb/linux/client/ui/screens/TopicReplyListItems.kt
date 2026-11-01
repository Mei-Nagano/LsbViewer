package sb.linux.client.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import sb.linux.client.ui.components.feedback.AppInlineLoading
import sb.linux.client.ui.components.navigation.PaginationBar
internal fun LazyListScope.topicReplyListItems(
    state: TopicReplyListState,
    actions: TopicReplyListActions,
) {
    if (state.replies.isNotEmpty() || state.page.loginVisibleReplies >= 0) {
        item(key = "comments-header") {
            Column {
                Row(
                    Modifier.fillMaxWidth().padding(start = 20.dp, end = 16.dp, top = 14.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("全部回复", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.width(6.dp))
                    Text(state.replyCountText, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.weight(1f))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh).padding(2.dp),
                    ) {
                        listOf("热度" to 0, "正序" to 1, "倒序" to 2).forEach { (label, index) ->
                            val selected = state.sortOrder == index
                            Text(
                                label,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.clip(CircleShape)
                                    .background(if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                    .clickable { if (!selected) actions.onSortChange(index) }
                                    .padding(horizontal = 10.dp, vertical = 3.dp),
                            )
                        }
                    }
                }
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                )
            }
        }
    }
    val visibleReplies = if (state.hasTree) state.flatTree else state.pagedReplies.map { it to 0 }
    itemsIndexed(
        visibleReplies,
        key = { _, value -> "r-" + value.first.id },
        contentType = { _, _ -> "reply" },
    ) { _, (post, depth) ->
        val lineColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
        Box(
            Modifier.fillMaxWidth()
                .padding(start = (depth.coerceAtMost(2) * 16).dp)
                .drawBehind {
                    if (depth > 0) drawLine(
                        color = lineColor,
                        start = Offset(6.dp.toPx(), 0f),
                        end = Offset(6.dp.toPx(), size.height),
                        strokeWidth = 2.dp.toPx(),
                    )
                }
        ) {
            PostCard(
                post = post,
                loggedIn = state.loggedIn,
                isOpAuthor = post.authorName == state.page.posts.firstOrNull()?.authorName,
                isMainPost = false,
                highlight = state.highlightPostId == post.id,
                replyParentAvatar = post.parentFloor.takeIf { it > 0 }?.let { state.repliesByFloor[it]?.avatarUrl } ?: "",
                showReplyCapsule = !state.hasTree,
                childReplyCount = state.treeChildCounts[post.id] ?: 0,
                childrenCollapsed = post.id in state.collapsedIds,
                onToggleChildren = { actions.onToggleCollapsed(post.id) },
                onUser = { actions.onUser(post.authorId) },
                onQuote = { actions.onQuote(post) },
                onLike = { actions.onLike(post) },
                onCoin = { actions.onCoin(post) },
                onReport = { actions.onReport(post) },
                onFloor = actions.onFloor,
                onThread = { actions.onThread(post) },
                threadEnabled = state.threadEnabled,
                threadReplyCount = state.threadReplyCount[post.floor] ?: 0,
                onCopy = { actions.onCopy(post) },
                onEditUser = actions.onEditUser,
                commentFavorited = post.id in state.favoriteCommentIds,
                onSearchPosition = { y -> actions.onSearchPosition(post.id, y) },
                onFavComment = { actions.onFavorite(post) },
                searchHighlight = state.searchQuery.takeIf { state.searchOpen }.orEmpty(),
            )
        }
    }
    if (state.page.loginVisibleReplies >= 0) {
        item(key = "login-prompt") {
            LoginPromptCard(count = state.page.loginVisibleReplies, onLogin = actions.onLogin)
        }
    }
    item(key = "pagination-" + state.replyModeTick) {
        if (state.infinite) {
            if ((state.page.page ?: 1) < (state.page.totalPages ?: 1)) {
                Box(Modifier.fillMaxWidth().padding(12.dp), contentAlignment = Alignment.Center) {
                    if (state.loadingMore) AppInlineLoading()
                    else TextButton(onClick = actions.onLoadMore) { Text("加载更多") }
                }
            }
        } else {
            PaginationBar(state.localPage, state.localPageTotal, onPage = actions.onLocalPage)
        }
    }
    item(key = "footer") { Spacer(Modifier.height(8.dp)) }
}
