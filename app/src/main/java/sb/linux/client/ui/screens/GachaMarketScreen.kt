@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package sb.linux.client.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.lifecycle.viewmodel.compose.viewModel
import sb.linux.client.domain.gacha.GachaMarketListing
import sb.linux.client.ui.session.Session
import sb.linux.client.ui.*
import sb.linux.client.ui.feature.gacha.GachaMarketViewModel
import sb.linux.client.ui.feature.gacha.GachaMarketViewModelFactory

@Composable
fun GachaMarketScreen(session: Session, nav: NavHostController) {
    val factory = remember(session.gachaMarketRepository) {
        GachaMarketViewModelFactory(session.gachaMarketRepository, session::showToast)
    }
    val viewModel: GachaMarketViewModel = viewModel(factory = factory)
    val state by viewModel.uiState.collectAsState()
    var buying by remember { mutableStateOf<GachaMarketListing?>(null) }
    var quantity by remember { mutableStateOf("1") }
    LaunchedEffect(viewModel) { viewModel.load(1) }
    LaunchedEffect(state.submitting) { if (!state.submitting) buying = null }

    buying?.let { item ->
        val count = quantity.toIntOrNull()?.coerceIn(1, item.available) ?: 1
        AlertDialog(
            onDismissRequest = { if (!state.submitting) buying = null },
            icon = { Icon(Icons.Filled.Storefront, null) },
            title = { Text("确认购买 ${item.badge.name}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = quantity,
                        onValueChange = { quantity = it.filter(Char::isDigit).take(4) },
                        label = { Text("数量（1-${item.available}）") },
                        keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    // 合计金额单独一行加粗，避免与单价混在一句话里。
                    Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                "单价 ${item.price} 积分",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                "合计 ${item.price * count}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            },
            dismissButton = { TextButton(enabled = !state.submitting, onClick = { buying = null }) { Text("取消") } },
            confirmButton = {
                Button(enabled = !state.submitting && quantity.toIntOrNull() in 1..item.available, onClick = {
                    viewModel.purchase(item, count)
                }) { Text(if (state.submitting) "购买中…" else "确认购买") }
            },
        )
    }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("称号交易") },
            navigationIcon = { IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } },
            actions = {
                IconButton(onClick = viewModel::load) { Icon(Icons.Filled.Refresh, "刷新") }
                TextButton(onClick = { nav.navigate("gachaOperation/marketMine") }) { Text("发布") }
            },
        )
    }) { pad ->
        LazyColumn(
            Modifier.padding(pad).fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                // 筛选区收进一张卡片，与下方出品卡片区分层级。
                Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            state.query, viewModel::setQuery,
                            Modifier.fillMaxWidth(),
                            label = { Text("搜索称号") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            leadingIcon = { Icon(Icons.Filled.Search, null, Modifier.size(20.dp)) },
                            trailingIcon = {
                                if (state.query.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.setQuery(""); viewModel.load(1) }) { Icon(Icons.Filled.Close, "清空") }
                                }
                            },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { viewModel.load(1) }),
                        )
                        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("" to "全部", "ur" to "UR", "ssr" to "SSR", "sr" to "SR", "r" to "R", "n" to "N").forEach { (value, label) ->
                                FilterChip(
                                    selected = state.rarity == value,
                                    onClick = { viewModel.selectRarity(value) },
                                    label = { Text(label) },
                                    shape = RoundedCornerShape(50),
                                )
                            }
                        }
                        // 排序为单选，用分段按钮表达互斥关系。
                        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                            listOf("latest" to "最新", "price_asc" to "价格↑", "price_desc" to "价格↓")
                                .forEachIndexed { index, (value, label) ->
                                    SegmentedButton(
                                        selected = state.sort == value,
                                        onClick = { viewModel.selectSort(value) },
                                        shape = SegmentedButtonDefaults.itemShape(index, 3),
                                    ) { Text(label, maxLines = 1) }
                                }
                        }
                        state.market?.summary?.takeIf { it.isNotBlank() }?.let {
                            Text(it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        state.market?.note?.takeIf { it.isNotBlank() }?.let {
                            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            if (state.loading) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            state.error?.let { message ->
                item { Text(message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
            }
            if (!state.loading && state.error == null && state.market?.listings.orEmpty().isEmpty()) {
                item { GachaEmptyCard("没有符合条件的出品") }
            }
            items(state.market?.listings.orEmpty()) { item ->
                RarityCard(item.badge.rarity) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TitleBadgeView(item.badge, modifier = Modifier.weight(1f, fill = false))
                        Spacer(Modifier.weight(1f))
                        GachaCountPill("剩余 ${item.available}")
                    }
                    Row(verticalAlignment = Alignment.Bottom) {
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(
                                    item.price.toString(),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                Text(
                                    "积分 / 个",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(bottom = 2.dp),
                                )
                            }
                            if (item.timeLeft.isNotBlank()) {
                                Text(
                                    "剩余时间 ${item.timeLeft}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        Button(
                            onClick = { quantity = "1"; buying = item },
                            shape = RoundedCornerShape(50),
                            contentPadding = PaddingValues(horizontal = 22.dp, vertical = 8.dp),
                        ) { Text("购买") }
                    }
                }
            }
            item { PaginationBar(state.page, state.market?.lastPage ?: state.page) { target -> if (!state.loading) viewModel.load(target) } }
        }
    }
}

// ---------------- 称号动态操作页 ----------------
