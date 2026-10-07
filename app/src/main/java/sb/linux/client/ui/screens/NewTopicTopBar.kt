package sb.linux.client.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NewTopicTopBar(
    form: NewTopicFormState,
    editId: Long,
    nav: NavHostController,
    hasSavedDraft: Boolean,
    body: String,
    hasDraftContent: Boolean,
    fullScreenEditor: Boolean,
    onFullScreenEditorChange: (Boolean) -> Unit,
    previewMode: Boolean,
    onPreviewModeChange: (Boolean) -> Unit,
    onSaveDraft: () -> Unit,
    onSubmit: () -> Unit,
) {
            TopAppBar(
                title = { Text(if (editId > 0) "编辑主题" else "发布新主题") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (fullScreenEditor) onFullScreenEditorChange(false)
                        else if (editId == 0L && hasDraftContent) form.exitDraftOpen = true
                        else nav.popBackStack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
                actions = {
                    if (!previewMode) {
                        IconButton(onClick = { onFullScreenEditorChange(!fullScreenEditor) }) {
                            Icon(
                                if (fullScreenEditor) Icons.Filled.FullscreenExit else Icons.Filled.Fullscreen,
                                if (fullScreenEditor) "退出全屏编辑" else "全屏编辑",
                            )
                        }
                    }
                    if (editId == 0L) {
                        Box {
                            IconButton(onClick = { form.draftMenuOpen = true }) { Icon(Icons.Filled.Save, "草稿箱") }
                            DropdownMenu(expanded = form.draftMenuOpen, onDismissRequest = { form.draftMenuOpen = false }) {
                                DropdownMenuItem(text = { Text("保存当前草稿") }, enabled = hasDraftContent, onClick = { onSaveDraft(); form.draftMenuOpen = false })
                                DropdownMenuItem(text = { Text("恢复已保存草稿") }, enabled = hasSavedDraft, onClick = { form.draftMenuOpen = false; form.restoreDraftOpen = true })
                            }
                        }
                    }
                    // 正文 Markdown 实时预览开关
                    if (!fullScreenEditor) IconButton(onClick = { onPreviewModeChange(!previewMode) }) {
                        Icon(
                            if (previewMode) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            if (previewMode) "退出预览" else "预览"
                        )
                    }
                    // 发布前先弹公告确认（每次都要确认）
                    TextButton(
                        onClick = {
                            if (form.title.isBlank() || body.isBlank()) {
                                form.error = "标题和内容不能为空"; return@TextButton
                            }
                            if (form.forumId == null) { form.error = "请选择版块"; return@TextButton }
                            when (form.specialType) {
                                "lottery" -> {
                                    if (form.drawAt.isBlank()) { form.error = "请填写开奖时间"; return@TextButton }
                                    if (form.prizes.none { it.name.isNotBlank() }) {
                                        form.error = "请至少添加一项奖品"; return@TextButton
                                    }
                                }
                                "virtual_card" -> {
                                    if (form.cardName.isBlank()) { form.error = "请填写卡片名称"; return@TextButton }
                                    if (form.cardValues.isBlank()) { form.error = "请添加卡密"; return@TextButton }
                                }
                            }
                            onSubmit()
                        },
                        enabled = !form.busy
                    ) { Text(if (form.busy) "提交中…" else if (editId > 0) "保存" else "发布") }
                }
            )
}
