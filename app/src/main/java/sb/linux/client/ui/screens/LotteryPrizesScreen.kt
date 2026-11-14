package sb.linux.client.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import sb.linux.client.ui.LocalLinkHandler
import sb.linux.client.ui.components.feedback.*
import sb.linux.client.ui.components.navigation.AppPageScaffold
import sb.linux.client.ui.feature.account.UserPageViewModel
import sb.linux.client.ui.feature.account.UserPageViewModelFactory
import sb.linux.client.ui.session.Session
import sb.linux.client.ui.theme.LocalAppSpacing

@Composable
internal fun LotteryPrizesScreen(session: Session, nav: NavHostController) {
    val owner = session.account.loginState.userId
    val factory = remember(owner, session.accountIdentityVersion) {
        UserPageViewModelFactory(session.userPageRepository, owner, "notifications")
    }
    val model: UserPageViewModel = viewModel(key = "lottery-prizes-$owner-${session.accountIdentityVersion}", factory = factory)
    val state by model.uiState.collectAsState()
    LaunchedEffect(model, owner) { if (owner > 0) model.load(forceRefresh = false) }
    val prizes = state.content?.notifications.orEmpty().filter { it.lotteryPrize.isNotBlank() }
    val spacing = LocalAppSpacing.current
    val clipboard = LocalClipboardManager.current
    val openLink = LocalLinkHandler.current
    AppPageScaffold("抽奖奖品记录", onBack = { nav.popBackStack() }, actions = {
        sb.linux.client.ui.components.navigation.AppRefreshButton(onClick = { model.load() }, enabled = owner > 0, loading = state.loading)
    }) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when {
                owner <= 0 -> EmptyBox("登录后查看获奖记录")
                state.loading -> LoadingBox()
                state.error != null -> ErrorBox(state.error.orEmpty()) { model.load() }
                prizes.isEmpty() -> EmptyBox("暂无抽奖奖品记录", "从源站可读取的抽奖获奖通知中整理")
                else -> LazyColumn(contentPadding = PaddingValues(spacing.pageHorizontal),
                    verticalArrangement = Arrangement.spacedBy(spacing.item)) {
                    item { Text("${prizes.size} 条获奖记录", style = MaterialTheme.typography.labelLarge) }
                    itemsIndexed(prizes) { _, prize ->
                        Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surfaceContainerLow) {
                            Column(Modifier.fillMaxWidth().padding(spacing.pageHorizontal),
                                verticalArrangement = Arrangement.spacedBy(spacing.contentGap)) {
                                Text(prize.lotteryPrize, style = MaterialTheme.typography.titleMedium)
                                Text(prize.timeText, style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                                SelectionContainer { Text(prize.content, style = MaterialTheme.typography.bodyMedium) }
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(spacing.item)) {
                                    if (prize.redemptionCode.isNotBlank()) FilledTonalButton(onClick = {
                                        clipboard.setText(AnnotatedString(prize.redemptionCode)); session.showToast("兑换码已复制")
                                    }) { Text("复制兑换码") }
                                    if (prize.link.isNotBlank() && openLink != null) OutlinedButton(onClick = { openLink(prize.link) }) { Text("抽奖帖子") }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
