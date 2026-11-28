package sb.linux.client.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.SentimentSatisfied
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.TextNode
import sb.linux.client.common.network.Endpoints
import sb.linux.client.data.StickerPackHtmlParser
import sb.linux.client.ui.session.Session
import sb.linux.client.ui.*
import sb.linux.client.ui.theme.*
import sb.linux.client.ui.content.*
import sb.linux.client.ui.patterns.topic.*
import sb.linux.client.ui.patterns.user.*
import sb.linux.client.ui.components.feedback.*
import sb.linux.client.ui.components.layout.*
import sb.linux.client.ui.components.navigation.*
import sb.linux.client.domain.community.PmMessage
import sb.linux.client.ui.feature.community.ChatViewModel
import sb.linux.client.ui.feature.community.ChatViewModelFactory

/** 私信聊天界面：直接读取原站完整会话，不聚合或持久化本地消息。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(session: Session, nav: NavHostController) {
    val entry = nav.currentBackStackEntry ?: return
    val partnerId = entry.arguments?.getString("userId")?.toLongOrNull() ?: 0L
    val name = entry.arguments?.getString("name") ?: ""
    val avatarUrl = entry.arguments?.getString("avatar") ?: ""
    val factory = remember(session.privateMessageRepository, partnerId, name, avatarUrl) {
        ChatViewModelFactory(
            session.privateMessageRepository,
            partnerId,
            name,
            avatarUrl,
            session::showToast,
        )
    }
    val viewModel: ChatViewModel = viewModel(factory = factory, key = "chat-$partnerId")
    val state by viewModel.uiState.collectAsState()
    var emojiOpen by remember { mutableStateOf(false) }
    var stickerOpen by remember { mutableStateOf(false) }
    var draftField by remember(partnerId) { mutableStateOf(TextFieldValue(state.draft, TextRange(state.draft.length))) }
    var viewerImage by remember(partnerId) { mutableStateOf<ViewerImage?>(null) }
    LaunchedEffect(state.draft) {
        if (draftField.text != state.draft) {
            draftField = TextFieldValue(state.draft, TextRange(state.draft.length))
        }
    }
    val listState = rememberLazyListState()
    val spacing = LocalAppSpacing.current
    val scope = rememberCoroutineScope()
    val activeStickerPicker = state.stickerPicker?.takeIf { it.enabled }
    val activeStickerCollect = state.stickerCollect?.takeIf { it.enabled }

    LaunchedEffect(viewModel) { viewModel.sync() }
    // 消息变化自动滚到底部
    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) listState.scrollToItem(state.messages.size - 1)
    }

    AppPageScaffold(
        title = "",
        onBack = { nav.popBackStack() },
        // 顶栏对方头像与消息列表共用用户面板入口。
        titleContent = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(LocalAppSpacing.current.contentGap)) {
                UserAvatar(partnerId, state.avatarUrl, 30,
                    onProfile = { nav.navigate("user/$partnerId") })
                Text(state.name.ifBlank { "用户 $partnerId" }, style = MaterialTheme.typography.titleLarge,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        },
        actions = {
            sb.linux.client.ui.components.navigation.AppRefreshButton(enabled = !state.sending, loading = state.loading, onClick = viewModel::sync, description = "同步私信")
        },
        bottomBar = {
            // 输入工具区用色调表面，不再叠 6dp 阴影
            Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
                Row(
                    Modifier.fillMaxWidth().navigationBarsPadding().imePadding().padding(horizontal = spacing.pageHorizontal, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box {
                        IconButton(
                            onClick = {
                                if (activeStickerPicker != null) stickerOpen = true
                                else emojiOpen = true
                            },
                            enabled = state.canSend && !state.sending,
                        ) { Icon(Icons.Filled.SentimentSatisfied, "表情") }
                        DropdownMenu(expanded = emojiOpen, onDismissRequest = { emojiOpen = false }) {
                            listOf("😀", "😂", "🥰", "😎", "🤔", "👍", "👏", "🎉", "❤️", "🙏").chunked(5).forEach { row ->
                                Row {
                                    row.forEach { emoji ->
                                        TextButton(onClick = {
                                            val updated = replaceReplySelection(draftField, emoji)
                                            draftField = updated
                                            viewModel.setDraft(updated.text)
                                            emojiOpen = false
                                        }) { Text(emoji) }
                                    }
                                }
                            }
                        }
                    }
                    OutlinedTextField(
                        enabled = state.canSend && !state.sending,
                        value = draftField,
                        onValueChange = { next ->
                            draftField = next
                            viewModel.setDraft(next.text)
                        },
                        placeholder = { Text("输入私信内容（最多 ${state.maxLength} 字）") },
                        supportingText = { Text("${state.draft.codePointCount(0, state.draft.length)}/${state.maxLength}") },
                        isError = state.draft.codePointCount(0, state.draft.length) > state.maxLength,
                        modifier = Modifier.weight(1f),
                        maxLines = 4,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = { viewModel.send() }),
                        shape = MaterialTheme.shapes.large,
                    )
                    FilledIconButton(
                        onClick = viewModel::send,
                        enabled = state.draft.isNotBlank() &&
                            state.draft.codePointCount(0, state.draft.length) <= state.maxLength &&
                            !state.sending && state.canSend && partnerId > 0,
                    ) {
                        Icon(Icons.Filled.Send, "发送")
                    }
                }
            }
        }
    ) { pad ->
        when {
            partnerId <= 0 -> EmptyBox("无效的会话", "请返回消息列表重新选择联系人", Modifier.padding(pad))
            state.loading && state.messages.isEmpty() -> LoadingBox(Modifier.padding(pad))
            state.syncError != null && state.messages.isEmpty() -> ErrorBox(state.syncError!!, Modifier.padding(pad)) { viewModel.sync() }
            state.messages.isEmpty() -> EmptyBox(
                "还没有消息", "在这里发送第一条，或对方发来私信后，往来消息会显示在此",
                Modifier.padding(pad),
            )
            else -> LazyColumn(
                Modifier.padding(pad).fillMaxSize(),
                state = listState,
                contentPadding = PaddingValues(horizontal = spacing.pageHorizontal, vertical = spacing.pageVertical),
            ) {
                state.syncError?.let { message -> item { AppNotice(message, actionLabel = "重试同步", onAction = viewModel::sync) } }
                items(state.messages) { m ->
                    ChatBubble(
                        m = m,
                        canCollectSticker = activeStickerCollect != null,
                        onPartnerClick = { if (m.partnerId > 0) nav.navigate("user/${m.partnerId}") },
                        onImageClick = { viewerImage = it },
                        onQuickCollectSticker = { url, alt, onDone ->
                            val config = activeStickerCollect
                            if (config == null) {
                                onDone(false)
                            } else {
                                scope.launch {
                                    val result = session.stickerPackRepository.collectImage(config, url, alt)
                                    session.showToast(result.message)
                                    onDone(result.ok)
                                }
                            }
                        },
                    )
                }
            }
        }
    }
    if (stickerOpen && activeStickerPicker != null) {
        StickerPickerDialog(
            session = session,
            config = activeStickerPicker,
            onDismiss = { stickerOpen = false },
            onInsert = { item ->
                val start = draftField.selection.min.coerceIn(0, draftField.text.length)
                val end = draftField.selection.max.coerceIn(0, draftField.text.length)
                val candidate = draftField.text.replaceRange(start, end, item.markdown)
                if (candidate.codePointCount(0, candidate.length) > state.maxLength) {
                    "私信内容已达到字数上限"
                } else {
                    val updated = replaceReplySelection(draftField, item.markdown)
                    draftField = updated
                    viewModel.setDraft(updated.text)
                    null
                }
            },
        )
    }
    viewerImage?.let { target ->
        val collectConfig = state.stickerCollect?.takeIf { it.enabled }
        ZoomImageViewer(
            images = listOf(target),
            initialIndex = 0,
            onCollectSticker = collectConfig?.let { config ->
                { item -> session.stickerPackRepository.collectImage(config, item.url, item.caption) }
            },
            onDismiss = { viewerImage = null },
        )
    }
}

private sealed interface ChatContentPart {
    data class TextPart(val text: String) : ChatContentPart
    data class ImagePart(val url: String, val alt: String, val widthDp: Int, val isSticker: Boolean) : ChatContentPart
}

private fun parseChatContentParts(html: String, fallbackText: String): List<ChatContentPart> {
    if (html.isBlank() || !html.contains("<img", ignoreCase = true)) {
        return if (fallbackText.isNotBlank()) listOf(ChatContentPart.TextPart(fallbackText)) else emptyList()
    }
    val doc = Jsoup.parseBodyFragment(html, Endpoints.BASE).body()
    val parts = mutableListOf<ChatContentPart>()
    val textBuf = StringBuilder()
    fun flushText() {
        val t = textBuf.toString().trim('\n', '\r')
        if (t.isNotBlank()) parts += ChatContentPart.TextPart(t)
        textBuf.clear()
    }
    fun walk(node: org.jsoup.nodes.Node) {
        when (node) {
            is TextNode -> textBuf.append(node.wholeText)
            is Element -> when (node.normalName()) {
                "br" -> textBuf.append('\n')
                "img" -> {
                    val src = node.attr("src").trim()
                    if (src.isNotBlank()) {
                        flushText()
                        val absUrl = Endpoints.abs(src)
                        val isSticker = StickerPackHtmlParser.isStickerImageUrl(absUrl)
                        val width = node.attr("width").toIntOrNull()?.coerceIn(24, 220) ?: if (isSticker) 64 else 180
                        val alt = node.attr("alt").ifBlank { node.attr("title") }.trim()
                        parts += ChatContentPart.ImagePart(absUrl, alt, width, isSticker)
                    }
                }
                else -> {
                    node.childNodes().forEach(::walk)
                    if (node.normalName() in setOf("p", "div")) textBuf.append('\n')
                }
            }
        }
    }
    doc.childNodes().forEach(::walk)
    flushText()
    return parts.ifEmpty { if (fallbackText.isNotBlank()) listOf(ChatContentPart.TextPart(fallbackText)) else emptyList() }
}

/** 单条气泡：对方靠左、头像打开用户面板，我方靠右无头像，气泡着色区分 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChatBubble(
    m: PmMessage,
    canCollectSticker: Boolean = false,
    onPartnerClick: () -> Unit = {},
    onImageClick: (ViewerImage) -> Unit = {},
    onQuickCollectSticker: (String, String, (Boolean) -> Unit) -> Unit = { _, _, done -> done(false) },
) {
    val mine = !m.incoming
    val parts = remember(m.contentHtml, m.content) { parseChatContentParts(m.contentHtml, m.content) }
    BoxWithConstraints(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        // 气泡最宽为会话可用宽度的 80%，并限制在 480dp 内
        val bubbleMax = (maxWidth * 0.8f).coerceAtMost(480.dp)
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start,
            verticalAlignment = Alignment.Top
        ) {
        // 对方头像固定在消息左侧（QQ/微信式），48dp 触区包住 32dp 视觉头像
        if (!mine) {
            sb.linux.client.ui.patterns.user.UserAvatar(m.partnerId, m.avatarUrl, 32,
                onProfile = onPartnerClick, minTouchTarget = true)
            Spacer(Modifier.width(8.dp))
        }
        Column(horizontalAlignment = if (mine) Alignment.End else Alignment.Start) {
            Surface(
                shape = MaterialTheme.shapes.large,
                // 接收 surfaceContainer，发送 primaryContainer，前景使用成对 on 色
                color = if (mine) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.widthIn(max = bubbleMax),
            ) {
                Column(
                    Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    // 回复引用预览（QQ/微信式）：对方回复时引用的我方消息
                    if (m.quoteText.isNotBlank()) {
                        Text(
                            m.quoteText,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (mine) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f)
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                    }
                    val textColor = if (mine) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                    var idx = 0
                    while (idx < parts.size) {
                        val part = parts[idx]
                        when (part) {
                            is ChatContentPart.TextPart -> {
                                Text(
                                    part.text,
                                    // 私信正文 bodyLarge 16/24，不照搬帖子长文 18sp
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = textColor,
                                )
                                idx++
                            }
                            is ChatContentPart.ImagePart -> {
                                // 连续多个表情包横排展示，与源站 .direct-messages-content>img 风格一致
                                val run = mutableListOf<ChatContentPart.ImagePart>()
                                while (idx < parts.size && parts[idx] is ChatContentPart.ImagePart) {
                                    run += parts[idx] as ChatContentPart.ImagePart
                                    idx++
                                }
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    run.forEach { img ->
                                        val collectable = img.isSticker && canCollectSticker && StickerPackHtmlParser.isCollectableImageUrl(img.url)
                                        var showAddMenu by remember(img.url) { mutableStateOf(false) }
                                        var addingSticker by remember(img.url) { mutableStateOf(false) }
                                        Box {
                                            StickerAsyncImage(
                                                url = img.url,
                                                contentDescription = img.alt.ifBlank { "表情" },
                                                contentScale = ContentScale.Fit,
                                                modifier = Modifier
                                                    .widthIn(max = img.widthDp.dp)
                                                    .heightIn(max = if (img.isSticker) (img.widthDp * 2).dp else 240.dp)
                                                    .clip(MaterialTheme.shapes.small)
                                                    .combinedClickable(
                                                        onClick = {
                                                            onImageClick(
                                                                ViewerImage(
                                                                    url = img.url,
                                                                    caption = img.alt,
                                                                    collectable = StickerPackHtmlParser.isCollectableImageUrl(img.url),
                                                                ),
                                                            )
                                                        },
                                                        onLongClick = if (collectable) ({ showAddMenu = true }) else null,
                                                    ),
                                            )
                                            if (showAddMenu) {
                                                androidx.compose.ui.window.Popup(
                                                    alignment = Alignment.TopCenter,
                                                    offset = androidx.compose.ui.unit.IntOffset(0, -110),
                                                    onDismissRequest = { if (!addingSticker) showAddMenu = false },
                                                ) {
                                                    Surface(
                                                        shape = androidx.compose.foundation.shape.RoundedCornerShape(999.dp),
                                                        color = MaterialTheme.colorScheme.surface,
                                                        shadowElevation = 6.dp,
                                                        tonalElevation = 4.dp,
                                                    ) {
                                                        Button(
                                                            enabled = !addingSticker,
                                                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                                            shape = androidx.compose.foundation.shape.RoundedCornerShape(999.dp),
                                                            onClick = {
                                                                addingSticker = true
                                                                onQuickCollectSticker(img.url, img.alt) { ok ->
                                                                    addingSticker = false
                                                                    if (ok) showAddMenu = false
                                                                }
                                                            },
                                                        ) {
                                                            Text(if (addingSticker) "正在添加…" else "加入我的表情", style = MaterialTheme.typography.labelMedium)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (m.imageUrl.isNotBlank()) {
                        StickerAsyncImage(
                            url = m.imageUrl,
                            contentDescription = "私信图片",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .widthIn(max = 220.dp)
                                .heightIn(max = 260.dp)
                                .clip(MaterialTheme.shapes.small)
                                .clickable {
                                    onImageClick(
                                        ViewerImage(
                                            url = m.imageUrl,
                                            collectable = StickerPackHtmlParser.isCollectableImageUrl(m.imageUrl),
                                        ),
                                    )
                                },
                        )
                    }
                }
            }
            if (m.timeText.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(m.timeText, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            }
        }
        }
    }
}

// ---------------- 浏览历史（本地） ----------------
