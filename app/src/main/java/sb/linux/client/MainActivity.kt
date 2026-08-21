package sb.linux.client

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
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
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current

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
    // 其余链接按设置选择应用内 WebView 或外部浏览器；不影响跳转楼层与过验证逻辑
    fun openLink(raw: String) {
        val url = Endpoints.abs(raw)
        if (session.settings.linkOpenMode == 0) {
            val uri = android.net.Uri.parse(url)
            val path = uri.path ?: ""
            if ((uri.host ?: "").endsWith("linux.sb")) {
                Regex("""/topic/(\d+)""").find(path)?.let { m -> nav.navigate("topic/${m.groupValues[1]}"); return }
                Regex("""/user/(\d+)""").find(path)?.let { m -> nav.navigate("user/${m.groupValues[1]}"); return }
                Regex("""/forum/(\d+)""").find(path)?.let { m -> nav.navigate("forum/${m.groupValues[1]}"); return }
                uri.getQueryParameter("uid")?.toIntOrNull()?.let { nav.navigate("user/$it"); return }
            }
            nav.navigate("web?url=${android.net.Uri.encode(url)}")
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
        AlertDialog(
            onDismissRequest = { updateTip = null },
            title = { Text("发现新版本") },
            text = {
                Text(
                    "${rel.name} 已发布，是否前往下载？\n\n" +
                        "（自动检查当前为「每次启动」。可前往 设置 → 常规设置 → 检查更新时机 关闭或改为按间隔）"
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
            dismissButton = { TextButton(onClick = { updateTip = null }) { Text("取消") } }
        )
    }

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
        CompositionLocalProvider(LocalLinkHandler provides { openLink(it) }) {
        NavHost(
            navController = nav,
            startDestination = "home",
            modifier = Modifier.padding(pad)
        ) {
            composable("home") { HomeScreen(session, nav) }
            composable("forums") { ForumListScreen(session, nav) }
            composable(
                "search?q={q}",
                arguments = listOf(navArgument("q") { type = androidx.navigation.NavType.StringType; defaultValue = "" })
            ) { SearchScreen(session, nav) }
            composable("me") { MeScreen(session, nav) }
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
            composable("appSettings") { AppSettingsScreen(session, nav) }
            composable("generalSettings") { GeneralSettingsScreen(session, nav) }
            composable("browseSettings") { BrowseSettingsScreen(session, nav) }
            composable("aiSettings") { AiSettingsScreen(session, nav) }
            composable("transferSettings") { TransferSettingsScreen(session, nav) }
            composable("themeSettings") { ThemeSettingsScreen(session, nav) }
            composable("exportedTopics") { ExportedTopicsScreen(session, nav) }
            composable("about") { AboutScreen(session, nav) }
            composable(
                "web?url={url}",
                arguments = listOf(
                    navArgument("url") { type = androidx.navigation.NavType.StringType; defaultValue = "" }
                )
            ) { WebScreen(session, nav) }
            composable("blockWords") { BlockWordsScreen(session, nav) }
            composable("checkin") { CheckinScreen(session, nav) }
            composable(
                "leaderboard?type={type}",
                arguments = listOf(navArgument("type") { type = androidx.navigation.NavType.StringType; defaultValue = "points" })
            ) { LeaderboardScreen(session, nav) }
            composable("notifications") { NotificationsScreen(session, nav) }
            composable("footprint") { FootprintScreen(session, nav) }
            composable("bail") { BailScreen(session, nav) }
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
        }
    }
}
