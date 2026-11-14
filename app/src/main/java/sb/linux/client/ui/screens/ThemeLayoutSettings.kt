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
import sb.linux.client.ui.patterns.settings.settingsSearchAnchor
import sb.linux.client.ui.patterns.settings.settingsRowIcon
import sb.linux.client.ui.patterns.settings.SettingsListRow

private val BOTTOM_OPTIONS = listOf(
    "home" to "社区", "forums" to "版块", "topicCollections" to "淘帖",
    "messages" to "私信", "me" to "我的",
)

internal fun LazyListScope.themeLayoutSettings(session: Session) {
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
                val icon = settingsRowIcon(label)
                SettingsListRow(
                    title = label, icon = icon,
                    subtitle = if (checked) "导航栏第 ${index + 1} 项" else "未加入导航栏",
                    modifier = Modifier.toggleable(checked, enabled = enabled, role = Role.Checkbox) {
                        session.preferences.saveBottomBarItems(if (it) selected + id else selected - id)
                    },
                    trailing = {
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
