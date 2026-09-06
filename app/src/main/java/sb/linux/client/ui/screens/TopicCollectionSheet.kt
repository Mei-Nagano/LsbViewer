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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
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
import sb.linux.client.common.collection.TopicCollectionActionForm
import sb.linux.client.common.collection.TopicCollectionPicker
import sb.linux.client.common.collection.TopicCollectionPickerOption
import sb.linux.client.common.collection.actionFor
import sb.linux.client.data.Session
import sb.linux.client.ui.Badge
import sb.linux.client.ui.ErrorBox
import sb.linux.client.ui.EmptyBox

/**
 * 帖子页「收录到淘帖」底部弹窗：专辑列表直接可见，点一行即向源站提交收录/移出。
 * 收录可逆，不加二次确认；「全部取消收录」是批量不可逆操作，保留确认对话框。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopicCollectionSheet(session: Session, topicPath: String, onDismiss: () -> Unit) {
    var picker by remember { mutableStateOf<TopicCollectionPicker?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    // 正在提交的专辑 id（0 = 新建/全部取消这类无专辑归属的操作），用于禁用该行并显示进度
    var submitting by remember { mutableStateOf<Long?>(null) }
    var createOpen by remember { mutableStateOf(false) }
    var createValues by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var confirmRemoveAll by remember { mutableStateOf<TopicCollectionActionForm?>(null) }
    val scope = rememberCoroutineScope()

    suspend fun reload(): Boolean {
        error = null
        try {
            val next = session.topicCollectionService.picker(topicPath)
            picker = next
            createValues = next?.createForm?.controls.orEmpty().associate { it.name to it.value }
            return true
        } catch (e: Exception) {
            error = e.message ?: "加载失败"
            return false
        }
    }

    LaunchedEffect(topicPath) {
        loading = true
        reload()
        loading = false
    }

    /** 提交一张源站表单，成功后重新读取源站状态；弹窗保持打开以便连续收录。 */
    fun submit(form: TopicCollectionActionForm, owner: Long, successMessage: String) {
        if (submitting != null) return
        submitting = owner
        scope.launch {
            try {
                session.topicCollectionService.execute(form)
                val refreshed = reload()
                session.showToast(if (refreshed) successMessage else "$successMessage，刷新状态失败")
            } catch (e: Exception) {
                session.showToast(e.message ?: "提交失败")
            } finally {
                submitting = null
            }
        }
    }

    val createForm = picker?.createForm
    if (createOpen && createForm != null) {
        NewCollectionDialog(
            form = createForm,
            values = createValues,
            submitting = submitting != null,
            onValueChange = { name, value -> createValues = createValues + (name to value) },
            onDismiss = { createOpen = false },
            onConfirm = {
                val controlNames = createForm.controls.map { it.name }.toSet()
                val filled = createValues.filterValues { it.isNotEmpty() }.toList()
                createOpen = false
                submit(
                    form = createForm.copy(fields = createForm.fields.filterNot { it.first in controlNames } + filled),
                    owner = 0L,
                    successMessage = "已创建并收录",
                )
            },
        )
    }

    confirmRemoveAll?.let { form ->
        AlertDialog(
            onDismissRequest = { if (submitting == null) confirmRemoveAll = null },
            title = { Text("全部取消收录") },
            text = { Text("将把本帖从所有专辑中移出，此操作无法撤回。") },
            dismissButton = {
                TextButton(enabled = submitting == null, onClick = { confirmRemoveAll = null }) { Text("取消") }
            },
            confirmButton = {
                Button(
                    enabled = submitting == null,
                    onClick = {
                        confirmRemoveAll = null
                        submit(form, owner = 0L, successMessage = "已全部取消收录")
                    },
                ) { Text("确认") }
            },
        )
    }

    ModalBottomSheet(onDismissRequest = { if (submitting == null) onDismiss() }) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("收录到淘帖", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            val current = picker
            when {
                loading -> Box(
                    Modifier.fillMaxWidth().height(180.dp),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator() }
                error != null && current == null -> Box(Modifier.fillMaxWidth().height(220.dp)) {
                    ErrorBox(error!!) { scope.launch { loading = true; reload(); loading = false } }
                }
                current == null -> Box(Modifier.fillMaxWidth().height(220.dp)) {
                    EmptyBox("源站未提供淘帖收录面板")
                }
                else -> {
                    // 保留旧列表时也要展示重载失败，避免操作后复选状态看起来像没有变化。
                    error?.let { message ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.errorContainer,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                Modifier.padding(start = 12.dp, end = 4.dp, top = 3.dp, bottom = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    message,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.weight(1f),
                                )
                                TextButton(onClick = {
                                    scope.launch { loading = true; reload(); loading = false }
                                }) { Text("重试") }
                            }
                        }
                    }
                    // 提交进行中：整块列表保留，仅顶部出进度条，避免选中态闪烁
                    if (submitting != null) LinearProgressIndicator(Modifier.fillMaxWidth())
                    if (current.options.isEmpty()) {
                        Text(
                            "还没有可收录的专辑，先新建一个",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        LazyColumn(
                            Modifier.fillMaxWidth().heightIn(max = 340.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            items(current.options, key = { it.collectionId }) { option ->
                                CollectionOptionRow(
                                    option = option,
                                    enabled = submitting == null,
                                    onToggle = {
                                        val form = current.actionFor(option)
                                        if (form == null) session.showToast("源站未提供收录表单，请刷新重试")
                                        else submit(
                                            form = form,
                                            owner = option.collectionId,
                                            successMessage = if (option.included) "已移出专辑" else "已收录",
                                        )
                                    },
                                )
                            }
                        }
                    }
                    current.createForm?.let {
                        SheetActionRow(
                            icon = Icons.Filled.Add,
                            label = "新建专辑并收录",
                            enabled = submitting == null,
                            onClick = { createOpen = true },
                        )
                    }
                    current.removeAllForm?.let { form ->
                        SheetActionRow(
                            icon = Icons.Filled.DeleteSweep,
                            label = "全部取消收录",
                            enabled = submitting == null && current.options.any { it.included },
                            onClick = { confirmRemoveAll = form },
                        )
                    }
                }
            }
        }
    }
}

/** 单个专辑行：复选框反映源站 included 状态，整行可点。 */
@Composable
private fun CollectionOptionRow(
    option: TopicCollectionPickerOption,
    enabled: Boolean,
    onToggle: () -> Unit,
) {
    Surface(
        onClick = onToggle,
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
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
private fun SheetActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
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
private fun NewCollectionDialog(
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
