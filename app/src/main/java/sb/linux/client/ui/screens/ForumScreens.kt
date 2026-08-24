package sb.linux.client.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import sb.linux.client.data.ForumInfo
import sb.linux.client.data.HtmlParser
import sb.linux.client.data.Session
import sb.linux.client.data.TopicCard
import sb.linux.client.ui.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForumListScreen(session: Session, nav: NavHostController) {
    var forums by remember { mutableStateOf<List<ForumInfo>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun load() {
        scope.launch {
            loading = true; error = null
            try {
                val resp = session.client.get("/forum_list")
                forums = HtmlParser.parseForumList(resp.html)
                if (forums.isEmpty()) {
                    val home = session.client.get("/")
                    forums = HtmlParser.parseForumList(home.html)
                }
            } catch (e: Exception) {
                error = e.message
            } finally { loading = false }
        }
    }

    LaunchedEffect(Unit) { load() }

    Scaffold(topBar = { TopAppBar(title = { Text("板块导航") }) }) { pad ->
        Box(Modifier.padding(pad)) {
            when {
                loading -> LoadingBox()
                error != null -> ErrorBox(error!!) { load() }
                forums.isEmpty() -> EmptyBox("暂无板块")
                else -> LazyColumn(
                    // 底部留白让末条内容可滚出玻璃底栏（23）：Scaffold 已消耗导航栏 inset；
                    // 悬浮玻璃高 58 + 悬浮边距 14；经典底栏不加此留白
                    contentPadding = PaddingValues(
                        start = 14.dp, end = 14.dp, top = 10.dp,
                        bottom = 10.dp + if (session.bottomBarStyle != 0) 72.dp else 0.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(forums, key = { it.id }) { f ->
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .clickable { nav.navigate("forum/${f.id}") },
                        ) {
                            Row(
                                Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        f.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (f.description.isNotBlank()) {
                                        Text(
                                            f.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    } else if (f.topicCount.isNotBlank()) {
                                        Text(
                                            "${f.topicCount} 个主题",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                Icon(
                                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForumScreen(session: Session, nav: NavHostController) {
    val fid = nav.currentBackStackEntry?.arguments?.getLong("id") ?: 1L
    var page by remember { mutableIntStateOf(1) }
    var topics by remember { mutableStateOf<List<TopicCard>>(emptyList()) }
    var totalPages by remember { mutableIntStateOf(1) }
    var forumName by remember { mutableStateOf("板块") }
    var loading by remember { mutableStateOf(true) }
    var loadingMore by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    // 板块内排序：新评论 / 新帖子（与首页同参数约定）
    var sort by remember { mutableStateOf("comment") }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    // 板块分类生效的滚动模式（浏览设置可按分类覆盖，3.13）
    val forumInfinite = session.effectiveInfiniteScroll("forum")

    fun load(p: Int, append: Boolean = false) {
        scope.launch {
            if (append) loadingMore = true else loading = true
            error = null
            try {
                val path = when {
                    p <= 1 && sort == "comment" -> "/forum/$fid"
                    p <= 1 -> "/forum/$fid?sort=$sort"
                    else -> "/forum/$fid?sort=$sort&p=$p"
                }
                val resp = session.client.get(path)
                val (items, pg) = HtmlParser.parseTopicList(resp.html)
                // 无限滚动：追加合并；翻页：整页替换
                topics = if (append) (topics + items).distinctBy { it.topicId } else items
                page = pg.first; totalPages = pg.second
                forumName = items.firstOrNull()?.forumName ?: forumName
            } catch (e: Exception) {
                error = e.message
            } finally { loading = false; loadingMore = false }
        }
    }

    LaunchedEffect(fid) { load(1) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(forumName) },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                }
            )
        }
    ) { pad ->
        Column(Modifier.padding(pad)) {
            // 排序 tab：新评论 / 新帖子
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("comment" to "新评论", "post" to "新帖子").forEach { (key, label) ->
                    val selected = sort == key
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (selected) MaterialTheme.colorScheme.secondaryContainer
                        else MaterialTheme.colorScheme.surfaceContainerHigh,
                        onClick = {
                            if (sort != key) {
                                sort = key
                                page = 1
                                load(1)
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
                    loading -> LoadingBox()
                    error != null -> ErrorBox(error!!) { load(page) }
                    topics.isEmpty() -> EmptyBox("暂无帖子")
                    else -> {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            items(topics, key = { "${it.topicId}-${it.pinned}" }) { t ->
                                TopicCardView(
                                    t,
                                    onClick = { nav.navigate("topic/${t.topicId}") },
                                    onForumClick = { /* 同板块 */ }
                                )
                            }
                            item {
                                if (forumInfinite) {
                                    // 无限滚动：加载更多按钮，不足自动结束
                                    if (page < totalPages) {
                                        OutlinedButton(
                                            onClick = { load(page + 1, append = true) },
                                            enabled = !loadingMore,
                                            shape = RoundedCornerShape(50),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                        ) { Text(if (loadingMore) "加载中…" else "加载更多") }
                                    }
                                } else {
                                    // 翻页模式：翻页条在列表底部
                                    PaginationBar(page, totalPages) { load(it) }
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
