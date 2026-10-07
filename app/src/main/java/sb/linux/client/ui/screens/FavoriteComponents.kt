package sb.linux.client.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import sb.linux.client.domain.community.CommentFavorite
import sb.linux.client.domain.topic.TopicCard
import sb.linux.client.ui.Avatar
import sb.linux.client.ui.TopicCardView

@Composable
internal fun TopicFavorites(
    topics: List<TopicCard>,
    query: String,
    syncing: Boolean,
    nav: NavHostController,
    onRemove: (Long) -> Unit,
) {
    if (topics.isEmpty()) {
        EmptyFavorites(if (syncing) "正在从源站同步收藏…" else "收藏的帖子会显示在这里（登录后进入本页会自动同步源站收藏）")
        return
    }
    val filtered = topics.filter { query.isBlank() || it.title.contains(query.trim(), ignoreCase = true) }
    if (filtered.isEmpty()) {
        EmptyFavorites("无匹配“$query”的收藏")
        return
    }
    LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
        items(filtered, key = { it.topicId }) { topic ->
            DismissibleFavorite(
                onDismiss = { onRemove(topic.topicId) },
            ) {
                TopicCardView(
                    topic,
                    onClick = { nav.navigate("topic/${topic.topicId}") },
                    onForumClick = { fid -> if (fid > 0) nav.navigate("forum/$fid") },
                )
            }
        }
    }
}

@Composable
internal fun CommentFavorites(
    comments: List<CommentFavorite>,
    query: String,
    nav: NavHostController,
    onRemove: (Long) -> Unit,
) {
    if (comments.isEmpty()) {
        EmptyFavorites("收藏的评论会显示在这里（点击收藏项可跳回原楼层）")
        return
    }
    val filtered = comments.filter {
        query.isBlank() || it.content.contains(query.trim(), ignoreCase = true) ||
            it.topicTitle.contains(query.trim(), ignoreCase = true) ||
            it.authorName.contains(query.trim(), ignoreCase = true)
    }
    if (filtered.isEmpty()) {
        EmptyFavorites("无匹配“$query”的收藏")
        return
    }
    LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
        items(filtered, key = { it.replyId }) { comment ->
            DismissibleFavorite(
                onDismiss = { onRemove(comment.replyId) },
            ) {
                CommentFavCard(comment) { nav.navigate("topic/${comment.topicId}?floor=${comment.floor}") }
            }
        }
    }
}

@Composable
private fun EmptyFavorites(message: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(24.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DismissibleFavorite(onDismiss: () -> Unit, content: @Composable () -> Unit) {
    val state = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDismiss()
                true
            } else false
        },
    )
    SwipeToDismissBox(
        state = state,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp, vertical = 5.dp)
                    .background(MaterialTheme.colorScheme.errorContainer, androidx.compose.foundation.shape.RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(
                    Icons.Filled.Delete,
                    "取消收藏",
                    Modifier.padding(end = 22.dp),
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        },
        content = { content() },
    )
}

@Composable
private fun CommentFavCard(c: CommentFavorite, onClick: () -> Unit) {
    Surface(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 5.dp)
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                Avatar(c.avatarUrl, 30)
                Column(Modifier.weight(1f)) {
                    Text(
                        c.authorName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (c.timeText.isNotBlank()) {
                        Text(c.timeText, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Surface(
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                ) {
                    Text(
                        "#${c.floor}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
                    )
                }
            }
            Text(c.content, style = MaterialTheme.typography.bodyMedium, maxLines = 3, overflow = TextOverflow.Ellipsis)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(Icons.AutoMirrored.Filled.Chat, null, Modifier.size(13.dp), tint = MaterialTheme.colorScheme.primary)
                Text(
                    c.topicTitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
