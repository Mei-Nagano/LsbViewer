package sb.linux.client.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.LazyListState
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import kotlinx.coroutines.CoroutineScope
import sb.linux.client.domain.community.HomeSidebar
import sb.linux.client.ui.session.HomeTabState
import sb.linux.client.ui.session.Session
import sb.linux.client.domain.topic.TopicCard

@Composable
internal fun HomeNavigationLayout(
    session: Session,
    nav: NavHostController,
    masterNav: NavHostController?,
    drawerState: DrawerState,
    sidebar: HomeSidebar?,
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
    onCloseDrawer: (() -> Unit) -> Unit,
    onSelectCombo: (Int, Int) -> Unit,
    onSubmitSearch: () -> Unit,
    scope: CoroutineScope,
    topBarVisible: Boolean,
    searchActive: Boolean,
    onSearchActiveChange: (Boolean) -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    searchField: String,
    onSearchFieldChange: (String) -> Unit,
    searchFieldMenu: Boolean,
    onSearchFieldMenuChange: (Boolean) -> Unit,
    menuOpen: Boolean,
    onMenuOpenChange: (Boolean) -> Unit,
    searchFocus: FocusRequester,
    sortDrawerOpen: Boolean,
    onSortDrawerOpenChange: (Boolean) -> Unit,
    tabIndex: Int,
    combo: Int,
    loadPage: (Int, Boolean, Boolean) -> Unit,
    localPage: (Int) -> Unit,
) {
    ModalNavigationDrawer(
        // 平板双栏：抽屉挂在主栏（左栏）内，而 ModalNavigationDrawer 的 Box 不裁剪内容——
        // 关闭态抽屉 offset=-width，整个矩形滑到主栏左边界外，正好盖在左侧导航栏（首页/我的按钮）
        // 上：视觉上"抽屉收不回去一直占着空间"，且拦截点击导致无法切换页面。
        // clipToBounds 把抽屉绘制与点击都限制在主栏内，关闭后完全滑出（不可见不可点）。
        // 手机模式抽屉本就滑出屏幕左缘外，此裁剪无副作用。
        modifier = Modifier.clipToBounds(),
        drawerState = drawerState,
        // 边缘手势仅在「全部」分类启用（其它组合的横向滑动交给列表分类切换），
        // 但抽屉打开后必须恢复手势与遮罩点击——否则非「全部」分类下打开抽屉后无法关闭
        gesturesEnabled = drawerState.isOpen || session.homeBrowse.homeCombo == 0,
        drawerContent = {
            // 关闭空闲态不组合侧边栏内容（用户卡片/版块列表/统计等会随首页重组白白耗费性能），
            // 用等宽占位符撑住抽屉宽度：ModalNavigationDrawer 以 drawerContent 实测宽度计算
            // 关闭锚点（-width），占位宽度与 sheet 一致（min(屏宽, 300dp)）锚点不漂移；
            // 打开/动画期间组合真实内容，滑入滑出动画正常
            if (drawerState.isOpen || drawerState.isAnimationRunning) {
            HomeSidebarDrawer(
                session = session,
                sidebar = sidebar,
                onClose = { onCloseDrawer {} },
                onRefresh = { onCloseDrawer { onRefresh() } },
                onOpenForum = { fid -> onCloseDrawer { if (fid > 0) nav.navigate("forum/$fid") } },
                onOpenTopic = { tid -> onCloseDrawer { if (tid > 0) nav.navigate("topic/$tid") } },
                onOpenUser = { uid -> onCloseDrawer { if (uid > 0) nav.navigate("user/$uid") } },
                onOpenLeaderboard = { onCloseDrawer { nav.navigate("leaderboard?type=points") } },
                onLogin = { onCloseDrawer { nav.navigate("login") } },
                // 平板双栏：抽屉里的顶层页面（应用设置）切主栏，其余内容进右栏
                onNavigate = { route ->
                    onCloseDrawer {
                        if (masterNav != null && route == "appSettings") {
                            masterNav.navigate(route) {
                                popUpTo(masterNav.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        } else {
                            nav.navigate(route)
                        }
                    }
                },
            )
            } else {
                // 等宽空占位：仅撑住 drawerContent 宽度，锚点不漂移（宽度和 sheet 一致）
                Spacer(Modifier.fillMaxHeight().widthIn(min = 300.dp))
            }
        }
    ) {
        // 状态栏空间常驻保留（不在 Scaffold topBar 槽内做显隐动画）：
        // 顶栏收起时列表只会补到状态栏下沿，不会错误渲染进状态栏区域
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            // 沉浸式顶栏：往下滚动时隐藏，回到顶部时显示（TopAppBar 不再自带状态栏 insets）
            AnimatedVisibility(visible = topBarVisible) {
                Column {
                    HomeTopBar(
                        session = session,
                        nav = nav,
                        current = current,
                        masterNav = masterNav,
                        searchActive = searchActive,
                        onSearchActiveChange = onSearchActiveChange,
                        searchQuery = searchQuery,
                        onSearchQueryChange = onSearchQueryChange,
                        searchField = searchField,
                        onSearchFieldChange = onSearchFieldChange,
                        searchFieldMenu = searchFieldMenu,
                        onSearchFieldMenuChange = onSearchFieldMenuChange,
                        menuOpen = menuOpen,
                        onMenuOpenChange = onMenuOpenChange,
                        homeInfinite = homeInfinite,
                        listState = listState,
                        scope = scope,
                        searchFocus = searchFocus,
                        onOpenDrawer = { onOpenDrawer() },
                        onRefresh = { onRefresh() },
                        onSubmitSearch = { onSubmitSearch() },
                    )
                    HomeSortControls(
                        session = session,
                        combo = combo,
                        tabIndex = tabIndex,
                        sortDrawerOpen = sortDrawerOpen,
                        onSortDrawerOpenChange = onSortDrawerOpenChange,
                        onSelectCombo = { target, direction -> onSelectCombo(target, direction) },
                    )
                }
            }
            HomeBody(
                session = session,
                nav = nav,
                viewsForTopic = viewsForTopic,
                current = current,
                homeInfinite = homeInfinite,
                loading = loading,
                error = error,
                visibleTopics = visibleTopics,
                pagedTopics = pagedTopics,
                localTotalPages = localTotalPages,
                pendingNewTopics = pendingNewTopics,
                onAcceptPendingTopics = onAcceptPendingTopics,
                loadingMore = loadingMore,
                categoryMotion = categoryMotion,
                listState = listState,
                backTopPosition = backTopPosition,
                onBackTopPositionChange = onBackTopPositionChange,
                onLoadPage = { page, append, foldNew -> loadPage(page, append, foldNew) },
                onLocalPage = { localPage(it) },
                onRefresh = { onRefresh() },
                onOpenDrawer = { onOpenDrawer() },
                onSelectCombo = { target, direction -> onSelectCombo(target, direction) },
                scope = scope,
            )
    }
}
}
