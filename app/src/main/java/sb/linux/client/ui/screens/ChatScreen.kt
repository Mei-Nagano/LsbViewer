package sb.linux.client.ui.screens

import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.lifecycle.viewmodel.compose.viewModel
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
    var confirmBlock by remember { mutableStateOf(false) }
    var emojiOpen by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val spacing = LocalAppSpacing.current

    LaunchedEffect(viewModel) { viewModel.sync() }
    // 消息变化自动滚到底部
    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) listState.scrollToItem(state.messages.size - 1)
    }

    if (confirmBlock) sb.linux.client.ui.components.feedback.AppConfirmDialog(
        title = state.blockLabel,
        message = "按原站规则更新与该用户的私信关系。",
        onDismiss = { confirmBlock = false },
        onConfirm = { confirmBlock = false; viewModel.toggleBlock() },
        confirmText = "确认", dismissText = "取消",
    )
    AppPageScaffold(
        title = "",
        onBack = { nav.popBackStack() },
        // 顶栏对方头像同样可点击进入其个人主页
        titleContent = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    Modifier.clip(MaterialTheme.shapes.extraSmall).clickable(enabled = partnerId > 0) {
                        nav.navigate("user/$partnerId")
                    }
                ) { sb.linux.client.ui.patterns.user.UserAvatar(partnerId, state.avatarUrl, 30) }
                Text(state.name.ifBlank { "用户 $partnerId" }, style = MaterialTheme.typography.titleLarge,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        },
        actions = {
            if (state.blockLabel.isNotBlank()) TextButton(enabled = !state.blocking && !state.loading,
                onClick = { confirmBlock = true }) { Text(state.blockLabel) }
            IconButton(enabled = !state.loading && !state.sending, onClick = viewModel::sync) { Icon(Icons.Filled.Refresh, "同步私信") }
        },
        bottomBar = {
            // 输入工具区用色调表面，不再叠 6dp 阴影
            Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
                Row(
                    Modifier.fillMaxWidth().navigationBarsPadding().imePadding().padding(horizontal = spacing.pageHorizontal, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box {
                        IconButton(onClick = { emojiOpen = true }) { Icon(Icons.Filled.SentimentSatisfied, "表情") }
                        DropdownMenu(expanded = emojiOpen, onDismissRequest = { emojiOpen = false }) {
                            listOf("😀", "😂", "🥰", "😎", "🤔", "👍", "👏", "🎉", "❤️", "🙏").chunked(5).forEach { row ->
                                Row { row.forEach { emoji -> TextButton(onClick = { viewModel.setDraft(state.draft + emoji); emojiOpen = false }) { Text(emoji) } } }
                            }
                        }
                    }
                    OutlinedTextField(
                        enabled = state.canSend && !state.sending,
                        value = state.draft, onValueChange = viewModel::setDraft,
                        placeholder = { Text("输入私信内容（最多 500 字）") },
                        supportingText = { Text("${state.draft.codePointCount(0, state.draft.length)}/500") },
                        isError = state.draft.codePointCount(0, state.draft.length) > 500,
                        modifier = Modifier.weight(1f),
                        maxLines = 4,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = { viewModel.send() }),
                        shape = MaterialTheme.shapes.large,
                    )
                    FilledIconButton(onClick = viewModel::send, enabled = state.draft.isNotBlank() && state.draft.codePointCount(0, state.draft.length) <= 500 && !state.sending && state.canSend && partnerId > 0) {
                        Icon(Icons.Filled.Send, "发送")
                    }
                }
            }
        }
    ) { pad ->
        when {
            partnerId <= 0 -> Box(Modifier.padding(pad).fillMaxSize(), contentAlignment = Alignment.Center) { Text("无效的会话") }
            state.loading && state.messages.isEmpty() -> LoadingBox(Modifier.padding(pad))
            state.syncError != null && state.messages.isEmpty() -> ErrorBox(state.syncError!!, Modifier.padding(pad)) { viewModel.sync() }
            state.messages.isEmpty() -> EmptyBox(
                "还没有消息", "在这里发送第一条，或对方发来私信后，往来消息会显示在此",
                Modifier.padding(pad).padding(AppStateDefaults.pagePadding),
            )
            else -> LazyColumn(
                Modifier.padding(pad).fillMaxSize(),
                state = listState,
                contentPadding = PaddingValues(horizontal = spacing.pageHorizontal, vertical = spacing.pageVertical),
            ) {
                state.syncError?.let { message -> item { AppNotice(message, actionLabel = "重试同步", onAction = viewModel::sync) } }
                items(state.messages) { m ->
                    ChatBubble(m, onPartnerClick = { if (m.partnerId > 0) nav.navigate("user/${m.partnerId}") })
                }
            }
        }
    }
}

/** 单条气泡：对方靠左、头像在消息左侧（可点头像进对方主页），我方靠右无头像，气泡着色区分 */
@Composable
private fun ChatBubble(m: PmMessage, onPartnerClick: () -> Unit = {}) {
    val mine = !m.incoming
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
            Box(
                Modifier.size(48.dp).clip(MaterialTheme.shapes.small).clickable(onClick = onPartnerClick),
                contentAlignment = Alignment.Center,
            ) {
                Avatar(m.avatarUrl, 32)
            }
            Spacer(Modifier.width(8.dp))
        }
        Column(horizontalAlignment = if (mine) Alignment.End else Alignment.Start) {
            Surface(
                shape = MaterialTheme.shapes.large,
                // 接收 surfaceContainer，发送 primaryContainer，前景使用成对 on 色
                color = if (mine) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.widthIn(max = bubbleMax),
            ) {
                Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    // 回复引用预览（QQ/微信式）：对方回复时引用的我方消息
                    if (m.quoteText.isNotBlank()) {
                        Text(
                            m.quoteText,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (mine) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f)
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(bottom = 3.dp),
                        )
                    }
                    Text(
                        m.content,
                        // 私信正文 bodyLarge 16/24，不照搬帖子长文 18sp
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (mine) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                    )
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
