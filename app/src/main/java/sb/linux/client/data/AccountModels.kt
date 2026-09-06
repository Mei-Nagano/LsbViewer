package sb.linux.client.data

/** 邀请中心（/invite_center）：分享链接 + 奖励统计 + 已邀请用户。 */
data class InviteCenter(
    val link: String,
    val desc: String,
    val rule: String,
    val invitedCount: String,
    val firstReward: String,
    val secondReward: String,
    val users: List<InviteUser> = emptyList(),
)

/** 通过分享链接注册的用户（.invite-center-list 列表项）。 */
data class InviteUser(
    val name: String,
    val userId: Long = 0,
    val statusText: String = "",
)
