package sb.linux.client.domain.account

data class UserSummaryStat(val label: String, val value: String, val url: String = "")

data class UserSummary(
    val id: Long,
    val username: String,
    val avatar: String,
    val home: String,
    val subtitle: String,
    val bio: String,
    val stats: List<UserSummaryStat>,
    val canBlock: Boolean,
    val blocked: Boolean,
    val self: Boolean,
    val canFollow: Boolean,
    val canNotify: Boolean,
    val notify: String,
)

interface UserSummaryRepository {
    suspend fun load(userId: Long): UserSummary
    suspend fun toggleBlock(userId: Long): UserSummary
}
