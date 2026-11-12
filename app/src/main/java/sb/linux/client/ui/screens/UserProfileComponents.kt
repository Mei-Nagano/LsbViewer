package sb.linux.client.ui.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import sb.linux.client.domain.account.UserProfile
import sb.linux.client.ui.patterns.user.UserInfoCard
import sb.linux.client.ui.patterns.user.toUserInfoCardData
import sb.linux.client.ui.theme.LocalAppSpacing

/** Full profile variant of the card also used by the sidebar and Me page. */
@Composable
internal fun UserProfileHeader(profile: UserProfile) {
    val spacing = LocalAppSpacing.current
    UserInfoCard(
        data = profile.toUserInfoCardData(),
        modifier = Modifier.padding(horizontal = spacing.pageHorizontal, vertical = spacing.item),
    )
}
