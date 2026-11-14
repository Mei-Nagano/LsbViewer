package sb.linux.client.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import sb.linux.client.data.SourceAttachmentHistoryStore
import sb.linux.client.data.SourceUploadedAttachment
import sb.linux.client.domain.topic.SourceEditorMedia
import sb.linux.client.ui.session.Session

@Stable
internal class EditorMediaActions {
    var original by mutableStateOf(false)
    var busy by mutableStateOf(false)
    var status by mutableStateOf("")
    var historyVisible by mutableStateOf(false)
    val history = mutableStateListOf<SourceUploadedAttachment>()
    val inserted = mutableStateListOf<String>()
    var choose: () -> Unit = {}
    var paste: () -> Unit = {}
    var insertPending: () -> Unit = {}
    var remove: (SourceUploadedAttachment) -> Unit = {}
    var undoPaste by mutableStateOf<(() -> Unit)?>(null)
}

@Composable
internal fun rememberEditorMedia(session: Session, media: SourceEditorMedia, onInsert: (String) -> Unit,
    onPaste: (String) -> (() -> Unit), onTransferred: (String, String) -> Unit): EditorMediaActions {
    val context = LocalContext.current
    val userId = session.account.loginState.userId
    val owner = session.accountIdentityVersion
    val store = remember(context) { SourceAttachmentHistoryStore(context) }
    val state = remember(owner, media.uploadUrl) { EditorMediaActions().apply { history.addAll(store.read(userId)) } }
    val insert by rememberUpdatedState(onInsert)
    val pasteText by rememberUpdatedState(onPaste)
    val transferred by rememberUpdatedState(onTransferred)
    val scope = rememberCoroutineScope()
    var pickerOwner by remember { mutableLongStateOf(-1L) }
    fun save() { if (owner == session.accountIdentityVersion) store.write(userId, state.history.toList()) }
    fun upload(uris: List<android.net.Uri>, pasteImages: Boolean = false) {
        if (uris.isEmpty() || state.busy) return
        state.busy = true
        val original = state.original
        scope.launch {
            try {
                for ((index, uri) in uris.distinct().withIndex()) {
                    if (owner != session.accountIdentityVersion) break
                    state.status = "正在上传 ${index + 1}/${uris.distinct().size}"
                    try {
                        val result = uploadSourceAttachment(context, session, media, uri, original, owner,
                            if (pasteImages) emptySet() else state.history.map { it.key }.toSet())
                        if (owner != session.accountIdentityVersion) break
                        if (result == null) { session.showToast("已忽略重复附件"); continue }
                        state.history.removeAll { it.key == result.key }
                        state.history.add(result)
                        while (state.history.size > 100) state.history.removeAt(0)
                        save()
                        state.historyVisible = true
                        if (pasteImages) { insert(result.markdown); state.inserted.add(result.key) }
                    } catch (cancelled: CancellationException) { throw cancelled }
                    catch (error: Exception) { session.showToast(error.message ?: "附件上传失败") }
                }
            } finally { state.busy = false; state.status = "" }
        }
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (pickerOwner == session.accountIdentityVersion && pickerOwner == owner) upload(uris)
        else if (uris.isNotEmpty()) session.showToast("账号已变化，请重新选择附件")
    }
    state.choose = { pickerOwner = owner; picker.launch(arrayOf("*/*")) }
    state.insertPending = {
        val pending = state.history.filter { it.key !in state.inserted }
        if (pending.isNotEmpty()) { insert(pending.joinToString("\n") { it.markdown }); state.inserted.addAll(pending.map { it.key }) }
    }
    state.remove = { state.history.remove(it); state.inserted.remove(it.key); save() }
    state.paste = {
        if (!state.busy) {
            val clip = readEditorClipboard(context)
            if (clip == null) session.showToast("剪贴板为空")
            else if (clip.images.isNotEmpty() && media.uploadUrl.isNotBlank()) upload(clip.images, pasteImages = true)
            else if (clip.markdown.isBlank()) session.showToast("剪贴板中没有可粘贴的文字")
            else {
                state.undoPaste = pasteText(clip.markdown)
                state.busy = true
                scope.launch {
                    try {
                        val failed = transferEditorClipboardImages(session, media, context, clip.markdown, owner, transferred)
                        if (failed > 0) session.showToast("$failed 张图片转存失败，已保留原链接")
                    } finally { state.busy = false }
                }
            }
        }
    }
    return state
}

@Composable
internal fun SourceEditorMediaControls(state: EditorMediaActions, media: SourceEditorMedia, enabled: Boolean) {
    val context = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            if (media.uploadUrl.isNotBlank()) TextButton(state.choose, enabled = enabled && !state.busy) { Text("上传附件") }
            TextButton(state.paste, enabled = enabled && !state.busy) { Text("粘贴保留格式") }
            if (state.undoPaste != null) TextButton(onClick = { state.undoPaste?.invoke(); state.undoPaste = null }, enabled = enabled && !state.busy) { Text("撤销粘贴") }
            if (media.uploadUrl.isNotBlank()) TextButton({ state.historyVisible = !state.historyVisible }) {
                Text("上传列表（${state.history.size}）")
            }
        }
        if (media.uploadUrl.isNotBlank()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(state.original, { state.original = it }, enabled = enabled && !state.busy)
                Text("原图上传（不压缩、不转 WebP）", style = MaterialTheme.typography.bodySmall)
            }
            if (state.historyVisible && state.history.isNotEmpty()) {
                val pending = state.history.count { it.key !in state.inserted }
                FilledTonalButton(state.insertPending, enabled = enabled && !state.busy && pending > 0, modifier = Modifier.fillMaxWidth()) {
                    Text(if (pending > 0) "将附件插入正文（$pending）" else "已全部插入正文")
                }
                Column(Modifier.heightIn(max = 240.dp).verticalScroll(rememberScrollState())) {
                    state.history.asReversed().forEach { item ->
                        ListItem(headlineContent = { Text(item.name) }, supportingContent = {
                            Text(if (item.key in state.inserted) "已插入正文" else "已上传 · 可复制 Markdown")
                        }, trailingContent = {
                            Row {
                                IconButton(onClick = {
                                    (context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager)
                                        .setPrimaryClip(android.content.ClipData.newPlainText(item.name, item.markdown))
                                }) { Icon(Icons.Default.ContentCopy, "复制附件 Markdown") }
                                IconButton(onClick = { state.remove(item) }, enabled = !state.busy) { Icon(Icons.Default.Close, "不插入此附件") }
                            }
                        })
                    }
                }
            }
        }
        if (state.busy) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
            if (state.status.isNotBlank()) Text(state.status, style = MaterialTheme.typography.labelSmall)
        }
    }
}
