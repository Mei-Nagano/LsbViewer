package sb.linux.client.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController

@Composable
internal fun NewTopicDialogs(
    form: NewTopicFormState,
    editId: Long,
    nav: NavHostController,
    hasDraftContent: Boolean,
    onRestoreDraft: () -> Unit,
    onSaveDraft: () -> Unit,
    onClearDraft: () -> Unit,
    onSubmit: () -> Unit,
) {
    if (form.restoreDraftOpen) {
        AlertDialog(
            onDismissRequest = { form.restoreDraftOpen = false },
            title = { Text("发现未发布草稿") },
            text = { Text("是否恢复标题、正文、版块及抽奖/发卡配置？") },
            confirmButton = { TextButton(onClick = {
                onRestoreDraft()
                form.restoreDraftOpen = false
            }) { Text("恢复") } },
            dismissButton = { TextButton(onClick = { onClearDraft(); form.restoreDraftOpen = false }) { Text("丢弃") } }
        )
    }
    if (form.exitDraftOpen) {
        AlertDialog(
            onDismissRequest = { form.exitDraftOpen = false },
            title = { Text("保存到草稿箱？") },
            text = { Text("当前内容尚未发布，可以保存后下次继续编辑。") },
            confirmButton = { TextButton(onClick = { onSaveDraft(); form.exitDraftOpen = false; nav.popBackStack() }) { Text("保存并退出") } },
            dismissButton = {
                Row {
                    TextButton(onClick = { form.exitDraftOpen = false }) { Text("取消") }
                    TextButton(onClick = { onClearDraft(); form.exitDraftOpen = false; nav.popBackStack() }) { Text("不保存") }
                }
            }
        )
    }

    // 发帖前确认弹窗：展示源站发帖公告，每次发布前都要确认
    if (form.confirmOpen) {
        AlertDialog(
            onDismissRequest = { form.confirmOpen = false },
            title = {
                Text(
                    if (editId > 0) "确认编辑"
                    else if (form.announcement.isNotBlank()) "发帖公告"
                    else "确认发布"
                )
            },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    val editMessage = listOf(form.sourceEditMeta.notice, form.sourceEditMeta.confirmation)
                        .filter { it.isNotBlank() }.distinct().joinToString("\n\n")
                    if (editId > 0 && editMessage.isNotBlank()) {
                        Text(editMessage, style = MaterialTheme.typography.bodyMedium)
                    } else if (form.announcement.isNotBlank()) {
                        Text(form.announcement, style = MaterialTheme.typography.bodyMedium)
                    } else {
                        Text(
                            "即将发布到「${form.forums.firstOrNull { it.id == form.forumId }?.name ?: "未选择"}」版块。" +
                                "请确认内容符合社区规则：不发布违规、广告或侵权内容。",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { form.confirmOpen = false; onSubmit() }, enabled = !form.busy) {
                    Text(if (form.busy) "提交中…" else if (editId > 0) "确认保存" else "确认发布")
                }
            },
            dismissButton = {
                TextButton(onClick = { form.confirmOpen = false }) { Text("取消") }
            }
        )
    }

}
