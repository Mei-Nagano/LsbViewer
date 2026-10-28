package sb.linux.client.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavGraph.Companion.findStartDestination
import kotlinx.coroutines.CoroutineScope
import sb.linux.client.ui.navigation.AppDestinations
import sb.linux.client.ui.navigation.LocalMasterNav
import sb.linux.client.ui.session.Session

@Composable
internal fun TopicScreenScaffold(
    session: Session,
    topicId: Long,
    initialPage: Int,
    nav: NavHostController,
    scope: CoroutineScope,
    paging: TopicPagingState,
    ui: TopicScreenUiState,
    actions: TopicScreenActions,
    listState: androidx.compose.foundation.lazy.LazyListState,
    view: TopicScreenViewState,
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    // 宽屏详情图没有 home：钉住必须由主栏导航承担，否则会抛“目标不存在”。
    val masterNav = LocalMasterNav.current
    val page = view.page
    val mainPost = view.mainPost
    BackHandler(ui.topicSearchOpen) {
        ui.topicSearchOpen = false
        ui.topicSearchListOpen = false
    }
    Scaffold(
        topBar = {
            TopicSearchOverlay(
                visible = ui.topicSearchOpen,
                hits = view.searchHits,
                query = ui.topicSearchQuery,
                selectedIndex = ui.topicSearchIndex,
                listOpen = ui.topicSearchListOpen,
                allPagesLoaded = paging.allSearchPagesLoaded.value,
                totalPages = page?.totalPages ?: 1,
                loadingMore = paging.loadingMore.value,
                onClose = { ui.topicSearchOpen = false; ui.topicSearchListOpen = false },
                onQueryChange = { ui.topicSearchQuery = it; ui.topicSearchIndex = 0 },
                onSelectedIndexChange = { ui.topicSearchIndex = it },
                onListOpenChange = { ui.topicSearchListOpen = it },
                onLoadAllPages = actions::loadAllForSearch,
                onOpenHit = { ui.pendingSearchPost = it.post },
            )
        },
        bottomBar = {
            // 阅读操作栏使用标准 MD3 表面；inset 由 Scaffold 消费，列表不再额外垫高。
            if (page != null && paging.error.value == null) {
                ReplyBar(
                    loggedIn = session.account.loginState.loggedIn,
                    liked = mainPost?.liked == true,
                    favorited = ui.isFav,
                    likeCount = mainPost?.likeCount ?: 0,
                    showLike = mainPost?.canLike == true,
                    showFavorite = page.canFavorite,
                    pinned = session.floatingTopicId == topicId,
                    onReply = {
                        if (session.account.loginState.loggedIn) {
                            ui.quoteText = ""
                            ui.showReply = true
                        } else nav.navigate("login")
                    },
                    onLike = {
                        if (session.account.loginState.loggedIn) mainPost?.let { actions.react(it, 0) }
                        else nav.navigate("login")
                    },
                    onFavorite = {
                        if (session.account.loginState.loggedIn) actions.requestFavorite()
                        else nav.navigate("login")
                    },
                    onPin = {
                        page.pinSnapshot(session, listState, paging, ui, topicId)
                        if (masterNav != null) {
                            // 宽屏：主栏切回社区，详情宿主退栈回到空态；快照供“继续阅读”恢复
                            masterNav.navigate(AppDestinations.home.route) {
                                popUpTo(masterNav.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                            }
                            runCatching { nav.popBackStack() }
                        } else {
                            nav.navigate(AppDestinations.home.route) {
                                popUpTo(AppDestinations.home.route) { inclusive = false }
                                launchSingleTop = true
                            }
                        }
                    },
                )
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize()) {
            TopicScreenBodyContent(
                padding = padding,
                session = session,
                nav = nav,
                topicId = topicId,
                initialPage = initialPage,
                scope = scope,
                listState = listState,
                ui = ui,
                paging = paging,
                actions = actions,
                view = view,
                listModifier = Modifier,
            )
            TopicScreenTopBarOverlay(
                session = session,
                nav = nav,
                topicId = topicId,
                scope = scope,
                listState = listState,
                paging = paging,
                ui = ui,
                actions = actions,
                view = view,
                context = context,
                clipboard = clipboard,
            )
        }
    }
    TopicScreenDialogs(
        session = session,
        topicId = topicId,
        nav = nav,
        paging = paging,
        ui = ui,
        view = view,
        actions = actions,
        exportColors = MaterialTheme.colorScheme,
    )
}
