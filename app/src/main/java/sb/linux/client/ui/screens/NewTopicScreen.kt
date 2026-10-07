package sb.linux.client.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import kotlinx.coroutines.delay
import sb.linux.client.ui.session.Session
import sb.linux.client.domain.topic.NewTopicSubmission
import sb.linux.client.ui.feature.topic.NewTopicViewModel
import sb.linux.client.ui.feature.topic.NewTopicViewModelFactory
import sb.linux.client.ui.feature.media.ImageUploadViewModel
import sb.linux.client.ui.feature.media.ImageUploadViewModelFactory


@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NewTopicScreen(session: Session, nav: NavHostController, editId: Long = 0L) {
    val factory = remember(session.newTopicRepository, session.usageStatisticsRepository, session.newTopicDraftRepository) {
        NewTopicViewModelFactory(
            session.newTopicRepository,
            session.usageStatisticsRepository,
            session.newTopicDraftRepository,
        )
    }
    val viewModel: NewTopicViewModel = viewModel(factory = factory, key = "new-topic-$editId")
    val imageUploadFactory = remember(session.imageUploadRepository, session.imageHostSettingsRepository) {
        ImageUploadViewModelFactory(session.imageUploadRepository, session.imageHostSettingsRepository)
    }
    val imageUploadViewModel: ImageUploadViewModel = viewModel(factory = imageUploadFactory)
    val editorState by viewModel.uiState.collectAsState()
    val form = remember { NewTopicFormState() }
    var forums by form::forums
    var forumId by form::forumId
    var forumExpanded by form::forumExpanded
    var title by form::title
    var internalId by form::internalId
    var specialType by form::specialType
    var drawAt by form::drawAt
    var participantTarget by form::participantTarget
    var minReplyChars by form::minReplyChars
    var replyCaptcha by form::replyCaptcha
    var prizes by form::prizes
    var cardName by form::cardName
    var cardCurrency by form::cardCurrency
    var cardPrice by form::cardPrice
    var cardLimit by form::cardLimit
    var cardAutoReply by form::cardAutoReply
    var cardAutoReplyContent by form::cardAutoReplyContent
    var cardValues by form::cardValues
    var currencyMenu by form::currencyMenu
    var prizeTypeMenu by form::prizeTypeMenu
    var announcement by form::announcement
    var sourceEditMeta by form::sourceEditMeta
    var replyOrder by form::replyOrder
    var replyOrderOptions by form::replyOrderOptions
    var replyOrderMenu by form::replyOrderMenu
    var loading by form::loading
    var busy by form::busy
    var error by form::error
    var confirmOpen by form::confirmOpen
    var restoreDraftOpen by form::restoreDraftOpen
    var exitDraftOpen by form::exitDraftOpen
    var draftMenuOpen by form::draftMenuOpen
    var previewMode by form::previewMode
    var imageUploadBusy by form::imageUploadBusy
    var bodyFocused by form::bodyFocused
    var body by rememberSaveable { mutableStateOf("") }
    var bodySelection by remember { mutableStateOf(TextRange.Zero) }
    var fullScreenEditor by rememberSaveable { mutableStateOf(false) }
    val pageScrollState = rememberScrollState()
    val bodyBringIntoViewRequester = remember { BringIntoViewRequester() }
    val imeVisible = WindowInsets.isImeVisible
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            imageUploadBusy = true
            imageUploadViewModel.upload(uri.toString()) { result ->
                result.onSuccess { url ->
                val at = bodySelection.min.coerceIn(0, body.length)
                val end = bodySelection.max.coerceIn(at, body.length)
                val image = "![图片]($url)"
                body = body.replaceRange(at, end, image)
                bodySelection = TextRange(at + image.length)
                session.showToast("图片上传成功")
                }.onFailure { session.showToast(it.message ?: "图片上传失败") }
                imageUploadBusy = false
            }
        }
    }

    // adjustResize 在 edge-to-edge 窗口中不会替滚动页保证焦点位置；IME 完成展开后再请求一次，
    // 让普通编辑模式和全屏模式一样，光标始终处于键盘上方。
    LaunchedEffect(bodyFocused, imeVisible) {
        if (bodyFocused && imeVisible) {
            delay(120)
            bodyBringIntoViewRequester.bringIntoView()
        }
    }

    fun saveDraft() {
        if (editId == 0L) {
            viewModel.saveDraft(form.toDraft(title, body, forumId))
            session.showToast("草稿已保存")
        }
    }
    fun clearDraft() = viewModel.clearDraft()

    NewTopicLoadEffect(viewModel, form, editId) { body = it }
    LaunchedEffect(editorState.submitting) { form.busy = editorState.submitting }
    LaunchedEffect(editorState.submitError) {
        editorState.submitError?.let { form.error = it; form.busy = false }
    }
    LaunchedEffect(editorState.submissionRevision) {
        if (editorState.submissionRevision > 0) {
            if (editId == 0L) clearDraft()
            session.showToast(if (editId > 0) "编辑成功" else "发布成功")
            nav.popBackStack()
        }
    }
    fun insert(before: String, after: String = before, placeholder: String = "") {
        val start = bodySelection.min.coerceIn(0, body.length)
        val end = bodySelection.max.coerceIn(start, body.length)
        val selection = body.substring(start, end).ifEmpty { placeholder }
        val replacement = before + selection + after
        body = body.replaceRange(start, end, replacement)
        bodySelection = if (before.isEmpty() && after.isEmpty()) TextRange(start + replacement.length)
            else TextRange(start + before.length, start + before.length + selection.length)
    }

    fun doSubmit() {
        val fid = forumId ?: run { error = "请选择版块"; return }
        busy = true
        error = null
        viewModel.submit(
            NewTopicSubmission(
                        editId = editId,
                        loggedIn = session.account.loginState.loggedIn,
                        internalId = internalId,
                        forumId = fid,
                        title = title,
                        body = body,
                        hiddenFields = sourceEditMeta.hiddenFields,
                        replyOrder = replyOrder,
                        specialType = specialType,
                        drawAt = drawAt,
                        participantTarget = participantTarget,
                        minReplyChars = minReplyChars,
                        replyCaptcha = replyCaptcha,
                        prizes = prizes,
                        cardName = cardName,
                        cardCurrency = cardCurrency,
                        cardPrice = cardPrice,
                        cardLimit = cardLimit,
                        cardAutoReply = cardAutoReply,
                        cardAutoReplyContent = cardAutoReplyContent,
                        cardValues = cardValues,
            )
        )
    }
    val hasDraftContent = title.isNotBlank() || body.isNotBlank() || specialType.isNotBlank()
    BackHandler(enabled = fullScreenEditor || (editId == 0L && hasDraftContent)) {
        if (fullScreenEditor) fullScreenEditor = false else exitDraftOpen = true
    }

    NewTopicDialogs(
        form = form,
        editId = editId,
        nav = nav,
        hasDraftContent = hasDraftContent,
        onRestoreDraft = { body = form.restoreDraft(viewModel.loadDraft()) },
        onSaveDraft = { saveDraft() },
        onClearDraft = { clearDraft() },
        onSubmit = { doSubmit() },
    )
    Scaffold(
        topBar = {
            NewTopicTopBar(
                form = form,
                editId = editId,
                nav = nav,
                hasSavedDraft = editorState.hasSavedDraft,
                body = body,
                hasDraftContent = hasDraftContent,
                fullScreenEditor = fullScreenEditor,
                onFullScreenEditorChange = { fullScreenEditor = it },
                previewMode = previewMode,
                onPreviewModeChange = { previewMode = it },
                onSaveDraft = { saveDraft() },
                onSubmit = { form.confirmOpen = true },
            )
        },
    ) { pad ->
        NewTopicFormContent(
            pad = pad,
            form = form,
            session = session,
            nav = nav,
            editId = editId,
            body = body,
            bodySelection = bodySelection,
            fullScreenEditor = fullScreenEditor,
            previewMode = previewMode,
            pageScrollState = pageScrollState,
            bodyBringIntoViewRequester = bodyBringIntoViewRequester,
            onBodyChange = { body = it.text; bodySelection = it.selection },
            onInsert = { before, after, placeholder -> insert(before, after, placeholder) },
            onUploadImage = { imagePicker.launch("image/*") },
        )
    }
}





