package sb.linux.client.domain.topic

data class ForumOption(val id: String, val name: String)

data class PrizeRow(
    val name: String = "",
    val type: String = "points",
    val quantity: String = "1",
    val value: String = "",
)

data class SourceEditMeta(
    val confirmation: String = "",
    val notice: String = "",
    val hiddenFields: Map<String, String> = emptyMap(),
)

data class LotteryInit(
    val drawAt: String,
    val target: String,
    val minChars: String,
    val captchaRequired: Boolean,
    val prizes: List<PrizeRow>,
)

data class CardInit(
    val name: String,
    val currency: String,
    val price: String,
    val limit: String,
    val autoReply: Boolean,
    val autoReplyContent: String,
    val values: String,
)

data class NewTopicFormData(
    val forums: List<ForumOption>,
    val forumId: String,
    val announcement: String,
    val sourceEditMeta: SourceEditMeta,
    val replyOrder: String,
    val replyOrderOptions: List<Pair<String, String>>,
    val specialType: String,
    val title: String,
    val body: String,
    val internalId: String,
    val lottery: LotteryInit?,
    val virtualCard: CardInit?,
)

data class NewTopicSubmission(
    val editId: Long,
    val loggedIn: Boolean,
    val internalId: String,
    val forumId: String,
    val title: String,
    val body: String,
    val hiddenFields: Map<String, String>,
    val replyOrder: String,
    val specialType: String,
    val drawAt: String,
    val participantTarget: String,
    val minReplyChars: String,
    val replyCaptcha: Boolean,
    val prizes: List<PrizeRow>,
    val cardName: String,
    val cardCurrency: String,
    val cardPrice: String,
    val cardLimit: String,
    val cardAutoReply: Boolean,
    val cardAutoReplyContent: String,
    val cardValues: String,
)

interface NewTopicRepository {
    suspend fun load(editId: Long): NewTopicFormData
    suspend fun submit(input: NewTopicSubmission): String?
}
