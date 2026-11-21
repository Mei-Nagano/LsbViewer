package sb.linux.client.data

import java.net.URLDecoder

/** 对齐源站 post_random_rewards；奖励金额只来自服务器通知。 */
internal object PostRewardNotice {
    const val COOKIE = "post_random_rewards_popup"
    private val format = Regex("""^(\d+):(\d+):(\d+)(?::((?:topic|reply|sofa|ur_reply)=\d+(?:,(?:topic|reply|sofa|ur_reply)=\d+)*)(?::([^:]+))?)?$""")
    private val labels = mapOf("topic" to "每日首帖奖励", "reply" to "每日首个回帖奖励", "sofa" to "抢沙发奖励", "ur_reply" to "互动奖励")

    fun message(raw: String, userId: String): String? {
        val value = decode(raw) ?: return null
        val match = format.matchEntire(value) ?: return null
        val groups = match.groupValues
        if (userId.isBlank() || groups[1] != userId) return null
        val activity = groups[2].toIntOrNull()?.takeIf { it in 0..100 } ?: return null
        val total = groups[3].toIntOrNull()?.takeIf { it in 1..3_000_000 } ?: return null
        val rewards = if (groups[4].isBlank()) emptyList() else groups[4].split(',').map { item ->
            val (kind, amount) = item.split('=')
            val points = amount.toIntOrNull()?.takeIf { it in 0..1_000_000 } ?: return null
            kind to points
        }
        if (rewards.isNotEmpty() && rewards.sumOf { it.second } != total) return null
        val interaction = rewards.firstOrNull { it.first == "ur_reply" }
        if (interaction != null) {
            val title = decode(groups[5]).orEmpty()
            val prefix = if (title.isNotBlank()) "【$title】互动奖励" else "互动奖励"
            return "$prefix +${interaction.second} 积分" +
                if (rewards.size > 1) "，本次共获得 $total 积分" else ""
        }
        return if (rewards.size == 1) "${labels.getValue(rewards.single().first)} +$total 积分"
            else "获得 $total 积分（活跃度 $activity）"
    }

    private fun decode(value: String): String? = try {
        // decodeURIComponent 保留加号；URLDecoder 的表单语义需要先转义它。
        URLDecoder.decode(value.replace("+", "%2B"), "UTF-8")
    } catch (_: IllegalArgumentException) { null }
}
