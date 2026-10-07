package sb.linux.client.ui.screens

import sb.linux.client.domain.topic.NewTopicDraft
import sb.linux.client.domain.topic.NewTopicDraftOptions

internal fun NewTopicFormState.toDraft(
    title: String,
    body: String,
    forumId: String?,
) = NewTopicDraft(
    title = title,
    body = body,
    forumId = forumId.orEmpty(),
    options = NewTopicDraftOptions(
        specialType = specialType,
        drawAt = drawAt,
        participantTarget = participantTarget,
        minReplyChars = minReplyChars,
        replyCaptcha = replyCaptcha,
        prizes = prizes,
        cardName = cardName,
        cardCurrency = cardCurrency,
        cardPrice = cardPrice,
        cardLimit = cardLimit,
        cardAutoReply = cardAutoReply,
        cardAutoReplyContent = cardAutoReplyContent,
        cardValues = cardValues,
    ),
)

internal fun NewTopicFormState.restoreDraft(draft: NewTopicDraft): String {
    title = draft.title
    draft.forumId.takeIf { it.isNotBlank() }?.let { forumId = it }
    draft.options?.let { options ->
        specialType = options.specialType
        drawAt = options.drawAt
        participantTarget = options.participantTarget
        minReplyChars = options.minReplyChars
        replyCaptcha = options.replyCaptcha
        options.prizes?.let { prizes = it }
        cardName = options.cardName
        cardCurrency = options.cardCurrency
        cardPrice = options.cardPrice
        cardLimit = options.cardLimit
        cardAutoReply = options.cardAutoReply
        cardAutoReplyContent = options.cardAutoReplyContent
        cardValues = options.cardValues
    }
    return draft.body
}
