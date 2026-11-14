package sb.linux.client.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import sb.linux.client.ui.patterns.settings.SettingsActionRow
import sb.linux.client.ui.patterns.settings.SettingsListRow

@Composable
internal fun EditableKeywordSection(
    title: String, emptyMessage: String, inputLabel: String, input: String,
    onInputChange: (String) -> Unit, values: List<String>, onAdd: () -> Unit,
    onRemove: (String) -> Unit, enabled: Boolean, supportingText: String? = null,
) {
    GroupLabel(title)
    SettingsTextField(input, onInputChange, label = { Text(inputLabel) }, searchLabel = inputLabel, singleLine = true, enabled = enabled)
    SettingsActionRow("添加屏蔽词", onAdd, enabled = enabled && input.isNotBlank())
    if (values.isEmpty()) Text(emptyMessage, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
    else {
        values.forEach { value ->
            SettingsListRow(value, "自定义屏蔽词", enabled = enabled, onClick = { onRemove(value) }, trailing = {
                Icon(Icons.Filled.Close, "移除屏蔽词")
            })
        }
        supportingText?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}
