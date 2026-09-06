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
import sb.linux.client.data.HtmlParser
import sb.linux.client.data.Session
import sb.linux.client.model.local.CommentFavorite
import sb.linux.client.ui.Avatar
import sb.linux.client.ui.TopicCardView

/** 本地帖子与评论收藏，进入页面时与源站收藏做一次合并。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(session: Session, nav: NavHostController) {
    var topics by remember { mutableStateOf(session.settings.favoriteList()) }
    var comments by remember { mutableStateOf(session.settings.commentFavoriteList()) }
    var tab by remember { mutableIntStateOf(0) }
    var showClearDialog by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var syncing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun refresh() {
        topics = session.settings.favoriteList()
        comments = session.settings.commentFavoriteList()
    }

    fun syncFromSource() {
        val uid = session.loginState.userId
        if (!session.loginState.loggedIn || uid <= 0 || syncing) return
        scope.launch {
            syncing = true
            try {
                val resp = session.client.get("/user/$uid?tab=favorites")
                session.settings.mergeFavorites(HtmlParser.parseTopicList(resp.html).first)
                refresh()
            } catch (_: Exception) {
            } finally {
                syncing = false
            }
        }
    }

    LaunchedEffect(Unit) { syncFromSource() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("收藏内容") },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
                actions = {
                    if (tab == 0) {
                        TextButton(onClick = { syncFromSource() }, enabled = !syncing) {
                            Text(if (syncing) "同步中…" else "刷新")
                        }
                    }
                    TextButton(
                        onClick = { showClearDialog = true },
                        enabled = if (tab == 0) topics.isNotEmpty() else comments.isNotEmpty(),
                    ) { Text("清空") }
                },
            )
        },
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            TabRow(selectedTabIndex = tab) {
                Tab(
                    selected = tab == 0,
                    onClick = { tab = 0; query = "" },
                    text = { Text("帖子${if (topics.isNotEmpty()) " (${topics.size})" else ""}") },
                )
                Tab(
                    selected = tab == 1,
                    onClick = { tab = 1; query = "" },
                    text = { Text("评论${if (comments.isNotEmpty()) " (${comments.size})" else ""}") },
                )
            }
            val listEmpty = if (tab == 0) topics.isEmpty() else comments.isEmpty()
            if (!listEmpty) {
                androidx.compose.material3.OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text(if (tab == 0) "搜索收藏帖子" else "搜索收藏评论") },
                    leadingIcon = { Icon(Icons.Filled.Search, null) },
                    trailingIcon = {
                        if (query.isNotBlank()) Icon(
                            Icons.Filled.Close,
                            "清空",
                            Modifier.clickable { query = "" },
                        )
                    },
                    singleLine = true,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
                    textStyle = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
            if (tab == 0) {
                TopicFavorites(topics, query, syncing, session, nav, ::refresh)
            } else {
                CommentFavorites(comments, query, session, nav, ::refresh)
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text(if (tab == 0) "清空收藏帖子" else "清空收藏评论") },
            text = {
                Text(
                    if (tab == 0) "确定清空全部 ${topics.size} 条本地收藏记录吗？"
                    else "确定清空全部 ${comments.size} 条收藏的评论吗？",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (tab == 0) session.settings.clearFavorites()
                    else session.settings.clearCommentFavorites()
                    refresh()
                    showClearDialog = false
                    session.showToast("收藏内容已清空")
                }) { Text("清空") }
            },
            dismissButton = { TextButton(onClick = { showClearDialog = false }) { Text("取消") } },
        )
    }
}

@Composable
private fun TopicFavorites(
    topics: List<sb.linux.client.data.TopicCard>,
    query: String,
    syncing: Boolean,
    session: Session,
    nav: NavHostController,
    refresh: () -> Unit,
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
                onDismiss = {
                    session.settings.removeFavorite(topic.topicId)
                    refresh()
                    session.showToast("已取消收藏")
                },
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
private fun CommentFavorites(
    comments: List<CommentFavorite>,
    query: String,
    session: Session,
    nav: NavHostController,
    refresh: () -> Unit,
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
                onDismiss = {
                    session.settings.removeCommentFavorite(comment.replyId)
                    refresh()
                    session.showToast("已取消收藏")
                },
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
