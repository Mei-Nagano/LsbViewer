package sb.linux.client.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import sb.linux.client.common.collection.TopicCollectionSummary
import sb.linux.client.common.collection.TopicCollectionTab
import sb.linux.client.data.Session
import sb.linux.client.ui.Avatar
import sb.linux.client.ui.Badge
import sb.linux.client.ui.EmptyBox
import sb.linux.client.ui.ErrorBox
import sb.linux.client.ui.LoadingBox
import sb.linux.client.ui.LoginRequiredBox

/**
 * 淘帖中心：原生解析「我的 / 大家的」专辑列表，专辑详情走 collectionActions 路由。
 * showBack=false 用于底栏入口（顶层页面没有上一级可返回）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopicCollectionsScreen(
    session: Session,
    nav: NavHostController,
    initialMine: Boolean = false,
    showBack: Boolean = true,
) {
    var mine by rememberSaveable(initialMine) { mutableStateOf(initialMine) }
    // 两个标签各自保留快照：切换标签时可以继续显示该标签的旧数据，不会串台。
    var rowsByTab by remember { mutableStateOf<Map<TopicCollectionTab, List<TopicCollectionSummary>>>(emptyMap()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    // 源站把未登录的「我的淘帖」重定向到登录页；requiresLogin 用来区分「真错误」与「未登录」
    var requiresLoginByTab by remember { mutableStateOf<Map<TopicCollectionTab, Boolean>>(emptyMap()) }
    var requestVersion by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    val selectedTab = if (mine) TopicCollectionTab.MINE else TopicCollectionTab.EVERYONE
    val rows = rowsByTab[selectedTab].orEmpty()
    val requiresLogin = requiresLoginByTab[selectedTab] == true

    fun load() {
        val requestedMine = mine
        val requestedUser = session.loginState.userId
        val version = ++requestVersion
        loading = true
        error = null
        scope.launch {
            try {
                val tab = if (requestedMine) TopicCollectionTab.MINE else TopicCollectionTab.EVERYONE
                if (tab == TopicCollectionTab.MINE && !session.loginState.loggedIn) {
                    if (version == requestVersion) {
                        requiresLoginByTab = requiresLoginByTab + (tab to true)
                        loading = false
                    }
                    return@launch
                }
                val page = session.topicCollectionService.list(tab)
                // 切 tab / 换账号后回来的旧响应不能覆盖当前视图
                if (version == requestVersion && requestedMine == mine && requestedUser == session.loginState.userId) {
                    rowsByTab = rowsByTab + (tab to page.items)
                    requiresLoginByTab = requiresLoginByTab + (tab to page.requiresLogin)
                }
            } catch (e: Exception) {
                if (version == requestVersion) error = e.message ?: "加载失败"
            } finally {
                if (version == requestVersion) loading = false
            }
        }
    }
    // 登录页返回后 userId / loggedIn 变化，自动重新读取「我的淘帖」。
    LaunchedEffect(mine, session.loginState.loggedIn, session.loginState.userId) { load() }

    // 未登录看「我的淘帖」：源站必然拒绝，直接给登录引导而不是把 repository 的报错抛在屏幕上
    val needsLogin = mine && (!session.loginState.loggedIn || requiresLogin)

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("淘帖中心") },
            navigationIcon = {
                if (showBack) IconButton(onClick = { nav.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                }
            },
            actions = {
                IconButton(enabled = !loading, onClick = { load() }) { Icon(Icons.Filled.Refresh, "刷新") }
            },
        )
    }) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            TabRow(selectedTabIndex = if (mine) 0 else 1) {
                Tab(selected = mine, onClick = { mine = true }, text = { Text("我的淘帖") })
                Tab(selected = !mine, onClick = { mine = false }, text = { Text("大家的淘帖") })
            }
            when {
                needsLogin -> LoginRequiredBox(
                    "我的淘帖需要登录",
                    "登录后可查看自己创建、协作与订阅的专辑",
                ) { nav.navigate("login") }
                // 整屏加载/错误态仅在首次（列表还空着）出现；后续刷新降级为条幅，保住正在看的内容
                loading && rows.isEmpty() -> LoadingBox()
                error != null && rows.isEmpty() -> ErrorBox(error!!) { load() }
                !loading && rows.isEmpty() -> EmptyBox(if (mine) "还没有创建或订阅专辑" else "暂无公开专辑")
                else -> CollectionList(
                    session = session,
                    rows = rows,
                    loading = loading,
                    error = error,
                    onRefresh = { load() },
                    onOpen = { id ->
                        nav.navigate("collectionActions?path=${android.net.Uri.encode("/topic_collection/$id")}")
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CollectionList(
    session: Session,
    rows: List<TopicCollectionSummary>,
    loading: Boolean,
    error: String?,
    onRefresh: () -> Unit,
    onOpen: (Long) -> Unit,
) {
    PullToRefreshBox(
        isRefreshing = loading && rows.isNotEmpty(),
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize(),
    ) {
        LazyColumn(
            Modifier.fillMaxSize(),
            // 悬浮玻璃底栏（高 58 + 边距 14）会盖住末条内容，底栏样式非 0 时补留白；
            // 经典底栏由 Scaffold 的 bottomBar inset 处理，不需要额外留白
            contentPadding = PaddingValues(
                start = 14.dp,
                end = 14.dp,
                top = 12.dp,
                bottom = 12.dp + if (session.bottomBarStyle != 0) {
                    72.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                } else 0.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (loading) item("progress") { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            // 已有内容时错误降级为条幅，不清空正在看的列表
            error?.let { message ->
                item("error") {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            Modifier.padding(start = 14.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                message,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f),
                            )
                            TextButton(onClick = onRefresh) { Text("重试") }
                        }
                    }
                }
            }
            items(rows, key = { it.collectionId }, contentType = { "collection-card" }) { summary ->
                CollectionCard(summary) { onOpen(summary.collectionId) }
            }
        }
    }
}

/** 专辑卡片：标题行 → 作者行 → 元信息 FlowRow（篇数/更新/创建/订阅数各自成项，不再被单行截断）。 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CollectionCard(summary: TopicCollectionSummary, onClick: () -> Unit) {
    ElevatedCard(onClick = onClick, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
        Column(
            Modifier.padding(start = 14.dp, top = 13.dp, end = 10.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Avatar(summary.avatarUrl, 36)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            summary.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        if (summary.visibility.isNotBlank()) {
                            Badge(
                                summary.visibility,
                                MaterialTheme.colorScheme.surfaceContainerHigh,
                                MaterialTheme.colorScheme.onSurfaceVariant,
                                small = true,
                            )
                        }
                        if (summary.subscribed) {
                            Badge(
                                "已订阅",
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.colorScheme.onPrimaryContainer,
                                small = true,
                            )
                        }
                    }
                    if (summary.authorName.isNotBlank()) {
                        Text(
                            summary.authorName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Icon(
                    Icons.Filled.ChevronRight,
                    contentDescription = "打开",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .4f),
                )
            }
            val meta = listOf(
                summary.articleCount,
                summary.updatedText,
                summary.createdText,
                summary.subscriberCount,
            ).filter { it.isNotBlank() }
            if (meta.isNotEmpty()) {
                FlowRow(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    meta.forEach {
                        Text(
                            it,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                }
            }
            if (summary.description.isNotBlank()) {
                Text(
                    summary.description,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .7f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
