package sb.linux.client.ui.screens

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.navigation.NavHostController
import androidx.lifecycle.viewmodel.compose.viewModel
import sb.linux.client.ui.components.feedback.LoginRequiredBox
import sb.linux.client.ui.components.feedback.EmptyBox
import sb.linux.client.ui.components.feedback.ErrorBox
import sb.linux.client.ui.components.feedback.LoadingBox
import sb.linux.client.ui.components.navigation.AppPageScaffold
import sb.linux.client.ui.feature.community.DirectMessagesUiState
import sb.linux.client.ui.feature.community.DirectMessagesViewModel
import sb.linux.client.ui.feature.community.DirectMessagesViewModelFactory
import sb.linux.client.ui.patterns.user.UserAvatar
import sb.linux.client.ui.session.Session
import sb.linux.client.ui.theme.LocalAppSpacing

/** 私信联系人页（兼容旧 directMessages 路由）：完整页面外壳 + 返回。 */
@Composable
fun DirectMessagesScreen(session: Session, nav: NavHostController) {
    val factory = remember(session.privateMessageRepository) {
        DirectMessagesViewModelFactory(session.privateMessageRepository)
    }
    val viewModel: DirectMessagesViewModel = viewModel(factory = factory)
    val state by viewModel.uiState.collectAsState()
    val blockedRoute = "web?url=" + Uri.encode("https://linux.sb/direct_messages?view=blocked")
    LaunchedEffect(viewModel, session.account.loginState.loggedIn) {
        if (session.account.loginState.loggedIn) viewModel.load()
    }

    AppPageScaffold(
        title = "我的私信",
        onBack = { nav.popBackStack() },
        actions = {
            TextButton(onClick = { nav.navigate(blockedRoute) }) { Text("拉黑名单") }
            IconButton(onClick = viewModel::load) { Icon(Icons.Filled.Refresh, "刷新") }
        },
    ) { pad ->
        DirectMessagesSection(session, nav, pad, viewModel, state)
    }
}

/**
 * 私信分区内容：搜索、联系人列表与各种加载状态。
 * 由「消息」根的分区宿主与旧路由共用，业务状态仍由调用方持有的 ViewModel 提供。
 */
@Composable
internal fun DirectMessagesSection(
    session: Session,
    nav: NavHostController,
    contentPadding: PaddingValues,
    viewModel: DirectMessagesViewModel,
    state: DirectMessagesUiState,
) {
    val spacing = LocalAppSpacing.current
    Column(Modifier.padding(contentPadding).fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = spacing.pageHorizontal, vertical = spacing.item),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::setQuery,
                modifier = Modifier.weight(1f),
                label = { Text("搜索用户名") },
                placeholder = { Text("输入用户名，发起对话") },
                singleLine = true,
            )
            TextButton(onClick = viewModel::load) { Text("搜索") }
        }
        when {
            !session.account.loginState.loggedIn ->
                LoginRequiredBox("我的私信", "登录后查看原站私信") { nav.navigate("login") }
            state.loading -> LoadingBox()
            state.error != null -> ErrorBox(state.error!!, onRetry = viewModel::load)
            state.contacts.isEmpty() -> EmptyBox("暂无私信")
            else -> LazyColumn(Modifier.fillMaxSize()) {
                items(state.contacts, key = { it.userId }) { contact ->
                    ListItem(
                        headlineContent = {
                            Text(contact.username, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        },
                        supportingContent = {
                            Text(contact.preview, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        },
                        leadingContent = { UserAvatar(contact.userId, contact.avatarUrl, 40, minTouchTarget = true) },
                        trailingContent = {
                            Text(contact.timeText, style = MaterialTheme.typography.labelSmall)
                        },
                        modifier = Modifier.clickable {
                            nav.navigate(
                                "chat/${contact.userId}" +
                                    "?name=${Uri.encode(contact.username)}" +
                                    "&avatar=${Uri.encode(contact.avatarUrl)}",
                            )
                        },
                    )
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
                    )
                }
            }
        }
    }
}
