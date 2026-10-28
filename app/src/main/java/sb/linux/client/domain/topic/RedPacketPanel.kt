package sb.linux.client.domain.topic

/** 金额、状态、参与条件直接来自原站，不在客户端推算分配规则。 */
data class RedPacketPanel(
    val title: String,
    val status: String,
    val remaining: String,
    val details: List<String>,
    val rules: List<String>,
)
