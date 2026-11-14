package sb.linux.client.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import sb.linux.client.domain.topic.SourceEditorPage
import sb.linux.client.ui.content.MarkdownEditorTools
import sb.linux.client.ui.content.HtmlContent
import sb.linux.client.ui.session.Session

@Composable
internal fun SourceBodyField(session: Session, page: SourceEditorPage, text: String,
    onChange: (String) -> Unit, enabled: Boolean, onBusy: (Boolean) -> Unit, onSubmit: () -> Unit = {}) {
    var body by rememberSaveable(page.url, session.accountIdentityVersion, stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue(text)) }
    var preview by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    var tool by remember { mutableStateOf<String?>(null) }
    var previewHtml by remember { mutableStateOf("") }
    var previewError by remember { mutableStateOf<String?>(null) }
    var previewLoading by remember { mutableStateOf(false) }
    var videoBusy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    fun update(value: TextFieldValue) { body = value; onChange(value.text) }
    val imageUpload = rememberEditorImageUpload(session) { update(insertReplyImage(body, it)) }
    val media = rememberEditorMedia(session, page.media,
        onInsert = { update(insertSourceEditorBlock(body, it)) },
        onPaste = { pasted ->
            val previous = body
            update(replaceReplySelection(body, pasted))
            val undo: () -> Unit = { update(previous) }
            undo
        },
        onTransferred = { old, new ->
            val changed = body.text.replace(old, new)
            update(TextFieldValue(changed, androidx.compose.ui.text.TextRange(body.selection.end.coerceAtMost(changed.length))))
        })
    val uploadBusy = videoBusy || media.busy || imageUpload.uploading
    LaunchedEffect(Unit) { if (body.text != text) onChange(body.text) }
    LaunchedEffect(uploadBusy) { onBusy(uploadBusy) }
    LaunchedEffect(preview, body.text, page, session.accountIdentityVersion) {
        if (!preview) return@LaunchedEffect
        delay(400)
        previewLoading = true
        try {
            previewHtml = if (body.text.isBlank()) "" else session.sourceEditorRepository.preview(page.previewUrl,
                page.media.csrf, body.text, page.editorKind, page.topicId)
            previewError = null
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (error: Exception) { previewError = error.message ?: "预览失败" }
        finally { previewLoading = false }
    }
    val editorContent: @Composable () -> Unit = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (enabled && !uploadBusy) MarkdownEditorTools(
                onInsert = { before, after, placeholder -> update(applySourceEditorMarkup(body, before, after, placeholder)) },
                onUpload = imageUpload.choose, onAdvanced = { tool = it })
            Row {
                TextButton(onClick = { preview = !preview }, enabled = page.previewUrl.isNotBlank()) { Text(if (preview) "关闭预览" else "实时预览") }
                TextButton(onClick = { expanded = !expanded }) { Text(if (expanded) "收起" else "全屏") }
            }
            SourceEditorMediaControls(media, page.media, enabled && !imageUpload.uploading && !videoBusy)
            if (imageUpload.uploading || videoBusy) LinearProgressIndicator(Modifier.fillMaxWidth())
            OutlinedTextField(body, { update(continueSourceEditorLine(body, it)) }, Modifier.fillMaxWidth().heightIn(min = 200.dp).sourceEditorKeys(body, enabled && !uploadBusy, ::update, onSubmit),
                label = { Text("内容") }, enabled = enabled && !uploadBusy, shape = MaterialTheme.shapes.large)
            MentionSuggestions(session, page.mentionUrl, body, ::update, enabled && !uploadBusy)
            if (preview) {
                HorizontalDivider()
                if (previewLoading) LinearProgressIndicator(Modifier.fillMaxWidth())
                previewError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                HtmlContent(previewHtml, cacheable = false)
            }
            Text("附件上传至附件空间，图片可选择上传方式", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    if (expanded) Dialog(onDismissRequest = { expanded = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.fillMaxSize().imePadding().systemBarsPadding().verticalScroll(rememberScrollState()).padding(16.dp)) { editorContent() }
        }
    } else editorContent()
    tool?.let { action ->
        SourceEditorInsertDialog(action, body.text.substring(body.selection.min, body.selection.max), onDismiss = { tool = null },
            onInsert = { update(replaceReplySelection(body, it)); tool = null }, onVideo = { share ->
                tool = null
                val identity = session.accountIdentityVersion
                videoBusy = true
                scope.launch {
                    try {
                        val markup = session.sourceEditorRepository.video(page.videoUrl,
                            page.form.hidden.firstOrNull { it.first == "_csrf" }?.second.orEmpty(), share, page.editorKind)
                        if (identity == session.accountIdentityVersion) update(replaceReplySelection(body, markup))
                    } catch (cancelled: CancellationException) { throw cancelled }
                    catch (error: Exception) { session.showToast(error.message ?: "视频解析失败") }
                    finally { videoBusy = false }
                }
            })
    }
}
