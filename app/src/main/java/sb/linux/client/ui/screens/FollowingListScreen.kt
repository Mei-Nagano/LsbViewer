package sb.linux.client.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import kotlinx.coroutines.CancellationException
import sb.linux.client.domain.community.FollowingView
import sb.linux.client.ui.components.feedback.*
import sb.linux.client.ui.components.navigation.*
import sb.linux.client.ui.patterns.topic.TopicCardView
import sb.linux.client.ui.session.HomeTabState
import sb.linux.client.ui.session.Session
import sb.linux.client.ui.theme.LocalAppSpacing

@Composable
internal fun FollowingListScreen(session: Session, nav: NavHostController, url: String) {
    val uri = remember(url) { android.net.Uri.parse(url) }
    val owner = uri.getQueryParameter("follow_uid")?.toLongOrNull() ?: session.account.loginState.userId
    var view by remember(url, session.accountIdentityVersion) { mutableStateOf(
        FollowingView.entries.firstOrNull { it.sourceValue == uri.getQueryParameter("follow_view") }
            ?: if (!uri.getQueryParameter("follow_uid").isNullOrBlank()) FollowingView.TOPICS else FollowingView.FOLLOWING
    ) }
    var page by remember(owner, view, session.accountIdentityVersion) { mutableIntStateOf(1) }
    var retry by remember(owner, session.accountIdentityVersion) { mutableIntStateOf(0) }
    var loading by remember(owner, session.accountIdentityVersion) { mutableStateOf(true) }
    var error by remember(owner, session.accountIdentityVersion) { mutableStateOf<String?>(null) }
    val current = remember(owner, view, session.accountIdentityVersion) { HomeTabState() }
    val list = rememberLazyListState()
    LaunchedEffect(owner, view, page, retry, session.accountIdentityVersion, session.homeBrowse.refreshRequest) {
        loading = true; error = null
        try {
            val result = session.homeRepository.loadFollowing(view, page, owner)
            current.followingUsers = result.followingUsers; current.topics = result.topics
            current.sourcePage = result.page; current.sourceTotalPages = result.totalPages
            current.emptyText = result.emptyText
            list.scrollToItem(0)
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (failure: Exception) { error = failure.message ?: "加载失败" }
        finally { loading = false }
    }
    val title = if (owner == session.account.loginState.userId) view.label else when (view) {
        FollowingView.FOLLOWERS -> "粉丝"; FollowingView.FOLLOWING -> "关注"; else -> "关注动态"
    }
    AppPageScaffold(title, onBack = { nav.popBackStack() }, actions = {
        AppRefreshButton(onClick = { retry++ }, enabled = !loading)
    }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            HomeFollowingFilters(view) { view = it }
            if (view != FollowingView.TOPICS) HomeFollowingUsersContent(
                current, view, loading, false, error, false, list,
                onLoad = { target, _ -> page = target; retry++ }, onRefresh = { retry++ },
                onOpenUser = { nav.navigate("user/$it") },
            ) else when {
                loading -> LoadingBox()
                error != null -> ErrorBox(error.orEmpty()) { retry++ }
                current.topics.isEmpty() -> EmptyBox(view.emptyText)
                else -> LazyColumn(state = list, contentPadding = PaddingValues(vertical = LocalAppSpacing.current.item)) {
                    items(current.topics, key = { it.topicId }) { topic ->
                        TopicCardView(topic, onClick = { nav.navigate("topic/${topic.topicId}") })
                    }
                    item { PaginationBar(current.sourcePage, current.sourceTotalPages) { page = it } }
                }
            }
        }
    }
}
