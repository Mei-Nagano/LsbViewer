package sb.linux.client.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import sb.linux.client.ui.components.navigation.AppPageScaffold
import sb.linux.client.ui.feature.community.DirectMessagesViewModel
import sb.linux.client.ui.feature.community.DirectMessagesViewModelFactory
import sb.linux.client.ui.feature.community.NotificationsViewModel
import sb.linux.client.ui.feature.community.NotificationsViewModelFactory
import sb.linux.client.ui.session.Session
import sb.linux.client.ui.theme.LocalAppSpacing
import android.net.Uri

/** 「消息」根的两个分区；只聚合表现层入口，不新增消息服务或本地消息库。 */
private enum class MessagesSection(val label: String) {
    DIRECT("私信"),
    NOTIFICATIONS("通知"),
}

private fun sectionOf(raw: String?): MessagesSection =
    if (raw == "notifications") MessagesSection.NOTIFICATIONS else MessagesSection.DIRECT

/**
 * 消息根（P08）：顶栏「消息」+ 私信/通知分段。
 *
 * 两个分区各自保留查询、分页、未读与滚动状态；旧 directMessagesRoot 直达私信，
 * 旧 notifications 仍走 [NotificationsScreen]，不因改名改变来源返回语义。
 */
@Composable
fun MessagesScreen(
    session: Session,
    nav: NavHostController,
    initialSection: String = "dm",
    showBack: Boolean = false,
) {
    val spacing = LocalAppSpacing.current
    var section by rememberSaveable { mutableStateOf(sectionOf(initialSection).name) }
    val active = MessagesSection.valueOf(section)
    val uid = session.account.loginState.userId

    val dmFactory = remember(session.privateMessageRepository) {
        DirectMessagesViewModelFactory(session.privateMessageRepository)
    }
    val dmViewModel: DirectMessagesViewModel = viewModel(factory = dmFactory, key = "messages-dm-$uid")
    val dmState by dmViewModel.uiState.collectAsState()

    val notifFactory = remember(session.notificationRepository, uid, session) {
        NotificationsViewModelFactory(
            session.notificationRepository, uid, session::updateNotifUnread, session::showToast,
        )
    }
    val notifViewModel: NotificationsViewModel =
        viewModel(factory = notifFactory, key = "messages-notifications-$uid")
    val notifState by notifViewModel.uiState.collectAsState()

    val blockedRoute = "web?url=" + Uri.encode("https://linux.sb/direct_messages?view=blocked")
    AppPageScaffold(
        title = "消息",
        onBack = if (showBack) ({ nav.popBackStack() }) else null,
        actions = {
            if (active == MessagesSection.DIRECT) {
                TextButton(onClick = { nav.navigate(blockedRoute) }) { Text("拉黑名单") }
                IconButton(onClick = dmViewModel::load) { Icon(Icons.Filled.Refresh, "刷新") }
            } else {
                val anyUnread = notifState.items.any { it.unread }
                TextButton(
                    onClick = notifViewModel::markRead,
                    enabled = notifState.items.isNotEmpty() && anyUnread,
                ) { Text("标记已读") }
                IconButton(onClick = notifViewModel::load, enabled = !notifState.loading) {
                    Icon(Icons.Filled.Refresh, "刷新")
                }
            }
        },
    ) { pad ->
        Column(Modifier.padding(pad)) {
            SingleChoiceSegmentedButtonRow(
                Modifier.fillMaxWidth().padding(
                    horizontal = spacing.pageHorizontal,
                    vertical = spacing.item,
                ),
            ) {
                MessagesSection.entries.forEachIndexed { index, item ->
                    SegmentedButton(
                        selected = active == item,
                        onClick = { section = item.name },
                        shape = SegmentedButtonDefaults.itemShape(index, MessagesSection.entries.size),
                    ) { Text(item.label) }
                }
            }
            when (active) {
                MessagesSection.DIRECT -> DirectMessagesSection(
                    session = session,
                    nav = nav,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                    viewModel = dmViewModel,
                    state = dmState,
                )
                MessagesSection.NOTIFICATIONS -> NotificationsSection(
                    session = session,
                    nav = nav,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                    state = notifState,
                    onRetry = notifViewModel::load,
                )
            }
        }
    }

    // 分区首次显示时才加载对应来源；loaded 独立于 loading，避免初始 loading 挡住首次请求。
    LaunchedEffect(active, session.account.loginState.loggedIn, uid) {
        if (!session.account.loginState.loggedIn) return@LaunchedEffect
        when (active) {
            MessagesSection.DIRECT -> if (!dmState.loaded) dmViewModel.load()
            MessagesSection.NOTIFICATIONS -> if (!notifState.loaded) notifViewModel.load()
        }
    }
}
