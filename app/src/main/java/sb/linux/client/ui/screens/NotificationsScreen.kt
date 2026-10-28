@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package sb.linux.client.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.lifecycle.viewmodel.compose.viewModel
import sb.linux.client.ui.components.feedback.EmptyBox
import sb.linux.client.ui.components.feedback.ErrorBox
import sb.linux.client.ui.components.feedback.LoadingBox
import sb.linux.client.ui.components.feedback.LoginRequiredBox
import sb.linux.client.ui.components.navigation.AppPageScaffold
import sb.linux.client.ui.feature.community.NotificationsUiState
import sb.linux.client.ui.feature.community.NotificationsViewModel
import sb.linux.client.ui.feature.community.NotificationsViewModelFactory
import sb.linux.client.ui.session.Session
import sb.linux.client.ui.theme.LocalAppSpacing

/** 通知页（兼容旧 notifications 路由）：完整页面外壳 + 返回。 */
@Composable
fun NotificationsScreen(session: Session, nav: NavHostController) {
    val uid = session.account.loginState.userId
    val factory = remember(session.notificationRepository, uid, session) {
        NotificationsViewModelFactory(
            session.notificationRepository, uid, session::updateNotifUnread, session::showToast,
        )
    }
    val viewModel: NotificationsViewModel = viewModel(factory = factory, key = "notifications-$uid")
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(viewModel) {
        if (session.account.loginState.loggedIn) viewModel.load()
    }

    AppPageScaffold(
        title = "我的通知",
        onBack = { nav.popBackStack() },
        actions = {
            // 源站在打开通知页时已同步标记为已读，这里只更新界面展示
            val anyUnread = state.items.any { it.unread }
            TextButton(onClick = viewModel::markRead, enabled = state.items.isNotEmpty() && anyUnread) {
                Text("标记已读")
            }
            IconButton(onClick = viewModel::load, enabled = !state.loading) {
                Icon(Icons.Filled.Refresh, "刷新")
            }
        },
    ) { pad ->
        NotificationsSection(session, nav, pad, state, onRetry = viewModel::load)
    }
}

/** 通知分区内容；由「消息」根分区宿主与旧路由共用。 */
@Composable
internal fun NotificationsSection(
    session: Session,
    nav: NavHostController,
    contentPadding: PaddingValues,
    state: NotificationsUiState,
    onRetry: () -> Unit,
) {
    val spacing = LocalAppSpacing.current
    when {
        !session.account.loginState.loggedIn -> LoginRequiredBox(
            "通知与源站账号绑定", "登录后可查看系统与互动通知", Modifier.padding(contentPadding),
        ) { nav.navigate("login") }
        state.loading -> LoadingBox(Modifier.padding(contentPadding))
        state.error != null -> ErrorBox(state.error!!, Modifier.padding(contentPadding), onRetry = onRetry)
        state.items.isEmpty() -> EmptyBox("暂无通知", modifier = Modifier.padding(contentPadding))
        else -> Column(Modifier.padding(contentPadding).fillMaxSize()) {
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = spacing.item),
            ) {
                // 通知内容可能完全重复：key 必须带 index 防止重复 key 崩溃
                itemsIndexed(state.items) { _, item ->
                    NotificationSummaryCard(item, nav)
                }
            }
        }
    }
}
