@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package sb.linux.client.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.lifecycle.viewmodel.compose.viewModel
import sb.linux.client.ui.session.Session
import sb.linux.client.ui.*
import sb.linux.client.ui.feature.community.NotificationsViewModel
import sb.linux.client.ui.feature.community.NotificationsViewModelFactory

@Composable
fun NotificationsScreen(session: Session, nav: NavHostController) {
    val uid = session.account.loginState.userId
    val factory = remember(session.notificationRepository, uid, session) {
        NotificationsViewModelFactory(
            session.notificationRepository, uid, session::updateNotifUnread, session::showToast,
        )
    }
    val viewModel: NotificationsViewModel = viewModel(
        factory = factory,
        key = "notifications-$uid",
    )
    val state by viewModel.uiState.collectAsState()
    val items = state.items
    val loading = state.loading
    val error = state.error

    LaunchedEffect(viewModel) {
        if (session.account.loginState.loggedIn) viewModel.load()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("我的通知") },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
                actions = {
                    // 标记已读：源站在打开通知页时已同步标记为已读，这里仅更新界面展示
                    val anyUnread = items.any { it.unread }
                    TextButton(
                        onClick = viewModel::markRead,
                        enabled = items.isNotEmpty() && anyUnread,
                    ) { Text("标记已读") }
                    // 刷新：重新请求源站通知列表
                    IconButton(onClick = viewModel::load, enabled = !loading) {
                        Icon(Icons.Filled.Refresh, "刷新")
                    }
                }
            )
        }
    ) { pad ->
        when {
            !session.account.loginState.loggedIn -> Column(
                Modifier.padding(pad).fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("通知与源站账号绑定", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(6.dp))
                Text("登录后可查看系统与互动通知", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))
                Button(onClick = { nav.navigate("login") }) { Text("去登录") }
            }
            loading -> LoadingBox()
            error != null -> ErrorBox(error!!) { viewModel.load() }
            items.isEmpty() -> EmptyBox("暂无通知")
            else -> LazyColumn(Modifier.padding(pad), contentPadding = PaddingValues(vertical = 8.dp)) {
                // 通知内容可能完全重复：key 必须带 index 防止重复 key 崩溃
                itemsIndexed(items) { idx, n ->
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 5.dp),
                    ) {
                        Column(Modifier.padding(14.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(9.dp)
                            ) {
                                // 通知发起者头像：解析到 URL 时加载真实头像（用户头像/系统 dylan），
                                // 无头像时退回首字占位圆
                                if (n.partnerAvatar.isNotBlank()) {
                                    Avatar(n.partnerAvatar, 34)
                                } else {
                                    Box(
                                        Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            n.fromUser.take(1).ifBlank { "通" },
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
                                }
                                Column(Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(n.fromUser, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                                        // 源站未读标记（与源站「未读」标签一致）
                                        if (n.unread) {
                                            Text(
                                                "未读",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onError,
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(MaterialTheme.colorScheme.error)
                                                    .padding(horizontal = 6.dp, vertical = 1.dp),
                                            )
                                        }
                                    }
                                    Text(n.timeText, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Spacer(Modifier.height(10.dp))
                            Text(n.content, style = MaterialTheme.typography.bodyMedium, maxLines = 4)
                            // 操作入口：
                            // 帖子/系统通知带 /topic/ 链接 → 原生打开主题
                            // 私信通知带识别到的对方 uid → 打开聊天界面（本地聚合 + 可直接发送）
                            val topicId = Regex("""/topic/(\d+)""").find(n.link)?.groupValues?.get(1)
                            when {
                                topicId != null -> {
                                    Spacer(Modifier.height(4.dp))
                                    TextButton(onClick = { nav.navigate("topic/$topicId") }) { Text("查看主题") }
                                }
                                n.partnerId > 0 || n.link.contains("/notify/") -> {
                                    val pid = n.partnerId.takeIf { it > 0 }
                                        ?: Regex("""/notify/(\d+)""").find(n.link)?.groupValues?.get(1)?.toLongOrNull() ?: 0L
                                    if (pid > 0) {
                                        Spacer(Modifier.height(4.dp))
                                        TextButton(onClick = {
                                            nav.navigate("chat/$pid?name=${android.net.Uri.encode(n.fromUser)}&avatar=${android.net.Uri.encode(n.partnerAvatar)}")
                                        }) { Text("回复") }
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

// ---------------- 本地聚合私信聊天（QQ/微信式） ----------------
