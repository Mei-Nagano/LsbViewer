package sb.linux.client.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import sb.linux.client.ui.components.feedback.AppConfirmDialog
import sb.linux.client.ui.components.input.LocalFilterField
import sb.linux.client.ui.components.navigation.AppPageScaffold
import sb.linux.client.ui.feature.community.FavoritesViewModel
import sb.linux.client.ui.feature.community.FavoritesViewModelFactory
import sb.linux.client.ui.session.Session
import sb.linux.client.ui.theme.LocalAppSpacing

/** 本地帖子与评论收藏，进入页面时与源站收藏做一次合并。 */
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
    val spacing = LocalAppSpacing.current
    LaunchedEffect(viewModel, uid, loggedIn) {
        viewModel.load()
        viewModel.syncFromSource(uid, loggedIn)
    }

    val currentEmpty = if (tab == 0) topics.isEmpty() else comments.isEmpty()

    AppPageScaffold(
        title = "收藏内容",
        onBack = { nav.popBackStack() },
        actions = {
            if (tab == 0) {
                TextButton(
                    onClick = { viewModel.syncFromSource(uid, loggedIn) },
                    enabled = !state.syncing,
                ) { Text(if (state.syncing) "同步中…" else "刷新") }
            }
            TextButton(onClick = { showClearDialog = true }, enabled = !currentEmpty) { Text("清空") }
        },
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            TabRow(
                selectedTabIndex = tab,
                containerColor = MaterialTheme.colorScheme.surface,
            ) {
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
            if (!currentEmpty) {
                LocalFilterField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = if (tab == 0) "搜索收藏帖子" else "搜索收藏评论",
                    modifier = Modifier.padding(
                        start = spacing.pageHorizontal,
                        end = spacing.pageHorizontal,
                        top = spacing.item,
                        bottom = spacing.item,
                    ),
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
        AppConfirmDialog(
            title = if (tab == 0) "清空收藏帖子" else "清空收藏评论",
            message = if (tab == 0) "确定清空全部 ${topics.size} 条本地收藏记录吗？不会取消源站收藏。"
            else "确定清空全部 ${comments.size} 条收藏的评论吗？不会取消源站收藏。",
            onDismiss = { showClearDialog = false },
            onConfirm = {
                if (tab == 0) viewModel.clearTopics() else viewModel.clearComments()
                showClearDialog = false
                session.showToast("收藏内容已清空")
            }, confirmText = "清空", destructive = true,
        )
    }
}
