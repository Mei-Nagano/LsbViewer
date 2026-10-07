package sb.linux.client.domain.community

data class InviteCenter(
    val link: String,
    val desc: String,
    val rule: String,
    val invitedCount: String,
    val firstReward: String,
    val secondReward: String,
    val users: List<InviteUser> = emptyList(),
)

data class InviteUser(val name: String, val userId: Long = 0, val statusText: String = "")

interface InviteRepository {
    suspend fun loadInvitations(): InviteCenter
}
