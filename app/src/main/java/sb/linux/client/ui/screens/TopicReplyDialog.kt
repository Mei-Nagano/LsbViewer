package sb.linux.client.ui.screens

import sb.linux.client.common.content.HtmlTextConversion
import sb.linux.client.ui.session.Session
import sb.linux.client.ui.theme.LocalAppSpacing


import android.view.WindowManager
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import sb.linux.client.ui.content.HtmlContent
import sb.linux.client.domain.account.LoginVerification
/** 回复编辑：底部抽屉，预填引用内容，Markdown 工具栏 + 人机验证（照搬源站编辑器）。
 *  title/submitLabel/showToolbar 可定制：私信等场景复用此编辑器但隐藏 Markdown 工具栏。
 *  抽屉内为紧凑输入框（内嵌「展开」按钮）；点击展开进入全屏编辑（3.21） */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReplyDialog(
    session: Session,
    initial: String,
    captcha: LoginVerification?,
    onDismiss: () -> Unit,
    onSubmit: (String, String, (Boolean) -> Unit) -> Unit,
    title: String = "发表回复",
    submitLabel: String = "发布",
    showToolbar: Boolean = true,
    onRefreshCaptcha: (() -> Unit)? = null,
    mentionUrl: String = "",
    submissionNotice: String = "",
    previewUrl: String = "", previewCsrf: String = "", previewTopicId: Long = 0,
    editorMedia: sb.linux.client.domain.topic.SourceEditorMedia = sb.linux.client.domain.topic.SourceEditorMedia(),
    maxBodyLength: Int = Int.MAX_VALUE,
) {
    val context = LocalContext.current
    val draftStore = remember(context) { sb.linux.client.data.SourceReplyDraftStore(context) }
    val owner = session.accountIdentityVersion
    val userId = session.account.loginState.userId
    val priorDraft = remember(owner, previewTopicId) { draftStore.read(userId, previewTopicId) }
    var restoreDraft by remember { mutableStateOf(false) }
    var draftRestored by remember(owner, previewTopicId) { mutableStateOf(initial.isBlank()) }
    var body by remember(initial, owner) {
        val text = if (initial.isBlank()) priorDraft ?: initial else initial
        mutableStateOf(TextFieldValue(text, selection = TextRange(text.length)))
    }
    var answer by remember { mutableStateOf("") }
    var captchaRevision by remember { mutableIntStateOf(0) }
    var busy by remember { mutableStateOf(false) }
    var published by remember(owner, previewTopicId) { mutableStateOf(false) }
    LaunchedEffect(body.text, busy, owner) {
        if (previewTopicId <= 0 || busy || published) return@LaunchedEffect
        delay(500)
        if (owner == session.accountIdentityVersion) draftStore.save(userId, previewTopicId, body.text)
    }
    val latestBody by rememberUpdatedState(body.text)
    DisposableEffect(owner, previewTopicId) {
        onDispose { if (previewTopicId > 0 && !published && owner == session.accountIdentityVersion) draftStore.save(userId, previewTopicId, latestBody) }
    }
    var expanded by remember { mutableStateOf(false) }
    var previewReply by remember { mutableStateOf(false) }
    var tool by remember { mutableStateOf<String?>(null) }
    var previewHtml by remember { mutableStateOf("") }
    var previewError by remember { mutableStateOf<String?>(null) }
    var previewLoading by remember { mutableStateOf(false) }
    LaunchedEffect(previewReply, body.text, previewUrl, session.accountIdentityVersion) {
        if (!previewReply || previewUrl.isBlank()) return@LaunchedEffect
        delay(400)
        previewLoading = true
        try {
            previewHtml = session.sourceEditorRepository.preview(previewUrl, previewCsrf, body.text, "reply", previewTopicId.toString())
            previewError = null
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (error: Exception) { previewError = error.message ?: "预览失败" }
        finally { previewLoading = false }
    }
    tool?.let { action ->
        SourceEditorInsertDialog(action, body.text.substring(body.selection.min, body.selection.max), onDismiss = { tool = null },
            onInsert = { body = replaceReplySelection(body, it); tool = null }, onVideo = { tool = null })
    }
    val imageUpload = rememberEditorImageUpload(session) { body = insertReplyImage(body, it) }
    val media = rememberEditorMedia(session, editorMedia,
        onInsert = { body = insertSourceEditorBlock(body, it) },
        onPaste = { pasted ->
            val previous = body
            body = replaceReplySelection(body, pasted)
            val undo: () -> Unit = { body = previous }
            undo
        },
        onTransferred = { old, new ->
            val changed = body.text.replace(old, new)
            body = TextFieldValue(changed, TextRange(body.selection.end.coerceAtMost(changed.length)))
        })
    val imageUploadBusy = imageUpload.uploading || media.busy
    val canSubmit = body.text.isNotBlank() && body.text.length <= maxBodyLength && !busy && !imageUploadBusy && (captcha == null || answer.isNotBlank())
    LaunchedEffect(captcha) {
        if (answer.isNotBlank()) answer = ""
        captchaRevision++
    }
    fun chooseImage() {
        imageUpload.choose()
    }

    fun submit() {
        busy = true
        onSubmit(body.text, answer) { succeeded ->
            if (succeeded && owner == session.accountIdentityVersion) { published = true; draftStore.remove(userId, previewTopicId) }
            busy = false
            if (!succeeded && captcha is sb.linux.client.domain.account.LoginVerification.Cap) {
                answer = ""
                captchaRevision++
            }
        }
    }

    if (restoreDraft) AlertDialog(onDismissRequest = { restoreDraft = false }, title = { Text("恢复回复草稿？") },
        text = { Text("将用之前的草稿替换当前输入。") },
        confirmButton = { TextButton(onClick = {
            priorDraft?.let { body = TextFieldValue(it, TextRange(it.length)) }
            draftRestored = true; restoreDraft = false
        }) { Text("恢复") } }, dismissButton = { TextButton(onClick = { restoreDraft = false }) { Text("取消") } })

    if (expanded) {
        Dialog(
            onDismissRequest = { if (!busy) expanded = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            val dialogView = LocalView.current
            LaunchedEffect(dialogView) {
                @Suppress("DEPRECATION") // Dialog window 上 ADJUST_RESIZE 仍是让 IME insets 生效的可靠手段
                (dialogView.parent as? DialogWindowProvider)?.window?.setSoftInputMode(
                    WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
                )
            }
            Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .imePadding()
                        .navigationBarsPadding()
                        .statusBarsPadding()
                        .padding(horizontal = LocalAppSpacing.current.pageHorizontal)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                        Spacer(Modifier.weight(1f))
                        if (showToolbar) TextButton(onClick = { previewReply = !previewReply }) { Text(if (previewReply) "关闭预览" else "实时预览") }
                        TextButton(onClick = { if (!busy) expanded = false }) { Text("收起") }
                        if (submissionNotice.isBlank()) TextButton(onClick = { submit() }, enabled = canSubmit) {
                            Text(if (busy) "发送中…" else submitLabel)
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    if (showToolbar) ReplyToolbar(
                        onInsert = { b, a, p -> body = applySourceEditorMarkup(body, b, a, p) },
                        onUploadImage = ::chooseImage,
                        imageUploadBusy = imageUploadBusy,
                        onAdvanced = { tool = it },
                    )
                    Spacer(Modifier.height(8.dp))
                    if (showToolbar) SourceEditorMediaControls(media, editorMedia, !busy && !imageUpload.uploading)
                    if (priorDraft != null && !draftRestored) TextButton({ restoreDraft = true }, enabled = !busy && !imageUploadBusy) { Text("恢复回复草稿") }
                    OutlinedTextField(
                        value = body,
                        onValueChange = { body = continueSourceEditorLine(body, it) },
                        enabled = !busy && !imageUploadBusy,
                        isError = body.text.length > maxBodyLength,
                        supportingText = { if (body.text.length > maxBodyLength) Text("回复最多 $maxBodyLength 字") },
                        modifier = Modifier.fillMaxWidth().weight(if (previewReply) 0.5f else 1f)
                            .sourceEditorKeys(body, !busy && !imageUploadBusy, { body = it }, { if (canSubmit) submit() }),
                        shape = MaterialTheme.shapes.large
                    )
                    MentionSuggestions(session, mentionUrl, body, { body = it }, !busy && !imageUploadBusy)
                    if (previewReply) {
                        Column(Modifier.fillMaxWidth().weight(0.5f).verticalScroll(rememberScrollState())) {
                            if (previewLoading) LinearProgressIndicator(Modifier.fillMaxWidth())
                            previewError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                            HtmlContent(if (previewUrl.isNotBlank()) previewHtml else remember(body.text) { HtmlTextConversion.markdownToHtml(body.text) }, cacheable = false)
                        }
                    }
                    ReplyCaptchaField(
                        session = session,
                        captcha = captcha,
                        answer = answer,
                        onAnswerChange = {
                            if (captcha !is sb.linux.client.domain.account.LoginVerification.Native || it.length <= 8) answer = it
                        },
                        onRefresh = onRefreshCaptcha,
                        topPadding = 10.dp,
                        revision = captchaRevision,
                        onReloadCap = { answer = ""; captchaRevision++ },
                    )
                    Spacer(Modifier.height(12.dp))
                    ReplySubmissionNotice(submissionNotice, submitLabel, busy, canSubmit, ::submit)
                }
            }
        }
        return
    }
    ModalBottomSheet(
        onDismissRequest = { if (!busy) onDismiss() },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        contentWindowInsets = {
            androidx.compose.foundation.layout.WindowInsets.ime
                .union(androidx.compose.foundation.layout.WindowInsets.navigationBars)
        }
    ) {
        Column(
            Modifier
                .padding(horizontal = LocalAppSpacing.current.pageHorizontal)
                .verticalScroll(rememberScrollState())
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // 关闭排在标题前：模态始终有明确、可访问的退出动作；提交中不可关闭。
                IconButton(onClick = { if (!busy) onDismiss() }, enabled = !busy) {
                    Icon(Icons.Filled.Close, "关闭")
                }
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                Spacer(Modifier.weight(1f))
                if (showToolbar) TextButton(onClick = { previewReply = !previewReply }) { Text(if (previewReply) "关闭预览" else "实时预览") }
                if (submissionNotice.isBlank()) TextButton(onClick = { submit() }, enabled = canSubmit) {
                    Text(if (busy) "发送中…" else submitLabel)
                }
            }
            Spacer(Modifier.height(4.dp))
            if (showToolbar) ReplyToolbar(
                onInsert = { b, a, p -> body = applySourceEditorMarkup(body, b, a, p) },
                onUploadImage = ::chooseImage,
                imageUploadBusy = imageUploadBusy,
                onAdvanced = { tool = it },
            )
            Spacer(Modifier.height(8.dp))
            if (showToolbar) SourceEditorMediaControls(media, editorMedia, !busy && !imageUpload.uploading)
            if (priorDraft != null && !draftRestored) TextButton({ restoreDraft = true }, enabled = !busy && !imageUploadBusy) { Text("恢复回复草稿") }
            OutlinedTextField(
                value = body,
                onValueChange = { body = continueSourceEditorLine(body, it) },
                enabled = !busy && !imageUploadBusy,
                isError = body.text.length > maxBodyLength,
                supportingText = { if (body.text.length > maxBodyLength) Text("回复最多 $maxBodyLength 字") },
                modifier = Modifier.fillMaxWidth().heightIn(min = 96.dp)
                    .sourceEditorKeys(body, !busy && !imageUploadBusy, { body = it }, { if (canSubmit) submit() }),
                shape = MaterialTheme.shapes.large,
                trailingIcon = {
                    IconButton(onClick = { expanded = true }) {
                        Icon(Icons.Filled.OpenInFull, "全屏编辑", Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            )
            MentionSuggestions(session, mentionUrl, body, { body = it }, !busy && !imageUploadBusy)
            if (previewReply) {
                Column(Modifier.fillMaxWidth().heightIn(min = 96.dp, max = 320.dp).verticalScroll(rememberScrollState())) {
                    if (previewLoading) LinearProgressIndicator(Modifier.fillMaxWidth())
                    previewError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    HtmlContent(if (previewUrl.isNotBlank()) previewHtml else remember(body.text) { HtmlTextConversion.markdownToHtml(body.text) }, cacheable = false)
                }
            }
            ReplyCaptchaField(
                session = session,
                captcha = captcha,
                answer = answer,
                onAnswerChange = {
                    if (captcha is sb.linux.client.domain.account.LoginVerification.Cap || it.length <= 8) answer = it
                },
                onRefresh = onRefreshCaptcha,
                topPadding = 10.dp,
                revision = captchaRevision,
                onReloadCap = { answer = ""; captchaRevision++ },
            )
            Spacer(Modifier.height(24.dp))
            ReplySubmissionNotice(submissionNotice, submitLabel, busy, canSubmit, ::submit)
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun ReplySubmissionNotice(notice: String, label: String, busy: Boolean, enabled: Boolean, onSubmit: () -> Unit) {
    if (notice.isBlank()) return
    Column(verticalArrangement = Arrangement.spacedBy(LocalAppSpacing.current.item)) {
        Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.secondaryContainer) {
            Text(notice, modifier = Modifier.fillMaxWidth().heightIn(max = 140.dp)
                .verticalScroll(rememberScrollState()).padding(LocalAppSpacing.current.contentGap),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
        }
        Button(onClick = onSubmit, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
            Text(if (busy) "发送中…" else label)
        }
    }
}
