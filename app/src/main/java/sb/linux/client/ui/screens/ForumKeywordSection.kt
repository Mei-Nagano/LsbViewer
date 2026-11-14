package sb.linux.client.ui.screens

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import sb.linux.client.ui.patterns.settings.SettingsCheckRow

@Composable
internal fun ForumKeywordSection(
    options: List<Pair<Long, String>>, selected: Set<Long>, enabled: Boolean,
    blockingEnabled: Boolean, warning: String, onSelectionChange: (Set<Long>) -> Unit,
) {
    if (blockingEnabled && options.isNotEmpty()) {
        GroupLabel("屏蔽版块")
        options.forEach { (id, name) ->
            SettingsCheckRow(name, checked = id in selected, enabled = enabled,
                onCheckedChange = { onSelectionChange(if (it) selected + id else selected - id) })
        }
        Text("进入具体版块时版块屏蔽不生效。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    } else if (warning.isNotBlank()) Text(warning, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
}
