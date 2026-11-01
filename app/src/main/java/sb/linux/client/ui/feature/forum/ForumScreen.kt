package sb.linux.client.ui.feature.forum

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import sb.linux.client.domain.community.ForumInfo
import sb.linux.client.ui.session.Session
import sb.linux.client.domain.topic.TopicCard
import sb.linux.client.ui.*
import sb.linux.client.ui.theme.*
import sb.linux.client.ui.content.*
import sb.linux.client.ui.patterns.topic.*
import sb.linux.client.ui.patterns.user.*
import sb.linux.client.ui.components.feedback.*
import sb.linux.client.ui.components.layout.*
import sb.linux.client.ui.components.navigation.*
import sb.linux.client.util.TopicFilter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForumScreen(session: Session, nav: NavHostController) {
    val fid = nav.currentBackStackEntry?.arguments?.getLong("id") ?: 1L
    val factory = remember(session.forumRepository, fid) {
        ForumTopicsViewModelFactory(session.forumRepository, fid)
    }
    val viewModel: ForumTopicsViewModel = viewModel(factory = factory, key = "forum-$fid")
    val state by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    // 板块分类生效的滚动模式（浏览设置可按分类覆盖，3.13）
    val forumInfinite = session.preferences.effectiveInfiniteScroll("forum")
    val filteredTopics = remember(state.topics, session.keywordFilter) {
        // 源站版块页仍应用关键词/用户规则，但不应用首页版块规则。
        TopicFilter.apply(state.topics, session.keywordFilter.settings, isHome = false)
    }

    LaunchedEffect(viewModel) { viewModel.load(1) }

    AppPageScaffold(title = state.forumName, onBack = { nav.popBackStack() }) { pad ->
        Column(Modifier.padding(pad)) {
            // 排序 tab：新评论 / 新帖子
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("comment" to "新评论", "post" to "新帖子").forEach { (key, label) ->
                    val selected = state.sort == key
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = if (selected) MaterialTheme.colorScheme.secondaryContainer
                        else MaterialTheme.colorScheme.surfaceContainerHigh,
                        onClick = {
                            if (state.sort != key) {
                                viewModel.selectSort(key)
                                scope.launch { listState.scrollToItem(0) }
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            label,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = if (selected) MaterialTheme.colorScheme.onSecondaryContainer
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 9.dp)
                        )
                    }
                }
            }
            Box(Modifier.weight(1f).fillMaxWidth()) {
                when {
                    state.loading -> LoadingBox()
                    state.error != null -> ErrorBox(state.error!!) { viewModel.load(state.page) }
                    filteredTopics.isEmpty() -> EmptyBox(
                        if (state.topics.isEmpty()) "暂无帖子" else "本页帖子已按屏蔽规则过滤"
                    )
                    else -> {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            items(filteredTopics, key = { "${it.topicId}-${it.pinned}" }) { t ->
                                TopicCardView(
                                    t,
                                    onClick = { nav.navigate("topic/${t.topicId}") },
                                    onForumClick = { /* 同板块 */ }
                                )
                            }
                            item {
                                if (forumInfinite) {
                                    // 无限滚动：加载更多按钮，不足自动结束
                                    if (state.page < state.totalPages) {
                                        OutlinedButton(
                                            onClick = { viewModel.load(state.page + 1, append = true) },
                                            enabled = !state.loadingMore,
                                            shape = CircleShape,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                        ) { Text(if (state.loadingMore) "加载中…" else "加载更多") }
                                    }
                                } else {
                                    // 翻页模式：翻页条在列表底部
                                    PaginationBar(state.page, state.totalPages) { viewModel.load(it) }
                                }
                                // 底部留白：为悬浮按钮和底栏留出空间
                                Spacer(Modifier.height(64.dp))
                            }
                        }
                    }
                }
                // 返回顶部悬浮按钮：滚过前几条后出现
                val showBackTop by remember {
                    derivedStateOf { listState.firstVisibleItemIndex > 2 }
                }
                if (showBackTop) {
                    SmallFloatingActionButton(
                        onClick = { scope.launch { listState.animateScrollToItem(0) } },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 16.dp, bottom = 24.dp),
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.92f),
                        contentColor = MaterialTheme.colorScheme.onSurface,
                    ) {
                        Icon(Icons.Filled.KeyboardArrowUp, "回到顶部")
                    }
                }
            }
        }
    }
}
