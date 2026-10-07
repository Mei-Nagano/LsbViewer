package sb.linux.client.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import sb.linux.client.ui.navigation.LocalMasterNav
import sb.linux.client.ui.session.Session
import sb.linux.client.ui.Avatar
import sb.linux.client.ui.TitleBadgeView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeScreen(session: Session, nav: NavHostController) {
    val state = session.account.loginState
    // 平板双栏：主栏页面内切换顶层页面（应用设置/首页）时导航主栏，手机为 null 走原逻辑
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

    Scaffold(topBar = { TopAppBar(title = { Text("我的") }) }) { pad ->
        Column(
            Modifier
                .padding(pad)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            if (state.loggedIn) {
                // 用户卡片：品牌色横幅 + 大头像 + 统计信息（点按进入个人页面）
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .clickable { nav.navigate("user/${state.userId}") },
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow
                ) {
                    Column(Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Avatar(state.avatarUrl, 60)
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                // 称号跟在名字后面同一行：名字可省略号截断，称号先占位不被挤走
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Text(
                                        state.username,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false),
                                    )
                                    state.titleBadge?.let { TitleBadgeView(it, small = true) }
                                }
                                Spacer(Modifier.height(3.dp))
                                Text(
                                    "${state.userGroup.ifBlank { "饼友" }} · UID ${state.userId}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis
                                )
                            }
                            Icon(
                                Icons.Filled.ChevronRight, null,
                                Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.height(14.dp))
                        // 积分摘要
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            SummaryChip(Icons.Filled.Paid, "积分 ${state.points.ifBlank { "-" }}")
                        }
                    }
                }

                MenuGroup("功能") {
                    MenuRow(Icons.Filled.Leaderboard, "用户榜单") { nav.navigate("leaderboard?type=points") }
                    MenuRow(Icons.Filled.PersonAdd, "邀请中心") { nav.navigate("inviteCenter") }
                    MenuRow(Icons.Filled.MilitaryTech, "我的称号") { nav.navigate("gachaProfile") }
                    MenuRow(Icons.Filled.Email, "我的私信") { nav.navigate("directMessages") }
                    MenuRow(Icons.Filled.CollectionsBookmark, "我的淘帖") { nav.navigate("topicCollections?tab=mine") }
                    MenuRow(Icons.Filled.History, "浏览历史") { nav.navigate("footprint") }
                    MenuRow(Icons.Filled.Favorite, "收藏内容") { nav.navigate("favorites") }
                }
                MenuGroup("应用") {
                    MenuRow(Icons.Filled.Tune, "应用设置") { openMasterTab("appSettings") }
                    // 关于应用（与应用设置平级，3.18）
                    MenuRow(Icons.Filled.Info, "关于应用") { nav.navigate("about") }
                }
            } else {
                // 未登录引导卡
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow
                ) {
                    Column(
                        Modifier.padding(vertical = 30.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            Modifier
                                .size(60.dp)
                                .clip(RoundedCornerShape(50))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.Person, null, Modifier.size(28.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Spacer(Modifier.height(14.dp))
                        Text("未登录", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "登录后可发帖、回帖、投币与私信",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(18.dp))
                        Button(
                            onClick = { nav.navigate("login") },
                            shape = RoundedCornerShape(50)
                        ) { Text("去登录") }
                    }
                }
                MenuGroup("游客可用") {
                    MenuRow(Icons.Filled.Leaderboard, "用户榜单") { nav.navigate("leaderboard?type=points") }
                    MenuRow(Icons.Filled.Home, "浏览首页") { openMasterTab("home") }
                }
                MenuGroup("应用") {
                    MenuRow(Icons.Filled.Tune, "应用设置") { openMasterTab("appSettings") }
                    // 关于应用（与应用设置平级，3.18）
                    MenuRow(Icons.Filled.Info, "关于应用") { nav.navigate("about") }
                }
            }
            Spacer(Modifier.height(80.dp))
        }
    }
}

/** 摘要胶囊：图标 + 文本 */
