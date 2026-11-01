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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import sb.linux.client.common.collection.TopicCollectionActionForm
import sb.linux.client.common.collection.TopicCollectionPicker
import sb.linux.client.common.collection.TopicCollectionPickerOption
import sb.linux.client.common.collection.actionFor
import sb.linux.client.ui.session.Session
import sb.linux.client.ui.Badge
import sb.linux.client.ui.components.feedback.ErrorBox
import sb.linux.client.ui.theme.LocalAppSpacing
import sb.linux.client.ui.components.feedback.EmptyBox
import sb.linux.client.ui.feature.collection.TopicCollectionPickerViewModel
import sb.linux.client.ui.feature.collection.TopicCollectionPickerViewModelFactory

/**
 * 帖子页「收录到淘帖」底部弹窗：专辑列表直接可见，点一行即向源站提交收录/移出。
 * 收录可逆，不加二次确认；「全部取消收录」是批量不可逆操作，保留确认对话框。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopicCollectionSheet(session: Session, topicPath: String, onDismiss: () -> Unit) {
    val factory = remember(session.topicCollectionRepository) {
        TopicCollectionPickerViewModelFactory(session.topicCollectionRepository)
    }
    val viewModel: TopicCollectionPickerViewModel = viewModel(factory = factory)
    val state by viewModel.uiState.collectAsState()
    val picker = state.picker
    val loading = state.loading
    val error = state.error
    val submitting = state.submittingOwner
    val createValues = state.createValues
    // 正在提交的专辑 id（0 = 新建/全部取消这类无专辑归属的操作），用于禁用该行并显示进度
    var createOpen by remember { mutableStateOf(false) }
    var confirmRemoveAll by remember { mutableStateOf<TopicCollectionActionForm?>(null) }
    LaunchedEffect(topicPath, viewModel) { viewModel.load(topicPath) }
    LaunchedEffect(state.actionMessage) {
        state.actionMessage?.let { session.showToast(it); viewModel.clearActionMessage() }
    }

    /** 提交一张源站表单，成功后重新读取源站状态；弹窗保持打开以便连续收录。 */
    fun submit(form: TopicCollectionActionForm, owner: Long, successMessage: String) {
        if (submitting != null) return
        viewModel.submit(form, owner, successMessage, topicPath)
    }

    val createForm = picker?.createForm
    if (createOpen && createForm != null) {
        NewCollectionDialog(
            form = createForm,
            values = createValues,
            submitting = submitting != null,
            onValueChange = viewModel::updateCreateValue,
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
        sb.linux.client.ui.components.feedback.AppConfirmDialog(
            title = "全部取消收录", message = "将把本帖从所有专辑中移出，此操作无法撤回。",
            enabled = submitting == null, destructive = true,
            onDismiss = { confirmRemoveAll = null },
            onConfirm = {
                confirmRemoveAll = null
                submit(form, owner = 0L, successMessage = "已全部取消收录")
            },
        )
    }

    ModalBottomSheet(onDismissRequest = { if (submitting == null) onDismiss() }) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = LocalAppSpacing.current.pageHorizontal)
                .padding(bottom = LocalAppSpacing.current.pageVertical),
            verticalArrangement = Arrangement.spacedBy(LocalAppSpacing.current.contentGap),
        ) {
            Text("收录到淘帖", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            val current = picker
            when {
                loading -> Box(
                    Modifier.fillMaxWidth().height(180.dp),
                    contentAlignment = Alignment.Center,
                ) { sb.linux.client.ui.components.feedback.LoadingBox() }
                error != null && current == null -> Box(Modifier.fillMaxWidth().height(220.dp)) {
                    ErrorBox(error!!) { viewModel.load(topicPath) }
                }
                current == null -> Box(Modifier.fillMaxWidth().height(220.dp)) {
                    EmptyBox("源站未提供淘帖收录面板")
                }
                else -> {
                    // 保留旧列表时也要展示重载失败，避免操作后复选状态看起来像没有变化。
                    error?.let { message ->
                        sb.linux.client.ui.components.feedback.AppNotice(message, onAction = { viewModel.load(topicPath) })
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
