package sb.linux.client.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.focus.FocusRequester
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import sb.linux.client.ui.session.Session
import sb.linux.client.domain.topic.TopicCard
import sb.linux.client.ui.feature.home.HomePageViewModel
import sb.linux.client.ui.feature.home.HomePageViewModelFactory
import sb.linux.client.ui.navigation.LocalMasterNav

/** 源站首页 tab：新评论 / 新帖子 / 精华（抽奖/发卡通过组合过滤进入） */
internal val SORTS = listOf(
    "comment" to "新评论",
    "post" to "新帖子",
    "featured" to "精华",
)

/** 顶栏搜索范围值 → 中文标签 */
internal fun fieldLabelOf(field: String): String = when (field) {
    "all" -> "全部内容"
    "body" -> "主题正文"
    "reply" -> "回帖"
    "user" -> "用户"
    else -> "标题"
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    session: Session,
    nav: NavHostController,
    // 抽屉状态提升：平板双栏下左侧导航栏顶部的侧边栏按钮也能打开抽屉（手机模式内部自建）
    drawerState: DrawerState = rememberDrawerState(DrawerValue.Closed),
) {
    // 状态全部保存在 Session：进帖返回后仍在原分类、原位置
    val tabIndex = session.homeBrowse.homeTabIndex
    val combo = session.homeBrowse.homeCombo
    val tabStates = session.homeBrowse.homeTabs
    val current = tabStates[if (combo == 3) 3 else tabIndex.coerceIn(0, SORTS.size - 1)]
    val currentSort = if (combo == 3) "essence" else SORTS[tabIndex.coerceIn(0, SORTS.size - 1)].first
    // 首页分类生效的滚动模式（浏览设置可按分类覆盖，3.13）
    val homeInfinite = session.preferences.effectiveInfiniteScroll("home")

    val pageFactory = remember(
        session.homeRepository, session.homeBrowseCache, session.topicPageCacheRepository,
        session.topicViewsRepository, current, currentSort, homeInfinite,
    ) {
        HomePageViewModelFactory(
            repository = session.homeRepository,
            cache = session.homeBrowseCache,
            topicPageCache = session.topicPageCacheRepository,
            topicViews = session.topicViewsRepository,
            current = current,
            currentSort = currentSort,
            homeInfinite = homeInfinite,
            pageSize = { session.topicsPerPage },
            showToast = session::showToast,
        )
    }
    val pageViewModel: HomePageViewModel = viewModel(
        factory = pageFactory,
        key = "home-page-$currentSort",
    )
    val pageState by pageViewModel.uiState.collectAsState()
    val loading = pageState.loading
    val loadingMore = pageState.loadingMore
    val error = pageState.error
    val sidebar = pageState.sidebar
    val pendingNewTopics = pageState.pendingTopics
    var menuOpen by remember { mutableStateOf(false) }
    var floatingExpanded by remember { mutableStateOf(false) }

    // 顶栏搜索态（t10）：搜索时整条顶栏变为搜索框；退出 / 失焦恢复
    var searchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    // 搜索范围（顶栏左侧按钮选择）：与源站 scope 参数保持一致
    var searchField by rememberSaveable { mutableStateOf("title") }
    var searchFieldMenu by remember { mutableStateOf(false) }
    val searchFocus = remember { FocusRequester() }
    // 进入搜索态时自动聚焦输入框（否则输入框一出现就因「未失焦且为空」被立刻关闭，表现为闪现）
    // 排序抽屉（t8）：(全部/仅抽奖/仅发卡) 上方的 (新评论/新帖子/精华) 折叠区，状态持久化
    var sortDrawerOpen by remember { mutableStateOf(session.homeBrowse.homeSortDrawerOpen) }
    // 源站首页侧板数据（与首次首页请求同步解析，并本地永久缓存，避免每次访问源站）

    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    var backTopPosition by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    val categoryMotion = remember { Animatable(0f) }

    var topBarVisible by remember { mutableStateOf(listState.firstVisibleItemIndex == 0) }
    HomeSearchEffects(
        listState = listState,
        searchFocus = searchFocus,
        searchActive = searchActive,
        topBarVisible = topBarVisible,
        onTopBarVisibleChange = { topBarVisible = it },
        onSearchClose = { searchActive = false; searchQuery = "" },
    )

    val display = rememberHomeTopicDisplay(session, current, combo, homeInfinite)
    val visibleTopics = display.visibleTopics
    val pagedTopics = display.pagedTopics
    val localTotalPages = display.totalPages

    val actions = remember(session, current, pageState, pageViewModel, listState, drawerState, categoryMotion, scope) {
        HomeScreenActions(session, scope, listState, drawerState, current, categoryMotion, pageViewModel, pageState)
    }
    LaunchedEffect(pageState.scrollToTopVersion) {
        if (pageState.scrollToTopVersion > 0) listState.scrollToItem(0)
    }
    val masterNav = LocalMasterNav.current
    HomeListEffects(
        session = session,
        current = current,
        tabIndex = tabIndex,
        combo = combo,
        homeInfinite = homeInfinite,
        visibleTopics = visibleTopics,
        loading = loading,
        loadingMore = loadingMore,
        listState = listState,
        backTopPosition = backTopPosition,
        onBackTopPositionChange = { backTopPosition = it },
        loadCachedHome = { pageViewModel.loadCachedHome(session.homeCacheEnabled) },
        onLoad = { page, append, foldNew -> pageViewModel.load(page, append, foldNew) },
        onLocalPage = { pageViewModel.goLocalPage(it, display.filterTopics) },
    )

    HomeRefreshRequestEffect(session) { actions.refresh() }

    HomeNavigationLayout(
        session = session,
        nav = nav,
        masterNav = masterNav,
        drawerState = drawerState,
        sidebar = sidebar,
        viewsForTopic = pageViewModel::viewsFor,
        current = current,
        homeInfinite = homeInfinite,
        loading = loading,
        error = error,
        visibleTopics = visibleTopics,
        pagedTopics = pagedTopics,
        localTotalPages = localTotalPages,
        pendingNewTopics = pendingNewTopics,
        onAcceptPendingTopics = pageViewModel::acceptPendingTopics,
        loadingMore = loadingMore,
        categoryMotion = categoryMotion,
        listState = listState,
        backTopPosition = backTopPosition,
        onBackTopPositionChange = { backTopPosition = it },
        onLoadPage = { page, append, foldNew -> pageViewModel.load(page, append, foldNew) },
        onLocalPage = { pageViewModel.goLocalPage(it, display.filterTopics) },
        onRefresh = { actions.refresh() },
        onOpenDrawer = { actions.openDrawer() },
        onCloseDrawer = { action -> actions.closeDrawer(action) },
        onSelectCombo = { target, direction -> actions.selectCombo(target, direction) },
        onSubmitSearch = { submitHomeSearch(searchQuery, searchField, nav) { searchActive = false; searchQuery = "" } },
        scope = scope,
        topBarVisible = topBarVisible,
        searchActive = searchActive,
        onSearchActiveChange = { searchActive = it },
        searchQuery = searchQuery,
        onSearchQueryChange = { searchQuery = it },
        searchField = searchField,
        onSearchFieldChange = { searchField = it },
        searchFieldMenu = searchFieldMenu,
        onSearchFieldMenuChange = { searchFieldMenu = it },
        menuOpen = menuOpen,
        onMenuOpenChange = { menuOpen = it },
        searchFocus = searchFocus,
        sortDrawerOpen = sortDrawerOpen,
        onSortDrawerOpenChange = { sortDrawerOpen = it },
        tabIndex = tabIndex,
        combo = combo,
        loadPage = { page, append, foldNew -> pageViewModel.load(page, append, foldNew) },
        localPage = { pageViewModel.goLocalPage(it, display.filterTopics) },
    )

}

