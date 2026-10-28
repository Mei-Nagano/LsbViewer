package sb.linux.client.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import sb.linux.client.domain.community.ForumCount
import sb.linux.client.domain.community.HomeSidebar
import sb.linux.client.ui.session.Session
import sb.linux.client.ui.patterns.user.UserAvatar
/** 源站首页侧板：用户卡片 / 签到天数 / 版块列表 / 快捷功能 / 每日热帖 / 站点统计 / 最新用户
 *  关闭方式：点击侧边栏以外区域（遮罩）自动关闭（13）；平板双栏下右栏/导航栏区域的
 *  关闭由 MainActivity 中的透明点击层兜底 */
@Composable
internal fun HomeSidebarDrawer(
    session: Session,
    sidebar: HomeSidebar?,
    onClose: () -> Unit,
    onRefresh: () -> Unit,
    onOpenForum: (Long) -> Unit,
    onOpenTopic: (Long) -> Unit,
    onOpenUser: (Long) -> Unit,
    onOpenLeaderboard: () -> Unit,
    onLogin: () -> Unit,
    onNavigate: (String) -> Unit,
) {
    // 背景容器用正常矩形（默认右上/右下为圆角）。
    // 宽度上限 300dp：不能 fillMaxWidth（会占满全屏，遮罩无处可点导致抽屉关不掉），
    // 收窄后右侧留出的遮罩区域可点击关闭；窄屏/窄主栏时自动收缩不溢出。
    ModalDrawerSheet(
        modifier = Modifier.widthIn(max = 300.dp),
        drawerShape = RectangleShape,
    ) {
        val sb = sidebar
        Column(Modifier.fillMaxSize()) {
            HomeSidebarHeader(session, onClose, onOpenUser, onLogin)
            LazyColumn(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp)
            ) {
                // 快捷功能（无图标，统一圆角矩形背景；按侧板单/双列设置排布）
                item { DrawerTitle("快捷功能") }
                val quickActions = listOf(
                    "原站广告" to { onNavigate("sourcePromotions") },
                    "我的通知" to { onNavigate("notifications") },
                    "浏览足迹" to { onNavigate("footprint") },
                    "收藏内容" to { onNavigate("favorites") },
                    "用户榜单" to { onOpenLeaderboard() },
                    "应用设置" to { onNavigate("appSettings") },
                    "关于应用" to { onNavigate("about") },
                )
                if (session.sidebarTwoColumns) {
                    quickActions.chunked(2).forEachIndexed { ri, row ->
                        item(key = "q-$ri") {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                row.forEach { (label, action) ->
                                    DrawerPillRow(label, action, Modifier.weight(1f))
                                }
                                if (row.size == 1) Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                } else {
                    quickActions.forEachIndexed { i, (label, action) ->
                        item(key = "q-$i") { DrawerPillRow(label, action, Modifier.fillMaxWidth()) }
                    }
                }

                // 板块（来自源站侧边栏解析 + 本地永久缓存，点击进入才请求该板块内容）
                if (sb?.forums?.isNotEmpty() == true) {
                    item { DrawerTitle("板块") }
                    val twoCol = session.sidebarTwoColumns
                    if (twoCol) {
                        val forumRows = sb.forums.chunked(2)
                        items(forumRows, key = { "f-${it.first().id}" }) { row: List<ForumCount> ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                row.forEach { f: ForumCount ->
                                    ForumEntryRow(f, onOpenForum, Modifier.weight(1f))
                                }
                                if (row.size == 1) Spacer(Modifier.weight(1f))
                            }
                        }
                    } else {
                        // 单列展示：每条横跨整行（避免右侧留白），并在右侧展示帖子数量
                        items(sb.forums, key = { "f2-${it.id}" }) { f: ForumCount ->
                            ForumEntryRow(f, onOpenForum, Modifier.fillMaxWidth(), showCount = true)
                        }
                    }
                }

                // 每日热帖
                if (sb?.hotTopics?.isNotEmpty() == true) {
                    item { DrawerTitle("每日热帖 · 近 24 小时") }
                    items(sb.hotTopics, key = { "h-${it.topicId}" }) { t ->
                        HotTopicRow(t, onOpenTopic, Modifier.fillMaxWidth())
                    }
                }

                // 站点统计：结构化为「数值 + 标签」网格，容器风格与其他卡片一致
                if (!sb?.statsText.isNullOrBlank()) {
                    item { DrawerTitle("站点统计") }
                    item {
                        val stats = remember(sb!!.statsText) { parseSidebarStats(sb!!.statsText) }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier.padding(vertical = 2.dp),
                        ) {
                            if (stats.isEmpty()) {
                                // 兜底：原文格式不认识时按普通文本展示
                                Text(
                                    sb!!.statsText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                )
                            } else {
                                SidebarStatsCard(stats)
                            }
                        }
                    }
                }

                // 当前在线（源站侧栏 online-users-card）：在线总数 + 头像网格 + 「还有 N 人在线」
                if (session.showOnlineUsers && (!sb?.onlineUsers?.count.isNullOrBlank() || sb?.onlineUsers?.items?.isNotEmpty() == true)) {
                    item { DrawerTitle("当前在线${sb!!.onlineUsers.count.takeIf { it.isNotBlank() }?.let { " · $it" } ?: ""}") }
                    item {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier.padding(vertical = 2.dp),
                        ) {
                            Column(Modifier.padding(horizontal = 10.dp, vertical = 10.dp)) {
                                val online = sb!!.onlineUsers
                                // 头像 + 用户名网格（每行 4 个，点击进入用户主页）
                                online.items.chunked(4).forEach { row ->
                                    Row(
                                        Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    ) {
                                        row.forEach { u ->
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .clickable { onOpenUser(u.userId) }
                                                    .padding(vertical = 3.dp),
                                            ) {
                                                UserAvatar(u.userId, u.avatarUrl, 36, online = true)
                                                Spacer(Modifier.height(3.dp))
                                                Text(
                                                    u.username,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.padding(horizontal = 2.dp),
                                                )
                                            }
                                        }
                                        repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                                    }
                                    Spacer(Modifier.height(4.dp))
                                }
                                if (online.moreText.isNotBlank()) {
                                    Text(
                                        online.moreText,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }

                if (sb == null) {
                    item {
                        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                            Text(
                                "侧板加载失败，请重试",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            TextButton(onClick = onRefresh) { Text("重新加载") }
                        }
                    }
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

