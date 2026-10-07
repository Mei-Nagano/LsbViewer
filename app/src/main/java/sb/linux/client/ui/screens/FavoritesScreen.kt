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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import sb.linux.client.ui.session.Session
import sb.linux.client.ui.Avatar
import sb.linux.client.ui.TopicCardView
import sb.linux.client.ui.feature.community.FavoritesViewModel
import sb.linux.client.ui.feature.community.FavoritesViewModelFactory

/** 本地帖子与评论收藏，进入页面时与源站收藏做一次合并。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(session: Session, nav: NavHostController) {
    val factory = remember(session.favoritesRepository) {
        FavoritesViewModelFactory(session.favoritesRepository)
    }
    val viewModel: FavoritesViewModel = viewModel(factory = factory)
    val state by viewModel.uiState.collectAsState()
    val topics = state.topics
    val comments = state.comments
    var tab by remember { mutableIntStateOf(0) }
    var showClearDialog by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    val loggedIn = session.account.loginState.loggedIn
    val uid = session.account.loginState.userId
    LaunchedEffect(viewModel, uid, loggedIn) {
        viewModel.load()
        viewModel.syncFromSource(uid, loggedIn)
    }

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
                        TextButton(onClick = { viewModel.syncFromSource(uid, loggedIn) }, enabled = !state.syncing) {
                            Text(if (state.syncing) "同步中…" else "刷新")
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
                TopicFavorites(topics, query, state.syncing, nav) {
                    viewModel.removeTopic(it)
                    session.showToast("已取消收藏")
                }
            } else {
                CommentFavorites(comments, query, nav) {
                    viewModel.removeComment(it)
                    session.showToast("已取消收藏")
                }
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
                    if (tab == 0) viewModel.clearTopics() else viewModel.clearComments()
                    showClearDialog = false
                    session.showToast("收藏内容已清空")
                }) { Text("清空") }
            },
            dismissButton = { TextButton(onClick = { showClearDialog = false }) { Text("取消") } },
        )
    }
}
