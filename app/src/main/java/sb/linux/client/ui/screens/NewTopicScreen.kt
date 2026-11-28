package sb.linux.client.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import sb.linux.client.domain.topic.SourceEditorPage
import sb.linux.client.domain.topic.SourceEditorDraft
import sb.linux.client.ui.components.feedback.LoadingBox
import sb.linux.client.ui.components.feedback.ErrorBox
import sb.linux.client.ui.patterns.settings.SettingsChoiceRow
import sb.linux.client.ui.session.Session

@Composable
fun NewTopicScreen(session: Session, nav: NavHostController, editId: Long = 0L, sourceUrl: String? = null) {
    val account = session.account.loginState
    val path = sourceUrl?.takeIf { it.isNotBlank() } ?: if (editId > 0) "/topic_edit/$editId" else "/topic_edit"
    val identity = session.accountIdentityVersion
    var page by remember(identity, editId, path) { mutableStateOf<SourceEditorPage?>(null) }
    var loading by remember(identity, editId) { mutableStateOf(true) }
    var error by remember(identity, editId) { mutableStateOf<String?>(null) }
    var busy by remember(identity, editId) { mutableStateOf(false) }
    var selected by rememberSaveable(identity, editId) { mutableIntStateOf(-1) }
    var dirty by rememberSaveable(identity, editId) { mutableStateOf(false) }
    var draftBox by remember(identity, editId) { mutableStateOf(false) }
    var drafts by remember(identity, editId) { mutableStateOf<List<SourceEditorDraft>>(emptyList()) }
    var restoredDraft by remember(identity, editId) { mutableStateOf<SourceEditorDraft?>(null) }
    var restoredDraftId by rememberSaveable(identity, editId) { mutableStateOf<String?>(null) }
    var pendingRestore by remember(identity, editId) { mutableStateOf<SourceEditorDraft?>(null) }
    var draftRevision by remember(identity, editId) { mutableIntStateOf(0) }
    var draftFields by rememberSaveable(identity, editId, stateSaver = listSaver<List<Pair<String, String>>, String>(
        save = { fields -> fields.flatMap { listOf(it.first, it.second) } },
        restore = { fields -> fields.chunked(2).map { it[0] to it[1] } },
    )) { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var savedAt by remember(identity, editId) { mutableStateOf(0L) }
    val draftStore = session.sourceEditorDraftRepository
    val autoDraftId = "auto:$editId"
    val scope = rememberCoroutineScope()
    fun saveDraft(automatic: Boolean): Boolean {
        if (account.userId <= 0 || identity != session.accountIdentityVersion || loading || page == null) return false
        if (draftFields.none { it.first in setOf("title", "body") && it.second.isNotBlank() }) {
            if (automatic) { draftStore.remove(account.userId, autoDraftId); savedAt = 0L }
            return true
        }
        val draft = SourceEditorDraft(
            if (automatic) autoDraftId else java.util.UUID.randomUUID().toString(), account.userId, editId,
            System.currentTimeMillis(), page?.extensions?.getOrNull(selected)?.toggle?.first.orEmpty(),
            draftFields, automatic,
        )
        return runCatching { draftStore.save(draft) }.onSuccess {
            savedAt = draft.updatedAt
            drafts = draftStore.list(account.userId, editId)
            if (!automatic) session.showToast("草稿已保存")
        }.onFailure { session.showToast("草稿保存失败，请保留当前编辑内容") }.isSuccess
    }
    fun restore(draft: SourceEditorDraft) {
        if (draft.userId != account.userId || draft.topicId != editId) return
        restoredDraft = draft
        restoredDraftId = draft.id
        selected = page?.extensions?.indexOfFirst { it.toggle.first == draft.extensionName } ?: -1
        draftFields = draft.fields
        draftRevision++
        dirty = true
        draftBox = false
        pendingRestore = null
        session.showToast("草稿已恢复")
    }
    fun load() {
        loading = true
        scope.launch {
            try {
                val result = session.sourceEditorRepository.load(path)
                if (identity == session.accountIdentityVersion) {
                    if (dirty && draftFields.isNotEmpty()) {
                        restoredDraft = SourceEditorDraft(
                            restoredDraftId ?: autoDraftId, account.userId, editId, System.currentTimeMillis(),
                            result.extensions.getOrNull(selected)?.toggle?.first.orEmpty(), draftFields, true,
                        )
                    } else selected = result.extensions.indexOfFirst { it.selected }
                    page = result
                    error = null
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (failure: Exception) { error = failure.message ?: "编辑器加载失败" }
            finally { loading = false }
        }
    }
    LaunchedEffect(path, identity) {
        if (account.loggedIn) {
            drafts = draftStore.list(account.userId, editId)
            load()
        }
    }
    LaunchedEffect(draftFields, selected, dirty, identity, busy, loading) {
        if (dirty && !busy && !loading) { delay(500); saveDraft(automatic = true) }
    }
    val flushDraft by rememberUpdatedState<() -> Unit>({ if (dirty) saveDraft(automatic = true) })
    DisposableEffect(identity, editId) { onDispose { flushDraft() } }
    val exit = rememberSettingsExit(dirty) { nav.popBackStack() }
    AccountPageGate(account.loggedIn, "发表主题", "登录后发表主题", nav, settingsStyle = true) {
        SettingsPage(if (editId > 0) "编辑主题" else "发表主题", nav, onBack = { if (!busy) exit() }) {
            when {
                loading -> LoadingBox()
                page == null -> ErrorBox(error ?: "编辑器不可用", onRetry = ::load)
                else -> {
                    val source = page!!
                    Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        TextButton(onClick = { drafts = draftStore.list(account.userId, editId); draftBox = true }, enabled = !busy) {
                            Text("草稿箱${if (drafts.isNotEmpty()) " (${drafts.size})" else ""}")
                        }
                        if (savedAt > 0) Text("已自动保存", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    SettingsChoiceRow("主帖类型", listOf("-1" to "普通主题") + source.extensions.mapIndexed { i, extension ->
                        i.toString() to extension.label
                    }, selected.toString(), {
                        val choice = it.toInt()
                        val unavailable = source.extensions.getOrNull(choice)?.unavailableReason.orEmpty()
                        if (unavailable.isNotBlank()) session.showToast(unavailable)
                        else { selected = choice; dirty = true }
                    }, enabled = !busy)
                    val fields = (restoredDraft?.let { restoreSourceDraftFields(source.form.fields, it) } ?: source.form.fields) + source.extensions.flatMapIndexed { i, extension ->
                        (restoredDraft?.let { restoreSourceDraftFields(extension.form.fields, it) } ?: extension.form.fields)
                            .map { field -> field.copy(disabled = i != selected || field.disabled) }
                    }
                    val extension = source.extensions.getOrNull(selected)
                    val form = source.form.copy(fields = fields, hidden = source.form.hidden +
                        extension?.let { it.form.hidden + it.toggle }.orEmpty(), notes = source.form.notes + extension?.form?.notes.orEmpty())
                    val selectedForumId = draftFields.firstOrNull { it.first == "forum_id" }?.second
                        ?: fields.firstOrNull { it.name == "forum_id" }?.let { f ->
                            f.options.firstOrNull { it.selected }?.value ?: f.options.firstOrNull { !it.disabled }?.value ?: f.value
                        }.orEmpty()
                    val activeStickerPicker = source.stickerPicker?.takeIf { picker ->
                        picker.enabled && (picker.allowedForums.isEmpty() || selectedForumId in picker.allowedForums)
                    }?.let { picker ->
                        picker.copy(forumId = selectedForumId.ifBlank { picker.forumId })
                    }
                    key(identity, editId, draftRevision) { NativeAccountForm(form, session.accountManagementRepository, !busy,
                        onDirty = { dirty = true }, hideDisabled = true,
                        bodyEditor = { text, change, enabled, uploadBusy, submit -> SourceBodyField(session, source, text, change, enabled, uploadBusy, submit, activeStickerPicker) },
                        onValuesChange = { draftFields = it },
                        onSubmit = { submitted, pairs ->
                            busy = true
                            scope.launch {
                                try {
                                    val topicId = session.sourceEditorRepository.submit(submitted, pairs)
                                    if (identity == session.accountIdentityVersion) {
                                        dirty = false
                                        draftStore.remove(account.userId, autoDraftId)
                                        restoredDraftId?.let { draftStore.remove(account.userId, it) }
                                        session.topicPageCacheRepository.clearTopic(topicId)
                                        session.showToast("主题已保存")
                                        nav.popBackStack(); nav.navigate("topic/$topicId")
                                    }
                                } catch (cancelled: CancellationException) { throw cancelled }
                                catch (failure: Exception) { error = failure.message ?: "保存失败，输入已保留" }
                                finally { busy = false }
                            }
                        }) }
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                }
            }
        }
    }
    if (draftBox) SourceDraftBox(drafts,
        canSave = !busy && draftFields.any { it.first in setOf("title", "body") && it.second.isNotBlank() },
        onSave = { saveDraft(automatic = false) },
        onRestore = { if (dirty) pendingRestore = it else restore(it) },
        onDelete = { draftStore.remove(account.userId, it.id); drafts = draftStore.list(account.userId, editId) },
        onClose = { draftBox = false })
    pendingRestore?.let { draft ->
        AlertDialog(onDismissRequest = { pendingRestore = null }, title = { Text("恢复草稿？") },
            text = { Text("恢复草稿会替换当前编辑内容，将先把当前内容另存为草稿。") },
            confirmButton = { TextButton(onClick = { if (saveDraft(automatic = false)) restore(draft) }) { Text("恢复") } },
            dismissButton = { TextButton(onClick = { pendingRestore = null }) { Text("取消") } })
    }
}
