package sb.linux.client.ui.screens
import sb.linux.client.ui.session.Session


import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.*
import sb.linux.client.ui.feature.topic.TopicInteractionViewModel
import sb.linux.client.domain.topic.TopicPageData
import sb.linux.client.domain.ai.AiAssistantSettings
internal fun LazyListScope.topicHeaderItem(
    d: TopicPageData,
    session: Session,
    interactionViewModel: TopicInteractionViewModel,
    aiSettings: AiAssistantSettings,
    readingStats: Pair<Int, Int>?,
    aiSummary: String?,
    aiBusy: Boolean,
    aiError: String?,
    onRunAi: () -> Unit,
    onRegenerateAi: () -> Unit,
    onDismissAi: () -> Unit,
    onJoinReply: () -> Unit,
    onRefresh: () -> Unit,
    onManageNodes: () -> Unit,
    onTitleBottomInRoot: (Float) -> Unit,
) {
item(key = "header") {
                        Column {
                            // 完整标题独立成段（22/32 加粗，对齐 v1.0.70 的标题观感），元信息另起一行 bodySmall；
                            // onGloballyPositioned：标题行滑出顶栏后，顶栏切换为显示标题（11）
                            Text(
                                d.title,
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontSize = 22.sp,
                                    lineHeight = 32.sp,
                                ),
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .semantics { heading() }
                                    .onGloballyPositioned { onTitleBottomInRoot(it.boundsInRoot().bottom) }
                                    .padding(
                                        start = 16.dp,
                                        end = 16.dp,
                                        top = 10.dp,
                                        bottom = 6.dp
                                    ),
                            )
                            Text(
                                buildString {
                                    append(d.viewsText.ifBlank { "-" })
                                    append("阅读 · ")
                                    append(d.repliesText.ifBlank { "${d.posts.size}" })
                                    append("评论")
                                    readingStats?.let { (words, minutes) ->
                                        append(" · ${words}字 · 约${minutes}分钟")
                                    }
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
                            )
                            TopicNodeLinks(d.nodes, session.account.loginState.loggedIn, onManageNodes)
                            // AI 总结：开启总开关后显示入口，默认等用户点按钮才请求
                            if (aiSettings.enabled) {
                                AiSummaryCard(
                                    summary = aiSummary,
                                    busy = aiBusy,
                                    error = aiError,
                                    enabled = aiSettings.apiUrl.isNotBlank() && aiSettings.model.isNotBlank(),
                                    onRun = { onRunAi() },
                                    onRegenerate = { onRegenerateAi() },
                                    onDismiss = onDismissAi,
                                )
                            }
                            // 抽奖面板
                            d.lottery?.let { lp ->
                                LotteryPanelView(
                                    lp = lp,
                                    loggedIn = session.account.loginState.loggedIn,
                                    selfName = session.account.loginState.username,
                                    onJoin = onJoinReply,
                                    onJoinForm = { option ->
                                        interactionViewModel.joinLottery(d, option) { result ->
                                            result.onSuccess { session.showToast(it.message); onRefresh() }
                                                .onFailure { session.showToast(it.message ?: "失败") }
                                        }
                                    },
                                )
                            }
                            // 发卡帖兑换卡片
                            d.virtualCard?.let { vc ->
                                VirtualCardView(
                                    vc = vc,
                                    loggedIn = session.account.loginState.loggedIn,
                                    onBuy = {
                                        interactionViewModel.buyVirtualCard(d, vc) { result ->
                                            result.onSuccess {
                                                session.showToast(it.message)
                                                if (it.succeeded) onRefresh()
                                            }.onFailure { session.showToast(it.message ?: "失败") }
                                        }
                                    }
                                )
                            }
                        }
                    }
}
