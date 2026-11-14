package sb.linux.client.ui.screens

import androidx.compose.runtime.Composable
import sb.linux.client.domain.account.LoginState
import sb.linux.client.ui.patterns.user.UserInfoCard
import sb.linux.client.ui.patterns.user.toUserInfoCardData

/** 我的页只提供账号名称与导航；所有排版由公共个人卡片负责。 */
@Composable
internal fun MeAccountCard(account: LoginState, onOpenProfile: () -> Unit) {
    val name = if (account.loggedIn) account.username.ifBlank { "已登录" } else "点击登录"
    UserInfoCard(account.toUserInfoCardData().copy(username = name), onProfile = onOpenProfile)
}
