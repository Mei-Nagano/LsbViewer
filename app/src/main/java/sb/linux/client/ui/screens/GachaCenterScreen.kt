@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package sb.linux.client.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.lifecycle.viewmodel.compose.viewModel
import sb.linux.client.domain.gacha.GachaAction
import sb.linux.client.ui.session.Session
import sb.linux.client.ui.*
import sb.linux.client.ui.feature.gacha.GachaCenterViewModel
import sb.linux.client.ui.feature.gacha.GachaCenterViewModelFactory

@Composable
fun GachaCenterScreen(session: Session, nav: NavHostController) {
    val factory = remember(session.gachaCenterRepository) {
        GachaCenterViewModelFactory(session.gachaCenterRepository, session::showToast)
    }
    val viewModel: GachaCenterViewModel = viewModel(factory = factory)
    val state by viewModel.uiState.collectAsState()
    var pending by remember { mutableStateOf<GachaAction?>(null) }
    LaunchedEffect(viewModel) { viewModel.load() }
    LaunchedEffect(state.submitting) { if (!state.submitting) pending = null }
    var skipConfirmation by remember { mutableStateOf(!state.confirmPullEnabled) }

    pending?.let { action ->
        AlertDialog(
            onDismissRequest = { if (!state.submitting) pending = null },
            icon = { Icon(Icons.Filled.Casino, null) },
            title = { Text("确认${action.label}") },
            text = {
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .clickable { skipConfirmation = !skipConfirmation }
                        .padding(end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(checked = skipConfirmation, onCheckedChange = { skipConfirmation = it })
                    Text("以后不再显示二次确认", style = MaterialTheme.typography.bodyMedium)
                }
            },
            dismissButton = { TextButton(enabled = !state.submitting, onClick = { pending = null }) { Text("取消") } },
            confirmButton = {
                Button(enabled = !state.submitting, onClick = {
                    viewModel.setConfirmPullEnabled(!skipConfirmation)
                    viewModel.pull(action)
                }) { Text(if (state.submitting) "处理中…" else "确认") }
            },
        )
    }

    state.receipt?.let { receipt ->
        GachaPullResultDialog(result = receipt.result, cost = receipt.cost, onDismiss = viewModel::dismissReceipt)
    }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("称号抽取") },
            navigationIcon = { IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } },
            actions = { IconButton(onClick = viewModel::load) { Icon(Icons.Filled.Refresh, "刷新") } },
        )
    }) { pad ->
        when {
            state.loading && state.center == null -> Box(Modifier.padding(pad).fillMaxSize()) { LoadingBox() }
            state.error != null && state.center == null -> Box(Modifier.padding(pad).fillMaxSize()) { ErrorBox(state.error!!) { viewModel.load() } }
            else -> Column(
                Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Spacer(Modifier.height(6.dp))
                if (state.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
                state.error?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
                state.center?.let { center ->
                    GachaStatCard(
                        icon = Icons.Filled.Paid,
                        primary = center.pointsText.ifBlank { "称号抽取" },
                        secondary = center.statsText,
                    )
                    if (center.pullActions.isNotEmpty()) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            center.pullActions.forEach { action ->
                                Button(
                                    onClick = { if (state.confirmPullEnabled) pending = action else viewModel.pull(action) },
                                    enabled = action.enabled && !state.submitting,
                                    shape = RoundedCornerShape(50),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                                    modifier = Modifier.weight(1f),
                                ) { Text(action.label, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                            }
                        }
                    }
                    // 二次确认开关与抽取按钮相邻，避免误触后才发现设置在下方。
                    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
                        Row(
                            Modifier.fillMaxWidth().padding(start = 16.dp, end = 12.dp, top = 4.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("抽取前二次确认", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                            Switch(checked = state.confirmPullEnabled, onCheckedChange = {
                                skipConfirmation = !it
                                viewModel.setConfirmPullEnabled(it)
                            })
                        }
                    }
                    if (center.pool.isNotEmpty()) {
                        GachaSectionTitle("称号池")
                        center.pool.forEach { row -> GachaPoolRowView(row) }
                    }
                    if (center.news.isNotEmpty()) {
                        GachaSectionTitle("最近喜报")
                        GachaNews(center.news)
                    }
                    if (center.allTitles.isNotEmpty()) {
                        GachaSectionTitle("全部称号", "${center.allTitles.distinct().size} 种")
                        GachaTitleGroups(center.allTitles)
                    }
                }
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

// ---------------- 称号交易 ----------------
