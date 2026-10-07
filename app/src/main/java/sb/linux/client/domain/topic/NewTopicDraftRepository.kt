package sb.linux.client.domain.topic

data class NewTopicDraft(
    val title: String,
    val body: String,
    val forumId: String,
    val options: NewTopicDraftOptions?,
)

data class NewTopicDraftOptions(
    val specialType: String,
    val drawAt: String,
    val participantTarget: String,
    val minReplyChars: String,
    val replyCaptcha: Boolean,
    val prizes: List<PrizeRow>?,
    val cardName: String,
    val cardCurrency: String,
    val cardPrice: String,
    val cardLimit: String,
    val cardAutoReply: Boolean,
    val cardAutoReplyContent: String,
    val cardValues: String,
)

interface NewTopicDraftRepository {
    fun save(draft: NewTopicDraft)
    fun load(): NewTopicDraft
    fun hasRestorableDraft(): Boolean
    fun hasSavedDraft(): Boolean
    fun clear()
}
