package sb.linux.client.ui.patterns.user

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Shape
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import sb.linux.client.domain.account.UserLookupRepository
import sb.linux.client.domain.account.UserSummaryRepository
import sb.linux.client.ui.LocalLinkHandler

internal val LocalUserBlockChanged = staticCompositionLocalOf<() -> Unit> { {} }

internal val LocalUserSummaryRepository = staticCompositionLocalOf<UserSummaryRepository?> { null }
internal val LocalAvatarUserLookupRepository = staticCompositionLocalOf<UserLookupRepository?> { null }
internal val LocalAvatarInfoCardEnabled = staticCompositionLocalOf { true }
internal val LocalAvatarAccountId = staticCompositionLocalOf { 0L }
internal val LocalAvatarAccountVersion = staticCompositionLocalOf { 0L }
internal val LocalUserFollowChanged = staticCompositionLocalOf<() -> Unit> { {} }

/**
 * 根据阅读设置打开用户信息卡或直接进入主页。
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
    shape: Shape = MaterialTheme.shapes.extraSmall,
    username: String = "",
) {
    val repository = LocalUserSummaryRepository.current
    val lookup = LocalAvatarUserLookupRepository.current
    val openLink = LocalLinkHandler.current
    val infoCardEnabled = LocalAvatarInfoCardEnabled.current
    val currentInfoCardEnabled by rememberUpdatedState(infoCardEnabled)
    val accountId = LocalAvatarAccountId.current
    val accountVersion = LocalAvatarAccountVersion.current
    val currentVersion by rememberUpdatedState(accountVersion)
    val scope = rememberCoroutineScope()
    var resolvedId by remember(userId, username, accountVersion) { mutableLongStateOf(userId) }
    var resolving by remember(userId, username, accountVersion) { mutableStateOf(false) }
    var failure by remember(userId, username, accountVersion) { mutableStateOf<String?>(null) }
    var expanded by remember(userId, username, accountId, accountVersion, infoCardEnabled) { mutableStateOf(false) }
    fun showUser(targetId: Long) {
        if (currentInfoCardEnabled && repository != null && targetId != accountId) expanded = true
        else if (onProfile != null) onProfile() else openLink?.invoke("/user/$targetId")
    }
    val slot = if (minTouchTarget) maxOf(48, size + 8).dp else size.dp
    Box(modifier) {
        Box(
            Modifier.size(slot).clickable(enabled = !resolving &&
                (resolvedId > 0 || username.isNotBlank() && lookup != null)) {
                if (resolvedId > 0) showUser(resolvedId)
                else if (!infoCardEnabled) {
                    if (onProfile != null) onProfile()
                    else openLink?.invoke("/user?username=${android.net.Uri.encode(username)}")
                } else {
                    resolving = true
                    failure = null
                    scope.launch {
                        try {
                            val targetId = lookup!!.resolve(username)
                            check(targetId > 0) { "未找到此用户" }
                            if (currentVersion == accountVersion) {
                                resolvedId = targetId
                                showUser(targetId)
                            }
                        } catch (cancelled: CancellationException) { throw cancelled
                        } catch (error: Exception) {
                            if (currentVersion == accountVersion) {
                                failure = error.message ?: "用户查询失败"
                                expanded = true
                            }
                        } finally { resolving = false }
                    }
                }
            },
            contentAlignment = Alignment.Center,
        ) {
            Avatar(url, size, Modifier, online, shape)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            if (failure != null) androidx.compose.material3.Text(
                failure.orEmpty(), color = MaterialTheme.colorScheme.error,
            )
            else if (repository != null && resolvedId > 0) UserSummaryContent(resolvedId, repository, { expanded = false }) { target ->
                expanded = false
                if (sb.linux.client.common.network.Endpoints.abs(target) ==
                    sb.linux.client.common.network.Endpoints.abs("/user/$resolvedId") && onProfile != null) onProfile()
                else openLink?.invoke(target)
            }
        }
    }
}
