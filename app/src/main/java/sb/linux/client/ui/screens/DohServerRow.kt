package sb.linux.client.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import sb.linux.client.domain.network.DohBenchmark
import sb.linux.client.domain.network.DohServer
import sb.linux.client.ui.patterns.settings.SettingsListRow
import sb.linux.client.ui.patterns.settings.settingsRowIcon
import sb.linux.client.ui.theme.LocalAppSpacing

// 名称与地址分两行，状态紧随地址；尾部保留直接测速和更多菜单，沿用设置分组样式。
@Composable
internal fun DohServerRow(
    server: DohServer,
    selected: Boolean,
    testing: Boolean,
    running: Boolean,
    result: DohBenchmark?,
    onSelect: () -> Unit,
    onTest: () -> Unit,
    onCancel: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    var menu by remember(server.id) { mutableStateOf(false) }
    var details by remember(server.id) { mutableStateOf(false) }
    val spacing = LocalAppSpacing.current
    val status = when {
        testing -> if (running) "测速中" else "等待中"
        result != null -> result.medianMs?.let { "$it ms" } ?: "失败"
        else -> ""
    }
    SettingsListRow(server.name, listOf(server.url, status).filter { it.isNotBlank() }.joinToString(" · "),
        icon = settingsRowIcon("DoH 服务器"),
        modifier = Modifier.selectable(selected, role = Role.RadioButton, onClick = onSelect),
        trailing = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = selected, onClick = null)
                IconButton(onClick = { if (testing) onCancel() else onTest() }) {
                    Icon(if (testing) Icons.Filled.Close else Icons.Filled.Bolt,
                        if (testing) "取消${server.name}测速" else "测试${server.name}速度")
                }
                Box {
                    IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, "${server.name}的更多操作") }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }, shape = MaterialTheme.shapes.extraLarge,
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
                        DropdownMenuItem(text = { Text("查看详情") }, onClick = { menu = false; details = true })
                        DropdownMenuItem(text = { Text("编辑") }, enabled = !testing, onClick = { menu = false; onEdit() })
                        if (!server.builtIn) DropdownMenuItem(text = { Text("删除") }, enabled = !testing, onClick = { menu = false; onDelete() })
                    }
                }
            }
        })
    if (details) AlertDialog(
        onDismissRequest = { details = false }, title = { Text("DoH 服务器详情") },
        text = {
            SelectionContainer {
                Column(Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(spacing.contentGap)) {
                    Text(server.name, style = MaterialTheme.typography.titleMedium)
                    Text(server.url, style = MaterialTheme.typography.bodyMedium)
                    Text(if (server.builtIn) "内置服务器" else "自定义服务器",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (server.note.isNotBlank()) Text(server.note, style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = { TextButton(onClick = { details = false }) { Text("关闭") } },
    )
}
