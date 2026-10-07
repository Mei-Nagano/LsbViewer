@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package sb.linux.client.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import sb.linux.client.ui.session.Session
import sb.linux.client.ui.*
import androidx.lifecycle.viewmodel.compose.viewModel
import sb.linux.client.ui.feature.community.BrowsingHistoryViewModel
import sb.linux.client.ui.feature.community.BrowsingHistoryViewModelFactory

@Composable
fun FootprintScreen(session: Session, nav: NavHostController) {
    // 本地浏览历史：不再解析源站足迹，左划卡片可删除（3.2）
    val factory = remember(session.browsingHistoryRepository) {
        BrowsingHistoryViewModelFactory(session.browsingHistoryRepository)
    }
    val viewModel: BrowsingHistoryViewModel = viewModel(factory = factory)
    val topics = viewModel.topics
    var showClearDialog by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }

    // 从帖子返回时刷新（浏览时间可能变化）
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val obs = androidx.lifecycle.LifecycleEventObserver { _, e ->
            if (e == androidx.lifecycle.Lifecycle.Event.ON_RESUME) viewModel.refresh()
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("浏览历史") },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
                actions = {
                    TextButton(
                        onClick = { showClearDialog = true },
                        enabled = topics.isNotEmpty(),
                    ) { Text("清空") }
                }
            )
        }
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            // 本地搜索（只过滤标题，纯本地无需访问源站）
            if (topics.isNotEmpty()) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("搜索浏览历史") },
                    leadingIcon = { Icon(Icons.Filled.Search, null) },
                    trailingIcon = {
                        if (query.isNotBlank()) Icon(
                            Icons.Filled.Close, "清空",
                            Modifier.clickable { query = "" }
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    textStyle = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
            if (topics.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "暂无浏览历史",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(24.dp)
                    )
                }
            } else {
                val filtered = topics.filter {
                    query.isBlank() || it.title.contains(query.trim(), ignoreCase = true)
                }
                if (filtered.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            "无匹配“${query}”的记录",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(24.dp)
                        )
                    }
                } else {
                    LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
                        items(filtered, key = { it.topicId }) { t ->
                    val state = rememberSwipeToDismissBoxState(
                        confirmValueChange = { v ->
                            if (v == SwipeToDismissBoxValue.EndToStart) {
                                viewModel.remove(t.topicId)
                                session.showToast("已删除该记录")
                                true
                            } else false
                        }
                    )
                    SwipeToDismissBox(
                        state = state,
                        enableDismissFromStartToEnd = false,
                        backgroundContent = {
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 14.dp, vertical = 5.dp)
                                    .background(
                                        MaterialTheme.colorScheme.errorContainer,
                                        RoundedCornerShape(18.dp)
                                    ),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                Icon(
                                    Icons.Filled.Delete, "删除",
                                    Modifier.padding(end = 22.dp),
                                    tint = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    ) {
                        TopicCardView(
                            t,
                            onClick = { nav.navigate("topic/${t.topicId}") },
                            onForumClick = { fid -> if (fid > 0) nav.navigate("forum/$fid") },
                            showComments = false,   // 浏览历史只展示浏览时间，不显示评论数
                            showForum = false,      // 浏览历史不显示所属板块
                        )
                    }
                    }
                }
            }
        }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("清空浏览历史") },
            text = { Text("确定清空全部 ${topics.size} 条本地浏览记录吗？") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clear()
                    showClearDialog = false
                    session.showToast("浏览历史已清空")
                }) { Text("清空") }
            },
            dismissButton = { TextButton(onClick = { showClearDialog = false }) { Text("取消") } }
        )
    }
}

// ---------------- 举报 ----------------
