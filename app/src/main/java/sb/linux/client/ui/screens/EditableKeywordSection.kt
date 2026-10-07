package sb.linux.client.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun EditableKeywordSection(
    title: String,
    emptyMessage: String,
    inputLabel: String,
    input: String,
    onInputChange: (String) -> Unit,
    values: List<String>,
    onAdd: () -> Unit,
    onRemove: (String) -> Unit,
    enabled: Boolean,
    supportingText: String? = null,
) {
    Text(title, style = MaterialTheme.typography.titleSmall)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = input,
            onValueChange = onInputChange,
            modifier = Modifier.weight(1f),
            label = { Text(inputLabel) },
            singleLine = true,
        )
        IconButton(onClick = onAdd, enabled = enabled) { Icon(Icons.Filled.Add, "添加") }
    }
    if (values.isEmpty()) {
        Text(emptyMessage, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
    } else {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            values.forEach { value ->
                InputChip(
                    selected = false,
                    onClick = { onRemove(value) },
                    enabled = enabled,
                    label = { Text(value) },
                    trailingIcon = { Icon(Icons.Filled.Close, "删除", Modifier) },
                )
            }
        }
        supportingText?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
