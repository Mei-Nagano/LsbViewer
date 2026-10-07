package sb.linux.client.domain.gacha

data class CardRedemptionRecord(
    val topicId: Long,
    val topicTitle: String,
    val cardTitle: String,
    val code: String,
    val price: String,
    val sourceTime: String,
    val recordedAt: Long,
)
