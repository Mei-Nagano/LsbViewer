package sb.linux.client.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import sb.linux.client.ui.theme.LocalAppSpacing
import sb.linux.client.ui.session.Session
import sb.linux.client.ui.patterns.user.UserInfoCard
import sb.linux.client.ui.patterns.user.toUserInfoCardData

@Composable
internal fun HomeSidebarHeader(
    session: Session,
    onClose: () -> Unit,
    onOpenUser: (Long) -> Unit,
    onLogin: () -> Unit,
) {
    val openLink = sb.linux.client.ui.LocalLinkHandler.current
    val state = session.account.loginState
    if (state.loggedIn) {
        UserInfoCard(
            data = state.toUserInfoCardData(),
            modifier = Modifier.padding(horizontal = LocalAppSpacing.current.pageHorizontal, vertical = 12.dp),
            onProfile = { onClose(); onOpenUser(state.userId) },
            onStatistic = { onClose(); openLink?.invoke(it) },
        )
    } else {
        UserInfoCard(data = state.toUserInfoCardData(),
            modifier = Modifier.padding(horizontal = LocalAppSpacing.current.pageHorizontal, vertical = LocalAppSpacing.current.contentGap),
            onProfile = onLogin)
    }
}
