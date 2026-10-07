package sb.linux.client.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import sb.linux.client.domain.community.PmMessage
import sb.linux.client.ui.feature.community.ChatViewModel
import sb.linux.client.ui.feature.community.ChatViewModelFactory

/** 私信聊天界面：优先同步新版源站完整会话，本地聚合仅作离线兜底。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(session: Session, nav: NavHostController) {
    val entry = nav.currentBackStackEntry ?: return
    val partnerId = entry.arguments?.getString("userId")?.toLongOrNull() ?: 0L
    val name = entry.arguments?.getString("name") ?: ""
    val avatarUrl = entry.arguments?.getString("avatar") ?: ""
    val factory = remember(session.privateMessageRepository, session.privateMessageHistory, partnerId, name, avatarUrl) {
        ChatViewModelFactory(
            session.privateMessageRepository,
            session.privateMessageHistory,
            partnerId,
            name,
            avatarUrl,
            session::showToast,
        )
    }
    val viewModel: ChatViewModel = viewModel(factory = factory, key = "chat-$partnerId")
    val state by viewModel.uiState.collectAsState()
    var emojiOpen by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    LaunchedEffect(viewModel) { viewModel.sync() }
    // 消息变化自动滚到底部
    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) listState.scrollToItem(state.messages.size - 1)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        // 顶栏对方头像同样可点击进入其个人主页
                        Box(
                            Modifier.clip(CircleShape).clickable(enabled = partnerId > 0) {
                                nav.navigate("user/$partnerId")
                            }
                        ) { Avatar(state.avatarUrl, 30) }
                        Text(state.name.ifBlank { "用户 $partnerId" }, style = MaterialTheme.typography.titleMedium,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                },
                actions = { IconButton(enabled = !state.loading && !state.sending, onClick = viewModel::sync) { Icon(Icons.Filled.Refresh, "同步私信") } },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") }
                }
            )
        },
        bottomBar = {
            Surface(shadowElevation = 6.dp) {
                Row(
                    Modifier.fillMaxWidth().navigationBarsPadding().imePadding().padding(horizontal = 10.dp, vertical = 8.dp),
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
                        value = state.draft, onValueChange = viewModel::setDraft,
                        placeholder = { Text("输入私信内容") },
                        modifier = Modifier.weight(1f),
                        maxLines = 4,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = { viewModel.send() }),
                        shape = RoundedCornerShape(20.dp),
                    )
                    FilledIconButton(onClick = viewModel::send, enabled = state.draft.isNotBlank() && !state.sending && partnerId > 0) {
                        Icon(Icons.Filled.Send, "发送")
                    }
                }
            }
        }
    ) { pad ->
        when {
            partnerId <= 0 -> Box(Modifier.padding(pad).fillMaxSize(), contentAlignment = Alignment.Center) { Text("无效的会话") }
            state.loading && state.messages.isEmpty() -> Box(Modifier.padding(pad).fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            state.syncError != null && state.messages.isEmpty() -> ErrorBox(state.syncError!!) { viewModel.sync() }
            state.messages.isEmpty() -> Column(
                Modifier.padding(pad).fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
            ) {
                Text("还没有消息", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(6.dp))
                Text("在这里发送第一条，或对方发来私信后，往来消息会显示在此", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            else -> LazyColumn(
                Modifier.padding(pad).fillMaxSize(),
                state = listState,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
            ) {
                state.syncError?.let { message -> item { Text(message, color = MaterialTheme.colorScheme.error); TextButton(onClick = viewModel::sync) { Text("重试同步") } } }
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
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        // 对方头像固定在消息左侧（QQ/微信式），点击进入对方个人主页
        if (!mine) {
            Box(Modifier.clip(CircleShape).clickable(onClick = onPartnerClick)) {
                Avatar(m.avatarUrl, 32)
            }
            Spacer(Modifier.width(8.dp))
        }
        Column(horizontalAlignment = if (mine) Alignment.End else Alignment.Start) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (mine) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.widthIn(max = 280.dp)
            ) {
                Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    // 回复引用预览（QQ/微信式）：对方回复时引用的我方消息
                    if (m.quoteText.isNotBlank()) {
                        Text(
                            m.quoteText,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (mine) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.72f)
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(bottom = 3.dp),
                        )
                    }
                    Text(
                        m.content,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (mine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
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

// ---------------- 浏览历史（本地） ----------------
