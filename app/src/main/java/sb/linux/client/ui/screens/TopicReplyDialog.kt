package sb.linux.client.ui.screens

import sb.linux.client.common.content.HtmlTextConversion
import sb.linux.client.ui.session.Session


import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import sb.linux.client.ui.content.HtmlContent
import sb.linux.client.domain.account.LoginVerification
import sb.linux.client.ui.feature.media.ImageUploadViewModel
import sb.linux.client.ui.feature.media.ImageUploadViewModelFactory
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
) {
    var body by remember(initial) {
        mutableStateOf(TextFieldValue(initial, selection = TextRange(initial.length)))
    }
    var answer by remember { mutableStateOf("") }
    var captchaRevision by remember { mutableIntStateOf(0) }
    var busy by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    var previewReply by remember { mutableStateOf(false) }
    var imageUploadBusy by remember { mutableStateOf(false) }
    val imageUploadFactory = remember(session.imageUploadRepository, session.imageHostSettingsRepository) {
        ImageUploadViewModelFactory(session.imageUploadRepository, session.imageHostSettingsRepository)
    }
    val imageUploadViewModel: ImageUploadViewModel = viewModel(factory = imageUploadFactory)
    val canSubmit = body.text.isNotBlank() && !busy && (captcha == null || answer.isNotBlank())
    LaunchedEffect(captcha) {
        if (answer.isNotBlank()) answer = ""
        captchaRevision++
    }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            imageUploadBusy = true
            imageUploadViewModel.upload(uri.toString()) { result ->
                result.onSuccess {
                    body = insertReplyImage(body, it)
                    session.showToast("图片上传成功")
                }.onFailure { session.showToast(it.message ?: "图片上传失败") }
                imageUploadBusy = false
            }
        }
    }

    fun chooseImage() {
        if (!imageUploadViewModel.hasUsableHost()) {
            session.showToast("请先在设置中配置图床")
        } else {
            imagePicker.launch("image/*")
        }
    }

    fun submit() {
        busy = true
        onSubmit(body.text, answer) { succeeded ->
            busy = false
            if (!succeeded && captcha is sb.linux.client.domain.account.LoginVerification.Cap) {
                answer = ""
                captchaRevision++
            }
        }
    }

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
                        .padding(horizontal = 20.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                        Spacer(Modifier.weight(1f))
                if (showToolbar) TextButton(onClick = { previewReply = !previewReply }) { Text(if (previewReply) "编辑" else "预览") }
                        TextButton(onClick = { if (!busy) expanded = false }) { Text("收起") }
                        TextButton(onClick = { submit() }, enabled = canSubmit) {
                            Text(if (busy) "发送中…" else submitLabel)
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    if (showToolbar) ReplyToolbar(
                        onInsert = { b, a, p -> body = insertReplyMarkup(body, b, a, p) },
                        onUploadImage = ::chooseImage,
                        imageUploadBusy = imageUploadBusy,
                    )
                    Spacer(Modifier.height(8.dp))
                    if (previewReply) {
                        Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState())) {
                            HtmlContent(remember(body.text) { HtmlTextConversion.markdownToHtml(body.text) }, cacheable = false)
                        }
                    } else OutlinedTextField(
                        value = body,
                        onValueChange = { body = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        shape = MaterialTheme.shapes.large
                    )
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
                    Spacer(Modifier.height(12.dp))
                }
            }
        }
        return
    }
    ModalBottomSheet(
        onDismissRequest = { if (!busy) onDismiss() },
        contentWindowInsets = {
            androidx.compose.foundation.layout.WindowInsets.ime
                .union(androidx.compose.foundation.layout.WindowInsets.navigationBars)
        }
    ) {
        Column(
            Modifier
                .padding(horizontal = 20.dp)
                .imePadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // 关闭排在标题前：模态始终有明确、可访问的退出动作；提交中不可关闭。
                IconButton(onClick = { if (!busy) onDismiss() }, enabled = !busy) {
                    Icon(Icons.Filled.Close, "关闭")
                }
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                Spacer(Modifier.weight(1f))
                if (showToolbar) TextButton(onClick = { previewReply = !previewReply }) { Text(if (previewReply) "编辑" else "预览") }
                TextButton(onClick = { submit() }, enabled = canSubmit) {
                    Text(if (busy) "发送中…" else submitLabel)
                }
            }
            Spacer(Modifier.height(4.dp))
            if (showToolbar) ReplyToolbar(
                onInsert = { b, a, p -> body = insertReplyMarkup(body, b, a, p) },
                onUploadImage = ::chooseImage,
                imageUploadBusy = imageUploadBusy,
            )
            Spacer(Modifier.height(8.dp))
            if (previewReply) {
                Column(Modifier.fillMaxWidth().heightIn(min = 96.dp, max = 320.dp).verticalScroll(rememberScrollState())) {
                    HtmlContent(remember(body.text) { HtmlTextConversion.markdownToHtml(body.text) }, cacheable = false)
                }
            } else OutlinedTextField(
                value = body,
                onValueChange = { body = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 96.dp),
                shape = MaterialTheme.shapes.large,
                trailingIcon = {
                    IconButton(onClick = { expanded = true }) {
                        Icon(
                            Icons.Filled.OpenInFull,
                            "全屏编辑",
                            Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
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
        }
    }
}
