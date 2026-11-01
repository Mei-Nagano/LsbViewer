package sb.linux.client.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import androidx.navigation.NavHostController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import sb.linux.client.ui.session.HomeTabState
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
@Composable
internal fun HomeBody(
    topInset: Dp,
    session: Session,
    nav: NavHostController,
    viewsForTopic: (Long) -> Int,
    current: HomeTabState,
    homeInfinite: Boolean,
    loading: Boolean,
    error: String?,
    visibleTopics: List<TopicCard>,
    pagedTopics: List<TopicCard>,
    localTotalPages: Int,
    pendingNewTopics: List<TopicCard>,
    onAcceptPendingTopics: (List<TopicCard>) -> List<TopicCard>,
    loadingMore: Boolean,
    categoryMotion: Animatable<Float, AnimationVector1D>,
    listState: LazyListState,
    backTopPosition: Pair<Int, Int>?,
    onBackTopPositionChange: (Pair<Int, Int>?) -> Unit,
    onLoadPage: (Int, Boolean, Boolean) -> Unit,
    onLocalPage: (Int) -> Unit,
    onRefresh: () -> Unit,
    onOpenDrawer: () -> Unit,
    onSelectCombo: (Int, Int) -> Unit,
    scope: CoroutineScope,
) {
            Box(Modifier.fillMaxSize()) {
                when {
                    loading && current.topics.isEmpty() -> LoadingBox()
                    error != null && current.topics.isEmpty() -> ErrorBox(error!!) { onLoadPage(1, false, false) }
                    visibleTopics.isEmpty() -> EmptyBox(
                        if (session.keywordFilter.settings.presets.isEmpty() &&
                            session.keywordFilter.settings.custom.isEmpty() &&
                            session.keywordFilter.settings.users.isEmpty() &&
                            session.keywordFilter.settings.forumExcludedIds.isEmpty() &&
                            session.keywordFilter.settings.forumExtraIds.isEmpty()
                        ) "暂无帖子"
                        else "暂无帖子（已按源站屏蔽规则过滤）"
                    )
                    else -> Box(
                        Modifier
                            .fillMaxSize()
                            // 分类切换动画结束后移除整页 graphicsLayer，避免列表滚动时始终走额外合成层。
                            .then(if (categoryMotion.value != 0f) Modifier.graphicsLayer {
                                translationX = categoryMotion.value
                                alpha = 1f - (kotlin.math.abs(categoryMotion.value) / 260f).coerceIn(0f, 0.22f)
                            } else Modifier)
                            // 主页左右滑动在（全部/仅抽奖/仅发卡）之间切换；垂直滚动不受影响
                            .pointerInput(Unit) {
                                var totalX = 0f
                                val threshold = 60.dp.toPx()
                                detectHorizontalDragGestures(
                                    onHorizontalDrag = { _, dragAmount -> totalX += dragAmount },
                                    onDragEnd = {
                                        val c = session.homeBrowse.homeCombo
                                        val left = totalX <= -threshold
                                        val right = totalX >= threshold
                                        totalX = 0f
                                        val categories = listOf(0, 1, 2, 4, 3)
                                        val categoryIndex = categories.indexOf(c).coerceAtLeast(0)
                                        when {
                                            left && categoryIndex < categories.lastIndex ->
                                                onSelectCombo(categories[categoryIndex + 1], 1)
                                            // 最后一个分类已是右边界：继续左滑不循环。
                                            left -> Unit
                                            right && categoryIndex > 0 ->
                                                onSelectCombo(categories[categoryIndex - 1], -1)
                                            // “全部”已是左边界：向右滑直接打开侧边栏。
                                            right -> onOpenDrawer()
                                        }
                                    },
                                    onDragCancel = { totalX = 0f },
                                )
                            }
                    ) {
                        // 翻页模式：翻页条作为列表最后一个 item，滚动到底部即可看到
                        PullToRefreshBox(
                            isRefreshing = loading && current.topics.isNotEmpty(),
                            // 下拉刷新必须走完整刷新流程，同时失效已浏览帖子的详情缓存。
                            // 否则首页卡片虽已更新，首次进入详情仍会命中旧内容。
                            onRefresh = { onRefresh() },
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier.fillMaxSize(),
                                // 根导航由外壳 Scaffold 的 bottomBar inset 处理，列表不再叠加固定底栏留白
                                contentPadding = PaddingValues(
                                    top = topInset + LocalAppSpacing.current.item,
                                    bottom = LocalAppSpacing.current.pageVertical,
                                )
                            ) {
                                if (pendingNewTopics.isNotEmpty()) {
                                    item(key = "pending-new-topics") {
                                        Surface(
                                            onClick = {
                                                current.topics = onAcceptPendingTopics(current.topics)
                                                scope.launch { listState.animateScrollToItem(0) }
                                            },
                                            shape = MaterialTheme.shapes.large,
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            modifier = Modifier.fillMaxWidth().padding(horizontal = LocalAppSpacing.current.pageHorizontal, vertical = LocalAppSpacing.current.item),
                                        ) {
                                            Row(
                                                Modifier.fillMaxWidth().padding(horizontal = LocalAppSpacing.current.pageHorizontal, vertical = LocalAppSpacing.current.item),
                                                verticalAlignment = Alignment.CenterVertically,
                                            ) {
                                                Text(
                                                    "发现 ${pendingNewTopics.size} 个新帖子",
                                                    style = MaterialTheme.typography.labelLarge,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                )
                                                Spacer(Modifier.weight(1f))
                                                Text("点击展开", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                            }
                                        }
                                    }
                                }
                                items(
                                    pagedTopics,
                                    key = { t -> t.topicId },
                                    contentType = { "topic-card" },
                                ) { t ->
                                    TopicCardView(
                                        t,
                                        views = viewsForTopic(t.topicId),
                                        onClick = { nav.navigate("topic/${t.topicId}") },
                                        onForumClick = { fid -> if (fid > 0) nav.navigate("forum/$fid") },
                                    )
                                }
                                if (homeInfinite) {
                                    item {
                                        // 无限滚动：移动到最下面帖子后出现「加载更多」按钮
                                        if (current.page < current.totalPages) {
                                            Box(
                                                Modifier
                                                    .fillMaxWidth()
                                                    .padding(LocalAppSpacing.current.contentGap),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (loadingMore) AppInlineLoading()
                                                else Button(onClick = { onLoadPage(current.page + 1, true, false) }) {
                                                    Text("加载更多")
                                                }
                                            }
                                        }
                                        // 底部留白：为悬浮按钮和底栏留出空间
                                        Spacer(Modifier.height(64.dp))
                                    }
                                } else {
                                    // 翻页模式：翻页条紧贴最后一项（前面不留大段空白），滑到底即完整露出，无需再多次滑动
                                    item {
                                        PaginationBar(current.page, localTotalPages) { onLocalPage(it) }
                                    }
                                }
                            }
                        }
                        // 回到顶部按钮：列表滚过前几条后出现（缩放动画，不用 AnimatedVisibility 避免嵌套作用域限制）
                        val showBackTop by remember {
                            derivedStateOf { listState.firstVisibleItemIndex > 2 || backTopPosition != null }
                        }
                        val backTopScale by animateFloatAsState(
                            targetValue = if (showBackTop) 1f else 0f,
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                            label = "backTop"
                        )
                        if (backTopScale > 0.01f) {
                            SmallFloatingActionButton(
                                onClick = {
                                    val saved = backTopPosition
                                    if (listState.firstVisibleItemIndex <= 1 && saved != null) {
                                        onBackTopPositionChange(null)
                                        scope.launch { listState.animateScrollToItem(saved.first, saved.second) }
                                    } else {
                                        onBackTopPositionChange(listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset)
                                        scope.launch { listState.animateScrollToItem(0) }
                                    }
                                },
                                modifier = Modifier
                                    // 发帖 FAB 固定在右下，回到顶部移到左下避免触区重叠
                                    .align(Alignment.BottomStart)
                                    .padding(start = 16.dp, bottom = 24.dp)
                                    .graphicsLayer {
                                        scaleX = backTopScale
                                        scaleY = backTopScale
                                        alpha = backTopScale
                                    },
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.92f),
                                contentColor = MaterialTheme.colorScheme.onSurface,
                            ) {
                                val canReturn = listState.firstVisibleItemIndex <= 1 && backTopPosition != null
                                Icon(
                                    if (canReturn) Icons.Filled.KeyboardArrowDown else Icons.Filled.KeyboardArrowUp,
                                    if (canReturn) "回到刚才位置" else "回到顶部",
                                )
                            }
                        }
                        if (session.floatingTopicId > 0L) {
                            sb.linux.client.ui.patterns.topic.PinnedTopicCapsule(session.floatingTopicId, session.floatingTopicTitle) {
                                nav.navigate("topic/${session.floatingTopicId}")
                            }
                        }
                    }
    }
}
}
