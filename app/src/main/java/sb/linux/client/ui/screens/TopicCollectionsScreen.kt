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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import sb.linux.client.common.collection.TopicCollectionSummary
import sb.linux.client.common.collection.TopicCollectionTab
import sb.linux.client.ui.session.Session
import sb.linux.client.ui.feature.collection.TopicCollectionsViewModel
import sb.linux.client.ui.feature.collection.TopicCollectionsViewModelFactory
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
    val factory = remember(session.topicCollectionRepository, initialMine) {
        TopicCollectionsViewModelFactory(session.topicCollectionRepository, initialMine)
    }
    val viewModel: TopicCollectionsViewModel = viewModel(factory = factory)
    val state by viewModel.uiState.collectAsState()
    val mine = state.mine
    val selectedTab = state.selectedTab
    val rows = state.rowsByTab[selectedTab]?.items.orEmpty()
    val loading = state.loading
    val error = state.error
    val loggedIn = session.account.loginState.loggedIn
    val userId = session.account.loginState.userId
    val requiresLogin = state.requiresLoginByTab[selectedTab] == true
    LaunchedEffect(viewModel, mine, loggedIn, userId) { viewModel.load(userId, loggedIn) }

    // 未登录看「我的淘帖」：源站必然拒绝，直接给登录引导而不是把 repository 的报错抛在屏幕上
    val needsLogin = mine && (!session.account.loginState.loggedIn || requiresLogin)

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("淘帖中心") },
            navigationIcon = {
                if (showBack) IconButton(onClick = { nav.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                }
            },
            actions = {
                IconButton(enabled = !loading, onClick = { viewModel.load(userId, loggedIn) }) { Icon(Icons.Filled.Refresh, "刷新") }
            },
        )
    }) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            TabRow(selectedTabIndex = if (mine) 0 else 1) {
                Tab(selected = mine, onClick = { viewModel.selectMine(true) }, text = { Text("我的淘帖") })
                Tab(selected = !mine, onClick = { viewModel.selectMine(false) }, text = { Text("大家的淘帖") })
            }
            when {
                needsLogin -> LoginRequiredBox(
                    "我的淘帖需要登录",
                    "登录后可查看自己创建、协作与订阅的专辑",
                ) { nav.navigate("login") }
                // 整屏加载/错误态仅在首次（列表还空着）出现；后续刷新降级为条幅，保住正在看的内容
                loading && rows.isEmpty() -> LoadingBox()
                error != null && rows.isEmpty() -> ErrorBox(error!!) { viewModel.load(userId, loggedIn) }
                !loading && rows.isEmpty() -> EmptyBox(if (mine) "还没有创建或订阅专辑" else "暂无公开专辑")
                else -> CollectionList(
                    session = session,
                    rows = rows,
                    loading = loading,
                    error = error,
                    onRefresh = { viewModel.load(userId, loggedIn) },
                    onOpen = { id ->
                        nav.navigate("collectionActions?path=${android.net.Uri.encode("/topic_collection/$id")}")
                    },
                )
            }
        }
    }
}
