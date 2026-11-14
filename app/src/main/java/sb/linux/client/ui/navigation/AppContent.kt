package sb.linux.client.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DrawerState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import sb.linux.client.ui.session.Session
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * 应用外壳（P01）：窗口策略、根导航、pane 宿主与全局 SnackbarHost。
 *
 * 页面请求、表单保存和帖子操作仍属于各页面；外壳只负责把同一目的地模型投影成
 * 底部导航 / rail / 顶部两项兼容 Tab，并在社区根提供发帖 FAB。
 */
@Composable
internal fun AppContent(
    session: Session,
    nav: NavHostController,
    masterNav: NavHostController,
    detailNav: NavHostController,
    snackbar: SnackbarHostState,
    scope: CoroutineScope,
    homeDrawerState: DrawerState,
    layout: AppWindowLayout,
    destinations: List<AppDestination>,
    selectedId: String,
    route: String,
    onNavigateBottom: (String) -> Unit,
    onNavigateMaster: (String) -> Unit,
) {
    val rawRoute = route.substringBefore("?")
    // 根导航只出现在当前可见的根目的地上；从侧边栏等次级入口进入的页面不抢占常驻导航。
    val onRoot = destinations.any { destination ->
        rawRoute == destination.route ||
            (destination.id == AppDestinations.messages.id && rawRoute == "directMessagesRoot")
    }
    val twoItemLayout = destinations.size == 2
    val openSourceSidebar = {
        scope.launch {
            val targetNav = if (layout.useTwoPane) masterNav else nav
            if (targetNav.currentDestination?.route != AppDestinations.home.route) {
                targetNav.navigate(AppDestinations.home.route) { launchSingleTop = true }
            }
            if (homeDrawerState.isOpen) homeDrawerState.close() else homeDrawerState.open()
        }
        Unit
    }

    if (layout.useTwoPane) {
        // 双栏时 rail 切换的是主栏 NavHost；切换根目的地后右栏回到空态。
        val masterBackStack by masterNav.currentBackStackEntryAsState()
        val masterRoute = masterBackStack?.destination?.route ?: AppDestinations.home.route
        var lastMasterRoute by remember { mutableStateOf(masterRoute) }
        LaunchedEffect(masterRoute) {
            if (lastMasterRoute != masterRoute) {
                lastMasterRoute = masterRoute
                detailNav.navigate("detailEmpty") {
                    popUpTo(detailNav.graph.findStartDestination().id) { saveState = false }
                    launchSingleTop = true
                }
            }
        }
        Scaffold(
            contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
            snackbarHost = { SnackbarHost(snackbar) },
        ) { pad ->
            Column(Modifier.padding(pad).fillMaxSize()) {
                if (twoItemLayout) {
                    AppTopPrimaryTabs(destinations, selectedId, onNavigateMaster)
                }
                Row(Modifier.fillMaxSize()) {
                    if (!twoItemLayout) {
                        AppNavigationRail(
                            destinations = destinations,
                            selectedId = selectedId,
                            onSelect = onNavigateMaster,
                            header = { IconButton(onClick = openSourceSidebar) { Icon(Icons.Filled.Menu, "打开源站侧栏") } },
                        )
                    }
                    Box(Modifier.fillMaxHeight().width(layout.masterPaneWidthDp.dp)) {
                        AppNavHost(
                            navController = masterNav,
                            startDestination = AppDestinations.home.route,
                            modifier = Modifier.fillMaxSize(),
                        ) { masterRoutes(session, detailNav, homeDrawerState) }
                    }
                    VerticalDivider(Modifier.fillMaxHeight())
                    AppNavHost(
                        navController = detailNav,
                        startDestination = "detailEmpty",
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    ) { detailRoutes(session, detailNav) }
                }
            }
        }
        return
    }

    Scaffold(
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            if (twoItemLayout && onRoot) {
                AppTopPrimaryTabs(destinations, selectedId, onNavigateBottom)
            }
        },
        bottomBar = {
            if (!layout.useRail && !twoItemLayout && onRoot) {
                AppBottomNavigation(destinations, selectedId, onNavigateBottom)
            }
        },
    ) { pad ->
        Row(Modifier.padding(pad).fillMaxSize()) {
            if (layout.useRail && !twoItemLayout) {
                AppNavigationRail(
                    destinations = destinations,
                    selectedId = selectedId,
                    onSelect = onNavigateBottom,
                    header = { IconButton(onClick = openSourceSidebar) { Icon(Icons.Filled.Menu, "打开源站侧栏") } },
                )
            }
            AppNavHost(
                navController = nav,
                startDestination = AppDestinations.home.route,
                modifier = Modifier.weight(1f).fillMaxHeight(),
            ) {
                masterRoutes(session, nav, homeDrawerState)
                detailRoutes(session, nav)
            }
        }
    }
}

/** 统一页面转场：淡入淡出，减少动画时由系统设置接管。 */
@Composable
private fun AppNavHost(
    navController: NavHostController,
    startDestination: String,
    modifier: Modifier = Modifier,
    content: NavGraphBuilder.() -> Unit,
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
        enterTransition = { fadeIn(tween(NAV_FADE_MS)) },
        exitTransition = { fadeOut(tween(NAV_FADE_MS)) },
        popEnterTransition = { fadeIn(tween(NAV_FADE_MS)) },
        popExitTransition = { fadeOut(tween(NAV_FADE_MS)) },
        builder = content,
    )
}
