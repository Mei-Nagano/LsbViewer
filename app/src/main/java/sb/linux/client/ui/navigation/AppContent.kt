package sb.linux.client.ui.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import sb.linux.client.data.Session
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** 根据设备与底栏设置装配手机/平板 NavHost。 */
@Composable
internal fun AppContent(
    session: Session,
    nav: NavHostController,
    masterNav: NavHostController,
    detailNav: NavHostController,
    snackbar: SnackbarHostState,
    scope: CoroutineScope,
    homeDrawerState: DrawerState,
    twoPane: Boolean,
    route: String,
    bottomRoutes: Set<String>,
    bottomItems: List<String>,
    onNavigateBottom: (String) -> Unit,
) {
    if (!twoPane) {
            if (session.bottomBarStyle != 0) {
                val bgColor = MaterialTheme.colorScheme.background
                val backdrop = rememberLayerBackdrop {
                    drawRect(bgColor)
                    drawContent()
                }
                Scaffold(
                    contentWindowInsets = WindowInsets(0, 0, 0, 0),
                    snackbarHost = { SnackbarHost(snackbar) },
                    bottomBar = {
                        if (route in bottomRoutes) {
                            LiquidGlassBottomBar(
                                backdrop = backdrop,
                                route = route,
                                items = bottomItems,
                                enabled = homeDrawerState.isClosed,
                                onNavigate = onNavigateBottom,
                            )
                        }
                    },
                ) { scaffoldPadding ->
                    NavHost(
                        navController = nav,
                        startDestination = "home",
                        modifier = Modifier.padding(scaffoldPadding).fillMaxSize().layerBackdrop(backdrop),
                        enterTransition = { fadeIn(tween(NAV_FADE_MS)) },
                        exitTransition = { fadeOut(tween(NAV_FADE_MS)) },
                        popEnterTransition = { fadeIn(tween(NAV_FADE_MS)) },
                        popExitTransition = { fadeOut(tween(NAV_FADE_MS)) },
                    ) {
                        masterRoutes(session, nav, homeDrawerState)
                        detailRoutes(session, nav)
                    }
                }
            } else {
                Scaffold(
                    contentWindowInsets = WindowInsets(0, 0, 0, 0),
                    snackbarHost = { SnackbarHost(snackbar) },
                    bottomBar = {
                        if (route in bottomRoutes) {
                            NavigationBar {
                                bottomItems.forEach { item ->
                                    val label = when (item) {
                                        "home" -> "首页"
                                        "topicCollections" -> "淘帖"
                                        "directMessages" -> "私信"
                                        "newTopic" -> "发帖"
                                        else -> "我的"
                                    }
                                    val icon = when (item) {
                                        "home" -> Icons.Filled.Home
                                        "topicCollections" -> Icons.Filled.ViewList
                                        "directMessages" -> Icons.Filled.Email
                                        "newTopic" -> Icons.Filled.Add
                                        else -> Icons.Filled.Person
                                    }
                                    NavigationBarItem(
                                        selected = BottomDestination.canonical(route) == BottomDestination.canonical(item),
                                        onClick = { onNavigateBottom(item) },
                                        icon = { Icon(icon, null) },
                                        label = { Text(label) },
                                    )
                                }
                            }
                        }
                    },
                ) { pad ->
                    NavHost(
                        navController = nav,
                        startDestination = "home",
                        modifier = Modifier.padding(pad),
                        enterTransition = { fadeIn(tween(NAV_FADE_MS)) },
                        exitTransition = { fadeOut(tween(NAV_FADE_MS)) },
                        popEnterTransition = { fadeIn(tween(NAV_FADE_MS)) },
                        popExitTransition = { fadeOut(tween(NAV_FADE_MS)) },
                    ) {
                        masterRoutes(session, nav, homeDrawerState)
                        detailRoutes(session, nav)
                    }
                }
            }
    } else {
            Scaffold(
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                snackbarHost = { SnackbarHost(snackbar) },
            ) { pad ->
                Row(Modifier.padding(pad).fillMaxSize()) {
                    val masterBackStack by masterNav.currentBackStackEntryAsState()
                    val masterRoute = masterBackStack?.destination?.route ?: "home"

                    fun switchTab(target: String) {
                        masterNav.navigate(target) {
                            popUpTo(masterNav.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }

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
                    TabletNavRail(
                        selected = masterRoute,
                        onSelect = ::switchTab,
                        onOpenSidebar = {
                            if (masterRoute != "home") switchTab("home")
                            scope.launch { homeDrawerState.open() }
                        },
                    )
                    val screenWidth = LocalConfiguration.current.screenWidthDp
                    val masterWidth = (screenWidth * 0.32f).coerceIn(300f, 420f).dp
                    NavHost(
                        navController = masterNav,
                        startDestination = "home",
                        modifier = Modifier.fillMaxHeight().width(masterWidth),
                        enterTransition = { fadeIn(tween(NAV_FADE_MS)) },
                        exitTransition = { fadeOut(tween(NAV_FADE_MS)) },
                        popEnterTransition = { fadeIn(tween(NAV_FADE_MS)) },
                        popExitTransition = { fadeOut(tween(NAV_FADE_MS)) },
                    ) {
                        masterRoutes(session, detailNav, homeDrawerState)
                    }
                    VerticalDivider(Modifier.fillMaxHeight())
                    NavHost(
                        navController = detailNav,
                        startDestination = "detailEmpty",
                        modifier = Modifier.weight(2f).fillMaxHeight(),
                        enterTransition = { fadeIn(tween(NAV_FADE_MS)) },
                        exitTransition = { fadeOut(tween(NAV_FADE_MS)) },
                        popEnterTransition = { fadeIn(tween(NAV_FADE_MS)) },
                        popExitTransition = { fadeOut(tween(NAV_FADE_MS)) },
                    ) {
                        detailRoutes(session, detailNav)
                    }
                }
            }
    }
}
