package sb.linux.client.ui.screens

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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

/** 私信根与独立联系人页共用同一列表，返回按钮由路由决定。 */
@Composable
fun DirectMessagesScreen(session: Session, nav: NavHostController, showBack: Boolean = true) {
    val uid = session.account.loginState.userId
    val identity = session.accountIdentityVersion
    val factory = remember(session.privateMessageRepository) {
        DirectMessagesViewModelFactory(session.privateMessageRepository)
    }
    val viewModel: DirectMessagesViewModel = viewModel(factory = factory, key = "private-messages:$uid:$identity")
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(viewModel, session.account.loginState.loggedIn) {
        if (session.account.loginState.loggedIn && !state.loaded) viewModel.load()
    }

    AppPageScaffold(
        title = if (showBack) "我的私信" else "私信",
        primaryPage = !showBack,
        onBack = if (showBack) ({ nav.popBackStack() }) else null,
        actions = {
            sb.linux.client.ui.components.navigation.AppRefreshButton(onClick = viewModel::load, enabled = !state.loading)
        },
    ) { pad ->
        DirectMessagesSection(session, nav, pad, viewModel, state)
    }
}

/**
 * 私信分区内容：联系人列表与各种加载状态。
 * 根页面与旧独立路由共用，业务状态仍由调用方持有的 ViewModel 提供。
 */
@Composable
internal fun DirectMessagesSection(
    session: Session,
    nav: NavHostController,
    contentPadding: PaddingValues,
    viewModel: DirectMessagesViewModel,
    state: DirectMessagesUiState,
) {
    Column(Modifier.padding(contentPadding).fillMaxSize()) {
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
