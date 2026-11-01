package sb.linux.client.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import sb.linux.client.ui.navigation.LocalMasterNav
import sb.linux.client.ui.session.Session
import sb.linux.client.ui.*
import sb.linux.client.ui.theme.*
import sb.linux.client.ui.content.*
import sb.linux.client.ui.patterns.topic.*
import sb.linux.client.ui.patterns.user.*
import sb.linux.client.ui.components.feedback.*
import sb.linux.client.ui.components.layout.*
import sb.linux.client.ui.components.navigation.*
import sb.linux.client.ui.feature.account.UserPageViewModel
import sb.linux.client.ui.feature.account.UserPageViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserScreen(session: Session, nav: NavHostController) {
    val entry = nav.currentBackStackEntry ?: return
    val uid = entry.arguments?.getLong("uid") ?: return
    val initialTab = entry.arguments?.getString("tab") ?: "topics"
    val factory = remember(session.userPageRepository, uid, initialTab) {
        UserPageViewModelFactory(session.userPageRepository, uid, initialTab)
    }
    val viewModel: UserPageViewModel = viewModel(factory = factory, key = "user-page-$uid")
    val state by viewModel.uiState.collectAsState()
    val tab = state.tab
    val profile = state.content?.profile
    val loading = state.loading
    val error = state.error
    val scope = rememberCoroutineScope()
    val userListState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
    val topicCards = state.content?.topics.orEmpty()
    val replies = state.content?.replies.orEmpty()
    val pointRows = state.content?.points.orEmpty()
    val notifications = state.content?.notifications.orEmpty()
    val isSelf = session.account.loginState.userId == uid
    // 平板双栏的主栏控制器（手机为 null）：退出登录时切主栏并清空右栏
    val masterNav = LocalMasterNav.current
    val spacing = LocalAppSpacing.current

    LaunchedEffect(viewModel, session.homeBrowse.refreshRequest) { viewModel.load() }

    // 点进该用户主页即刷新其头像缓存：清掉 Coil 内存/磁盘里该 URL 的旧图，
    // 大头像随后以新拉取的字节解码，侧栏等处的旧头像也能随之更新。
    val context = LocalContext.current
    LaunchedEffect(profile?.avatarUrl) {
        val url = profile?.avatarUrl ?: return@LaunchedEffect
        if (url.isNotBlank()) {
            val loader = coil.Coil.imageLoader(context)
            val memCache = loader.memoryCache
            memCache?.keys?.filter { it.key == url }?.forEach { key -> memCache.remove(key) }
            loader.diskCache?.remove(url)
        }
    }

    val tabs = buildList {
        add("topics" to "主题")
        add("replies" to "回帖")
        add("points_rewards" to "积分")
        add("favorites" to "收藏")
        if (isSelf) add("notifications" to "通知")
    }

    sb.linux.client.ui.components.navigation.AppPageScaffold(
        title = profile?.username ?: "用户",
        onBack = { nav.popBackStack() },
        actions = {
            if (session.account.loginState.loggedIn && !isSelf && !profile?.blockUrl.isNullOrBlank()) {
                TextButton(onClick = {
                    nav.navigate("userBlock?url=" + android.net.Uri.encode(profile!!.blockUrl))
                }) { Text(profile?.blockLabel.orEmpty().ifBlank { "用户屏蔽" }) }
            }
            if (session.account.loginState.loggedIn && !isSelf) {
                IconButton(onClick = {
                    // 进入原站私信会话。
                    nav.navigate(
                        "chat/$uid?name=${android.net.Uri.encode(profile?.username ?: "")}" +
                            "&avatar=${android.net.Uri.encode(profile?.avatarUrl ?: "")}"
                    )
                }) {
                    Icon(Icons.AutoMirrored.Filled.Send, "私信")
                }
            }
        },
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            profile?.let { UserProfileHeader(it) }
            // 本人主页：个人设置与退出登录入口（原"我的"页账号分类移入，3.1）
            if (isSelf && session.account.loginState.loggedIn) UserAccountActions(session, nav, masterNav)
            ScrollableTabRow(
                selectedTabIndex = tabs.indexOfFirst { it.first == tab }.coerceAtLeast(0),
                edgePadding = spacing.pageHorizontal,
                containerColor = MaterialTheme.colorScheme.surface,
            ) {
                tabs.forEach { (t, label) ->
                    Tab(selected = tab == t, onClick = { viewModel.selectTab(t) }, text = { Text(label) })
                }
            }
            Box(Modifier.weight(1f).fillMaxWidth()) {
                when {
                    loading -> LoadingBox()
                    error != null -> ErrorBox(error!!) { viewModel.load() }
                    else -> LazyColumn(
                        state = userListState,
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        when (tab) {
                            "replies" -> userReplyRows(replies) { topicId -> nav.navigate("topic/$topicId") }
                            "points_rewards" -> if (pointRows.isEmpty()) {
                                item { EmptyBox("暂无积分记录", modifier = Modifier.fillParentMaxHeight(0.5f)) }
                            } else itemsIndexed(pointRows, key = { i, row -> "${row.timeText}-${row.reason}-$i" }) { index, row ->
                                ListItem(
                                    headlineContent = { Text(row.reason) },
                                    supportingContent = { Text(row.timeText) },
                                    trailingContent = {
                                        Text(
                                            "${if (row.positive) "+" else ""}${row.change} 积分",
                                            color = if (row.positive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                            style = MaterialTheme.typography.labelLarge
                                        )
                                    }
                                )
                                if (index != pointRows.lastIndex) {
                                    HorizontalDivider(
                                        Modifier.padding(start = spacing.pageHorizontal),
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                    )
                                }
                            }
                            "notifications" -> userNotificationRows(notifications, nav)
                            else -> if (topicCards.isEmpty()) {
                                item { EmptyBox("暂无主题", modifier = Modifier.fillParentMaxHeight(0.5f)) }
                            } else itemsIndexed(topicCards, key = { i, t -> "${t.topicId}-${t.pinned}-$i" }) { _, t ->
                                TopicCardView(t, onClick = { nav.navigate("topic/${t.topicId}") },
                                    onForumClick = { fid -> if (fid > 0) nav.navigate("forum/$fid") })
                            }
                        }
                    }
                }
                // 返回顶部悬浮按钮：滚过前几条后出现
                val showBackTop by remember {
                    derivedStateOf { userListState.firstVisibleItemIndex > 2 }
                }
                if (showBackTop) {
                    SmallFloatingActionButton(
                        onClick = { scope.launch { userListState.animateScrollToItem(0) } },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = spacing.pageHorizontal, bottom = spacing.section),
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ) {
                        Icon(Icons.Filled.KeyboardArrowUp, "回到顶部")
                    }
                }
            }
        }
    }
}
