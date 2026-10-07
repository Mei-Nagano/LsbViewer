package sb.linux.client.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.lifecycle.viewmodel.compose.viewModel
import sb.linux.client.ui.session.Session
import sb.linux.client.ui.*
import sb.linux.client.ui.feature.gacha.GachaOperationViewModel
import sb.linux.client.ui.feature.gacha.GachaOperationViewModelFactory

private data class PendingGachaSubmit(
    val label: String,
    val action: String,
    val fields: List<Pair<String, String>>,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GachaOperationScreen(session: Session, nav: NavHostController, kind: String) {
    val preferredTitle = nav.currentBackStackEntry?.arguments?.getString("title").orEmpty()
    // 熔炼/回收改走与其它称号操作相同的原生解析：内嵌 WebView 依赖 ProxyController，
    // 机型不支持时 DoH 下会整页失败；解析源站表单后用应用自己的网络栈提交更可靠。
    val config = when (kind) {
        "forge" -> "称号熔炼" to "/gacha_forge_center"
        "recycle" -> "称号回收" to "/gacha_recycle_center"
        "gift" -> "称号赠送" to "/gacha_profile"
        "recipes" -> "UR 合成" to "/gacha_recipes"
        "marketMine" -> "发布称号" to "/gacha_market_mine"
        "marketOrders" -> "交易记录" to "/gacha_market_orders"
        "identity" -> "认证申请" to "/identity_center"
        else -> "称号操作" to "/gacha_profile"
    }
    val factory = remember(session.gachaOperationRepository, kind, preferredTitle, config.second) {
        GachaOperationViewModelFactory(
            session.gachaOperationRepository, kind, preferredTitle, config.second, session::showToast,
        )
    }
    val viewModel: GachaOperationViewModel = viewModel(factory = factory, key = "gacha-operation-$kind")
    val state by viewModel.uiState.collectAsState()
    var pending by remember { mutableStateOf<PendingGachaSubmit?>(null) }
    LaunchedEffect(viewModel) { viewModel.load() }
    LaunchedEffect(state.submitting) { if (!state.submitting) pending = null }

    pending?.let { request ->
        AlertDialog(
            onDismissRequest = { if (!state.submitting) pending = null },
            icon = { Icon(Icons.Filled.AutoAwesome, null) },
            title = { Text("确认${request.label}") },
            text = { Text("将按源站当前规则提交此操作，积分、材料和库存以源站返回结果为准。") },
            dismissButton = { TextButton(enabled = !state.submitting, onClick = { pending = null }) { Text("取消") } },
            confirmButton = {
                Button(enabled = !state.submitting, onClick = {
                    viewModel.submit(request.label, request.action, request.fields)
                }) { Text(if (state.submitting) "处理中…" else "确认") }
            },
        )
    }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text(state.page?.title?.ifBlank { config.first } ?: config.first) },
            navigationIcon = { IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } },
            actions = { IconButton(onClick = viewModel::load) { Icon(Icons.Filled.Refresh, "刷新") } },
        )
    }) { pad ->
        if (state.loading && state.page == null) {
            Box(Modifier.padding(pad).fillMaxSize()) { LoadingBox() }
            return@Scaffold
        }
        if (state.error != null && state.page == null) {
            Box(Modifier.padding(pad).fillMaxSize()) { ErrorBox(state.error!!) { viewModel.load() } }
            return@Scaffold
        }
        LazyColumn(
            Modifier.padding(pad).fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (state.loading) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            state.error?.let { message ->
                item { Text(message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
            }
            // 源站说明收进一张提示卡，与下方可提交的表单区分开。
            state.page?.notes.orEmpty().takeIf { it.isNotEmpty() }?.let { notes ->
                item {
                    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Icon(
                                Icons.Filled.Info, null,
                                Modifier.size(18.dp).padding(top = 1.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                notes.forEach { note ->
                                    Text(note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
            if (state.page?.forms.orEmpty().isNotEmpty()) item { GachaSectionTitle("可执行操作") }
            itemsIndexed(state.page?.forms.orEmpty(), key = { index, form -> "${form.action}-$index" }) { _, form ->
                GachaDynamicForm(form = form, enabled = !state.submitting) { fields ->
                    pending = PendingGachaSubmit(form.label, form.action, fields)
                }
            }
            if (!state.loading && state.error == null && state.page?.forms.isNullOrEmpty() && state.page?.records.isNullOrEmpty()) {
                item { GachaEmptyCard("当前没有可执行的操作或记录") }
            }
            if (state.page?.records.orEmpty().isNotEmpty()) item { GachaSectionTitle("记录", "${state.page?.records?.size ?: 0} 条") }
            state.page?.records.orEmpty().forEach { record ->
                item {
                    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
                        Text(
                            record,
                            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
            // 源站同页内的切换链接：做成一行胶囊，不再是竖着排的文字按钮。
            state.page?.links.orEmpty().filter { it.second.substringBefore('?') == config.second }
                .takeIf { it.isNotEmpty() }?.let { links ->
                    item {
                        Row(
                            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            links.forEach { (label, href) ->
                                AssistChip(
                                    onClick = { viewModel.openPath(href) },
                                    enabled = !state.loading && !state.submitting,
                                    label = { Text(label) },
                                    shape = RoundedCornerShape(50),
                                )
                            }
                        }
                    }
                }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}
