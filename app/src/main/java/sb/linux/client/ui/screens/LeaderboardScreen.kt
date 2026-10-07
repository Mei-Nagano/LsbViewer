@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package sb.linux.client.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import sb.linux.client.ui.session.Session
import sb.linux.client.ui.*
import sb.linux.client.ui.feature.community.LeaderboardViewModel
import sb.linux.client.ui.feature.community.LeaderboardViewModelFactory

@Composable
fun LeaderboardScreen(session: Session, nav: NavHostController) {
    val entry = nav.currentBackStackEntry ?: return
    val initialType = entry.arguments?.getString("type") ?: "points"
    val factory = remember(session.leaderboardRepository, initialType) {
        LeaderboardViewModelFactory(session.leaderboardRepository, initialType)
    }
    val viewModel: LeaderboardViewModel = viewModel(factory = factory)
    val state by viewModel.uiState.collectAsState()
    val type = state.type
    val rows = state.rows
    LaunchedEffect(viewModel) { viewModel.load() }

    Scaffold(topBar = { TopAppBar(title = { Text("用户榜单") }) }) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            ScrollableTabRow(selectedTabIndex = LeaderTabs.indexOfFirst { it.first == type }.coerceAtLeast(0), edgePadding = 8.dp) {
                LeaderTabs.forEach { (t, label) ->
                    Tab(selected = type == t, onClick = { viewModel.selectType(t) }, text = { Text(label) })
                }
            }
            when {
                state.loading -> LoadingBox()
                state.error != null -> ErrorBox(state.error!!) { viewModel.load() }
                rows.isEmpty() -> EmptyBox("暂无数据")
                else -> LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
                    // 前三名：奖牌卡片（金银铜色调）
                    if (rows.size >= 3) {
                        item(key = "top3") {
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Bottom
                            ) {
                                listOf(rows[1], rows[0], rows[2]).forEach { r ->
                                    val (bg, fg, medal) = when (r.rank) {
                                        1 -> Triple(Color(0xFFFFF3CD), Color(0xFF7A5900), "1")
                                        2 -> Triple(Color(0xFFECEFF4), Color(0xFF444A54), "2")
                                        else -> Triple(Color(0xFFFBE2D5), Color(0xFF7A3B00), "3")
                                    }
                                    val dark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
                                    val cardBg = if (dark) MaterialTheme.colorScheme.surfaceContainerHigh else bg
                                    val cardFg = if (dark) MaterialTheme.colorScheme.onSurface else fg
                                    Surface(
                                        shape = RoundedCornerShape(18.dp),
                                        color = cardBg,
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(18.dp))
                                            .clickable { nav.navigate("user/${r.userId}") },
                                    ) {
                                        Column(
                                            Modifier.height(when (r.rank) { 1 -> 194.dp; 2 -> 172.dp; else -> 152.dp }).padding(vertical = 14.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            // 奖牌徽记
                                            Box(
                                                Modifier
                                                    .size(30.dp)
                                                    .clip(CircleShape)
                                                    .background(cardFg.copy(alpha = 0.14f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(medal, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = cardFg)
                                            }
                                            Spacer(Modifier.height(8.dp))
                                            Avatar(r.avatarUrl, 40, online = r.online)
                                            Spacer(Modifier.height(6.dp))
                                            Text(
                                                r.username,
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1, overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                r.valueText,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    // 其余名次：扁平列表
                    items(rows.drop(3), key = { "${it.rank}-${it.userId}" }) { r ->
                        ListItem(
                            leadingContent = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        "${r.rank}",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.width(26.dp)
                                    )
                                    Avatar(r.avatarUrl, 38, online = r.online)
                                }
                            },
                            headlineContent = { Text(r.username, style = MaterialTheme.typography.bodyLarge) },
                            supportingContent = { if (r.userGroup.isNotBlank()) Text(r.userGroup) },
                            trailingContent = { Text(r.valueText, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold) },
                            modifier = Modifier.clickable { nav.navigate("user/${r.userId}") }
                        )
                    }
                }
            }
        }
    }
}

// ---------------- 我的通知 ----------------
