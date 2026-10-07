package sb.linux.client.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import sb.linux.client.ui.feature.topic.NewTopicViewModel

@Composable
internal fun NewTopicLoadEffect(
    viewModel: NewTopicViewModel,
    form: NewTopicFormState,
    editId: Long,
    onBodyChange: (String) -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(viewModel, editId) { viewModel.load(editId) }
    LaunchedEffect(state.formData, state.error, state.loading) {
        form.loading = state.loading
        state.error?.let { form.error = it }
        val initial = state.formData ?: return@LaunchedEffect
        form.forums = initial.forums
        form.announcement = initial.announcement
        form.sourceEditMeta = initial.sourceEditMeta
        form.replyOrder = initial.replyOrder
        form.replyOrderOptions = initial.replyOrderOptions
        form.specialType = initial.specialType
        form.forumId = initial.forumId
        if (editId > 0) {
            form.title = initial.title
            onBodyChange(initial.body)
            form.internalId = initial.internalId
            initial.lottery?.let { lottery ->
                form.drawAt = lottery.drawAt
                form.participantTarget = lottery.target
                form.minReplyChars = lottery.minChars
                form.replyCaptcha = lottery.captchaRequired
                if (lottery.prizes.isNotEmpty()) form.prizes = lottery.prizes
            }
            initial.virtualCard?.let { card ->
                form.cardName = card.name
                form.cardCurrency = card.currency
                form.cardPrice = card.price
                form.cardLimit = card.limit
                form.cardAutoReply = card.autoReply
                form.cardAutoReplyContent = card.autoReplyContent
                form.cardValues = card.values
            }
        } else {
            form.internalId = "0"
            if (state.hasRestorableDraft) form.restoreDraftOpen = true
        }
    }
}
