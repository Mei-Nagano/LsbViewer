package sb.linux.client.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Alignment
import sb.linux.client.ui.LocalLinkHandler
import sb.linux.client.ui.patterns.user.UserAvatar
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import sb.linux.client.domain.community.HomeSidebar
import sb.linux.client.ui.session.Session
import sb.linux.client.ui.theme.LocalAppSpacing

/** 复用源站左导航内容；显示策略继续由应用抽屉拥有。 */
@Composable
internal fun HomeSidebarDrawer(
    session: Session, sidebar: HomeSidebar?, onClose: () -> Unit, onRefresh: () -> Unit,
    forumsExpanded: Boolean, onToggleForums: () -> Unit,
    onOpenForum: (Long) -> Unit, onOpenTopic: (Long) -> Unit, onOpenUser: (Long) -> Unit,
    onOpenLeaderboard: () -> Unit, onLogin: () -> Unit, onNavigate: (String) -> Unit,
) {
    val openLink = LocalLinkHandler.current
    val spacing = LocalAppSpacing.current
    ModalDrawerSheet(modifier = Modifier.widthIn(max = 300.dp), drawerShape = RectangleShape) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = LocalAppSpacing.current.section)) {
            item { HomeSidebarHeader(session, onClose, onOpenUser, onLogin) }
            item { NavigationDrawerItem(label = { Text("社区首页") }, selected = session.homeBrowse.homeCombo == 0 && session.homeBrowse.homeTabIndex == 0,
                icon = { SidebarDot() }, onClick = { session.homeBrowse.homeCombo = 0; session.homeBrowse.homeTabIndex = 0; onClose() },
                modifier = Modifier.padding(horizontal = LocalAppSpacing.current.item)) }
            item { NavigationDrawerItem(label = { Text("我的关注") }, selected = session.homeBrowse.homeCombo == 0 && session.homeBrowse.homeTabIndex == 4,
                icon = { SidebarDot() }, onClick = {
                    if (!session.account.loginState.loggedIn) onLogin()
                    else { session.homeBrowse.homeCombo = 0; session.homeBrowse.homeTabIndex = 4; onClose() }
                }, modifier = Modifier.padding(horizontal = LocalAppSpacing.current.item)) }
            if (!sidebar?.forums.isNullOrEmpty()) {
                // 分组标题优先于版块条目，数量为辅助信息；沿用 Material 3 排版与 item 间距。
                item(key = "forum_selector") {
                    NavigationDrawerItem(
                        label = { Text("选择板块", style = MaterialTheme.typography.titleSmall) },
                        selected = false,
                        icon = { Icon(Icons.Default.FolderOpen, contentDescription = null) },
                        badge = {
                            Icon(
                                if (forumsExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = if (forumsExpanded) "收起板块" else "展开板块",
                            )
                        },
                        onClick = onToggleForums,
                        modifier = Modifier.padding(horizontal = spacing.item).semantics {
                            stateDescription = if (forumsExpanded) "已展开" else "已收起"
                        },
                    )
                }
                if (forumsExpanded) {
                    items(sidebar!!.forums, key = { it.id }) { forum ->
                        NavigationDrawerItem(label = { Text(forum.name) }, selected = false,
                            icon = { SidebarDot() }, badge = { Text(forum.count) },
                            onClick = { onOpenForum(forum.id) }, modifier = Modifier.padding(horizontal = spacing.item))
                    }
                }
            }
            if (!sidebar?.announcements.isNullOrEmpty()) item {
                Surface(Modifier.fillMaxWidth().padding(spacing.item), shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.surfaceContainerLow) {
                    Column(Modifier.padding(spacing.pageHorizontal)) {
                        Text("站务公告", style = MaterialTheme.typography.titleMedium)
                        sidebar!!.announcements.forEach { notice ->
                            Text(notice.title, style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.fillMaxWidth().clickable { onClose(); openLink?.invoke(notice.url) }
                                    .padding(vertical = spacing.contentGap))
                        }
                    }
                }
            }
            if (!sidebar?.dailyStars.isNullOrEmpty()) item {
                Surface(Modifier.fillMaxWidth().padding(spacing.item), shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.surfaceContainerLow) {
                    Column(Modifier.padding(spacing.pageHorizontal), verticalArrangement = Arrangement.spacedBy(spacing.contentGap)) {
                        sidebar!!.dailyStars.forEachIndexed { index, star ->
                            if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            Text("★  ${star.heading}", style = MaterialTheme.typography.titleMedium)
                            Row(Modifier.fillMaxWidth().clickable { onOpenUser(star.userId) }.padding(vertical = spacing.inline),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(spacing.item)) {
                                UserAvatar(star.userId, star.avatarUrl, online = star.online,
                                    onProfile = { onOpenUser(star.userId) }, shape = MaterialTheme.shapes.large)
                                Column(Modifier.weight(1f)) {
                                    Text(star.username, style = MaterialTheme.typography.titleSmall)
                                    Text(star.description, style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                        Text("最近24小时", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            if (sidebar?.forums.isNullOrEmpty()) item {
                TextButton(onClick = onRefresh, modifier = Modifier.fillMaxWidth()) { Text("加载版块列表") }
            }
        }
    }
}

@Composable
private fun SidebarDot() {
    Box(Modifier.size(7.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
}
