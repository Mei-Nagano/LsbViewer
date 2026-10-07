package sb.linux.client.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.lifecycle.viewmodel.compose.viewModel
import sb.linux.client.domain.gacha.GachaAction
import sb.linux.client.ui.session.Session
import sb.linux.client.ui.*
import sb.linux.client.ui.feature.gacha.GachaProfileViewModel
import sb.linux.client.ui.feature.gacha.GachaProfileViewModelFactory

/** 我的称号：收藏、装备操作与称号系统子页入口。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GachaProfileScreen(session: Session, nav: NavHostController) {
    val factory = remember(session.gachaProfileRepository) {
        GachaProfileViewModelFactory(
            session.gachaProfileRepository,
            session::showToast,
            session::refreshSession,
        )
    }
    val viewModel: GachaProfileViewModel = viewModel(factory = factory)
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(session.account.loginState.loggedIn, viewModel) {
        if (session.account.loginState.loggedIn) viewModel.load()
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("我的称号") },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
                actions = {
                    if (session.account.loginState.loggedIn) {
                        IconButton(onClick = viewModel::load) { Icon(Icons.Filled.Refresh, "刷新") }
                    }
                },
            )
        }
    ) { pad ->
        if (!session.account.loginState.loggedIn) {
            Box(Modifier.padding(pad).fillMaxSize()) { GachaLoginPrompt { nav.navigate("login") } }
            return@Scaffold
        }
        when {
            state.loading && state.profile == null -> Box(Modifier.padding(pad).fillMaxSize()) { LoadingBox() }
            state.error != null && state.profile == null -> Box(Modifier.padding(pad).fillMaxSize()) { ErrorBox(state.error!!) { viewModel.load() } }
            else -> Column(
                Modifier
                    .padding(pad)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Spacer(Modifier.height(6.dp))
                if (state.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
                state.error?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
                state.profile?.let { data ->
                    val equipped = data.titles.count { it.equipped }
                    GachaStatCard(
                        icon = Icons.Filled.MilitaryTech,
                        primary = data.stat.ifBlank { "${data.titles.size} 种称号" },
                        secondary = if (data.titles.isEmpty()) "尚未获得称号"
                        else "共 ${data.titles.sumOf { it.count }} 个 · 已装备 $equipped",
                    )
                }
                // 收藏与操作入口合并在同一张称号系统卡里；材料页保留源站的动态计算与提交逻辑。
                GachaSectionTitle("称号系统")
                GachaSystemCard(
                    titles = state.profile?.titles.orEmpty(),
                    enabled = !state.acting,
                    onAction = viewModel::submit,
                    nav = nav,
                )
                Spacer(Modifier.height(40.dp))
            }
        }
    }
}

// ---------------- 称号抽取 ----------------
