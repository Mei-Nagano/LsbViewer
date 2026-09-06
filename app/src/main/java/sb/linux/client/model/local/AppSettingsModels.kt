package sb.linux.client.model.local

/** 本地保存的 AI 配置预设。 */
data class AiConfigPreset(
    val name: String,
    val url: String,
    val key: String,
    val model: String,
    val temperature: Float,
    val prompt: String,
    val includeComments: Boolean,
)

/** 本地记录的虚拟卡兑换条目。 */
data class CardRedemptionRecord(
    val topicId: Long,
    val topicTitle: String,
    val cardTitle: String,
    val code: String,
    val price: String,
    val sourceTime: String,
    val recordedAt: Long,
)

/** 本地统计事件。 */
data class UsageEvent(
    val type: String,
    val topicId: Long,
    val title: String,
    val value: Int,
    val at: Long,
)
