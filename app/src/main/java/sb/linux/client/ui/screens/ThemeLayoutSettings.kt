package sb.linux.client.ui.screens

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import sb.linux.client.ui.session.Session
import sb.linux.client.ui.theme.AppMenuDefaults
import sb.linux.client.ui.patterns.settings.SettingsChoiceRow

private val BOTTOM_OPTIONS = listOf(
    "home" to "社区", "forums" to "版块", "topicCollections" to "淘帖",
    "messages" to "私信", "me" to "我的",
)

internal fun LazyListScope.themeLayoutSettings(session: Session) {
    item { GroupLabel("布局") }
    item {
        GroupCard {
            SettingsChoiceRow("宽屏列表与详情", listOf(false to "自动并排", true to "单页显示"),
                session.singleContentColumn, { session.preferences.saveTabletMode(!it) },
                subtitle = "宽屏时可同时查看列表与详情", icon = Icons.Filled.ViewWeek)
        }
    }
    item { GroupLabel("导航栏") }
    item {
        GroupCard {
            Text("选择 2—5 项，按顺序显示", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = AppMenuDefaults.contentHorizontal, vertical = 12.dp))
            val selected = session.bottomBarItems
            (selected.mapNotNull { id -> BOTTOM_OPTIONS.firstOrNull { it.first == id } } +
                BOTTOM_OPTIONS.filter { it.first !in selected }).forEach { (id, label) ->
                val index = selected.indexOf(id)
                val checked = index >= 0
                val enabled = if (checked) selected.size > 2 else selected.size < 5
                val icon = when (id) {
                    "home" -> Icons.Filled.Home
                    "forums" -> Icons.Filled.GridView
                    "topicCollections" -> Icons.Filled.CollectionsBookmark
                    "messages" -> Icons.Filled.ChatBubbleOutline
                    else -> Icons.Filled.PersonOutline
                }
                ListItem(
                    headlineContent = { Text(label) },
                    leadingContent = { Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                    modifier = Modifier.toggleable(checked, enabled = enabled, role = Role.Checkbox) {
                        session.preferences.saveBottomBarItems(if (it) selected + id else selected - id)
                    },
                    colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
                    trailingContent = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked, null, enabled = enabled)
                            IconButton(enabled = index > 0, onClick = {
                                val changed = selected.toMutableList()
                                java.util.Collections.swap(changed, index, index - 1)
                                session.preferences.saveBottomBarItems(changed)
                            }) { Icon(Icons.Filled.KeyboardArrowUp, "上移$label") }
                            IconButton(enabled = index in 0 until selected.lastIndex, onClick = {
                                val changed = selected.toMutableList()
                                java.util.Collections.swap(changed, index, index + 1)
                                session.preferences.saveBottomBarItems(changed)
                            }) { Icon(Icons.Filled.KeyboardArrowDown, "下移$label") }
                        }
                    },
                )
            }
        }
    }
}
