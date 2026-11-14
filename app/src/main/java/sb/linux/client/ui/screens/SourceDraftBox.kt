package sb.linux.client.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import sb.linux.client.domain.account.AccountField
import sb.linux.client.domain.topic.SourceEditorDraft
import sb.linux.client.ui.theme.LocalAppSpacing
import java.text.DateFormat
import java.util.Date

@Composable
internal fun SourceDraftBox(
    drafts: List<SourceEditorDraft>, canSave: Boolean, onSave: () -> Unit,
    onRestore: (SourceEditorDraft) -> Unit, onDelete: (SourceEditorDraft) -> Unit, onClose: () -> Unit,
) {
    var deleting by remember { mutableStateOf<SourceEditorDraft?>(null) }
    val spacing = LocalAppSpacing.current
    AlertDialog(onDismissRequest = onClose, title = { Text("草稿箱") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.contentGap)) {
            Text("保存在当前设备，按账号和编辑目标区分，保留 7 天。",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (drafts.isEmpty()) Text("暂无草稿")
            LazyColumn(Modifier.heightIn(max = 360.dp), verticalArrangement = Arrangement.spacedBy(spacing.item)) {
                items(drafts, key = SourceEditorDraft::id) { draft ->
                    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerLow) {
                        Column(Modifier.fillMaxWidth().padding(spacing.contentGap),
                            verticalArrangement = Arrangement.spacedBy(spacing.inline)) {
                            Text(draft.title.ifBlank { "未命名草稿" }, style = MaterialTheme.typography.titleSmall,
                                maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                            Text((if (draft.automatic) "自动保存 · " else "") +
                                DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(draft.updatedAt)),
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (draft.body.isNotBlank()) Text(draft.body, maxLines = 2,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Row {
                                TextButton(onClick = { onRestore(draft) }) { Text("恢复") }
                                TextButton(onClick = { deleting = draft }) { Text("删除") }
                            }
                        }
                    }
                }
            }
        }
    }, confirmButton = { TextButton(onClick = onSave, enabled = canSave) { Text("保存当前草稿") } },
        dismissButton = { TextButton(onClick = onClose) { Text("关闭") } })
    deleting?.let { draft ->
        AlertDialog(onDismissRequest = { deleting = null }, title = { Text("删除草稿？") },
            text = { Text(draft.title.ifBlank { "未命名草稿" }) },
            confirmButton = { TextButton(onClick = { deleting = null; onDelete(draft) }) { Text("删除") } },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("取消") } })
    }
}

internal fun restoreSourceDraftFields(fields: List<AccountField>, draft: SourceEditorDraft): List<AccountField> {
    val grouped = draft.fields.groupBy({ it.first }, { it.second })
    val positions = mutableMapOf<String, Int>()
    val prizes = fields.filter { it.name.startsWith("lottery_prize_") }.distinctBy { it.name }
    val missing = (grouped["lottery_prize_name[]"].orEmpty().size - fields.count { it.name == "lottery_prize_name[]" }).coerceAtLeast(0)
    val expanded = fields + (0 until missing).flatMap { prizes }
    return expanded.map { field ->
        val saved = grouped[field.name] ?: return@map field
        when {
            field.type == "radio" -> field.copy(checked = field.value in saved)
            field.type == "select" && field.multiple -> field.copy(options = field.options.map { it.copy(selected = it.value in saved) })
            else -> {
                val index = positions.getOrDefault(field.name, 0)
                positions[field.name] = index + 1
                val value = saved.getOrNull(index) ?: return@map field
                when (field.type) {
                    "checkbox" -> field.copy(checked = value.isNotBlank() && value == field.value)
                    "select" -> field.copy(options = field.options.map { it.copy(selected = it.value == value) })
                    else -> field.copy(value = value)
                }
            }
        }
    }
}
