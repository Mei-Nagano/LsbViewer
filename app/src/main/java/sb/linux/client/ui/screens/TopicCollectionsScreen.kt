package sb.linux.client.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import sb.linux.client.ui.session.Session
import sb.linux.client.ui.feature.collection.TopicCollectionsViewModel
import sb.linux.client.ui.feature.collection.TopicCollectionsViewModelFactory
import sb.linux.client.ui.components.feedback.EmptyBox
import sb.linux.client.ui.components.feedback.ErrorBox
import sb.linux.client.ui.components.feedback.LoadingBox
import sb.linux.client.ui.components.feedback.LoginRequiredBox
import sb.linux.client.ui.components.navigation.AppPageTitleSelector

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

    sb.linux.client.ui.components.navigation.AppPageScaffold(
        title = if (mine) "我的淘帖" else "大家的淘帖",
        titleContent = {
            AppPageTitleSelector(
                options = listOf(false to "大家的淘帖", true to "我的淘帖"),
                selectedValue = mine,
                onSelect = viewModel::selectMine,
                description = "选择淘帖来源",
            )
        },
        primaryPage = !showBack,
        // 作为根目的地嵌在宿主里时没有可返回的上一页；此时外壳不渲染返回图标
        onBack = if (showBack) ({ nav.popBackStack() }) else null,
        actions = {
            sb.linux.client.ui.components.navigation.AppRefreshButton(enabled = !loading, onClick = { viewModel.load(userId, loggedIn) })
        },
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
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
