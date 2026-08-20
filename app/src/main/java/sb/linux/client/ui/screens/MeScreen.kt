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
import androidx.navigation.NavHostController
import sb.linux.client.data.Session
import sb.linux.client.ui.Avatar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeScreen(session: Session, nav: NavHostController) {
    val state = session.loginState

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
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                ) {
                    Column(Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Avatar(state.avatarUrl, 60, userId = state.userId)
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text(state.username, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
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
                        // 积分 / 签到 摘要条
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            SummaryChip(Icons.Filled.Paid, "积分 ${state.points.ifBlank { "-" }}")
                            if (session.checkinText.isNotBlank()) {
                                SummaryChip(Icons.Filled.CheckCircle, session.checkinText)
                            }
                        }
                    }
                }

                MenuGroup("功能") {
                    MenuRow(Icons.Filled.CheckCircle, "每日签到") { nav.navigate("checkin") }
                    MenuRow(Icons.Filled.Leaderboard, "用户榜单") { nav.navigate("leaderboard?type=points") }
                    MenuRow(Icons.Filled.History, "浏览历史") { nav.navigate("footprint") }
                    MenuRow(Icons.Filled.VolunteerActivism, "社区保释所") { nav.navigate("bail") }
                }
                MenuGroup("应用") {
                    MenuRow(Icons.Filled.Tune, "应用设置") { nav.navigate("appSettings") }
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
                            "登录后可发帖、回帖、投币、签到与私信",
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
                    MenuRow(Icons.Filled.Home, "浏览首页") { nav.navigate("home") }
                }
                MenuGroup("应用") {
                    MenuRow(Icons.Filled.Tune, "应用设置") { nav.navigate("appSettings") }
                    // 关于应用（与应用设置平级，3.18）
                    MenuRow(Icons.Filled.Info, "关于应用") { nav.navigate("about") }
                }
            }
            Spacer(Modifier.height(80.dp))
        }
    }
}

/** 摘要胶囊：图标 + 文本 */
@Composable
private fun SummaryChip(icon: ImageVector, text: String) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.65f)
    ) {
        Row(
            Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(icon, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
            Text(
                text,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun MenuGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    Text(
        title,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 24.dp, top = 16.dp, bottom = 6.dp)
    )
    Surface(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column { content() }
    }
}

@Composable
private fun MenuRow(icon: ImageVector, label: String, tint: Color = Color.Unspecified, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 图标色调圆角容器
        val container = if (tint == Color.Unspecified) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
        else tint.copy(alpha = 0.12f)
        val iconTint = if (tint == Color.Unspecified) MaterialTheme.colorScheme.onSecondaryContainer else tint
        Box(
            Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(container),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, Modifier.size(18.dp), tint = iconTint)
        }
        Spacer(Modifier.width(13.dp))
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            color = if (tint == Color.Unspecified) MaterialTheme.colorScheme.onSurface else tint
        )
        Spacer(Modifier.weight(1f))
        Icon(
            Icons.Filled.ChevronRight, null,
            Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
