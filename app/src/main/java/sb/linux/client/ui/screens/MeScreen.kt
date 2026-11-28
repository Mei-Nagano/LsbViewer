package sb.linux.client.ui.screens

import sb.linux.client.ui.patterns.settings.settingsRowIcon
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.navigation.NavHostController
import androidx.navigation.NavGraph.Companion.findStartDestination
import sb.linux.client.ui.navigation.LocalMasterNav
import sb.linux.client.ui.components.feedback.AppConfirmDialog
import sb.linux.client.ui.components.navigation.AppPageScaffold
import sb.linux.client.ui.feature.settings.SettingsSearchIndex
import sb.linux.client.ui.patterns.settings.*
import sb.linux.client.ui.session.Session
import sb.linux.client.ui.theme.LocalAppSpacing

@Composable
fun MeScreen(session: Session, nav: NavHostController) {
    var query by rememberSaveable { mutableStateOf("") }
    var searching by rememberSaveable { mutableStateOf(false) }
    val focus = remember { FocusRequester() }
    val spacing = LocalAppSpacing.current
    val account = session.account.loginState
    var showLogout by remember(account.userId) { mutableStateOf(false) }
    val masterNav = LocalMasterNav.current
    fun openMasterTab(route: String) {
        if (masterNav != null) masterNav.navigate(route) {
            popUpTo(masterNav.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        } else nav.navigate(route)
    }
    BackHandler(searching) { searching = false; query = "" }
    LaunchedEffect(searching) { if (searching) focus.requestFocus() }
    LaunchedEffect(session) { session.refreshSession() }

    AppPageScaffold(title = "我的", onBack = null, primaryPage = true,
        actions = {
                IconButton(onClick = { searching = !searching; query = "" }) {
                    Icon(if (searching) Icons.Filled.Close else Icons.Filled.Search,
                        if (searching) "关闭搜索" else "搜索设置")
                }
                IconButton(onClick = { nav.navigate("about") }) { Icon(Icons.Filled.Info, "关于应用") }
        }) { padding ->
            Column(Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.pageHorizontal, vertical = spacing.pageVertical)) {
            if (searching) {
                OutlinedTextField(
                    value = query, onValueChange = { query = it },
                    placeholder = { Text("搜索设置") }, singleLine = true,
                    leadingIcon = { Icon(Icons.Filled.Search, null) },
                    shape = MaterialTheme.shapes.extraLarge,
                    modifier = Modifier.fillMaxWidth().focusRequester(focus).padding(bottom = spacing.section),
                )
            }
            if (query.isBlank()) {
                MeAccountCard(account) {
                    nav.navigate(if (account.loggedIn && account.userId > 0) "user/${account.userId}" else "login")
                }
                Spacer(Modifier.height(spacing.contentGap))
                SettingsCategoryGrid(meAccountCategories(account.userId)) {
                    if (!account.loggedIn || account.userId <= 0) nav.navigate("login")
                    else if (it.route == "logout") showLogout = true
                    else nav.navigate(it.route)
                }
                Spacer(Modifier.height(spacing.contentGap))
                Column(verticalArrangement = Arrangement.spacedBy(spacing.inline)) {
                    val actions = listOf(
                        Triple("我的内容", settingsRowIcon("我的内容"), { nav.navigate("myContent") }),
                        Triple("原站广告", settingsRowIcon("原站广告"), { nav.navigate("sourcePromotions") }),
                        Triple("应用设置", settingsRowIcon("应用设置"), { openMasterTab("appSettings") }),
                        Triple("关于", settingsRowIcon("关于"), { nav.navigate("about") }),
                    )
                    actions.forEachIndexed { index, (title, icon, onClick) ->
                        SettingMenuRow(title, "", icon, showChevron = false,
                            titleWeight = MaterialTheme.typography.bodyLarge.fontWeight,
                            shape = settingsItemShape(index, actions.size), onClick = onClick)
                    }
                }
            } else {
                val results = SettingsSearchIndex.search(query)
                if (results.isEmpty()) Text("没有找到相关设置",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = spacing.section))
                Column(verticalArrangement = Arrangement.spacedBy(spacing.inline)) {
                    results.forEachIndexed { index, entry ->
                        SettingMenuRow(entry.title, entry.category, shape = settingsItemShape(index, results.size),
                            titleWeight = MaterialTheme.typography.bodyLarge.fontWeight) {
                            sb.linux.client.ui.feature.settings.openSettingsSearchResult(nav, entry)
                        }
                    }
                }
            }
            Spacer(Modifier.height(spacing.largeSection))
        }
    }
    if (showLogout) AppConfirmDialog(
        title = "退出登录", message = "确定要退出当前账号吗？",
        onDismiss = { showLogout = false },
        onConfirm = {
            showLogout = false
            session.onLoggedOut()
            if (masterNav != null) {
                masterNav.navigate("home") {
                    popUpTo(masterNav.graph.findStartDestination().id) { inclusive = true }
                }
                nav.popBackStack("detailEmpty", false)
            } else nav.navigate("home") { popUpTo("home") { inclusive = true } }
        }, confirmText = "退出", dismissText = "取消", destructive = true,
    )
}

internal fun meAccountCategories(userId: Long) = listOf(
    SettingsCategory("主题", "查看我发布的主题", "user/$userId?tab=topics", settingsRowIcon("主题")),
    SettingsCategory("回复", "查看我发布的回复", "user/$userId?tab=replies", settingsRowIcon("回复")),
    SettingsCategory("私信", "查看我的私信", "directMessages", settingsRowIcon("私信")),
    SettingsCategory("通知", "查看我的通知", "notifications", settingsRowIcon("通知")),
    SettingsCategory("隐私", "管理我的隐私", "userPrivacy", settingsRowIcon("隐私")),
    SettingsCategory("收藏", "打开我的收藏", "favorites", settingsRowIcon("收藏")),
    SettingsCategory("节点", "打开我的节点", "nodes?url=${android.net.Uri.encode("/nodes?tab=mine")}", settingsRowIcon("节点")),
    SettingsCategory("签名", "管理个人签名", "personalSignature", settingsRowIcon("签名")),
    SettingsCategory("活跃度", "查看个人社区活跃度", "communityActivity", settingsRowIcon("活跃度")),
    SettingsCategory("附件", "管理我的附件", "attachments", settingsRowIcon("附件")),
    SettingsCategory("表情", "管理我的表情", "stickerManage", settingsRowIcon("表情")),
    SettingsCategory("资料", "在应用内打开个人资料", "settings", settingsRowIcon("资料")),
    SettingsCategory("邀请", "打开邀请中心", "inviteCenter", settingsRowIcon("邀请")),
    SettingsCategory("烧饼", "打开我的烧饼", "communityWallet", settingsRowIcon("烧饼")),
    SettingsCategory("称号", "打开我的称号", "gachaProfile", settingsRowIcon("称号")),
    SettingsCategory("广告", "打开我的广告", "myPromotions", settingsRowIcon("广告")),
    SettingsCategory("退出", "确认退出登录", "logout", Icons.AutoMirrored.Filled.Logout),
)
