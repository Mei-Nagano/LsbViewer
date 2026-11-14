package sb.linux.client.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.*
import sb.linux.client.domain.topic.*
import sb.linux.client.ui.session.Session

/** 面板位于主楼之后；查看结果只替换投票，不重建帖子和阅读位置。 */
@Composable
internal fun TopicInteractivePanels(
    page: TopicPageData, session: Session, actions: TopicScreenActions, ui: TopicScreenUiState,
    onLogin: () -> Unit, onRefresh: () -> Unit,
) {
    var displayedPoll by remember(page) { mutableStateOf(page.poll) }
    val interaction = actions.interactionViewModel
    fun vote(poll: TopicPoll, selected: List<TopicPollOption>, reason: String) {
        if (ui.pollBusy || ui.pollViewBusy) return
        ui.pollBusy = true
        interaction.vote(page, poll, selected, reason) { result ->
            result.onSuccess { session.showToast(it.message); if (it.succeeded) onRefresh() }
                .onFailure { session.showToast(it.message ?: "投票失败") }
            ui.pollBusy = false
        }
    }
    Column {
        if (displayedPoll != null || page.essenceReview != null || page.essenceApplication != null) TopicPollView(
            poll = displayedPoll, essence = page.essenceReview, application = page.essenceApplication,
            loggedIn = session.account.loginState.loggedIn, busy = ui.pollBusy || ui.essenceBusy,
            viewBusy = ui.pollViewBusy, topUpBusy = ui.essenceTopUpBusy, onLogin = onLogin,
            onView = {
                val poll = displayedPoll
                if (poll != null && !ui.pollBusy && !ui.pollViewBusy && poll.viewUrl.isNotBlank()) {
                    ui.pollViewBusy = true
                    interaction.loadPollView(page.topicId, poll.viewUrl) { result ->
                        result.onSuccess { displayedPoll = it }.onFailure { session.showToast(it.message ?: "读取投票失败") }
                        ui.pollViewBusy = false
                    }
                }
            },
            onVote = { selected, reason -> displayedPoll?.let { vote(it, selected, reason) } },
            onApply = { application ->
                ui.essenceBusy = true
                interaction.applyForEssence(page, application) { result ->
                    result.onSuccess { session.showToast(it.message); if (it.succeeded) onRefresh() }
                        .onFailure { session.showToast(it.message ?: "申请失败") }
                    ui.essenceBusy = false
                }
            },
            onTopUp = { amount ->
                page.essenceReview?.topUp?.let { topUp ->
                    ui.essenceTopUpBusy = true
                    interaction.topUpEssence(page, topUp, amount) { result ->
                        result.onSuccess { session.showToast(it.message); if (it.succeeded) onRefresh() }
                            .onFailure { session.showToast(it.message ?: "追加失败") }
                        ui.essenceTopUpBusy = false
                    }
                }
            },
        )
        page.resourceVote?.let { panel ->
            ResourceVoteCard(panel, session.account.loginState.loggedIn, ui.pollBusy, onLogin) { selected ->
                vote(panel.poll, selected, "")
            }
        }
        TopicFooterSourceActions(page, session, interaction, onRefresh)
    }
}
