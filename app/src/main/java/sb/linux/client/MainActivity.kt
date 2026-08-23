package sb.linux.client

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import sb.linux.client.data.Endpoints
import sb.linux.client.data.Session
import sb.linux.client.data.UpdateChecker
import sb.linux.client.ui.LsbTheme
import sb.linux.client.ui.LocalLinkHandler
import sb.linux.client.ui.ThemeMode
import sb.linux.client.ui.VerificationDialog
import sb.linux.client.ui.screens.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 全面屏适配：系统栏完全透明（不加半透明遮罩），内容由 Scaffold 绘制到状态栏/导航栏后面
        enableEdgeToEdge(
            statusBarStyle = androidx.activity.SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT
            ),
            navigationBarStyle = androidx.activity.SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT
            ),
        )
        setContent {
            val session: Session = viewModel()
            // 跟随应用内深色模式切换系统栏图标颜色（浅色模式用深色图标，深色模式用浅色图标）
            val dark = when (session.themeMode) {
                sb.linux.client.data.ThemeModePref.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
                sb.linux.client.data.ThemeModePref.LIGHT -> false
                sb.linux.client.data.ThemeModePref.DARK -> true
            }
            LaunchedEffect(dark) {
                val controller = androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
                controller.isAppearanceLightStatusBars = !dark
                controller.isAppearanceLightNavigationBars = !dark
            }
            LsbTheme(
                themeMode = when (session.themeMode) {
                    sb.linux.client.data.ThemeModePref.SYSTEM -> ThemeMode.SYSTEM
                    sb.linux.client.data.ThemeModePref.LIGHT -> ThemeMode.LIGHT
                    sb.linux.client.data.ThemeModePref.DARK -> ThemeMode.DARK
                },
                dynamicColor = session.dynamicColor,
                themeColorKey = session.themeColorKey,
                pureDark = session.oledDark,
                style = sb.linux.client.ui.PaletteStyles.getOrElse(session.themeStyle) {
                    com.materialkolor.PaletteStyle.TonalSpot
                },
                contrastLevel = session.themeContrast.toDouble(),
                primary = session.themePrimary,
                secondary = session.themeSecondary,
                tertiary = session.themeTertiary
            ) {
                LsbApp(session)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LsbApp(session: Session) {
    val nav = rememberNavController()
    // 平板双栏控制器：主栏（底部导航的顶层页面）与详情栏（帖子/用户/设置子页等）
    val masterNav = rememberNavController()
    val detailNav = rememberNavController()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current

    // 平板模式（3.21）：开关开启且屏幕宽度 ≥ 600dp 时启用左右双栏布局
    val twoPane = session.tabletMode && LocalConfiguration.current.screenWidthDp >= 600

    val verification = session.pendingVerification
    if (verification != null) {
        VerificationDialog(
            url = verification.url,
            initialHtml = verification.html,
            onSucceeded = {
                session.client.importWebCookies(verification.url)
                session.completeVerification(true)
            },
            onCancel = { session.completeVerification(false) },
        )
    }

    LaunchedEffect(session.toast) {
        session.toast?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            session.toast = null
        }
    }

    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route ?: "home"
    val bottomRoutes = setOf("home", "forums", "me")

    // 常规设置「打开链接方式」（3.20）：站内链接（帖子/用户/板块）始终路由原生页面，
    // 其余链接按设置选择应用内 WebView 或外部浏览器；不影响跳转楼层与过验证逻辑。
    // 双栏布局下链接内容（帖子/用户/板块/网页）一律进右栏
    val linkNav = if (twoPane) detailNav else nav
    fun openLink(raw: String) {
        val url = Endpoints.abs(raw)
        if (session.settings.linkOpenMode == 0) {
            val uri = android.net.Uri.parse(url)
            val path = uri.path ?: ""
            if ((uri.host ?: "").endsWith("linux.sb")) {
                Regex("""/topic/(\d+)""").find(path)?.let { m -> linkNav.navigate("topic/${m.groupValues[1]}"); return }
                Regex("""/user/(\d+)""").find(path)?.let { m -> linkNav.navigate("user/${m.groupValues[1]}"); return }
                Regex("""/forum/(\d+)""").find(path)?.let { m -> linkNav.navigate("forum/${m.groupValues[1]}"); return }
                uri.getQueryParameter("uid")?.toIntOrNull()?.let { linkNav.navigate("user/$it"); return }
            }
            linkNav.navigate("web?url=${android.net.Uri.encode(url)}")
        } else {
            runCatching {
                context.startActivity(
                    android.content.Intent(
                        android.content.Intent.ACTION_VIEW,
                        android.net.Uri.parse(url)
                    ).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
        }
    }

    // 启动时按设置检查更新（3.20）：每次打开 或 按间隔；2 = 从不
    var updateTip by remember { mutableStateOf<UpdateChecker.UpdateInfo?>(null) }
    LaunchedEffect(Unit) {
        if (session.settings.updateCheckMode == 2) return@LaunchedEffect
        val due = session.settings.updateCheckMode == 0 ||
            System.currentTimeMillis() - session.settings.lastUpdateCheck >
            session.settings.updateCheckIntervalHours * 3600_000L
        if (!due) return@LaunchedEffect
        session.settings.lastUpdateCheck = System.currentTimeMillis()
        val rel = UpdateChecker.latestRelease() ?: return@LaunchedEffect
        val cur = runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: ""
        if (UpdateChecker.isNewer(rel.tag, cur)) updateTip = rel
    }
    updateTip?.let { rel ->
        // 仅当检查时机为「每次启动」时提供「不再提示」：点击即把时机改为「从不」；
        // 其他时机（按间隔）弹窗频率本就不高，不提供该按钮
        val askNever = session.settings.updateCheckMode == 0
        AlertDialog(
            onDismissRequest = { updateTip = null },
            title = { Text("发现新版本") },
            text = {
                Text(
                    "${rel.name} 已发布，是否前往下载？" +
                        if (!askNever) "\n\n（自动检查当前为「按间隔」。可前往 设置 → 常规设置 → 检查更新时机 调整）" else ""
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    updateTip = null
                    runCatching {
                        context.startActivity(
                            android.content.Intent(
                                android.content.Intent.ACTION_VIEW,
                                android.net.Uri.parse(rel.url)
                            ).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }
                }) { Text("前往下载") }
            },
            dismissButton = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (askNever) {
                        TextButton(onClick = {
                            session.settings.updateCheckMode = 2
                            updateTip = null
                            session.showToast("已设置为不再提示更新")
                        }) { Text("不再提示") }
                    }
                    TextButton(onClick = { updateTip = null }) { Text("取消") }
                }
            }
        )
    }

    CompositionLocalProvider(
        LocalLinkHandler provides { openLink(it) },
        // 平板双栏下提供主栏控制器：屏幕内切换顶层页面（首页/我的/应用设置）时
        // 跳转主栏而非右栏；手机模式为 null，屏幕走原有单栈逻辑
        LocalMasterNav provides (if (twoPane) masterNav else null),
    ) {
        if (!twoPane) {
            Scaffold(
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                snackbarHost = { SnackbarHost(snackbar) },
                bottomBar = {
                    if (route in bottomRoutes) {
                        NavigationBar {
                            NavigationBarItem(
                                selected = route == "home",
                                onClick = { nav.navigate("home") { popUpTo(nav.graph.findStartDestination().id) { saveState = true }; launchSingleTop = true; restoreState = true } },
                                icon = { Icon(Icons.Filled.Home, null) },
                                label = { Text("首页") }
                            )
                            NavigationBarItem(
                                selected = route == "me",
                                onClick = { nav.navigate("me") { popUpTo(nav.graph.findStartDestination().id) { saveState = true }; launchSingleTop = true; restoreState = true } },
                                icon = { Icon(Icons.Filled.Person, null) },
                                label = { Text("我的") }
                            )
                        }
                    }
                }
            ) { pad ->
                NavHost(
                    navController = nav,
                    startDestination = "home",
                    modifier = Modifier.padding(pad)
                ) {
                    masterRoutes(session, nav)
                    detailRoutes(session, nav)
                }
            }
        } else {
            // 平板双栏（3.21）：底部导航移到左侧 NavigationRail，
            // 主栏（顶层页面/应用设置菜单）占左 1/3，详情（帖子/用户/设置子页）占右 2/3
            Scaffold(
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                snackbarHost = { SnackbarHost(snackbar) }
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
                    NavigationRail(containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
                        NavigationRailItem(
                            selected = masterRoute == "home",
                            onClick = { switchTab("home") },
                            icon = { Icon(Icons.Filled.Home, null) },
                            label = { Text("首页") }
                        )
                        NavigationRailItem(
                            selected = masterRoute == "me",
                            onClick = { switchTab("me") },
                            icon = { Icon(Icons.Filled.Person, null) },
                            label = { Text("我的") }
                        )
                    }
                    // 主栏：顶层页面与应用设置菜单；屏幕内容导航(detailNav)进右栏
                    NavHost(
                        navController = masterNav,
                        startDestination = "home",
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    ) {
                        masterRoutes(session, detailNav)
                    }
                    VerticalDivider(Modifier.fillMaxHeight())
                    // 详情栏：帖子/用户/搜索/设置子页等
                    NavHost(
                        navController = detailNav,
                        startDestination = "detailEmpty",
                        modifier = Modifier.weight(2f).fillMaxHeight()
                    ) {
                        detailRoutes(session, detailNav)
                    }
                }
            }
        }
    }
}

// ---------------- 双栏路由图与辅助 ----------------

/**
 * 平板双栏下提供主栏 NavHostController；手机模式为 null。
 * 屏幕内切换顶层页面（首页/我的/应用设置）时优先用它导航主栏。
 */
val LocalMasterNav = staticCompositionLocalOf<NavHostController?> { null }

/** 主栏路由：底部导航顶层页面 + 应用设置菜单（平板双栏时占左栏） */
private fun androidx.navigation.NavGraphBuilder.masterRoutes(session: Session, nav: NavHostController) {
    composable("home") { HomeScreen(session, nav) }
    composable("forums") { ForumListScreen(session, nav) }
    composable("me") { MeScreen(session, nav) }
    composable("appSettings") { AppSettingsScreen(session, nav) }
}

/** 详情路由：帖子/用户/搜索/设置子页等（平板双栏时占右栏；detailEmpty 为右栏空态占位） */
private fun androidx.navigation.NavGraphBuilder.detailRoutes(session: Session, nav: NavHostController) {
    composable("detailEmpty") { DetailEmptyPane() }
    composable(
        "search?q={q}",
        arguments = listOf(navArgument("q") { type = androidx.navigation.NavType.StringType; defaultValue = "" })
    ) { SearchScreen(session, nav) }
    composable("login") { LoginScreen(session, nav) }
    composable(
        "forum/{id}?p={p}",
        arguments = listOf(
            navArgument("id") { type = androidx.navigation.NavType.LongType },
            navArgument("p") { type = androidx.navigation.NavType.IntType; defaultValue = 1 }
        )
    ) { ForumScreen(session, nav) }
    composable(
        "topic/{tid}?p={p}",
        arguments = listOf(
            navArgument("tid") { type = androidx.navigation.NavType.LongType },
            navArgument("p") { type = androidx.navigation.NavType.IntType; defaultValue = 1 }
        )
    ) { TopicScreen(session, nav) }
    composable("newTopic") { NewTopicScreen(session, nav) }
    composable(
        "editTopic/{tid}",
        arguments = listOf(navArgument("tid") { type = androidx.navigation.NavType.LongType })
    ) { NewTopicScreen(session, nav, editId = it.arguments?.getLong("tid") ?: 0L) }
    composable(
        "user/{uid}?tab={tab}",
        arguments = listOf(
            navArgument("uid") { type = androidx.navigation.NavType.LongType },
            navArgument("tab") { type = androidx.navigation.NavType.StringType; defaultValue = "topics" }
        )
    ) { UserScreen(session, nav) }
    composable("settings") { SettingsScreen(session, nav) }
    composable("generalSettings") { GeneralSettingsScreen(session, nav) }
    composable("browseSettings") { BrowseSettingsScreen(session, nav) }
    composable("aiSettings") { AiSettingsScreen(session, nav) }
    composable("transferSettings") { TransferSettingsScreen(session, nav) }
    composable("themeSettings") { ThemeSettingsScreen(session, nav) }
    composable("exportedTopics") { ExportedTopicsScreen(session, nav) }
    composable(
        "exportedHtml?path={path}",
        arguments = listOf(
            navArgument("path") { type = androidx.navigation.NavType.StringType; defaultValue = "" }
        )
    ) { ExportedHtmlScreen(session, nav) }
    composable("about") { AboutScreen(session, nav) }
    composable(
        "web?url={url}",
        arguments = listOf(
            navArgument("url") { type = androidx.navigation.NavType.StringType; defaultValue = "" }
        )
    ) { WebScreen(session, nav) }
    composable("blockWords") { BlockWordsScreen(session, nav) }
    composable("checkin") { CheckinScreen(session, nav) }
    composable("inviteCenter") { InviteCenterScreen(session, nav) }
    composable(
        "leaderboard?type={type}",
        arguments = listOf(navArgument("type") { type = androidx.navigation.NavType.StringType; defaultValue = "points" })
    ) { LeaderboardScreen(session, nav) }
    composable("notifications") { NotificationsScreen(session, nav) }
    composable("footprint") { FootprintScreen(session, nav) }
    composable("favorites") { FavoritesScreen(session, nav) }
    composable(
        "chat/{userId}?name={name}&avatar={avatar}",
        arguments = listOf(
            navArgument("userId") { type = androidx.navigation.NavType.StringType },
            navArgument("name") { type = androidx.navigation.NavType.StringType; defaultValue = "" },
            navArgument("avatar") { type = androidx.navigation.NavType.StringType; defaultValue = "" },
        )
    ) { ChatScreen(session, nav) }
    composable(
        "donate/{tid}",
        arguments = listOf(navArgument("tid") { type = androidx.navigation.NavType.LongType })
    ) { DonateScreen(session, nav) }
    composable(
        "report/{type}/{id}",
        arguments = listOf(
            navArgument("type") { type = androidx.navigation.NavType.StringType },
            navArgument("id") { type = androidx.navigation.NavType.LongType }
        )
    ) { ReportScreen(session, nav) }
}

/** 平板双栏右栏空态：尚未选择帖子/详情时的占位 */
@Composable
private fun DetailEmptyPane() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "从左侧选择内容查看",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "帖子、用户主页与设置子页将在此展示",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}
