package sb.linux.client
import androidx.compose.material.icons.filled.Email

import android.os.Bundle
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberDrawerState
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import kotlinx.coroutines.launch
import sb.linux.client.data.Endpoints
import sb.linux.client.data.LinkPreviewClient
import sb.linux.client.data.LinkPreviewInfo
import sb.linux.client.data.Session
import sb.linux.client.data.UpdateChecker
import sb.linux.client.ui.LsbTheme
import sb.linux.client.ui.LocalLinkHandler
import sb.linux.client.ui.LocalTableDisplayMode
import sb.linux.client.ui.LocalShowUid
import sb.linux.client.ui.LocalSmartDecode
import sb.linux.client.ui.ThemeMode
import sb.linux.client.ui.VerificationDialog
import sb.linux.client.ui.screens.*

/**
 * 页面转场统一时长（6）：短时淡入淡出取代瞬切（snap）与 700ms 长转场——
 * 保留动画感但不拖沓，同时大幅缩短新旧页重叠期（转场期间退出页浮在上层
 * 仍可交互，长转场会接住本该落在下层页面的点击），快速连点不易误触
 */
private const val NAV_FADE_MS = 150

class MainActivity : ComponentActivity() {
    private var activeSession: Session? = null

    override fun onResume() {
        super.onResume()
        activeSession?.recoverSession()
    }

    override fun onStop() {
        super.onStop()
        // 正常退出立即清理；系统直接杀进程无回调时由下次启动补做。
        if (isFinishing && !isChangingConfigurations) activeSession?.let {
            it.clearDataItems(it.settings.autoClearItems, notify = false)
        }
    }

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
            SideEffect { activeSession = session }
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
            // 应用字体（24）：默认跟随系统；自定义字体从私有目录文件加载
            // 以 fontKey + customFontName 为键：重导入不同字体文件后立即刷新
            val fontFamily = remember(session.fontKey, session.customFontName) {
                if (session.fontKey == "custom") {
                    session.customFontFile.takeIf { it.exists() }?.let {
                        androidx.compose.ui.text.font.FontFamily(
                            androidx.compose.ui.text.font.Font(it)
                        )
                    }
                } else null
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
                tertiary = session.themeTertiary,
                customBackground = session.themeBackground,
                keepBackgroundColor = session.keepBackgroundColor,
                backgroundImage = if (session.backgroundImageEnabled) session.backgroundImage else "",
                backgroundImageOpacity = session.backgroundImageOpacity,
                surfaceOpacity = session.surfaceOpacity,
                fontFamily = fontFamily,
                fontScale = session.fontScale,
                fontWeightLevel = session.fontWeightLevel,
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
    val scope = rememberCoroutineScope()
    // 首页抽屉状态提升到应用级：平板双栏下左侧导航栏顶部的侧边栏按钮也要能打开它
    val homeDrawerState = rememberDrawerState(DrawerValue.Closed)

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
    // 路由归一化：带可选参数的路由（topicCollections?tab=…）取参数前的名字；
    // 底栏进入的淘帖/私信用独立的 *Root 路由（不显示返回箭头），这里映射回底栏项名。
    val rawRoute = (backStack?.destination?.route ?: "home").substringBefore("?")
    val route = when (rawRoute) {
        "topicCollectionsRoot" -> "topicCollections"
        "directMessagesRoot" -> "directMessages"
        else -> rawRoute
    }
    val bottomRoutes = setOf("home", "forums", "topicCollections", "directMessages", "me")
    val bottomItems = session.bottomBarItems

    fun navigateBottom(target: String) {
        if (target == "newTopic") {
            nav.navigate("newTopic") { launchSingleTop = true }
            return
        }
        if (target == route) {
            if (target == "home") session.requestHomeRefresh()
            return
        }
        // 顶层页面互相切换：一律弹回起始页再进目标页，不保存/恢复各自的子栈——
        // 保存态在淘帖/私信这类详情路由上会让下一次切换被旧栈吞掉（点「我的」无反应）。
        val destination = when (target) {
            "topicCollections" -> "topicCollectionsRoot"
            "directMessages" -> "directMessagesRoot"
            else -> target
        }
        nav.navigate(destination) {
            popUpTo(nav.graph.findStartDestination().id)
            launchSingleTop = true
        }
    }

    // 常规设置「打开链接方式」（3.20）：站内链接（帖子/用户/板块）始终路由原生页面，
    // 其余链接按设置选择应用内 WebView 或外部浏览器；不影响跳转楼层与过验证逻辑。
    // 双栏布局下链接内容（帖子/用户/板块/网页）一律进右栏
    val linkNav = if (twoPane) detailNav else nav
    var linkPreviewTarget by remember { mutableStateOf<String?>(null) }
    var linkPreviewInfo by remember { mutableStateOf<LinkPreviewInfo?>(null) }
    var linkPreviewLoading by remember { mutableStateOf(false) }

    fun openLinkDirect(raw: String) {
        val url = Endpoints.abs(raw)
        val internalUri = android.net.Uri.parse(url)
        if (internalUri.host in setOf("linux.sb", "www.linux.sb")) {
            val internalPath = internalUri.path.orEmpty().let { if (it.length > 1) it.trimEnd('/') else it }
            when (internalPath) {
                "/", "" -> { linkNav.navigate("home"); return }
                "/forum_list" -> { linkNav.navigate("forums"); return }
                "/topic_collections" -> {
                    val tab = if (internalUri.getQueryParameter("tab") == "everyone") "everyone" else "mine"
                    linkNav.navigate("topicCollections?tab=$tab"); return
                }
                "/direct_messages" -> { linkNav.navigate("directMessages"); return }
                "/notifications" -> { linkNav.navigate("notifications"); return }
                "/invite_center" -> { linkNav.navigate("inviteCenter"); return }
                "/gacha" -> { linkNav.navigate("gachaCenter"); return }
                "/gacha_profile" -> { linkNav.navigate("gachaProfile"); return }
                "/gacha_market" -> { linkNav.navigate("gachaMarket"); return }
                "/gacha_forge_center" -> { linkNav.navigate("gachaOperation/forge"); return }
                "/gacha_recycle_center" -> { linkNav.navigate("gachaOperation/recycle"); return }
                "/gacha_recipes" -> { linkNav.navigate("gachaOperation/recipes"); return }
                "/gacha_market_mine" -> { linkNav.navigate("gachaOperation/marketMine"); return }
                "/leaderboard" -> {
                    val type = internalUri.getQueryParameter("type") ?: "points"
                    linkNav.navigate("leaderboard?type=${android.net.Uri.encode(type)}"); return
                }
                "/search" -> {
                    val query = internalUri.getQueryParameter("q") ?: internalUri.getQueryParameter("keyword") ?: ""
                    val field = internalUri.getQueryParameter("field") ?: "title"
                    linkNav.navigate("search?q=${android.net.Uri.encode(query)}&field=${android.net.Uri.encode(field)}"); return
                }
                "/identity_center", "/daily_checkin" -> {
                    session.showToast("此功能暂不提供"); return
                }
            }
            Regex("^/topic_collection/\\d+$").takeIf { it.matches(internalPath) }?.let {
                val path = internalPath + internalUri.query?.let { "?$it" }.orEmpty()
                linkNav.navigate("collectionActions?path=${android.net.Uri.encode(path)}"); return
            }
            Regex("^/direct_messages/(\\d+)$").find(internalPath)?.let {
                linkNav.navigate("chat/${it.groupValues[1]}"); return
            }
            Regex("^/topic/(\\d+)$").find(internalPath)?.let {
                val page = internalUri.getQueryParameter("p")?.toIntOrNull() ?: 1
                val floor = internalUri.getQueryParameter("floor")?.toIntOrNull() ?: 0
                linkNav.navigate("topic/${it.groupValues[1]}?p=$page&floor=$floor"); return
            }
            Regex("^/forum/(\\d+)$").find(internalPath)?.let {
                val page = internalUri.getQueryParameter("p")?.toIntOrNull() ?: 1
                linkNav.navigate("forum/${it.groupValues[1]}?p=$page"); return
            }
            Regex("^/user/(\\d+)$").find(internalPath)?.let {
                val tab = internalUri.getQueryParameter("tab") ?: "topics"
                if (tab == "notifications") linkNav.navigate("notifications")
                else linkNav.navigate("user/${it.groupValues[1]}?tab=${android.net.Uri.encode(tab)}")
                return
            }
        }
        if (session.settings.linkOpenMode == 0) {
            val uri = android.net.Uri.parse(url)
            val path = uri.path ?: ""
            if (uri.host in setOf("linux.sb", "www.linux.sb")) {
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

    fun openLink(raw: String) {
        val url = Endpoints.abs(raw)
        val host = runCatching { android.net.Uri.parse(url).host.orEmpty() }.getOrDefault("")
        // 站内帖子/用户/版块继续直接走原生路由；预览只拦截外部网站。
        if (!session.linkPreviewEnabled || host in setOf("linux.sb", "www.linux.sb")) {
            openLinkDirect(url)
        } else {
            linkPreviewInfo = null
            linkPreviewTarget = url
        }
    }

    LaunchedEffect(linkPreviewTarget) {
        val url = linkPreviewTarget ?: return@LaunchedEffect
        linkPreviewLoading = true
        linkPreviewInfo = runCatching { LinkPreviewClient.load(url) }
            .getOrElse { LinkPreviewInfo(url, android.net.Uri.parse(url).host ?: url, "", "") }
        linkPreviewLoading = false
    }

    if (linkPreviewTarget != null) {
        AlertDialog(
            onDismissRequest = { linkPreviewTarget = null; linkPreviewInfo = null },
            title = { Text("链接预览") },
            text = {
                if (linkPreviewLoading) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(10.dp))
                        Text("正在读取网站信息…")
                    }
                } else {
                    val info = linkPreviewInfo
                    Row(verticalAlignment = Alignment.Top) {
                        coil.compose.AsyncImage(
                            model = info?.iconUrl,
                            contentDescription = null,
                            modifier = Modifier.size(42.dp).clip(RoundedCornerShape(10.dp)),
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(info?.title.orEmpty().ifBlank { "未知网站" }, style = MaterialTheme.typography.titleMedium)
                            if (!info?.description.isNullOrBlank()) {
                                Spacer(Modifier.height(4.dp))
                                Text(info!!.description, style = MaterialTheme.typography.bodySmall, maxLines = 4)
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(info?.url ?: linkPreviewTarget.orEmpty(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val target = linkPreviewInfo?.url ?: linkPreviewTarget ?: return@TextButton
                        linkPreviewTarget = null; linkPreviewInfo = null
                        openLinkDirect(target)
                    },
                ) { Text("打开链接") }
            },
            dismissButton = {
                TextButton(onClick = { linkPreviewTarget = null; linkPreviewInfo = null }) { Text("取消") }
            },
        )
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
        LocalTableDisplayMode provides session.tableDisplayMode,
        LocalShowUid provides session.showUid,
        LocalSmartDecode provides session.smartDecodeEnabled,
        // 平板双栏下提供主栏控制器：屏幕内切换顶层页面（首页/我的/应用设置）时
        // 跳转主栏而非右栏；手机模式为 null，屏幕走原有单栈逻辑
        LocalMasterNav provides (if (twoPane) masterNav else null),
    ) {
        if (!twoPane) {
            if (session.bottomBarStyle != 0) {
                // 液态玻璃底栏（23）：NavHost 内容绘制进 backdrop 图层，玻璃条
                // 从身后内容实时取样折射（vibrancy + blur + lens）；内容可滚到玻璃条后方
                // 居中悬浮胶囊样式（原「贴底通栏」铺满样式已移除）
                Scaffold(
                    contentWindowInsets = WindowInsets(0, 0, 0, 0),
                    snackbarHost = { SnackbarHost(snackbar) },
                ) { scaffoldPadding ->
                    val bgColor = MaterialTheme.colorScheme.background
                    val backdrop = rememberLayerBackdrop {
                        drawRect(bgColor)
                        drawContent()
                    }
                    Box(Modifier.fillMaxSize().padding(scaffoldPadding)) {
                        NavHost(
                            navController = nav,
                            startDestination = "home",
                            modifier = Modifier
                                .fillMaxSize()
                                .layerBackdrop(backdrop),
                            // 统一短淡入淡出（6）：替代默认 700ms 转场，重叠期短不易误触
                            enterTransition = { fadeIn(tween(NAV_FADE_MS)) },
                            exitTransition = { fadeOut(tween(NAV_FADE_MS)) },
                            popEnterTransition = { fadeIn(tween(NAV_FADE_MS)) },
                            popExitTransition = { fadeOut(tween(NAV_FADE_MS)) },
                        ) {
                            masterRoutes(session, nav, homeDrawerState)
                            detailRoutes(session, nav)
                        }
                        // 液态玻璃悬浮底栏；抽屉打开或滑动中隐藏避免盖住抽屉
                        AnimatedVisibility(
                            visible = route in bottomRoutes &&
                                homeDrawerState.targetValue == DrawerValue.Closed &&
                                !homeDrawerState.isAnimationRunning,
                            enter = fadeIn(),
                            exit = fadeOut(),
                            modifier = Modifier.align(Alignment.BottomCenter),
                        ) {
                            LiquidGlassBottomBar(
                                backdrop = backdrop,
                                route = route,
                                items = bottomItems,
                                onNavigate = ::navigateBottom,
                            )
                        }
                    }
                }
            } else {
                // 经典底栏：通栏 NavigationBar，Scaffold 自动处理内容 inset
                Scaffold(
                    contentWindowInsets = WindowInsets(0, 0, 0, 0),
                    snackbarHost = { SnackbarHost(snackbar) },
                    bottomBar = {
                        if (route in bottomRoutes) {
                            NavigationBar {
                                bottomItems.forEach { item ->
                                    val label = when (item) { "home" -> "首页"; "topicCollections" -> "淘帖"; "directMessages" -> "私信"; "newTopic" -> "发帖"; else -> "我的" }
                                    val icon = when (item) { "home" -> Icons.Filled.Home; "topicCollections" -> Icons.Filled.ViewList; "directMessages" -> Icons.Filled.Email; "newTopic" -> Icons.Filled.Add; else -> Icons.Filled.Person }
                                    NavigationBarItem(
                                        selected = route == item,
                                        onClick = { navigateBottom(item) },
                                        icon = { Icon(icon, null) },
                                        label = { Text(label) },
                                    )
                                }
                            }
                        }
                    }
                ) { pad ->
                    NavHost(
                        navController = nav,
                        startDestination = "home",
                        modifier = Modifier.padding(pad),
                        // 统一短淡入淡出（6）：替代默认 700ms 转场，重叠期短不易误触
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
            // 平板双栏（3.21）：底部导航移到左侧导航栏（顶部为侧边栏按钮，首页/我的平分
            // 剩余高度），主栏（顶层页面/应用设置菜单）占左 1/3，详情（帖子/用户/设置子页）占右 2/3
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
                    // 切换顶层页面（首页/我的/应用设置）时清空右栏详情，回到空态占位
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
                    // 左侧导航栏：侧边栏按钮固定在最上方，首页/我的各占剩余高度一半
                    TabletNavRail(
                        selected = masterRoute,
                        onSelect = { switchTab(it) },
                        onOpenSidebar = {
                            // 不在首页时先切回首页再打开抽屉（抽屉挂在首页上）
                            if (masterRoute != "home") switchTab("home")
                            scope.launch { homeDrawerState.open() }
                        },
                    )
                    // 主栏：顶层页面与应用设置菜单；屏幕内容导航(detailNav)进右栏。
                    // 固定比例 weight(1f) 在 600dp 级平板上只剩 ~173dp，首页抽屉
                    // （material3 最小宽 240dp）被压到贴边且遮罩无处可点 →
                    // 改为按屏宽 32% 动态计算并夹在 300~420dp：抽屉始终能完整展开，
                    // 右栏吃剩余空间
                    val screenWidth = LocalConfiguration.current.screenWidthDp
                    val masterWidth = (screenWidth * 0.32f).coerceIn(300f, 420f).dp
                    NavHost(
                        navController = masterNav,
                        startDestination = "home",
                        modifier = Modifier.fillMaxHeight().width(masterWidth),
                        // 统一短淡入淡出（6）：替代默认 700ms 转场，重叠期短不易误触
                        enterTransition = { fadeIn(tween(NAV_FADE_MS)) },
                        exitTransition = { fadeOut(tween(NAV_FADE_MS)) },
                        popEnterTransition = { fadeIn(tween(NAV_FADE_MS)) },
                        popExitTransition = { fadeOut(tween(NAV_FADE_MS)) },
                    ) {
                        masterRoutes(session, detailNav, homeDrawerState)
                    }
                    VerticalDivider(Modifier.fillMaxHeight())
                    // 详情栏：帖子/用户/搜索/设置子页等
                    NavHost(
                        navController = detailNav,
                        startDestination = "detailEmpty",
                        modifier = Modifier.weight(2f).fillMaxHeight(),
                        // 统一短淡入淡出（6）：替代默认 700ms 转场，重叠期短不易误触
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
}

/** 平板左侧导航栏（3.21）：顶部侧边栏按钮（打开首页抽屉），下方首页/我的平分剩余高度 */
@Composable
private fun TabletNavRail(
    selected: String,
    onSelect: (String) -> Unit,
    onOpenSidebar: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RectangleShape,
        modifier = Modifier.fillMaxHeight().width(80.dp)
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 侧边栏按钮固定在导航栏最上方
            IconButton(
                onClick = onOpenSidebar,
                modifier = Modifier.padding(top = 10.dp).size(44.dp)
            ) {
                Icon(Icons.Filled.Menu, "侧边栏", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(6.dp))
            TabletRailItem(
                icon = Icons.Filled.Home,
                label = "首页",
                selected = selected == "home",
                onClick = { onSelect("home") },
                modifier = Modifier.weight(1f),
            )
            TabletRailItem(
                icon = Icons.Filled.Person,
                label = "我的",
                selected = selected == "me",
                onClick = { onSelect("me") },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** 平板导航栏按钮：整块区域可点（大触控区），内容居中；选中时胶囊指示器高亮 */
@Composable
private fun TabletRailItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
            ) {
                Box(
                    Modifier.size(width = 52.dp, height = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon, null,
                        Modifier.size(22.dp),
                        tint = if (selected) MaterialTheme.colorScheme.onSecondaryContainer
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(2.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ---------------- 双栏路由图与辅助 ----------------

/**
 * 平板双栏下提供主栏 NavHostController；手机模式为 null。
 * 屏幕内切换顶层页面（首页/我的/应用设置）时优先用它导航主栏。
 */
val LocalMasterNav = staticCompositionLocalOf<NavHostController?> { null }

/** 主栏路由：底部导航顶层页面 + 应用设置菜单（平板双栏时占左栏）
 *  homeDrawerState 为应用级提升的首页抽屉状态（平板左侧导航栏的侧边栏按钮需要打开它） */
private fun androidx.navigation.NavGraphBuilder.masterRoutes(
    session: Session,
    nav: NavHostController,
    homeDrawerState: DrawerState,
) {
    composable("home") { HomeScreen(session, nav, homeDrawerState) }
    composable("forums") { ForumListScreen(session, nav) }
    composable(
        "me",
        // 统一短淡入淡出（6）：不再瞬切也不 700ms 长转场，重叠期短、连点不误触
        exitTransition = { fadeOut(tween(NAV_FADE_MS)) },
        popEnterTransition = { fadeIn(tween(NAV_FADE_MS)) },
    ) { MeScreen(session, nav) }
    composable(
        "appSettings",
        // 短淡入淡出（6）：保留动画感，重叠期极短，返回后立即可点
        enterTransition = { fadeIn(tween(NAV_FADE_MS)) },
        popExitTransition = { fadeOut(tween(NAV_FADE_MS)) },
    ) { AppSettingsScreen(session, nav) }
}

/** 设置类子页：短淡入淡出（6）——不瞬切（保留动画），重叠期极短，
 *  返回上一级后立即可点，不会误触残留的旧页面 */
private fun androidx.navigation.NavGraphBuilder.settingsComposable(
    route: String,
    content: @Composable () -> Unit,
) {
    composable(
        route,
        enterTransition = { fadeIn(tween(NAV_FADE_MS)) },
        popExitTransition = { fadeOut(tween(NAV_FADE_MS)) },
    ) { content() }
}

/** 详情路由：帖子/用户/搜索/设置子页等（平板双栏时占右栏；detailEmpty 为右栏空态占位） */
private fun androidx.navigation.NavGraphBuilder.detailRoutes(session: Session, nav: NavHostController) {
    composable("detailEmpty") { DetailEmptyPane() }
    composable(
        "search?q={q}&field={field}",
        arguments = listOf(
            navArgument("q") { type = androidx.navigation.NavType.StringType; defaultValue = "" },
            navArgument("field") { type = androidx.navigation.NavType.StringType; defaultValue = "title" }
        )
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
        "topic/{tid}?p={p}&floor={floor}",
        arguments = listOf(
            navArgument("tid") { type = androidx.navigation.NavType.LongType },
            navArgument("p") { type = androidx.navigation.NavType.IntType; defaultValue = 1 },
            // 定位楼层（收藏评论跳转用）：>0 时加载完成后自动跳到该楼
            navArgument("floor") { type = androidx.navigation.NavType.IntType; defaultValue = 0 }
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
    settingsComposable("settings") { SettingsScreen(session, nav) }
    settingsComposable("generalSettings") { GeneralSettingsScreen(session, nav) }
    settingsComposable("networkSettings") { NetworkSettingsScreen(session, nav) }
    settingsComposable("dohSettings") { DohSettingsScreen(session, nav) }
    settingsComposable("browseSettings") { BrowseSettingsScreen(session, nav) }
    settingsComposable("dataManagement") { DataManagementScreen(session, nav) }
    settingsComposable("usageStats") { UsageStatsScreen(session, nav) }
    settingsComposable("aiSettings") { AiSettingsScreen(session, nav) }
    settingsComposable("transferSettings") { TransferSettingsScreen(session, nav) }
    settingsComposable("themeSettings") { ThemeSettingsScreen(session, nav) }
    settingsComposable("exportedTopics") { ExportedTopicsScreen(session, nav) }
    composable(
        "exportedHtml?path={path}",
        arguments = listOf(
            navArgument("path") { type = androidx.navigation.NavType.StringType; defaultValue = "" }
        ),
        enterTransition = { fadeIn(tween(NAV_FADE_MS)) },
        popExitTransition = { fadeOut(tween(NAV_FADE_MS)) },
    ) { ExportedHtmlScreen(session, nav) }
    settingsComposable("about") { AboutScreen(session, nav) }
    composable(
        "web?url={url}",
        arguments = listOf(
            navArgument("url") { type = androidx.navigation.NavType.StringType; defaultValue = "" }
        )
    ) { WebScreen(session, nav) }
    settingsComposable("blockWords") { BlockWordsScreen(session, nav) }
    composable("inviteCenter") { InviteCenterScreen(session, nav) }
    composable("gachaProfile") { GachaProfileScreen(session, nav) }
    composable("gachaCenter") { GachaCenterScreen(session, nav) }
    composable("gachaMarket") { GachaMarketScreen(session, nav) }
    composable(
        "gachaOperation/{kind}?title={title}",
        arguments = listOf(
            navArgument("kind") { type = androidx.navigation.NavType.StringType },
            navArgument("title") { type = androidx.navigation.NavType.StringType; defaultValue = "" },
        ),
    ) { entry -> GachaOperationScreen(session, nav, entry.arguments?.getString("kind").orEmpty()) }
    composable(
        "leaderboard?type={type}",
        arguments = listOf(navArgument("type") { type = androidx.navigation.NavType.StringType; defaultValue = "points" })
    ) { LeaderboardScreen(session, nav) }
    composable("notifications") { NotificationsScreen(session, nav) }
    composable("directMessages") { DirectMessagesScreen(session, nav) }
    composable("topicCollections?tab={tab}", arguments = listOf(
        navArgument("tab") { type = androidx.navigation.NavType.StringType; defaultValue = "everyone" }
    )) { TopicCollectionsScreen(session, nav, initialMine = it.arguments?.getString("tab") == "mine") }
    // 底栏入口用独立路由：与「我的」里的淘帖/私信入口区分开，不显示左上角返回箭头
    composable("directMessagesRoot") { DirectMessagesScreen(session, nav, showBack = false) }
    composable("topicCollectionsRoot") { TopicCollectionsScreen(session, nav, initialMine = false, showBack = false) }
    composable("collectionActions?path={path}") { CollectionDetailScreen(session, nav) }
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

// ---------------- 悬浮液态玻璃底栏（23） ----------------

/** 液态玻璃底栏：玻璃实时折射身后滚动内容（vibrancy 提饱和 + blur + lens 边缘折射），
 *  Backdrop 2.0 高级效果：lens 深度折射 + 边缘色散，自带高光描边与投影；
 *  选中项为半透明主色胶囊；低于 Android 12 时自动退化为普通半透明条。
 *  固定为居中悬浮胶囊，不占满屏宽（区别于经典通栏 NavigationBar；
 *  原「贴底通栏」铺满样式已移除） */
@Composable
private fun LiquidGlassBottomBar(
    backdrop: Backdrop,
    route: String,
    items: List<String>,
    onNavigate: (String) -> Unit,
) {
    // 玻璃表面色调：浅色主题垫白、深色主题垫黑，保证图标可读性
    val light = MaterialTheme.colorScheme.background.luminance() > 0.5f
    val surfaceTint = if (light) Color.White.copy(alpha = 0.35f) else Color.Black.copy(alpha = 0.4f)
    val width = ((LocalConfiguration.current.screenWidthDp - 32).coerceAtMost(items.size * 108)).dp
    Row(
        Modifier
            .navigationBarsPadding()
            .padding(bottom = 14.dp)
            .drawBackdrop(
                backdrop = backdrop,
                shape = { RoundedCornerShape(50) },
                effects = {
                    vibrancy()
                    blur(6.dp.toPx())
                    // Backdrop 2.0 高级效果：depthEffect 深度折射 + chromaticAberration 边缘色散
                    lens(18.dp.toPx(), 18.dp.toPx(), depthEffect = true, chromaticAberration = true)
                },
                onDrawSurface = { drawRect(surfaceTint) }
            )
            .height(58.dp)
            .width(width),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items.forEach { item ->
            GlassBottomTab(
                icon = when (item) { "home" -> Icons.Filled.Home; "topicCollections" -> Icons.Filled.ViewList; "directMessages" -> Icons.Filled.Email; "newTopic" -> Icons.Filled.Add; else -> Icons.Filled.Person },
                label = when (item) { "home" -> "首页"; "topicCollections" -> "淘帖"; "directMessages" -> "私信"; "newTopic" -> "发帖"; else -> "我的" },
                selected = route == item,
                onClick = { onNavigate(item) },
            )
        }
    }
}

/** 玻璃底栏的单个标签：选中时叠加半透明主色胶囊 */
@Composable
private fun RowScope.GlassBottomTab(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val tint = if (selected) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        Modifier
            .fillMaxHeight()
            .weight(1f)
            .padding(5.dp)
            .clip(RoundedCornerShape(50))
            .background(
                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                else Color.Transparent
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Icon(icon, null, Modifier.size(22.dp), tint = tint)
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = tint,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
            )
        }
    }
}
