package sb.linux.client.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.CircleShape
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import sb.linux.client.ui.patterns.user.LocalUserSummaryRepository
import sb.linux.client.ui.patterns.user.LocalUserFollowChanged
import sb.linux.client.ui.patterns.user.LocalAvatarAccountVersion
import sb.linux.client.ui.patterns.user.LocalAvatarAccountId
import sb.linux.client.domain.community.FollowingUser
import sb.linux.client.domain.community.FollowingView
import sb.linux.client.ui.TitleBadgeView
import sb.linux.client.ui.components.feedback.EmptyBox
import sb.linux.client.ui.components.feedback.ErrorBox
import sb.linux.client.ui.components.feedback.LoadingBox
import sb.linux.client.ui.components.navigation.PaginationBar
import sb.linux.client.ui.patterns.user.UserAvatar
import sb.linux.client.ui.patterns.user.SourceUserGroupBadge
import sb.linux.client.ui.session.HomeTabState
import sb.linux.client.ui.theme.LocalAppSpacing

// 筛选优先于列表；名字为主，用户组和关注信息为辅。使用现有 Material 3 字体、表面与间距。
@Composable
internal fun HomeFollowingFilters(selected: FollowingView, onSelect: (FollowingView) -> Unit) {
    val spacing = LocalAppSpacing.current
    Row(Modifier.fillMaxWidth().padding(horizontal = spacing.pageHorizontal),
        horizontalArrangement = Arrangement.spacedBy(spacing.item)) {
        FollowingView.entries.forEach { view ->
            FilterChip(selected = selected == view, onClick = { onSelect(view) }, label = { Text(view.label) })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HomeFollowingUsersContent(
    current: HomeTabState, view: FollowingView, loading: Boolean, loadingMore: Boolean,
    error: String?, homeInfinite: Boolean, listState: LazyListState,
    onLoad: (Int, Boolean) -> Unit, onRefresh: () -> Unit, onOpenUser: (Long) -> Unit,
) {
    val spacing = LocalAppSpacing.current
    val users = current.followingUsers
    PullToRefreshBox(isRefreshing = loading && users.isNotEmpty(), onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize()) {
        when {
            loading && users.isEmpty() -> LoadingBox()
            error != null && users.isEmpty() -> ErrorBox(error) { onLoad(1, false) }
            users.isEmpty() -> EmptyBox(current.emptyText.ifBlank { view.emptyText })
            else -> LazyColumn(
                state = listState, modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = spacing.pageHorizontal, vertical = spacing.item),
                verticalArrangement = Arrangement.spacedBy(spacing.item),
            ) {
                items(users, key = FollowingUser::userId) { user ->
                    FollowingUserRow(user, onOpenUser)
                }
                if (error != null) item {
                    Text(error, color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall)
                }
                item {
                    if (homeInfinite) {
                        if (current.sourcePage < current.sourceTotalPages) {
                            TextButton(onClick = { onLoad(current.sourcePage + 1, true) },
                                enabled = !loading && !loadingMore, modifier = Modifier.fillMaxWidth()) {
                                Text(if (loadingMore) "加载中" else "加载更多")
                            }
                        }
                    } else if (!loading && !loadingMore) {
                        PaginationBar(current.sourcePage, current.sourceTotalPages) { onLoad(it, false) }
                    }
                }
            }
        }
    }
}

@Composable
internal fun FollowingUserRow(user: FollowingUser, onOpenUser: (Long) -> Unit) {
    val spacing = LocalAppSpacing.current
    val repository = LocalUserSummaryRepository.current
    val accountVersion = LocalAvatarAccountVersion.current
    val accountId = LocalAvatarAccountId.current
    val onChanged = LocalUserFollowChanged.current
    val scope = rememberCoroutineScope()
    var followed by remember(user, accountVersion) { mutableStateOf(user.following) }
    var busy by remember(user.userId, accountVersion) { mutableStateOf(false) }
    var failure by remember(user.userId, accountVersion) { mutableStateOf<String?>(null) }
    var now by remember(user.capturedAt) { mutableLongStateOf(System.currentTimeMillis() / 1000) }
    LaunchedEffect(user.capturedAt) {
        while (true) { now = System.currentTimeMillis() / 1000; kotlinx.coroutines.delay(60_000) }
    }
    val meta = sb.linux.client.util.followingMeta(user, now)
    Surface(onClick = { onOpenUser(user.userId) }, shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(spacing.contentGap), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.contentGap)) {
            UserAvatar(user.userId, user.avatarUrl, online = user.online)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(spacing.inline)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(spacing.inline),
                    verticalArrangement = Arrangement.spacedBy(spacing.inline)) {
                    Text(user.username, style = MaterialTheme.typography.titleSmall)
                    user.titleBadge?.let { TitleBadgeView(it, small = true) }
                    user.groupBadge?.let { SourceUserGroupBadge(it) }
                    FollowingIdentityPill("UID ${user.userId}")
                }
                if (meta.isNotBlank()) Text(meta, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                failure?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
            }
            if (user.userId != accountId && user.canFollow && repository != null) {
                OutlinedButton(enabled = !busy, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    onClick = {
                        busy = true; failure = null
                        scope.launch {
                            try {
                                followed = repository.toggleFollow(user.userId).following
                                onChanged()
                            } catch (cancelled: CancellationException) { throw cancelled }
                            catch (error: Exception) { failure = error.message ?: "关注操作失败" }
                            finally { busy = false }
                        }
                    }) { Text(if (busy) "处理中" else if (followed) "已关注" else "关注TA", style = MaterialTheme.typography.labelMedium) }
            }
        }
    }
}

@Composable
private fun FollowingIdentityPill(text: String) {
    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Text(text, Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
