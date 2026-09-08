package sb.linux.client.ui.navigation

import android.widget.Toast
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.CompositionLocalProvider
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import sb.linux.client.data.Endpoints
import sb.linux.client.data.LinkPreviewClient
import sb.linux.client.data.LinkPreviewInfo
import sb.linux.client.data.Session
import sb.linux.client.data.UpdateChecker
import sb.linux.client.ui.LocalLinkHandler
import sb.linux.client.ui.LocalShowUid
import sb.linux.client.ui.LocalSmartDecode
import sb.linux.client.ui.LocalTableDisplayMode
import sb.linux.client.ui.VerificationDialog

/** 应用级 Compose 根节点，负责全局状态桥接与链接/更新副作用。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppRoot(session: Session) {
    val nav = rememberNavController()
    val masterNav = rememberNavController()
    val detailNav = rememberNavController()
    val snackbar = remember { androidx.compose.material3.SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val homeDrawerState = androidx.compose.material3.rememberDrawerState(androidx.compose.material3.DrawerValue.Closed)
    val twoPane = session.tabletMode && LocalConfiguration.current.screenWidthDp >= 600

    val verification = session.pendingVerification
    if (verification != null) {
        VerificationDialog(
            url = verification.url,
            initialHtml = verification.html,
            prepareCookies = { session.client.exportWebCookies() },
            onSucceeded = {
                session.client.importWebCookies(verification.url)
                session.completeVerification(verification, true)
            },
            onCancel = { session.completeVerification(verification, false) },
        )
    }

    LaunchedEffect(session.toast) {
        session.toast?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            session.toast = null
        }
    }

    val backStack by nav.currentBackStackEntryAsState()
    val rawRoute = (backStack?.destination?.route ?: "home").substringBefore("?")
    val route = BottomDestination.canonical(rawRoute)
    val bottomRoutes = setOf(
        BottomDestination.HOME,
        BottomDestination.FORUMS,
        BottomDestination.TOPIC_COLLECTIONS,
        BottomDestination.DIRECT_MESSAGES,
        BottomDestination.ME,
    )
    val bottomItems = session.bottomBarItems
    val bottomNavigation = remember(nav, scope, homeDrawerState) {
        BottomNavigationCoordinator(
            nav = nav,
            scope = scope,
            closeDrawer = {
                if (!homeDrawerState.isClosed) homeDrawerState.close()
            },
            onHomeReselect = session::requestHomeRefresh,
        )
    }

    val linkNav = if (twoPane) detailNav else nav
    var linkPreviewTarget by remember { mutableStateOf<String?>(null) }
    var linkPreviewInfo by remember { mutableStateOf<LinkPreviewInfo?>(null) }
    var linkPreviewLoading by remember { mutableStateOf(false) }

    fun openLinkDirect(raw: String) {
        val url = Endpoints.abs(raw)
        val internalUri = android.net.Uri.parse(url)
        if (Endpoints.isInternal(internalUri.host)) {
            val internalPath = internalUri.path.orEmpty().let { if (it.length > 1) it.trimEnd('/') else it }
            when (internalPath) {
                "/", "" -> { linkNav.navigate("home"); return }
                "/forum_list" -> { linkNav.navigate("forums"); return }
                "/topic_collections" -> {
                    val tab = if (internalUri.getQueryParameter("tab") == "everyone") "everyone" else "mine"
                    linkNav.navigate("topicCollections?tab=$tab"); return
                }
                // 源站的 @提及 有两种写法：/user/{uid} 与 /user?username={名字}。
                // 后者要先查 uid 才能落到 user/{uid} 路由，交给中转屏处理。
                "/user" -> {
                    val username = internalUri.getQueryParameter("username").orEmpty()
                    val tab = internalUri.getQueryParameter("tab") ?: "topics"
                    if (username.isNotBlank()) {
                        linkNav.navigate(
                            "userByName?name=${android.net.Uri.encode(username)}&tab=${android.net.Uri.encode(tab)}",
                        )
                        return
                    }
                    internalUri.getQueryParameter("uid")?.toLongOrNull()?.takeIf { it > 0 }?.let {
                        linkNav.navigate("user/$it?tab=${android.net.Uri.encode(tab)}"); return
                    }
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
                    val scope = internalUri.getQueryParameter("scope")
                        ?: internalUri.getQueryParameter("field") ?: "all"
                    val sort = internalUri.getQueryParameter("sort") ?: "relevance"
                    linkNav.navigate(
                        "search?q=${android.net.Uri.encode(query)}&scope=${android.net.Uri.encode(scope)}&sort=${android.net.Uri.encode(sort)}",
                    ); return
                }
                "/identity_center", "/daily_checkin" -> {
                    session.showToast("此功能暂不提供"); return
                }
            }
            Regex("^/topic_collection/\\d+$").takeIf { it.matches(internalPath) }?.let {
                val path = internalPath + internalUri.query?.let { "?$it" }.orEmpty()
                linkNav.navigate("collectionActions?path=${android.net.Uri.encode(path)}"); return
            }
            Regex("^/topic_collection_manage(?:/\\d+)?$").takeIf { it.matches(internalPath) }?.let {
                val path = internalPath + internalUri.query?.let { "?$it" }.orEmpty()
                linkNav.navigate("topicCollectionManage?path=${android.net.Uri.encode(path)}"); return
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
        // 站内兜底：上面的精确匹配没命中，但路径里仍带得出帖子/用户/板块 id 时留在应用内。
        // 设置页承诺「站内跳转始终在应用内进行」，所以这段不受 linkOpenMode 影响。
        val uri = android.net.Uri.parse(url)
        if (Endpoints.isInternal(uri.host)) {
            val path = uri.path ?: ""
            Regex("""/topic/(\d+)""").find(path)?.let { m -> linkNav.navigate("topic/${m.groupValues[1]}"); return }
            Regex("""/user/(\d+)""").find(path)?.let { m -> linkNav.navigate("user/${m.groupValues[1]}"); return }
            Regex("""/forum/(\d+)""").find(path)?.let { m -> linkNav.navigate("forum/${m.groupValues[1]}"); return }
            uri.getQueryParameter("username")?.takeIf { it.isNotBlank() }?.let {
                linkNav.navigate("userByName?name=${android.net.Uri.encode(it)}&tab=topics"); return
            }
            uri.getQueryParameter("uid")?.toLongOrNull()?.takeIf { it > 0 }?.let {
                linkNav.navigate("user/$it"); return
            }
        }
        if (session.settings.linkOpenMode == 0) {
            linkNav.navigate("web?url=${android.net.Uri.encode(url)}")
        } else {
            runCatching {
                context.startActivity(
                    android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
                        .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            }
        }
    }

    fun openLink(raw: String) {
        val url = Endpoints.abs(raw)
        val host = runCatching { android.net.Uri.parse(url).host.orEmpty() }.getOrDefault("")
        if (!session.linkPreviewEnabled || Endpoints.isInternal(host)) openLinkDirect(url)
        else {
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
                    androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                        androidx.compose.foundation.layout.Spacer(Modifier.width(10.dp))
                        Text("正在读取网站信息…")
                    }
                } else {
                    val info = linkPreviewInfo
                    androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.Top) {
                        coil.compose.AsyncImage(
                            model = info?.iconUrl,
                            contentDescription = null,
                            modifier = Modifier.size(42.dp).clip(RoundedCornerShape(10.dp)),
                        )
                        androidx.compose.foundation.layout.Spacer(Modifier.width(12.dp))
                        androidx.compose.foundation.layout.Column {
                            Text(info?.title.orEmpty().ifBlank { "未知网站" }, style = MaterialTheme.typography.titleMedium)
                            if (!info?.description.isNullOrBlank()) {
                                androidx.compose.foundation.layout.Spacer(Modifier.height(4.dp))
                                Text(info!!.description, style = MaterialTheme.typography.bodySmall, maxLines = 4)
                            }
                            androidx.compose.foundation.layout.Spacer(Modifier.height(6.dp))
                            Text(info?.url ?: linkPreviewTarget.orEmpty(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val target = linkPreviewInfo?.url ?: linkPreviewTarget ?: return@TextButton
                    linkPreviewTarget = null; linkPreviewInfo = null
                    openLinkDirect(target)
                }) { Text("打开链接") }
            },
            dismissButton = { TextButton(onClick = { linkPreviewTarget = null; linkPreviewInfo = null }) { Text("取消") } },
        )
    }

    var updateTip by remember { mutableStateOf<UpdateChecker.UpdateInfo?>(null) }
    LaunchedEffect(Unit) {
        if (session.settings.updateCheckMode == 2) return@LaunchedEffect
        val due = session.settings.updateCheckMode == 0 ||
            System.currentTimeMillis() - session.settings.lastUpdateCheck > session.settings.updateCheckIntervalHours * 3600_000L
        if (!due) return@LaunchedEffect
        session.settings.lastUpdateCheck = System.currentTimeMillis()
        val rel = UpdateChecker.latestRelease() ?: return@LaunchedEffect
        val cur = runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: ""
        if (UpdateChecker.isNewer(rel.tag, cur)) updateTip = rel
    }
    updateTip?.let { rel ->
        val askNever = session.settings.updateCheckMode == 0
        AlertDialog(
            onDismissRequest = { updateTip = null },
            title = { Text("发现新版本") },
            text = { Text("${rel.name} 已发布，是否前往下载？" + if (!askNever) "\n\n（自动检查当前为「按间隔」。可前往 设置 → 常规设置 → 检查更新时机 调整）" else "") },
            confirmButton = {
                TextButton(onClick = {
                    updateTip = null
                    runCatching {
                        context.startActivity(
                            android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(rel.url))
                                .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK),
                        )
                    }
                }) { Text("前往下载") }
            },
            dismissButton = {
                androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically) {
                    if (askNever) {
                        TextButton(onClick = {
                            session.settings.updateCheckMode = 2
                            updateTip = null
                            session.showToast("已设置为不再提示更新")
                        }) { Text("不再提示") }
                    }
                    TextButton(onClick = { updateTip = null }) { Text("取消") }
                }
            },
        )
    }

    CompositionLocalProvider(
        LocalLinkHandler provides { openLink(it) },
        LocalTableDisplayMode provides session.tableDisplayMode,
        LocalShowUid provides session.showUid,
        LocalSmartDecode provides session.smartDecodeEnabled,
        LocalMasterNav provides (if (twoPane) masterNav else null),
    ) {
        AppContent(
            session = session,
            nav = nav,
            masterNav = masterNav,
            detailNav = detailNav,
            snackbar = snackbar,
            scope = scope,
            homeDrawerState = homeDrawerState,
            twoPane = twoPane,
            route = route,
            bottomRoutes = bottomRoutes,
            bottomItems = bottomItems,
            onNavigateBottom = bottomNavigation::submit,
        )
    }
}
