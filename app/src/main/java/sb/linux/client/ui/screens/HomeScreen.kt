package sb.linux.client.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import sb.linux.client.data.ForumCount
import sb.linux.client.data.HomeSidebar
import sb.linux.client.data.HtmlParser
import sb.linux.client.data.Session
import sb.linux.client.data.TopicCard
import sb.linux.client.ui.Avatar
import sb.linux.client.ui.EmptyBox
import sb.linux.client.ui.ErrorBox
import sb.linux.client.ui.LoadingBox
import sb.linux.client.ui.PaginationBar
import sb.linux.client.ui.TopicCardView

/** 源站首页 tab：新评论 / 新帖子 / 精华（抽奖/发卡通过组合过滤进入） */
private val SORTS = listOf(
    "comment" to "新评论",
    "post" to "新帖子",
    "featured" to "精华",
)

private fun buildPath(sort: String, page: Int): String = when {
    sort == "featured" && page <= 1 -> "/topic_featured"
    sort == "featured" -> "/topic_featured?p=$page"
    page <= 1 -> "/?sort=$sort"
    else -> "/index.php?sort=$sort&p=$page"
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(session: Session, nav: NavHostController) {
    // 状态全部保存在 Session：进帖返回后仍在原分类、原位置
    val tabIndex = session.homeTabIndex
    val combo = session.homeCombo
    val tabStates = session.homeTabs
    val current = tabStates[tabIndex.coerceIn(0, SORTS.size - 1)]
    val currentSort = SORTS[tabIndex.coerceIn(0, SORTS.size - 1)].first
    // 首页分类生效的滚动模式（浏览设置可按分类覆盖，3.13）
    val homeInfinite = session.effectiveInfiniteScroll("home")

    var loading by remember { mutableStateOf(false) }
    var loadingMore by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var menuOpen by remember { mutableStateOf(false) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    // 源站首页侧板数据（与首次首页请求同步解析，并本地永久缓存，避免每次访问源站）
    var sidebar by remember { mutableStateOf<HomeSidebar?>(session.loadHomeSidebar()) }

    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    // 沉浸式顶栏：列表滚出首条时隐藏顶栏，回到顶部时重新显示
    var topBarVisible by remember { mutableStateOf(true) }
    val derivedTopVisible by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            (info.visibleItemsInfo.firstOrNull()?.index ?: 0) == 0
        }
    }
    LaunchedEffect(derivedTopVisible) { topBarVisible = derivedTopVisible }

    fun openDrawer() {
        scope.launch { drawerState.open() }
    }

    fun closeDrawer(action: () -> Unit = {}) {
        scope.launch { drawerState.close() }.invokeOnCompletion { action() }
    }

    // 源站屏蔽词 + 屏蔽用户过滤（服务端生效，本地兜底）+ 分类组合过滤
    fun filterTopics(list: List<TopicCard>): List<TopicCard> {
        val words = session.blockedWords.filter { it.isNotBlank() }
        val users = session.blockedUsers.filter { it.isNotBlank() }
        val base = if (words.isEmpty() && users.isEmpty()) list
        else list.filterNot { t ->
            words.any { t.title.contains(it) } || (users.isNotEmpty() && t.authorName in users)
        }
        return when (combo) {
            1 -> base.filter { it.lotteryStatus.isNotBlank() }
            2 -> base.filter { it.cardStatus.isNotBlank() }
            else -> base
        }
    }
    val visibleTopics = remember(current.topics, session.blockedWords, session.blockedUsers, combo) {
        filterTopics(current.topics)
    }

    // 翻页模式：按设置的每页条数本地切片分页；无限滚动：全部展示
    val perPage = session.topicsPerPage.coerceAtLeast(1)
    val pagedTopics = if (homeInfinite) visibleTopics
    else visibleTopics.drop((current.page - 1) * perPage).take(perPage)
    // 本地总页数：已加载条数算出的整页数；源站还有更多页时 +1（允许继续翻页并触发加载）
    val localTotalPages = if (homeInfinite) current.totalPages else run {
        val full = ((visibleTopics.size + perPage - 1) / perPage).coerceAtLeast(1)
        (if (current.sourcePage < current.sourceTotalPages) full + 1 else full).coerceAtLeast(current.page)
    }

    // 翻页模式：过滤后当前页越界且源站已无更多页时，自动回落到最后一页
    LaunchedEffect(visibleTopics.size, homeInfinite, current.page, current.sourceTotalPages) {
        if (!homeInfinite && current.page > 1) {
            val full = ((visibleTopics.size + perPage - 1) / perPage).coerceAtLeast(1)
            if (current.page > full && current.sourcePage >= current.sourceTotalPages) {
                current.page = full
            }
        }
    }

    fun load(p: Int, append: Boolean = false) {
        scope.launch {
            if (append) loadingMore = true else { loading = true; current.page = p }
            error = null
            try {
                val path = buildPath(currentSort, p)
                val resp = session.client.get(path)
                val (items, pg) = HtmlParser.parseTopicList(resp.html)
                current.topics = if (append) (current.topics + items).distinctBy { it.topicId } else items
                current.sourcePage = pg.first.coerceAtLeast(if (append) p else 1)
                current.sourceTotalPages = pg.second
                // 无限滚动：页码即源站页；翻页模式：页码为本地切片页（初始 1）
                current.page = if (homeInfinite) current.sourcePage else 1
                current.totalPages = pg.second
                current.loaded = true
                // 首页成功后持久化缓存（供"启动不刷新"使用）
                if (!append) session.saveHomeCache(currentSort, items)
                // 侧板与首页同一次请求解析：每次非追加加载都用最新结果合并更新
                // （板块/热帖/统计等），避免旧的残缺缓存被永久固定，导致侧边栏一直缺失每日热榜等数据
                if (!append) {
                    val parsed = runCatching { HtmlParser.parseHomeSidebar(resp.html) }.getOrNull()
                    if (parsed != null) {
                        val old = sidebar
                        val merged = if (old == null) parsed else old.copy(
                            forums = parsed.forums.ifEmpty { old.forums },
                            recentForums = parsed.recentForums.ifEmpty { old.recentForums },
                            hotTopics = parsed.hotTopics.ifEmpty { old.hotTopics },
                            statsText = parsed.statsText.ifBlank { old.statsText },
                            newUsers = parsed.newUsers.ifEmpty { old.newUsers },
                        )
                        sidebar = merged
                        session.saveHomeSidebar(merged)
                    }
                }
            } catch (e: Exception) {
                error = e.message ?: "加载失败"
            } finally {
                loading = false; loadingMore = false
            }
        }
    }

    // 翻页模式：本地按每页条数切片分页，目标页条数不足时自动连续加载源站后续页
    fun goLocalPage(target: Int) {
        scope.launch {
            val per = session.topicsPerPage.coerceAtLeast(1)
            val need = target * per
            error = null
            while (filterTopics(current.topics).size < need &&
                current.sourcePage < current.sourceTotalPages
            ) {
                loadingMore = true
                try {
                    val sp = current.sourcePage + 1
                    val resp = session.client.get(buildPath(currentSort, sp))
                    val (items, pg) = HtmlParser.parseTopicList(resp.html)
                    current.topics = (current.topics + items).distinctBy { it.topicId }
                    current.sourcePage = pg.first.coerceAtLeast(sp)
                    current.sourceTotalPages = pg.second.coerceAtLeast(sp)
                } catch (e: Exception) {
                    error = e.message ?: "加载失败"
                    break
                }
            }
            loadingMore = false
            current.page = target.coerceAtLeast(1)
            // 本地翻页跳回顶部
            listState.scrollToItem(0)
        }
    }

    // 首次进入分类：设置了"启动不刷新"且有缓存 → 直接用缓存，不发请求；
    // 否则正常加载
    LaunchedEffect(tabIndex) {
        if (!current.loaded) {
            val cached = if (session.homeCacheEnabled) session.loadHomeCache(currentSort) else emptyList()
            if (cached.isNotEmpty()) {
                current.topics = cached
                current.loaded = true
                current.page = 1
                current.sourcePage = 1
                // 缓存未知源站总页数：给个大值，翻页不足时允许继续请求源站
                current.sourceTotalPages = 999
            } else {
                load(1)
            }
        } else if (current.scrollIndex > 0 && listState.firstVisibleItemIndex == 0) {
            // 返回本分类时恢复滚动位置
            listState.scrollToItem(
                current.scrollIndex.coerceAtMost(current.topics.size - 1).coerceAtLeast(0),
                current.scrollOffset
            )
        }
    }

    // 离开分类/页面时记录滚动位置
    DisposableEffect(tabIndex) {
        onDispose {
            current.scrollIndex = listState.firstVisibleItemIndex
            current.scrollOffset = listState.firstVisibleItemScrollOffset
        }
    }

    // 无限滚动：按源站行为，移动到最下面的帖子后出现「加载更多」按钮，点击继续加载
    // （不再接近末尾自动加载）

    fun refresh() {
        current.topics = emptyList()
        current.loaded = false
        current.page = 1
        current.totalPages = 1
        current.sourcePage = 1
        current.sourceTotalPages = 1
        current.scrollIndex = 0
        current.scrollOffset = 0
        sidebar = null
        session.clearHomeCache()
        scope.launch { listState.scrollToItem(0) }
        load(1)
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = drawerState.isOpen,
        drawerContent = {
            HomeSidebarDrawer(
                session = session,
                sidebar = sidebar,
                onClose = { closeDrawer() },
                onRefresh = { closeDrawer { refresh() } },
                onOpenForum = { fid -> closeDrawer { if (fid > 0) nav.navigate("forum/$fid") } },
                onOpenTopic = { tid -> closeDrawer { if (tid > 0) nav.navigate("topic/$tid") } },
                onOpenUser = { uid -> closeDrawer { if (uid > 0) nav.navigate("user/$uid") } },
                onOpenLeaderboard = { closeDrawer { nav.navigate("leaderboard?type=points") } },
                onOpenBail = { closeDrawer { nav.navigate("bail") } },
                onLogin = { closeDrawer { nav.navigate("login") } },
                onNavigate = { route -> closeDrawer { nav.navigate(route) } },
            )
        }
    ) {
        // 状态栏空间常驻保留（不在 Scaffold topBar 槽内做显隐动画）：
        // 顶栏收起时列表只会补到状态栏下沿，不会错误渲染进状态栏区域
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            // 沉浸式顶栏：往下滚动时隐藏，回到顶部时显示（TopAppBar 不再自带状态栏 insets）
            AnimatedVisibility(visible = topBarVisible) {
                Column {
                    TopAppBar(
                        windowInsets = WindowInsets(0, 0, 0, 0),
                        title = { Text("烧饼社区") },
                        navigationIcon = {
                            IconButton(onClick = { openDrawer() }) {
                                Icon(Icons.Filled.Menu, "侧板")
                            }
                        },
                        actions = {
                            // 三点菜单：搜索 / 刷新 / 切换滚动模式（刷新也可下拉触发）
                            Box {
                                IconButton(onClick = { menuOpen = true }) {
                                    Icon(Icons.Filled.MoreVert, "菜单")
                                }
                                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                    DropdownMenuItem(
                                        text = { Text("搜索") },
                                        leadingIcon = { Icon(Icons.Filled.Search, null) },
                                        onClick = { menuOpen = false; nav.navigate("search") }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("刷新") },
                                        leadingIcon = { Icon(Icons.Filled.Refresh, null) },
                                        onClick = { menuOpen = false; refresh() }
                                    )
                                    DropdownMenuItem(
                                        text = {
                                            Text(if (homeInfinite) "切换为翻页模式" else "切换为无限滚动")
                                        },
                                        leadingIcon = { Icon(Icons.Filled.SwapVert, null) },
                                        onClick = {
                                            menuOpen = false
                                            session.saveInfiniteScroll(!homeInfinite)
                                            if (homeInfinite) {
                                                // 切回无限滚动：页码回到源站页
                                                current.page = current.sourcePage
                                                current.totalPages = current.sourceTotalPages
                                            } else {
                                                // 切到翻页模式：回到本地第一页
                                                current.page = 1
                                            }
                                            scope.launch { listState.scrollToItem(0) }
                                            session.showToast(
                                                if (homeInfinite) "已切换为无限滚动" else "已切换为翻页模式"
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    )
                    // 源站排序 tab：新评论/新帖子/精华（有层次感的圆角矩形）
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        tonalElevation = 2.dp,
                        shadowElevation = 1.dp,
                    ) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SORTS.forEachIndexed { i, (_, label) ->
                                val selected = tabIndex == i
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (selected) MaterialTheme.colorScheme.secondaryContainer
                                    else MaterialTheme.colorScheme.surfaceContainerHigh,
                                    shadowElevation = if (selected) 2.dp else 0.dp,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { session.homeTabIndex = i; session.homeCombo = 0 }
                                ) {
                                    Text(
                                        label,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (selected) MaterialTheme.colorScheme.onSecondaryContainer
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 9.dp)
                                    )
                                }
                            }
                        }
                    }
                    // 组合过滤：全部 / 仅抽奖 / 仅发卡
                    SingleChoiceSegmentedButtonRow(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        listOf("全部" to 0, "仅抽奖" to 1, "仅发卡" to 2).forEachIndexed { index, (label, c) ->
                            SegmentedButton(
                                selected = combo == c,
                                onClick = { session.homeCombo = c },
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = 3),
                            ) {
                                Text(label)
                            }
                        }
                    }
                }
            }
            Box(Modifier.weight(1f).fillMaxWidth()) {
                when {
                    loading && current.topics.isEmpty() -> LoadingBox()
                    error != null && current.topics.isEmpty() -> ErrorBox(error!!) { load(1) }
                    visibleTopics.isEmpty() -> EmptyBox(
                        if (session.blockedWords.isEmpty() && session.blockedUsers.isEmpty()) "暂无帖子"
                        else "暂无帖子（已按源站屏蔽规则过滤）"
                    )
                    else -> Box(Modifier.fillMaxSize()) {
                        // 单列列表 + 顶部下拉刷新（按源站解析的标题正常展示）
                        PullToRefreshBox(
                            isRefreshing = loading && current.topics.isNotEmpty(),
                            onRefresh = { load(1) },
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(vertical = 8.dp)
                            ) {
                                items(
                                    pagedTopics,
                                    key = { t -> t.topicId },
                                ) { t ->
                                    TopicCardView(
                                        t,
                                        views = session.getTopicViews(t.topicId),
                                        onClick = { nav.navigate("topic/${t.topicId}") },
                                        onForumClick = { fid -> if (fid > 0) nav.navigate("forum/$fid") },
                                    )
                                }
                                item {
                                    if (homeInfinite) {
                                        // 无限滚动：移动到最下面帖子后出现「加载更多」按钮，点击继续加载
                                        if (current.page < current.totalPages) {
                                            Box(
                                                Modifier
                                                    .fillMaxWidth()
                                                    .padding(12.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (loadingMore) CircularProgressIndicator(Modifier.size(22.dp))
                                                else Button(onClick = { load(current.page + 1, append = true) }) {
                                                    Text("加载更多")
                                                }
                                            }
                                        }
                                    } else {
                                        PaginationBar(current.page, localTotalPages) { goLocalPage(it) }
                                    }
                                    // 翻页模式翻页条下不留大空白；无限滚动为悬浮按钮留出空间
                                    Spacer(Modifier.height(if (homeInfinite) 64.dp else 12.dp))
                                }
                            }
                        }
                        // 回到顶部按钮：列表滚过前几条后出现（缩放动画，不用 AnimatedVisibility 避免嵌套作用域限制）
                        val showBackTop by remember {
                            derivedStateOf { listState.firstVisibleItemIndex > 2 }
                        }
                        val backTopScale by animateFloatAsState(
                            targetValue = if (showBackTop) 1f else 0f,
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                            label = "backTop"
                        )
                        if (backTopScale > 0.01f) {
                            SmallFloatingActionButton(
                                onClick = { scope.launch { listState.animateScrollToItem(0) } },
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(end = 16.dp, bottom = 24.dp)
                                    .graphicsLayer {
                                        scaleX = backTopScale
                                        scaleY = backTopScale
                                        alpha = backTopScale
                                    },
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
    }
}

/** 源站首页侧板：用户卡片 / 签到天数 / 版块列表 / 快捷功能 / 每日热帖 / 最近浏览版块 / 站点统计 / 最新用户 */
@Composable
private fun HomeSidebarDrawer(
    session: Session,
    sidebar: HomeSidebar?,
    onClose: () -> Unit,
    onRefresh: () -> Unit,
    onOpenForum: (Long) -> Unit,
    onOpenTopic: (Long) -> Unit,
    onOpenUser: (Long) -> Unit,
    onOpenLeaderboard: () -> Unit,
    onOpenBail: () -> Unit,
    onLogin: () -> Unit,
    onNavigate: (String) -> Unit,
) {
    ModalDrawerSheet(modifier = Modifier.width(300.dp)) {
        val sb = sidebar
        Column(Modifier.fillMaxSize()) {
            // 顶部用户卡片：品牌色横幅 + 头像 + 签到天数
            val state = session.loginState
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
            ) {
                Column(Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (state.loggedIn) {
                            Avatar(state.avatarUrl, 46, userId = state.userId)
                            Spacer(Modifier.width(11.dp))
                            Column(Modifier.weight(1f)) {
                                Text(state.username.ifBlank { "饼友" }, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text(
                                    "${state.userGroup.ifBlank { "饼友" }} · 积分 ${state.points.ifBlank { "-" }}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                                )
                            }
                            FilledTonalButton(
                                onClick = { onClose(); onOpenUser(state.userId) },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                            ) { Text("主页", style = MaterialTheme.typography.labelMedium) }
                        } else {
                            Column(Modifier.weight(1f)) {
                                Text("访客", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text(
                                    "登录后可发帖、回帖与签到",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            FilledTonalButton(
                                onClick = onLogin,
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
                            ) { Text("登录", style = MaterialTheme.typography.labelMedium) }
                        }
                    }
                    // 签到入口行
                    if (state.loggedIn && session.checkinText.isNotBlank()) {
                        Spacer(Modifier.height(10.dp))
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { onNavigate("checkin") },
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.6f)
                        ) {
                            Row(
                                Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Filled.CalendarMonth, null,
                                    Modifier.size(15.dp), tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    session.checkinText,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.weight(1f))
                                Text(
                                    if (session.checkinCheckedToday) "已签到" else "去签到",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
            // 单列 / 双列显示切换（作用于侧板的快捷功能与版块列表），放进容器显式分组；标题独占一行，按钮在其下方横排
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "侧栏显示",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = if (!session.sidebarTwoColumns) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                            onClick = { session.saveSidebarTwoColumns(false) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                Modifier.padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Filled.ViewList, "单列", Modifier.size(16.dp),
                                    tint = if (!session.sidebarTwoColumns) MaterialTheme.colorScheme.onPrimaryContainer
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    "单列", style = MaterialTheme.typography.labelMedium,
                                    color = if (!session.sidebarTwoColumns) MaterialTheme.colorScheme.onPrimaryContainer
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = if (session.sidebarTwoColumns) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                            onClick = { session.saveSidebarTwoColumns(true) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                Modifier.padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Filled.GridView, "双列", Modifier.size(16.dp),
                                    tint = if (session.sidebarTwoColumns) MaterialTheme.colorScheme.onPrimaryContainer
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    "双列", style = MaterialTheme.typography.labelMedium,
                                    color = if (session.sidebarTwoColumns) MaterialTheme.colorScheme.onPrimaryContainer
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
            HorizontalDivider()

            LazyColumn(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp)
            ) {
                // 快捷功能（无图标，统一圆角矩形背景；按侧板单/双列设置排布）
                item { DrawerTitle("快捷功能") }
                val quickActions = listOf(
                    "每日签到" to { onNavigate("checkin") },
                    "我的通知" to { onNavigate("notifications") },
                    "浏览足迹" to { onNavigate("footprint") },
                    "社区保释所" to { onOpenBail() },
                    "用户榜单" to { onOpenLeaderboard() },
                    "应用设置" to { onNavigate("appSettings") },
                    "关于应用" to { onNavigate("about") },
                )
                if (session.sidebarTwoColumns) {
                    quickActions.chunked(2).forEachIndexed { ri, row ->
                        item(key = "q-$ri") {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                row.forEach { (label, action) ->
                                    DrawerPillRow(label, action, Modifier.weight(1f))
                                }
                                if (row.size == 1) Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                } else {
                    quickActions.forEachIndexed { i, (label, action) ->
                        item(key = "q-$i") { DrawerPillRow(label, action, Modifier.fillMaxWidth()) }
                    }
                }

                // 板块（来自源站侧边栏解析 + 本地永久缓存，点击进入才请求该板块内容）
                if (sb?.forums?.isNotEmpty() == true) {
                    item { DrawerTitle("板块") }
                    val forumRows =
                        if (session.sidebarTwoColumns) sb.forums.chunked(2) else sb.forums.map { listOf(it) }
                    items(forumRows, key = { "f-${it.first().id}" }) { row: List<ForumCount> ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            row.forEach { f: ForumCount ->
                                ForumEntryRow(f, onOpenForum, Modifier.weight(1f))
                            }
                            if (row.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }

                // 每日热帖
                if (sb?.hotTopics?.isNotEmpty() == true) {
                    item { DrawerTitle("每日热帖 · 近 24 小时") }
                    items(sb.hotTopics, key = { "h-${it.topicId}" }) { t ->
                        HotTopicRow(t, onOpenTopic, Modifier.fillMaxWidth())
                    }
                }

                // 最近浏览版块
                if (sb?.recentForums?.isNotEmpty() == true) {
                    item { DrawerTitle("最近浏览版块") }
                    items(sb.recentForums, key = { "r-${it.id}" }) { f ->
                        DrawerPillRow(f.name, { onOpenForum(f.id) }, Modifier.fillMaxWidth())
                    }
                }

                // 站点统计
                if (!sb?.statsText.isNullOrBlank()) {
                    item {
                        ListItem(
                            headlineContent = {
                                Text("站点统计", style = MaterialTheme.typography.labelLarge)
                            },
                            supportingContent = {
                                Text(
                                    sb!!.statsText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            },
                        )
                    }
                }

                if (sb == null) {
                    item {
                        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                            Text(
                                "侧板加载失败，请重试",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            TextButton(onClick = onRefresh) { Text("重新加载") }
                        }
                    }
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
private fun DrawerTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 4.dp, top = 14.dp, bottom = 5.dp)
    )
}

/** 侧板选项条目：无图标，统一圆角矩形背景框住（快捷功能 / 最近版块等） */
@Composable
private fun DrawerPillRow(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier.padding(vertical = 2.dp),
    ) {
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        )
    }
}

/** 侧板热帖条目：标题/热度，圆角矩形背景，无图标 */
@Composable
private fun HotTopicRow(t: sb.linux.client.data.HotTopic, onOpenTopic: (Long) -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = { onOpenTopic(t.topicId) },
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier.padding(vertical = 2.dp),
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 7.dp)) {
            Text(
                t.title, style = MaterialTheme.typography.bodyMedium,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            if (t.meta.isNotBlank()) {
                Text(
                    t.meta, style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/** 侧板版块条目：仅名称（数量框已移除） */
@Composable
private fun ForumEntryRow(f: ForumCount, onOpenForum: (Long) -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = { onOpenForum(f.id) },
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier.padding(vertical = 2.dp),
    ) {
        Text(
            f.name, style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        )
    }
}
