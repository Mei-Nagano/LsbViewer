package sb.linux.client.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import sb.linux.client.ui.components.navigation.AppPageScaffold
import sb.linux.client.ui.navigation.LocalMasterNav
import sb.linux.client.ui.session.Session
import sb.linux.client.ui.patterns.user.UserInfoCard
import sb.linux.client.ui.patterns.user.toUserInfoCardData
import sb.linux.client.ui.theme.LocalAppSpacing

/**
 * 我的（P10）：资料摘要 + 我的内容 / 社区工具 / 应用三组入口。
 *
 * 游客头部解释登录价值并给一个主按钮，同时保留公开社区入口、应用设置与本地内容；
 * 自动签到不设按钮，只展示业务已提供的结果。
 */
@Composable
fun MeScreen(session: Session, nav: NavHostController) {
    val state = session.account.loginState
    val spacing = LocalAppSpacing.current
    // 双栏时在主栏内切换顶层页面（应用设置/首页），手机走原导航
    val masterNav = LocalMasterNav.current
    fun openMasterTab(route: String) {
        if (masterNav != null) {
            masterNav.navigate(route) {
                popUpTo(masterNav.graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        } else {
            nav.navigate(route)
        }
    }

    AppPageScaffold(title = "我的", onBack = null) { pad ->
        Column(
            Modifier
                .padding(pad)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = spacing.pageVertical),
        ) {
            if (state.loggedIn) {
                UserInfoCard(
                    data = state.toUserInfoCardData(),
                    modifier = Modifier.padding(
                        horizontal = spacing.pageHorizontal,
                        vertical = spacing.item,
                    ),
                    onProfile = { nav.navigate("user/${state.userId}") },
                )
                MenuGroup("我的内容") {
                    MenuRow(Icons.Filled.History, "浏览历史") { nav.navigate("footprint") }
                    MenuRow(Icons.Filled.Favorite, "收藏内容") { nav.navigate("favorites") }
                    MenuRow(Icons.Filled.CollectionsBookmark, "我的淘帖") { nav.navigate("topicCollections?tab=mine") }
                    MenuRow(Icons.Filled.Email, "我的私信") { nav.navigate("directMessages") }
                }
                MenuGroup("社区工具") {
                    MenuRow(Icons.Filled.Campaign, "原站广告") { nav.navigate("sourcePromotions") }
                    MenuRow(Icons.Filled.Insights, "社区活跃度") { nav.navigate("communityActivity") }
                    MenuRow(Icons.Filled.Leaderboard, "用户榜单") { nav.navigate("leaderboard?type=points") }
                    MenuRow(Icons.Filled.PersonAdd, "邀请中心") { nav.navigate("inviteCenter") }
                    MenuRow(Icons.Filled.MilitaryTech, "我的称号") { nav.navigate("gachaProfile") }
                    MenuRow(Icons.Filled.Lock, "我的隐私") { nav.navigate("userPrivacy") }
                    MenuRow(Icons.Filled.Block, "屏蔽名单") { nav.navigate("userBlock") }
                    MenuRow(Icons.Filled.AttachFile, "附件管理") { nav.navigate("attachments") }
                }
            } else {
                GuestHeader(onLogin = { nav.navigate("login") })
                MenuGroup("游客可用") {
                    MenuRow(Icons.Filled.Home, "浏览社区") { openMasterTab("home") }
                    MenuRow(Icons.Filled.Campaign, "原站广告") { nav.navigate("sourcePromotions") }
                    MenuRow(Icons.Filled.Insights, "社区活跃度") { nav.navigate("communityActivity") }
                    MenuRow(Icons.Filled.Leaderboard, "用户榜单") { nav.navigate("leaderboard?type=points") }
                }
            }
            MenuGroup("应用") {
                MenuRow(Icons.Filled.Tune, "我的内容") { nav.navigate("myContent") }
                MenuRow(Icons.Filled.Settings, "应用设置") { openMasterTab("appSettings") }
                MenuRow(Icons.Filled.Info, "关于应用") { nav.navigate("about") }
            }
        }
    }
}

/** 游客头部：解释登录价值并提供一个主按钮，不伪造账号信息。 */
@Composable
private fun GuestHeader(onLogin: () -> Unit) {
    val spacing = LocalAppSpacing.current
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.pageHorizontal, vertical = spacing.item),
    ) {
        Column(Modifier.padding(spacing.contentGap + spacing.inline)) {
            Text("未登录", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(spacing.inline))
            Text(
                "登录后可发帖、回帖、投币与私信；公开内容仍可直接阅读。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(spacing.contentGap))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.item)) {
                Button(onClick = onLogin) { Text("去登录") }
            }
        }
    }
}
