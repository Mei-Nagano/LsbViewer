package sb.linux.client.ui.patterns.user

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DropdownMenu
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import sb.linux.client.domain.account.UserSummaryRepository
import sb.linux.client.ui.LocalLinkHandler

internal val LocalUserBlockChanged = staticCompositionLocalOf<() -> Unit> { {} }

internal val LocalUserSummaryRepository = staticCompositionLocalOf<UserSummaryRepository?> { null }

/**
 * Avatar taps open a compact card; its home action opens the full profile.
 *
 * [minTouchTarget] 为 true 时，一个 48dp 的独立触区包住视觉头像（列表 40dp、评论 32dp），
 * 辅助技术只看到一个操作节点；视觉尺寸不变。
 */
@Composable
fun UserAvatar(
    userId: Long,
    url: String,
    size: Int = 40,
    modifier: Modifier = Modifier,
    online: Boolean = false,
    onProfile: (() -> Unit)? = null,
    minTouchTarget: Boolean = false,
) {
    val repository = LocalUserSummaryRepository.current
    val openLink = LocalLinkHandler.current
    var expanded by remember(userId) { mutableStateOf(false) }
    val slot = if (minTouchTarget) maxOf(48, size + 8).dp else size.dp
    Box(modifier) {
        Box(
            Modifier.size(slot).clickable(enabled = userId > 0) {
                if (repository != null) expanded = true
                else if (onProfile != null) onProfile() else openLink?.invoke("/user/$userId")
            },
            contentAlignment = Alignment.Center,
        ) {
            Avatar(url, size, Modifier, online)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            if (repository != null) UserSummaryContent(userId, repository, { expanded = false }) { target ->
                expanded = false
                if (sb.linux.client.common.network.Endpoints.abs(target) ==
                    sb.linux.client.common.network.Endpoints.abs("/user/$userId") && onProfile != null) onProfile()
                else openLink?.invoke(target)
            }
        }
    }
}
