package sb.linux.client.domain.account

import sb.linux.client.domain.topic.TitleBadge

data class LoginState(
    val loggedIn: Boolean = false,
    val userId: Long = 0,
    val username: String = "",
    val points: String = "",
    val avatarUrl: String = "",
    val userGroup: String = "",
    val titleBadge: TitleBadge? = null, // 当前佩戴称号，与用户组、积分分开保存
)
