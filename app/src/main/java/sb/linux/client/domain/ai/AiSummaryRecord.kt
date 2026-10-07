package sb.linux.client.domain.ai

data class AiSummaryRecord(
    val topicId: Long,
    val title: String,
    val summary: String,
    val savedAt: Long,
    val recordId: String = topicId.toString(),
    val favorite: Boolean = false,
)
