package sb.linux.client.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import sb.linux.client.data.HtmlParser.ProfileField
import sb.linux.client.ui.Avatar

/** 个人资料摘要卡：只负责展示用户头像和源站返回的优先信息。 */
@Composable
internal fun ProfileHeaderCard(avatarUrl: String, username: String, info: List<Pair<String, String>>) {
    Surface(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Avatar(avatarUrl, 64)
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(username.ifBlank { "未命名" }, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            // 关键信息优先展示：UID/邮箱/注册时间/积分
            val priority = listOf("UID", "邮箱", "注册", "积分", "用户名")
            val sorted = info.sortedByDescending { (k, _) ->
                val idx = priority.indexOfFirst { k.contains(it) }
                if (idx < 0) -1 else priority.size - idx
            }
            sorted.forEach { (k, v) ->
                Row(Modifier.fillMaxWidth()) {
                    Text(
                        k,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(88.dp),
                    )
                    Text(
                        v.removePrefix(k).removePrefix(":").removePrefix("：").trim(),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

/** 个人资料字段编辑器：按源站字段类型选择对应的 Compose 控件。 */
@Composable
internal fun ProfileFieldInput(f: ProfileField, value: String, onChange: (String) -> Unit) {
    when (f.type) {
        "textarea" -> OutlinedTextField(
            value = value,
            onValueChange = onChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(f.label) },
            shape = RoundedCornerShape(14.dp),
            minLines = 2,
        )

        "select" -> {
            var expanded by remember { mutableStateOf(false) }
            val current = f.options.firstOrNull { it.first == value }?.second ?: value
            OutlinedTextField(
                value = current,
                onValueChange = {},
                readOnly = true,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(f.label) },
                shape = RoundedCornerShape(14.dp),
                trailingIcon = { TextButton(onClick = { expanded = true }) { Text("选择") } },
            )
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                f.options.forEach { (v, label) ->
                    DropdownMenuItem(
                        text = { Text(label) },
                        onClick = { onChange(v); expanded = false },
                    )
                }
            }
        }

        "checkbox" -> Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Switch(checked = value == "1", onCheckedChange = { onChange(if (it) "1" else "0") })
            Spacer(Modifier.width(10.dp))
            Text(f.label)
        }

        else -> OutlinedTextField(
            value = value,
            onValueChange = onChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(f.label) },
            shape = RoundedCornerShape(14.dp),
            singleLine = true,
        )
    }
}
