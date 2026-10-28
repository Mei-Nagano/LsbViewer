package sb.linux.client.ui.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import sb.linux.client.domain.account.UserProfile
import sb.linux.client.ui.patterns.user.UserInfoCard
import sb.linux.client.ui.patterns.user.toUserInfoCardData

/** Full profile variant of the card also used by the sidebar and Me page. */
@Composable
internal fun UserProfileHeader(profile: UserProfile) {
    UserInfoCard(
        data = profile.toUserInfoCardData(),
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
    )
}
