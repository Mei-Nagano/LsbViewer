package sb.linux.client.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import sb.linux.client.domain.account.AccountForm
import sb.linux.client.domain.account.AccountSubmit
import sb.linux.client.domain.account.LoginVerification
import sb.linux.client.domain.account.fields
import sb.linux.client.domain.topic.*
import sb.linux.client.ui.LocalLinkHandler
import sb.linux.client.ui.content.ViewerImage
import sb.linux.client.ui.content.ZoomImageViewer
import sb.linux.client.ui.patterns.settings.SettingsCheckRow
import sb.linux.client.ui.patterns.settings.SettingsChoiceRow
import sb.linux.client.ui.session.Session
import sb.linux.client.ui.theme.LocalAppSpacing

/** 帖子页主楼下方的原生「邀请码接力」卡片，字段和按钮完全来自源站 HTML。 */
@Composable
internal fun InviteRelayCard(
    panel: InviteRelayPanel,
    loggedIn: Boolean,
    onLogin: () -> Unit,
    onOpenDialog: (String) -> Unit,
    onOpenLink: (String) -> Unit,
) {
    val spacing = LocalAppSpacing.current
    val border = if (panel.awaitingHandoff) {
        BorderStroke(1.5.dp, MaterialTheme.colorScheme.tertiary)
    } else null

    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = border,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.pageHorizontal, vertical = spacing.item),
    ) {
        Column(
            modifier = Modifier.padding(spacing.pageHorizontal),
            verticalArrangement = Arrangement.spacedBy(spacing.contentGap),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    panel.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                if (panel.pendingBadge.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                    ) {
                        Text(
                            panel.pendingBadge,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                    }
                }
            }

            if (panel.stockText.isNotBlank() || panel.totalText.isNotBlank() || panel.quotaText.isNotBlank()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    if (panel.stockText.isNotBlank()) {
                        Text(
                            panel.stockText,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    if (panel.totalText.isNotBlank()) {
                        Text(
                            panel.totalText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (panel.quotaText.isNotBlank()) {
                        Text(
                            panel.quotaText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            if (panel.forceNoticeTitle.isNotBlank() || panel.forceNoticeBody.isNotBlank()) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    FlowRow(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        if (panel.forceNoticeTitle.isNotBlank()) {
                            Text(
                                panel.forceNoticeTitle,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.tertiary,
                            )
                        }
                        if (panel.forceNoticeBody.isNotBlank()) {
                            Text(
                                panel.forceNoticeBody,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                        }
                    }
                }
            }

            if (panel.deadlineText.isNotBlank()) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        panel.deadlineText,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                }
            }

            panel.statusNotes.forEach { note ->
                Text(
                    note,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (panel.actions.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    panel.actions.forEach { action ->
                        val trigger = {
                            if (!loggedIn) onLogin()
                            else if (action.opensDialog) onOpenDialog(action.url)
                            else onOpenLink(action.url)
                        }
                        when (action.style) {
                            InviteRelayActionStyle.HANDOFF_PRIMARY -> Button(
                                onClick = trigger,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.tertiary,
                                    contentColor = MaterialTheme.colorScheme.onTertiary,
                                ),
                            ) { Text(action.label) }
                            InviteRelayActionStyle.PRIMARY -> Button(
                                onClick = trigger,
                                shape = RoundedCornerShape(12.dp),
                            ) { Text(action.label) }
                            InviteRelayActionStyle.MANAGE -> TextButton(
                                onClick = trigger,
                                shape = RoundedCornerShape(12.dp),
                            ) { Text(action.label) }
                            InviteRelayActionStyle.DEFAULT -> OutlinedButton(
                                onClick = trigger,
                                shape = RoundedCornerShape(12.dp),
                            ) { Text(action.label) }
                        }
                    }
                }
            }
        }
    }
}

private data class PendingRelaySubmission(
    val actionUrl: String,
    val pairs: List<Pair<String, String>>,
    val files: List<InviteRelayFileUpload>,
    val message: String,
    val publishCode: String? = null,
    val claimWarning: Boolean = false,
    val confirmLabel: String = "确认",
)

/** 源站 invite_relay 全功能原生交互弹窗：领取、交棒、贡献、复制、举报举证、作者管理与库存操作。 */
@Composable
internal fun InviteRelayDialog(
    session: Session,
    initialUrl: String,
    onDismiss: () -> Unit,
    onPanelUpdated: (InviteRelayPanel) -> Unit = {},
) {
    val repo = session.inviteRelayRepository
    val scope = rememberCoroutineScope()
    val linkHandler = LocalLinkHandler.current
    val history = remember(initialUrl) { mutableStateListOf<String>() }
    var currentUrl by remember(initialUrl) { mutableStateOf(initialUrl) }
    var viewData by remember(initialUrl) { mutableStateOf<InviteRelayViewData?>(null) }
    var loading by remember(initialUrl) { mutableStateOf(true) }
    var submitting by remember(initialUrl) { mutableStateOf(false) }
    var statusMessage by remember(initialUrl) { mutableStateOf("正在加载…") }
    var statusError by remember(initialUrl) { mutableStateOf(false) }
    var pendingConfirm by remember(initialUrl) { mutableStateOf<PendingRelaySubmission?>(null) }
    var previewImage by remember(initialUrl) { mutableStateOf<ViewerImage?>(null) }
    var loadNonce by remember(initialUrl) { mutableIntStateOf(0) }
    var pendingNotice by remember(initialUrl) { mutableStateOf("") }

    LaunchedEffect(currentUrl, loadNonce) {
        loading = true
        pendingConfirm = null
        statusMessage = "正在加载…"
        statusError = false
        try {
            val loaded = repo.loadView(currentUrl)
            viewData = loaded
            statusMessage = pendingNotice
            pendingNotice = ""
            statusError = false
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            viewData = null
            statusMessage = error.message?.takeIf { it.isNotBlank() } ?: "加载失败，请重试"
            statusError = true
        } finally {
            loading = false
        }
    }

    fun navigateTo(url: String) {
        if (submitting) return
        if (currentUrl.isNotBlank() && currentUrl != url) history.add(currentUrl)
        pendingNotice = ""
        currentUrl = url
        loadNonce++
    }

    fun goBack() {
        if (submitting) return
        if (pendingConfirm != null) {
            pendingConfirm = null
            return
        }
        val previous = history.removeLastOrNull()
        if (previous != null) {
            pendingNotice = ""
            currentUrl = previous
            loadNonce++
        } else {
            onDismiss()
        }
    }

    fun executeSubmit(actionUrl: String, pairs: List<Pair<String, String>>, files: List<InviteRelayFileUpload>) {
        if (submitting) return
        submitting = true
        statusMessage = "正在提交…"
        statusError = false
        scope.launch {
            try {
                val result = repo.submitForm(actionUrl, pairs, files)
                result.updatedPanel?.let(onPanelUpdated)
                pendingNotice = result.notice
                currentUrl = result.viewUrl
                loadNonce++
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                statusMessage = error.message?.takeIf { it.isNotBlank() } ?: "提交失败，请重试"
                statusError = true
            } finally {
                submitting = false
            }
        }
    }

    Dialog(
        onDismissRequest = { if (!submitting) goBack() },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 24.dp)
                .widthIn(max = 560.dp)
                .heightIn(max = 680.dp),
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp,
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(Modifier.fillMaxWidth()) {
                // 顶部标题栏
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    if (history.isNotEmpty() || pendingConfirm != null) {
                        IconButton(onClick = ::goBack, enabled = !submitting) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                        }
                    }
                    Text(
                        text = viewData?.title ?: "邀请码接力",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { if (!submitting) onDismiss() }, enabled = !submitting) {
                        Icon(Icons.Filled.Close, "关闭")
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // 状态栏
                if (statusMessage.isNotBlank()) {
                    Surface(
                        color = if (statusError) MaterialTheme.colorScheme.errorContainer
                        else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = statusMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (statusError) MaterialTheme.colorScheme.onErrorContainer
                            else MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }
                }

                // 主体内容
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    val confirm = pendingConfirm
                    when {
                        confirm != null -> {
                            Surface(
                                shape = MaterialTheme.shapes.large,
                                color = MaterialTheme.colorScheme.tertiaryContainer,
                                border = if (confirm.claimWarning) BorderStroke(1.5.dp, MaterialTheme.colorScheme.tertiary) else null,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    if (confirm.publishCode != null) {
                                        Text(
                                            "请核对以下邀请码是否正确，如果无效将会失去全站接力参与资格。",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                                        )
                                        Surface(
                                            shape = MaterialTheme.shapes.medium,
                                            color = MaterialTheme.colorScheme.surface,
                                            modifier = Modifier.fillMaxWidth(),
                                        ) {
                                            Text(
                                                confirm.publishCode,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                            )
                                        }
                                    } else {
                                        Text(
                                            confirm.message,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (confirm.claimWarning) FontWeight.SemiBold else FontWeight.Normal,
                                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                                        )
                                    }
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                            ) {
                                OutlinedButton(onClick = { pendingConfirm = null }, enabled = !submitting) {
                                    Text("取消")
                                }
                                Button(
                                    onClick = {
                                        val target = confirm
                                        pendingConfirm = null
                                        executeSubmit(target.actionUrl, target.pairs, target.files)
                                    },
                                    enabled = !submitting,
                                ) {
                                    Text(confirm.confirmLabel)
                                }
                            }
                        }
                        loading -> {
                            Box(
                                modifier = Modifier.fillMaxWidth().height(140.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(28.dp))
                            }
                        }
                        viewData == null -> {
                            OutlinedButton(onClick = { loadNonce++ }) {
                                Text("重试")
                            }
                        }
                        else -> {
                            CompositionLocalProvider(LocalAccountVerificationGateway provides session.accountVerificationGateway) {
                                InviteRelayNodes(
                                    nodes = viewData!!.nodes,
                                    enabled = !submitting,
                                    onOpenView = ::navigateTo,
                                    onOpenLink = { url ->
                                        onDismiss()
                                        linkHandler?.invoke(url)
                                    },
                                    onImageClick = { url, desc ->
                                        previewImage = ViewerImage(url, desc)
                                    },
                                    onRequestSubmit = { submission ->
                                        if (submission.message.isNotBlank() || submission.publishCode != null) {
                                            pendingConfirm = submission
                                        } else {
                                            executeSubmit(submission.actionUrl, submission.pairs, submission.files)
                                        }
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    previewImage?.let { img ->
        ZoomImageViewer(images = listOf(img), initialIndex = 0) { previewImage = null }
    }
}

@Composable
private fun InviteRelayNodes(
    nodes: List<InviteRelayNode>,
    enabled: Boolean,
    onOpenView: (String) -> Unit,
    onOpenLink: (String) -> Unit,
    onImageClick: (String, String) -> Unit,
    onRequestSubmit: (PendingRelaySubmission) -> Unit,
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        nodes.forEach { node ->
            when (node) {
                is InviteRelayNode.Text -> {
                    when (node.style) {
                        InviteRelayNode.TextStyleKind.DEADLINE,
                        InviteRelayNode.TextStyleKind.WARNING,
                        InviteRelayNode.TextStyleKind.PENDING -> {
                            Surface(
                                shape = MaterialTheme.shapes.medium,
                                color = MaterialTheme.colorScheme.tertiaryContainer,
                                border = if (node.style == InviteRelayNode.TextStyleKind.WARNING) {
                                    BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary)
                                } else null,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(
                                    node.value,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = if (node.style == InviteRelayNode.TextStyleKind.WARNING) FontWeight.SemiBold else FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                )
                            }
                        }
                        InviteRelayNode.TextStyleKind.MUTED -> Text(
                            node.value,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        InviteRelayNode.TextStyleKind.BODY -> Text(
                            node.value,
                            style = when (node.heading) {
                                1, 2 -> MaterialTheme.typography.titleMedium
                                3, 4 -> MaterialTheme.typography.titleSmall
                                else -> MaterialTheme.typography.bodyMedium
                            },
                            fontWeight = if (node.heading > 0) FontWeight.SemiBold else FontWeight.Normal,
                        )
                    }
                }

                is InviteRelayNode.CodeBox -> {
                    var copied by remember(node.code) { mutableStateOf(false) }
                    Surface(
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                node.label,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Surface(
                                    shape = MaterialTheme.shapes.medium,
                                    color = MaterialTheme.colorScheme.surface,
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Text(
                                        node.code,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    )
                                }
                                FilledTonalButton(
                                    onClick = {
                                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        cm.setPrimaryClip(ClipData.newPlainText(node.label, node.code))
                                        copied = true
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                ) {
                                    Icon(Icons.Filled.ContentCopy, null, Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(if (copied) "已复制" else node.copyLabel)
                                }
                            }
                        }
                    }
                }

                is InviteRelayNode.ActionBar -> {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        node.actions.forEach { action ->
                            val click = { if (action.opensDialog) onOpenView(action.url) else onOpenLink(action.url) }
                            when (action.style) {
                                InviteRelayActionStyle.PRIMARY, InviteRelayActionStyle.HANDOFF_PRIMARY ->
                                    Button(onClick = click, enabled = enabled, shape = RoundedCornerShape(12.dp)) { Text(action.label) }
                                InviteRelayActionStyle.MANAGE ->
                                    TextButton(onClick = click, enabled = enabled) { Text(action.label) }
                                InviteRelayActionStyle.DEFAULT ->
                                    OutlinedButton(onClick = click, enabled = enabled, shape = RoundedCornerShape(12.dp)) { Text(action.label) }
                            }
                        }
                    }
                }

                is InviteRelayNode.Link -> {
                    TextButton(onClick = { onOpenLink(node.url) }, enabled = enabled) {
                        Text(node.label)
                    }
                }

                is InviteRelayNode.EvidenceImage -> {
                    AsyncImage(
                        model = node.url,
                        contentDescription = node.description.ifBlank { "截图证据" },
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 220.dp)
                            .clip(MaterialTheme.shapes.medium)
                            .clickable { onImageClick(node.url, node.description) },
                    )
                }

                is InviteRelayNode.Details -> {
                    var expanded by remember(node.summary) { mutableStateOf(node.openByDefault) }
                    Surface(
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { expanded = !expanded }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    node.summary,
                                    style = MaterialTheme.typography.titleSmall,
                                    modifier = Modifier.weight(1f),
                                )
                                Icon(
                                    if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                    if (expanded) "收起" else "展开",
                                )
                            }
                            if (expanded) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                Box(Modifier.padding(12.dp)) {
                                    InviteRelayNodes(node.children, enabled, onOpenView, onOpenLink, onImageClick, onRequestSubmit)
                                }
                            }
                        }
                    }
                }

                is InviteRelayNode.Card -> {
                    Surface(
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            if (node.title.isNotBlank()) {
                                Text(node.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            }
                            InviteRelayNodes(node.children, enabled, onOpenView, onOpenLink, onImageClick, onRequestSubmit)
                        }
                    }
                }

                is InviteRelayNode.Form -> {
                    InviteRelayFormView(
                        relayForm = node.value,
                        enabled = enabled,
                        onRequestSubmit = onRequestSubmit,
                    )
                }
            }
        }
    }
}

@Composable
private fun InviteRelayFormView(
    relayForm: InviteRelayForm,
    enabled: Boolean,
    onRequestSubmit: (PendingRelaySubmission) -> Unit,
) {
    val form: AccountForm = relayForm.form
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val verificationGateway = LocalAccountVerificationGateway.current
    val values = remember(form.id, form.action) {
        mutableStateMapOf<Int, String>().apply {
            form.fields.forEachIndexed { index, field ->
                this[index] = if (field.type == "select") {
                    field.options.firstOrNull { it.selected }?.value
                        ?: field.options.firstOrNull { !it.disabled }?.value.orEmpty()
                } else field.value
            }
        }
    }
    val checked = remember(form.id, form.action) {
        mutableStateMapOf<Int, Boolean>().apply {
            form.fields.forEachIndexed { i, f -> this[i] = f.checked }
        }
    }
    val radio = remember(form.id, form.action) {
        mutableStateMapOf<String, String>().apply {
            form.fields.filter { it.type == "radio" && it.checked }.forEach { this[it.name] = it.value }
        }
    }
    val selectedFiles = remember(form.id, form.action) { mutableStateMapOf<String, InviteRelayFileUpload>() }
    var pickingField by remember { mutableStateOf<String?>(null) }
    var captchaAnswer by remember(form) { mutableStateOf("") }
    var captchaRevision by remember(form) { mutableIntStateOf(0) }
    var validationError by remember(form) { mutableStateOf<String?>(null) }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        val fieldName = pickingField
        pickingField = null
        if (uri == null || fieldName.isNullOrBlank()) return@rememberLauncherForActivityResult
        scope.launch {
            val uploaded = withContext(Dispatchers.IO) {
                runCatching {
                    val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return@runCatching null
                    val mime = context.contentResolver.getType(uri) ?: "image/jpeg"
                    val name = context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                        if (c.moveToFirst()) c.getString(0) else null
                    } ?: "evidence.jpg"
                    InviteRelayFileUpload(fieldName, name, mime, bytes)
                }.getOrNull()
            }
            if (uploaded != null) {
                selectedFiles[fieldName] = uploaded
                validationError = null
            } else {
                validationError = "读取所选文件失败"
            }
        }
    }

    val codeCheckboxIndices = remember(form) {
        form.fields.indices.filter { form.fields[it].name == "code_ids[]" && form.fields[it].type == "checkbox" }
    }
    val selectedCodeCount = codeCheckboxIndices.count { checked[it] == true }

    fun handleSubmit(buttonIndex: Int, button: AccountSubmit) {
        if (!enabled || !button.enabled) return
        val pairs = buildList {
            form.hidden.forEach { add(it) }
            form.fields.forEachIndexed { i, f ->
                if (f.disabled || f.name.isBlank() || f.type == "file") return@forEachIndexed
                when (f.type) {
                    "checkbox" -> if (checked[i] == true) add(f.name to f.value.ifBlank { "1" })
                    "radio" -> if (radio[f.name] == f.value) add(f.name to f.value)
                    else -> add(f.name to values[i].orEmpty())
                }
            }
            if (button.name.isNotBlank()) add(button.name to button.value)
        }
        // 必填校验
        form.fields.forEachIndexed { i, f ->
            if (!f.disabled && f.required && f.type != "file" && f.type != "checkbox" && f.type != "radio") {
                if (values[i].orEmpty().trim().isBlank()) {
                    validationError = "请填写${f.label}"
                    return
                }
            }
        }
        relayForm.fileFields.forEach { ff ->
            if (ff.required && selectedFiles[ff.name] == null) {
                validationError = "请选择${ff.label}"
                return
            }
        }
        val verification = form.verification
        if (verification != null && captchaAnswer.isBlank()) {
            validationError = "请先完成人机验证"
            return
        }
        val verificationPairs = try {
            when (verification) {
                is LoginVerification.Pow -> verification.fields(captchaAnswer).toList()
                is LoginVerification.Cap -> listOf(verification.fieldName to captchaAnswer)
                is LoginVerification.Native -> listOf("native_captcha_token" to verification.token, "native_captcha_answer" to captchaAnswer)
                null -> emptyList()
            }
        } catch (error: sb.linux.client.common.error.LsbException) {
            validationError = error.message
            captchaAnswer = ""
            captchaRevision++
            return
        }
        val verNames = verificationPairs.map { it.first }.toSet()
        val finalPairs = pairs.filterNot { it.first in verNames } + verificationPairs
        validationError = null

        val btnMeta = relayForm.submitMeta[buttonIndex]
        val confirmMessage = btnMeta?.confirmMessage.orEmpty().ifBlank { relayForm.confirmMessage }
        val isPublish = relayForm.publishConfirm
        val isClaim = btnMeta?.claimConfirm == true
        val publishCode = if (isPublish) {
            val codeIdx = form.fields.indexOfFirst { it.name == "code" }
            values[codeIdx].orEmpty().trim()
        } else null

        val confirmLabel = when {
            isClaim -> "已了解，确认领取"
            isPublish -> "确认发布"
            else -> "确认"
        }
        onRequestSubmit(
            PendingRelaySubmission(
                actionUrl = button.action.ifBlank { form.action },
                pairs = finalPairs,
                files = selectedFiles.values.toList(),
                message = confirmMessage,
                publishCode = publishCode,
                claimWarning = isClaim,
                confirmLabel = confirmLabel,
            ),
        )
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (relayForm.isInventoryBatch && codeCheckboxIndices.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val allChecked = selectedCodeCount == codeCheckboxIndices.size
                    Checkbox(
                        checked = allChecked,
                        onCheckedChange = { target ->
                            codeCheckboxIndices.forEach { checked[it] = target }
                        },
                        enabled = enabled,
                    )
                    Text("全选", style = MaterialTheme.typography.bodySmall)
                }
                Text(
                    "已选 $selectedCodeCount 项",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        form.fields.forEachIndexed { index, field ->
            val editable = enabled && !field.disabled
            when (field.type) {
                "file" -> Unit
                "checkbox" -> SettingsCheckRow(
                    title = field.label,
                    checked = checked[index] == true,
                    onCheckedChange = { checked[index] = it; validationError = null },
                    enabled = editable,
                )
                "radio" -> SettingsCheckRow(
                    title = field.label,
                    checked = radio[field.name] == field.value,
                    onCheckedChange = { radio[field.name] = field.value; validationError = null },
                    enabled = editable,
                )
                "select" -> SettingsChoiceRow(
                    title = field.label,
                    options = field.options.filterNot { it.disabled }.map { it.value to it.label },
                    selectedValue = values[index].orEmpty(),
                    onSelect = { values[index] = it; validationError = null },
                    enabled = editable,
                )
                else -> OutlinedTextField(
                    value = values[index].orEmpty(),
                    onValueChange = { next ->
                        if (next.length <= field.maxLength) {
                            values[index] = next
                            validationError = null
                        }
                    },
                    enabled = editable,
                    label = { Text(field.label) },
                    placeholder = if (field.placeholder.isNotBlank()) ({ Text(field.placeholder) }) else null,
                    singleLine = field.type != "textarea",
                    minLines = if (field.type == "textarea") 3 else 1,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = if (field.type == "password") PasswordVisualTransformation() else VisualTransformation.None,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = when (field.type) {
                            "number" -> KeyboardType.Number
                            "url" -> KeyboardType.Uri
                            "email" -> KeyboardType.Email
                            else -> KeyboardType.Text
                        },
                    ),
                )
            }
        }

        relayForm.fileFields.forEach { fileField ->
            val picked = selectedFiles[fileField.name]
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    enabled = enabled,
                    onClick = {
                        pickingField = fileField.name
                        filePicker.launch(if (fileField.accept.contains("image")) "image/*" else "*/*")
                    },
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(if (picked == null) "选择${fileField.label}" else "重新选择")
                }
                Text(
                    picked?.fileName ?: if (fileField.required) "未选择文件（必填）" else "未选择文件（可选）",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        val verification = form.verification
        if (verification is LoginVerification.Cap && verificationGateway != null) {
            CapVerificationWidget(
                verification = verification,
                revision = captchaRevision,
                gateway = verificationGateway,
                onToken = { captchaAnswer = it; validationError = null },
                onStatus = {},
                onError = { validationError = it; captchaAnswer = "" },
            )
            TextButton(onClick = { captchaAnswer = ""; captchaRevision++ }) {
                Text("重新加载验证")
            }
        } else if (verification is LoginVerification.Pow && verificationGateway != null) {
            PowVerificationWidget(verification, verificationGateway, { captchaAnswer = it; validationError = null }, revision = captchaRevision)
        } else if (verification is LoginVerification.Native) {
            OutlinedTextField(
                value = captchaAnswer,
                onValueChange = { captchaAnswer = it; validationError = null },
                label = { Text(verification.question) },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        form.notes.forEach { note ->
            Text(note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        validationError?.let { msg ->
            Text(msg, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }

        if (form.buttons.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                form.buttons.forEachIndexed { btnIndex, button ->
                    val batchBlocked = relayForm.isInventoryBatch && button.name == "inventory_op" && selectedCodeCount == 0
                    val btnEnabled = enabled && button.enabled && !batchBlocked && (verification == null || captchaAnswer.isNotBlank())
                    Button(
                        onClick = { handleSubmit(btnIndex, button) },
                        enabled = btnEnabled,
                        shape = RoundedCornerShape(12.dp),
                    ) { Text(button.label) }
                }
            }
        }
    }
}

/** 独立路由页：从通知或站内链接直接打开 /invite_relay/... 时使用。 */
@Composable
fun SourceInviteRelayScreen(
    session: Session,
    nav: NavHostController,
    url: String,
) {
    val account = session.account.loginState
    AccountPageGate(account.loggedIn && account.userId > 0, "邀请码接力", "登录后参与邀请码接力", nav) {
        WebScreen(
            session = session,
            nav = nav,
            sourceUrl = url.ifBlank { "/invite_relay" },
            title = "邀请码接力",
            initialContentSelector = ".invite-relay-page, .invite-relay-inventory, main",
        )
    }
}
