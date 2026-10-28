package sb.linux.client.ui.patterns.user

import sb.linux.client.domain.account.LoginState
import sb.linux.client.domain.account.UserProfile

internal fun LoginState.toUserInfoCardData() = UserInfoCardData(
    userId = userId, username = username, avatarUrl = avatarUrl,
    userGroup = userGroup, points = points, titleBadge = titleBadge,
)

internal fun UserProfile.toUserInfoCardData() = UserInfoCardData(
    userId = userId, username = username, avatarUrl = avatarUrl,
    userGroup = userGroup, points = points, titleBadge = titleBadge,
    online = online, bioHtml = bio,
)
