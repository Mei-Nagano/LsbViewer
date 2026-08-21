package sb.linux.client.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import kotlinx.coroutines.flow.distinctUntilChanged
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

    // 顶栏搜索态（t10）：搜索时整条顶栏变为搜索框；退出 / 失焦恢复
    var searchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val searchFocus = remember { FocusRequester() }
    // 进入搜索态时自动聚焦输入框（否则输入框一出现就因「未失焦且为空」被立刻关闭，表现为闪现）
    LaunchedEffect(searchActive) {
        if (searchActive) searchFocus.requestFocus()
    }
    // 排序抽屉（t8）：(全部/仅抽奖/仅发卡) 上方的 (新评论/新帖子/精华) 折叠区，状态持久化
    var sortDrawerOpen by remember { mutableStateOf(session.homeSortDrawerOpen) }
    // 源站首页侧板数据（与首次首页请求同步解析，并本地永久缓存，避免每次访问源站）
    var sidebar by remember { mutableStateOf<HomeSidebar?>(session.loadHomeSidebar()) }

    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    // 沉浸式顶栏：向下滚动时隐藏，向上滚动（或回到顶部）时重新显示
    var topBarVisible by remember { mutableStateOf(true) }
    LaunchedEffect(listState) {
        var lastIdx = listState.firstVisibleItemIndex
        var lastOff = listState.firstVisibleItemScrollOffset
        snapshotFlow {
            listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset
        }.collect { (idx, off) ->
            val atTop = idx == 0 && off <= 0
            // 换行：索引增大为向下；同一项内：像素偏移增大为向下
            val scrolledDown = when {
                idx != lastIdx -> idx > lastIdx
                off != lastOff -> off > lastOff
                else -> false
            }
            // 反向滚动即为向上（回顶也显示）；无位移时不改变，避免抖动
            when {
                atTop || !scrolledDown -> topBarVisible = true
                else -> topBarVisible = false
            }
            lastIdx = idx; lastOff = off
        }
    }

    // 分类（全部/仅抽奖/仅发卡）或排序 tab 切换后回到列表顶部。
    // 在点击回调里直接 scrollToItem 会与切换后的新数据布局竞争：LazyList 以 item key 锚定滚动位置，
    // 若当前首条可见项在新过滤结果中仍存在，先发的 scrollToItem(0) 会被随后的重布局覆盖回原位。
    // 这里在重组完成后等一帧（新列表已布局），再显式滚到 0，保证生效。
    // 记录上次值跳过首次执行：从帖子返回时保留列表位置（rememberSaveable 恢复的滚动不能被清掉）
    var lastCombo by remember { mutableStateOf(combo) }
    var lastTabIdx by remember { mutableStateOf(tabIndex) }
    LaunchedEffect(combo, tabIndex) {
        if (combo != lastCombo || tabIndex != lastTabIdx) {
            lastCombo = combo; lastTabIdx = tabIndex
            withFrameNanos { }
            listState.scrollToItem(0)
        }
    }

    fun openDrawer() {
        scope.launch { drawerState.open() }
    }

    // 先让抽屉完成关闭动画，再执行导航/刷新。
    // 若在导航后才启动关闭，rememberCoroutineScope 会随主页离开组合而被取消，
    // 抽屉（ModalDrawerSheet 是 Popup）的关闭动画被中断、收尾不完整，返回时残留半透明层盖住首页表现为白屏。
    // 在协程内先挂起等 drawerState.close() 结束，再执行 action()；此时主页尚未导航离开，作用域仍存活，回调必定执行。
    fun closeDrawer(action: () -> Unit = {}) {
        if (drawerState.isClosed) {
            action()
        } else {
            scope.launch {
                drawerState.close()
                action()
            }
        }
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
                // 通知红点跟随源站：每次首页请求顺带解析顶栏未读通知数
                session.updateNotifUnread(HtmlParser.parseNotifyBadgeCount(resp.html))
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

    // 翻页模式：切换分类（全部/仅抽奖/仅发卡）时是「本地过滤」已加载的帖子，可能不足一页。
    // 这里切到第一页并自动补足到设定的每页条数，避免切换后只显示寥寥几个帖子。
    LaunchedEffect(combo, homeInfinite) {
        if (!homeInfinite) {
            withFrameNanos { }
            goLocalPage(1)
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

    // 无限滚动：滚动接近末尾时自动加载下一页（一次只加载一页，避免一次性拉太多，降低对服务器压力）；
    // 到达底部仍会显示「加载更多」按钮，供自动加载未触发时手动兜底。
    LaunchedEffect(listState, homeInfinite, visibleTopics.size, current.sourceTotalPages, loadingMore, loading) {
        if (!homeInfinite || loadingMore || loading || current.topics.isEmpty()) return@LaunchedEffect
        val nearEnd = 2 // 列表还剩约 2 个帖子时触发
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1 }
            .distinctUntilChanged()
            .collect { lastIdx ->
                if (!loadingMore && !loading &&
                    lastIdx >= visibleTopics.size - nearEnd &&
                    current.sourcePage < current.sourceTotalPages
                ) {
                    load(current.sourcePage + 1, append = true)
                }
            }
    }

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
        // 主页刷新同时清空帖子分页缓存，保证进入帖子时重新抓最新内容
        session.clearAllTopicPageCache()
        scope.launch { listState.scrollToItem(0) }
        load(1)
    }

    // 顶栏搜索提交：带回显关键词跳转搜索页
    fun submitTopSearch() {
        if (searchQuery.isBlank()) { searchActive = false; return }
        val q = java.net.URLEncoder.encode(searchQuery.trim(), "UTF-8")
        nav.navigate("search?q=$q")
        searchQuery = ""; searchActive = false
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        // 仅「全部」分类下启用抽屉边缘手势：向左边缘划可拉出侧边栏（其它组合切换交给列表横向滑动）
        gesturesEnabled = session.homeCombo == 0,
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
                    // 顶栏：正常态 / 搜索态 二选一
                    if (searchActive) {
                        // 搜索态：整条顶栏变成搜索输入框 + 右侧搜索按钮；失焦后恢复
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { searchActive = false; searchQuery = "" }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, "取消搜索")
                            }
                            Surface(
                                shape = RoundedCornerShape(22.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp)
                            ) {
                                Box(
                                    Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    if (searchQuery.isEmpty()) {
                                        Text(
                                            "搜索全站…",
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                            modifier = Modifier.padding(start = 16.dp)
                                        )
                                    }
                                    BasicTextField(
                                        value = searchQuery,
                                        onValueChange = { searchQuery = it },
                                        singleLine = true,
                                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                                            color = MaterialTheme.colorScheme.onSurface
                                        ),
                                        keyboardOptions = KeyboardOptions(
                                            keyboardType = KeyboardType.Text,
                                            imeAction = ImeAction.Search
                                        ),
                                        keyboardActions = KeyboardActions(onSearch = { submitTopSearch() }),
                                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = 16.dp, end = 16.dp)
                                            .focusRequester(searchFocus),
                                    )
                                }
                            }
                            IconButton(onClick = { submitTopSearch() }) {
                                Icon(Icons.Filled.Search, "搜索")
                            }
                        }
                    } else {
                        TopAppBar(
                            windowInsets = WindowInsets(0, 0, 0, 0),
                            title = { Text("烧饼社区") },
                            navigationIcon = {
                                IconButton(onClick = { openDrawer() }) {
                                    Icon(Icons.Filled.Menu, "侧板")
                                }
                            },
                            actions = {
                                // 通知：源站有未读时右上角显示小红点（未读数来自首页 .notify-badge）
                                Box {
                                    IconButton(
                                        onClick = { nav.navigate("notifications") }
                                    ) {
                                        Icon(Icons.Filled.Notifications, "通知")
                                    }
                                    if (session.notifUnreadCount > 0) {
                                        Box(
                                            Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(top = 8.dp, end = 3.dp)
                                                .size(9.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.error)
                                        )
                                    }
                                }
                                // 搜索：点击展开为顶栏搜索框
                                IconButton(onClick = { searchActive = true }) {
                                    Icon(Icons.Filled.Search, "搜索")
                                }
                                // 三点菜单：刷新 / 切换滚动模式（刷新也可下拉触发）
                                Box {
                                    IconButton(onClick = { menuOpen = true }) {
                                        Icon(Icons.Filled.MoreVert, "菜单")
                                    }
                                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
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
                                                    // 离开无限滚动 → 切到翻页模式：回到本地第一页
                                                    current.page = 1
                                                } else {
                                                    // 离开翻页模式 → 切到无限滚动：页码回到已加载的源站页
                                                    current.page = current.sourcePage
                                                    current.totalPages = current.sourceTotalPages
                                                }
                                                scope.launch { listState.scrollToItem(0) }
                                                session.showToast(
                                                    if (homeInfinite) "已切换为翻页模式" else "已切换为无限滚动"
                                                )
                                            }
                                        )
                                    }
                                }
                            }
                        )
                    }
                    // 组合过滤（全部/仅抽奖/仅发卡）移到上方，并作为（新评论/新帖子/精华）的抽屉容器
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        tonalElevation = 2.dp,
                        shadowElevation = 1.dp,
                    ) {
                        // 仅折叠时在底部多留白，避免「全部/仅抽奖/仅发卡」贴住下边缘；展开时排序区自带底部间距
                        Column(Modifier.padding(bottom = if (sortDrawerOpen) 0.dp else 14.dp)) {
                            // 组合过滤行（质感样式）
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(start = 8.dp, end = 4.dp, top = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf("全部" to 0, "仅抽奖" to 1, "仅发卡" to 2).forEachIndexed { _, (label, c) ->
                                    val selected = combo == c
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (selected) MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.surfaceContainerHigh,
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable {
                                                session.homeCombo = c
                                                // 回到顶部由下方 LaunchedEffect(combo) 统一处理
                                            }
                                    ) {
                                        Text(
                                            label,
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                                            else MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 8.dp)
                                        )
                                    }
                                }
                                // 折叠 / 展开 排序抽屉按钮（状态持久化）
                                FilledTonalIconButton(
                                    onClick = {
                                        sortDrawerOpen = !sortDrawerOpen
                                        session.saveHomeSortDrawerOpen(sortDrawerOpen)
                                    },
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(
                                        if (sortDrawerOpen) Icons.Filled.KeyboardArrowUp
                                        else Icons.Filled.KeyboardArrowDown,
                                        if (sortDrawerOpen) "收起排序" else "展开排序",
                                        Modifier.size(20.dp)
                                    )
                                }
                            }
                            // 排序 tab（新评论/新帖子/精华）做成下拉抽屉，横向可滑动
                            AnimatedVisibility(
                                visible = sortDrawerOpen,
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut()
                            ) {
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 7.dp),
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
                                                .clip(RoundedCornerShape(12.dp))
                                                .clickable {
                                                    session.homeTabIndex = i; session.homeCombo = 0
                                                    // 回到顶部由下方 LaunchedEffect(combo, tabIndex) 统一处理
                                                }
                                        ) {
                                            Text(
                                                label,
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (selected) MaterialTheme.colorScheme.onSecondaryContainer
                                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(horizontal = 22.dp, vertical = 8.dp)
                                            )
                                        }
                                    }
                                }
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
                    else -> Box(
                        Modifier
                            .fillMaxSize()
                            // 主页左右滑动在（全部/仅抽奖/仅发卡）之间切换；垂直滚动不受影响
                            .pointerInput(Unit) {
                                var totalX = 0f
                                val threshold = 60.dp.toPx()
                                detectHorizontalDragGestures(
                                    onHorizontalDrag = { _, dragAmount -> totalX += dragAmount },
                                    onDragEnd = {
                                        val c = session.homeCombo
                                        val next = when {
                                            totalX <= -threshold -> (c + 1) % 3  // 左滑 → 下一个
                                            totalX >= threshold -> (c + 2) % 3  // 右滑 → 上一个
                                            else -> c
                                        }
                                        totalX = 0f
                                        if (next != c) {
                                            // 回到顶部由下方 LaunchedEffect(combo) 统一处理
                                            session.homeCombo = next
                                        }
                                    },
                                    onDragCancel = { totalX = 0f },
                                )
                            }
                    ) {
                        // 翻页模式：翻页条作为列表最后一个 item，滚动到底部即可看到
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
                                if (homeInfinite) {
                                    item {
                                        // 无限滚动：移动到最下面帖子后出现「加载更多」按钮
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
                                        // 底部留白：为悬浮按钮和底栏留出空间
                                        Spacer(Modifier.height(64.dp))
                                    }
                                } else {
                                    // 翻页模式：翻页条紧贴最后一项（前面不留大段空白），滑到底即完整露出，无需再多次滑动
                                    item {
                                        PaginationBar(current.page, localTotalPages) { goLocalPage(it) }
                                    }
                                }
                            }
                        }
                        // 回到顶部按钮：列表滚过前几条后出现（缩放动画，不用 AnimatedVisibility 避免嵌套作用域限制）
                        val showBackTop by remember {
                            derivedStateOf { listState.firstVisibleItemIndex > 2 }
                        }
                        // 接近列表底部（最后一条帖子或翻页条/加载更多进入视口）时按钮上移，
                        // 避免悬浮在右下角遮挡最后一条帖子和翻页按钮（两者高度区间与按钮重叠）
                        val nearBottom by remember {
                            derivedStateOf {
                                val info = listState.layoutInfo
                                val last = info.visibleItemsInfo.lastOrNull()?.index ?: -1
                                info.totalItemsCount > 0 && last >= info.totalItemsCount - 2
                            }
                        }
                        val backTopBottom by animateDpAsState(
                            targetValue = if (nearBottom) 92.dp else 24.dp,
                            label = "backTopBottom"
                        )
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
                                    .padding(end = 16.dp, bottom = backTopBottom)
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
                            Avatar(state.avatarUrl, 46)
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
                    "收藏内容" to { onNavigate("favorites") },
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
                    val twoCol = session.sidebarTwoColumns
                    if (twoCol) {
                        val forumRows = sb.forums.chunked(2)
                        items(forumRows, key = { "f-${it.first().id}" }) { row: List<ForumCount> ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                row.forEach { f: ForumCount ->
                                    ForumEntryRow(f, onOpenForum, Modifier.weight(1f))
                                }
                                if (row.size == 1) Spacer(Modifier.weight(1f))
                            }
                        }
                    } else {
                        // 单列展示：每条横跨整行（避免右侧留白），并在右侧展示帖子数量
                        items(sb.forums, key = { "f2-${it.id}" }) { f: ForumCount ->
                            ForumEntryRow(f, onOpenForum, Modifier.fillMaxWidth(), showCount = true)
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

                // 站点统计：结构化为「数值 + 标签」网格，容器风格与其他卡片一致
                if (!sb?.statsText.isNullOrBlank()) {
                    item { DrawerTitle("站点统计") }
                    item {
                        val stats = remember(sb!!.statsText) { parseSidebarStats(sb!!.statsText) }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier.padding(vertical = 2.dp),
                        ) {
                            if (stats.isEmpty()) {
                                // 兜底：原文格式不认识时按普通文本展示
                                Text(
                                    sb!!.statsText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                )
                            } else {
                                SidebarStatsCard(stats)
                            }
                        }
                    }
                }

                // 当前在线（源站侧栏 online-users-card）：在线总数 + 头像网格 + 「还有 N 人在线」
                if (!sb?.onlineUsers?.count.isNullOrBlank() || sb?.onlineUsers?.items?.isNotEmpty() == true) {
                    item { DrawerTitle("当前在线${sb!!.onlineUsers.count.takeIf { it.isNotBlank() }?.let { " · $it" } ?: ""}") }
                    item {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier.padding(vertical = 2.dp),
                        ) {
                            Column(Modifier.padding(horizontal = 10.dp, vertical = 10.dp)) {
                                val online = sb!!.onlineUsers
                                // 头像 + 用户名网格（每行 4 个，点击进入用户主页）
                                online.items.chunked(4).forEach { row ->
                                    Row(
                                        Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    ) {
                                        row.forEach { u ->
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .clickable { onOpenUser(u.userId) }
                                                    .padding(vertical = 3.dp),
                                            ) {
                                                Avatar(u.avatarUrl, 36, online = true)
                                                Spacer(Modifier.height(3.dp))
                                                Text(
                                                    u.username,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.padding(horizontal = 2.dp),
                                                )
                                            }
                                        }
                                        repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                                    }
                                    Spacer(Modifier.height(4.dp))
                                }
                                if (online.moreText.isNotBlank()) {
                                    Text(
                                        online.moreText,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
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

/** 站点统计卡片：数字项按每行 3 列「数值上、标签下」展示；尾部文本项（如最新用户）整体一行居中 */
@Composable
private fun SidebarStatsCard(stats: List<Pair<String, String>>) {
    val numeric = stats.filter { it.second.any(Char::isDigit) }
    val textual = stats.filter { !it.second.any(Char::isDigit) }
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        numeric.chunked(3).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                row.forEach { (label, value) ->
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            value,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                        )
                        Text(
                            label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
        textual.forEach { (label, value) ->
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (label.isNotEmpty()) {
                    Text(
                        label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.width(4.dp))
                }
                Text(
                    value,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/**
 * 解析站点统计原文为「标签-值」列表：
 * 数字项按「标签 数字」成对提取（兼容「标签: 数字」「标签 · 数字」等分隔变体）；
 * 尾部无数字文本（如「最新: 用户名」）按冒号或末个空格拆分。
 * 解析不出任何项时返回空列表，由调用方回退原文展示。
 */
private fun parseSidebarStats(text: String): List<Pair<String, String>> {
    val cleaned = text.replace(Regex("""\s+"""), " ").trim()
    if (cleaned.isEmpty()) return emptyList()
    val leadSep = Regex("""^[·|/、,，;；\-—:：\s]+""")
    fun cleanLabel(s: String) =
        s.replace(":", "").replace("：", "").replace(leadSep, "").trim()
    val pairs = mutableListOf<Pair<String, String>>()
    val numRe = Regex("""\d[\d,，]*""")
    var lastEnd = 0
    numRe.findAll(cleaned).forEach { m ->
        val label = cleanLabel(cleaned.substring(lastEnd, m.range.first))
        if (label.isNotEmpty()) pairs += label to m.value
        lastEnd = m.range.last + 1
    }
    val tail = cleaned.substring(lastEnd).replace(leadSep, "").trim()
    if (tail.isNotEmpty()) {
        val colon = tail.indexOfLast { it == ':' || it == '：' }
        if (colon >= 0) {
            val l = tail.substring(0, colon).trim()
            val v = tail.substring(colon + 1).trim()
            if (l.isNotEmpty() && v.isNotEmpty()) pairs += l to v
        } else {
            val sp = tail.indexOfLast { it == ' ' }
            if (sp > 0) pairs += tail.substring(0, sp).trim() to tail.substring(sp + 1).trim()
            else pairs += "" to tail
        }
    }
    return pairs.filter { it.first.isNotEmpty() || it.second.isNotEmpty() }
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

/** 侧板版块条目：名称 +（单列展示时的）帖子数量角标 */
@Composable
private fun ForumEntryRow(
    f: ForumCount,
    onOpenForum: (Long) -> Unit,
    modifier: Modifier = Modifier,
    showCount: Boolean = false,
) {
    Surface(
        onClick = { onOpenForum(f.id) },
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier.padding(vertical = 2.dp),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                f.name, style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            // 单列展示时在板块右侧显示帖量（源站侧边栏的版块主题数）
            if (showCount && f.count.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                ) {
                    Text(
                        f.count,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}
