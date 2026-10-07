package sb.linux.client.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
internal fun ForumKeywordSection(
    options: List<Pair<Long, String>>,
    selected: Set<Long>,
    enabled: Boolean,
    blockingEnabled: Boolean,
    warning: String,
    onSelectionChange: (Set<Long>) -> Unit,
) {
    if (blockingEnabled && options.isNotEmpty()) {
        Text("屏蔽版块", style = MaterialTheme.typography.titleSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            options.forEach { (id, name) ->
                InputChip(
                    selected = id in selected,
                    onClick = {
                        onSelectionChange(if (id in selected) selected - id else selected + id)
                    },
                    enabled = enabled,
                    label = { Text(name) },
                )
            }
        }
        Text(
            "进入具体版块时版块屏蔽不生效。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    } else if (warning.isNotBlank()) {
        Text(warning, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
    }
}
