package sb.linux.client.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import sb.linux.client.ui.LocalLinkHandler
import sb.linux.client.ui.components.feedback.*
import sb.linux.client.ui.feature.account.UserPageViewModel
import sb.linux.client.ui.feature.account.UserPageViewModelFactory
import sb.linux.client.ui.patterns.settings.SettingsCard
import sb.linux.client.ui.session.Session
import sb.linux.client.ui.theme.LocalAppSpacing

/** 与抽奖奖品共用完整通知缓存；内容相同的不同兑换通知仍保留为独立记录。 */
@Composable
fun CardRedemptionsScreen(session: Session, nav: NavHostController) {
    val owner = session.account.loginState.userId
    val factory = remember(owner, session.accountIdentityVersion) {
        UserPageViewModelFactory(session.userPageRepository, owner, "notifications")
    }
    val model: UserPageViewModel = viewModel(key = "card-notifications-$owner-${session.accountIdentityVersion}", factory = factory)
    val state by model.uiState.collectAsState()
    LaunchedEffect(model, owner) { if (owner > 0) model.load(forceRefresh = false) }
    val records = state.content?.notifications.orEmpty().filter { it.cardRedemptionTitle.isNotBlank() }
    val clipboard = LocalClipboardManager.current
    val openLink = LocalLinkHandler.current
    val spacing = LocalAppSpacing.current
    SettingsPage("发卡兑换记录", nav, headerActions = {
        sb.linux.client.ui.components.navigation.AppRefreshButton(onClick = { model.load() }, enabled = owner > 0 && !state.loading)
    }) {
        when {
            owner <= 0 -> EmptyBox("登录后查看兑换记录")
            records.isEmpty() && state.loading -> LoadingBox(Modifier.fillMaxWidth().height(160.dp))
            records.isEmpty() && state.error != null -> ErrorBox(state.error.orEmpty()) { model.load() }
            records.isEmpty() -> EmptyBox("暂无发卡兑换记录", "从源站兑换通知中整理")
            else -> {
                Text("${records.size} 条兑换记录", style = MaterialTheme.typography.labelLarge)
                if (state.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
                state.error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                records.forEachIndexed { index, record ->
                    var revealed by remember(owner, index, record) { mutableStateOf(false) }
                    SettingsCard {
                        Column(Modifier.fillMaxWidth().padding(spacing.pageHorizontal), verticalArrangement = Arrangement.spacedBy(spacing.item)) {
                            Text(record.cardRedemptionTitle, style = MaterialTheme.typography.titleMedium)
                            Text("${record.timeText} · ${record.cardRedemptionCost} 积分", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (record.redemptionCode.isBlank()) SelectionContainer {
                                Text(record.content, style = MaterialTheme.typography.bodyMedium)
                            } else {
                                SelectionContainer { Text(if (revealed) record.redemptionCode else "••••••", style = MaterialTheme.typography.bodyMedium) }
                            }
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(spacing.item)) {
                                if (record.redemptionCode.isNotBlank()) {
                                    TextButton(onClick = { revealed = !revealed }) { Text(if (revealed) "隐藏兑换码" else "查看兑换码") }
                                    FilledTonalButton(onClick = {
                                        clipboard.setText(AnnotatedString(record.redemptionCode)); session.showToast("兑换码已复制")
                                    }) { Text("复制兑换码") }
                                }
                                if (record.link.isNotBlank() && openLink != null) OutlinedButton(onClick = { openLink(record.link) }) { Text("发卡帖子") }
                            }
                        }
                    }
                }
            }
        }
    }
}
