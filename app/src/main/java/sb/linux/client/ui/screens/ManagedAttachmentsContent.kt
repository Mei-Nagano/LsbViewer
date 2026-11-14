package sb.linux.client.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import sb.linux.client.domain.account.*
import sb.linux.client.ui.theme.LocalAppSpacing

/** 文件与其引用和操作在同一行组，批量成功控件仅来自勾选附件。 */
@Composable
internal fun ManagedAttachmentsContent(node: AccountNode.Attachments, enabled: Boolean, onLink: (String) -> Unit,
    onDirty: () -> Unit, onSubmit: (AccountForm, List<Pair<String, String>>) -> Unit) {
    val spacing = LocalAppSpacing.current
    var selected by remember(node) { mutableStateOf(setOf<String>()) }
    var pending by remember(node) { mutableStateOf<PendingAttachmentAction?>(null) }
    val form = node.batchForm
    fun request(sourceForm: AccountForm, button: AccountSubmit, files: List<ManagedAttachment>, batch: Boolean) {
        if (!enabled || !button.enabled) return
        pending = PendingAttachmentAction(sourceForm, button, files, batch)
    }
    Column(verticalArrangement = Arrangement.spacedBy(spacing.item)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(spacing.inline), verticalArrangement = Arrangement.spacedBy(spacing.inline)) {
            node.filters.forEach { link -> FilterChip(link.url == node.activeFilter, { onLink(link.url) }, enabled = enabled,
                shape = CircleShape, label = { Text(link.label) }) }
        }
        form?.buttons?.filter { it.name == "attachment_upload_action" }?.let { buttons ->
            Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerLow) {
                Column(Modifier.fillMaxWidth().padding(spacing.contentGap), verticalArrangement = Arrangement.spacedBy(spacing.inline)) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(spacing.item)) {
                        buttons.forEach { button -> FilledTonalButton(enabled = enabled && button.enabled && selected.isNotEmpty(),
                            onClick = { request(form, button, node.files.filter { it.selector?.value in selected }, batch = true) }) { Text(button.label) } }
                    }
                    form.notes.forEach { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            }
        }
        if (node.files.isEmpty()) AccountSectionCard("附件", "此筛选下暂无附件") {}
        node.files.forEach { file ->
            Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerLow) {
                Column(Modifier.fillMaxWidth().padding(spacing.contentGap), verticalArrangement = Arrangement.spacedBy(spacing.item)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.item)) {
                        if (file.preview.isNotBlank()) Surface(onClick = { onLink(file.url) }, enabled = enabled,
                            shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                            AsyncImage(file.preview, file.name, Modifier.size(width = 72.dp, height = 54.dp), contentScale = ContentScale.Fit)
                        }
                        Column(Modifier.weight(1f)) {
                            Text(file.name, style = MaterialTheme.typography.titleSmall)
                            Text(file.meta, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        file.selector?.let { selector -> Checkbox(selector.value in selected, enabled = enabled && !selector.disabled,
                            onCheckedChange = { checked -> selected = if (checked) selected + selector.value else selected - selector.value; onDirty() }) }
                    }
                    Text(file.referenceText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(spacing.item), verticalArrangement = Arrangement.spacedBy(spacing.inline)) {
                        file.references.forEach { link -> TextButton(onClick = { onLink(link.url) }, enabled = enabled) { Text(link.label) } }
                        if (file.preview.isBlank()) TextButton(onClick = { onLink(file.url) }, enabled = enabled) { Text("打开附件") }
                        file.actions.forEach { action -> OutlinedButton(enabled = enabled && action.button.enabled,
                            onClick = { request(action.form, action.button, listOf(file), batch = false) }) { Text(action.button.label) } }
                    }
                }
            }
        }
    }
    pending?.let { (sourceForm, button, files, batch) ->
        AlertDialog(onDismissRequest = { pending = null }, title = { Text(button.label) },
            text = { Text("${files.size} 个附件\n" + sourceForm.confirmation.ifBlank { "确认执行此附件操作？" }) },
            confirmButton = { TextButton(onClick = {
                pending = null
                val pairs = sourceForm.hidden + (if (batch) files.mapNotNull { it.selector?.let { field -> field.name to field.value } } else emptyList()) +
                    listOf(button.name to button.value).filter { it.first.isNotBlank() }
                onSubmit(sourceForm.copy(action = button.action.ifBlank { sourceForm.action },
                    method = button.method.ifBlank { sourceForm.method }), pairs)
            }, enabled = enabled) { Text("确认") } },
            dismissButton = { TextButton(onClick = { pending = null }) { Text("取消") } })
    }
}

private data class PendingAttachmentAction(val form: AccountForm, val button: AccountSubmit,
    val files: List<ManagedAttachment>, val batch: Boolean)
