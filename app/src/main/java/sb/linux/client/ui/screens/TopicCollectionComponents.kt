package sb.linux.client.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import sb.linux.client.ui.theme.LocalAppSpacing
import sb.linux.client.common.collection.TopicCollectionActionForm
import sb.linux.client.common.collection.TopicCollectionPicker
import sb.linux.client.common.collection.TopicCollectionPickerOption
import sb.linux.client.common.collection.actionFor
import sb.linux.client.ui.session.Session
import sb.linux.client.ui.Badge
import sb.linux.client.ui.components.feedback.ErrorBox
import sb.linux.client.ui.components.feedback.EmptyBox

@Composable
internal fun CollectionOptionRow(
    option: TopicCollectionPickerOption,
    enabled: Boolean,
    onToggle: () -> Unit,
) {
    Surface(
        onClick = onToggle,
        enabled = enabled,
        shape = MaterialTheme.shapes.medium,
        color = if (option.included) MaterialTheme.colorScheme.secondaryContainer
        else MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Checkbox(checked = option.included, onCheckedChange = { onToggle() }, enabled = enabled)
            Text(
                option.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (option.included) FontWeight.Medium else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (option.visibility.isNotBlank()) {
                Badge(
                    option.visibility,
                    MaterialTheme.colorScheme.surfaceContainerHigh,
                    MaterialTheme.colorScheme.onSurfaceVariant,
                    small = true,
                )
            }
        }
    }
}

/** 弹窗底部的次级操作行（新建 / 全部取消）。 */
@Composable
internal fun SheetActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(horizontal = LocalAppSpacing.current.pageHorizontal, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                icon, null, Modifier.size(18.dp),
                tint = if (enabled) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            )
            Text(label, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

/** 新建专辑对话框：字段与校验完全跟随源站 createForm 的 controls。 */
@Composable
internal fun NewCollectionDialog(
    form: TopicCollectionActionForm,
    values: Map<String, String>,
    submitting: Boolean,
    onValueChange: (String, String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { if (!submitting) onDismiss() },
        title = { Text("新建专辑") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                form.controls.forEach { control ->
                    if (control.type.equals("checkbox", true)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(control.label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                            Checkbox(
                                checked = values[control.name] == "1",
                                onCheckedChange = { onValueChange(control.name, if (it) "1" else "") },
                            )
                        }
                    } else {
                        OutlinedTextField(
                            value = values[control.name].orEmpty(),
                            onValueChange = { onValueChange(control.name, it) },
                            label = { Text(control.label) },
                            singleLine = control.type != "textarea",
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        },
        dismissButton = { TextButton(enabled = !submitting, onClick = onDismiss) { Text("取消") } },
        confirmButton = {
            Button(
                enabled = !submitting && values["name"].orEmpty().isNotBlank(),
                onClick = onConfirm,
            ) { Text("创建并收录") }
        },
    )
}
