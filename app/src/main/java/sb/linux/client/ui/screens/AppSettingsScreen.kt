package sb.linux.client.ui.screens
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Redeem
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.lifecycle.viewmodel.compose.viewModel
import sb.linux.client.ui.navigation.LocalMasterNav
import sb.linux.client.ui.session.Session
import sb.linux.client.ui.feature.settings.SettingsSearchIndex
import sb.linux.client.ui.feature.gacha.CardRedemptionsViewModel
import sb.linux.client.ui.feature.gacha.CardRedemptionsViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSettingsScreen(session: Session, nav: NavHostController) {
    var resetTick by remember { mutableStateOf(0) }
    var settingsQuery by rememberSaveable { mutableStateOf("") }
    var showCardRedemptions by remember { mutableStateOf(false) }
    val redemptionFactory = remember(session.cardRedemptionsRepository) {
        CardRedemptionsViewModelFactory(session.cardRedemptionsRepository)
    }
    val redemptionViewModel: CardRedemptionsViewModel = viewModel(factory = redemptionFactory)
    val cardRedemptions = redemptionViewModel.records
    LaunchedEffect(showCardRedemptions) {
        if (showCardRedemptions) redemptionViewModel.refresh()
    }
    val entries = listOf(
        Triple("常规设置", "打开链接方式 · 匿名免费图床 · 检查更新", "generalSettings"),
        Triple("网络设置", "HTTP / SOCKS5 应用代理 · 自动检测 · DNS over HTTPS", "networkSettings"),
        Triple("浏览设置", "滚动模式 · 新帖子折叠 · Base64 · 表格 · UID · 评论 · 弹幕", "browseSettings"),
        Triple("数据管理", "缓存分析 · 分类清理 · 历史 · 收藏 · AI 数据 · 自动清理", "dataManagement"),
        Triple("使用统计", "浏览 · 收藏 · 发帖回帖 · 积分支出 · AI 总结", "usageStats"),
        Triple("外观设置", "深浅模式 · 主题色 · 背景 · 字体大小粗细 · 卡片颜色 · 底栏", "themeSettings"),
        Triple("AI 总结", "API · 模型列表 · 提示词 · 自动总结 · 总结历史", "aiSettings"),
        Triple("备份设置", "JSON · WebDAV · 导入 · 导出", "transferSettings"),
        Triple("屏蔽词管理", "源站同步 · 屏蔽用户 · 关键词过滤", "blockWords"),
        Triple("发卡兑换记录", "自动保存已兑换帖子、卡名与兑换码", "cardRedemptions"),
        Triple("已导出的帖子", "HTML · Markdown · 长图 · 多页导出", "exportedTopics"),
    )
    val visibleEntries = if (settingsQuery.isBlank()) entries else SettingsSearchIndex.search(settingsQuery)
        .map { Triple(it.title, it.category, it.route) }
    // 按目标路由取图标：搜索结果命中的是具体选项，也能借父分类的图标定位
    val entryIcons = mapOf(
        "generalSettings" to Icons.Filled.Tune,
        "networkSettings" to Icons.Filled.Dns,
        "dohSettings" to Icons.Filled.Dns,
        "browseSettings" to Icons.AutoMirrored.Filled.MenuBook,
        "dataManagement" to Icons.Filled.Storage,
        "usageStats" to Icons.Filled.Insights,
        "themeSettings" to Icons.Filled.Palette,
        "aiSettings" to Icons.Filled.AutoAwesome,
        "transferSettings" to Icons.Filled.Backup,
        "blockWords" to Icons.Filled.Block,
        "cardRedemptions" to Icons.Filled.Redeem,
        "exportedTopics" to Icons.Filled.Description,
    )
    // 平板双栏：本页位于主栏，返回箭头应弹出主栏（回到「我的」）；手机走单栈
    val masterNav = LocalMasterNav.current
    if (showCardRedemptions) {
        AlertDialog(
            onDismissRequest = { showCardRedemptions = false },
            title = { Text("发卡兑换记录（${cardRedemptions.size}）") },
            text = {
                if (cardRedemptions.isEmpty()) {
                    Text("暂无记录。打开已兑换过的发卡帖后，应用会从“我的购买记录”自动建档。")
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(cardRedemptions, key = { "${it.topicId}-${it.code}" }) { record ->
                            Surface(
                                modifier = Modifier.fillMaxWidth().clickable {
                                    showCardRedemptions = false
                                    nav.navigate("topic/${record.topicId}")
                                },
                                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                shape = RoundedCornerShape(14.dp),
                            ) {
                                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Text(record.topicTitle.ifBlank { "帖子 #${record.topicId}" }, fontWeight = FontWeight.SemiBold, maxLines = 2)
                                    Text(record.cardTitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    SelectionContainer { Text(record.code, style = MaterialTheme.typography.bodyMedium) }
                                    Text(
                                        listOf(record.price, record.sourceTime).filter { it.isNotBlank() }.joinToString(" · "),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                if (cardRedemptions.isNotEmpty()) TextButton(onClick = {
                    redemptionViewModel.clear()
                }) { Text("清空记录") }
            },
            dismissButton = { TextButton(onClick = { showCardRedemptions = false }) { Text("关闭") } },
        )
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("应用设置") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (masterNav != null) masterNav.popBackStack() else nav.popBackStack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
                actions = {
                    // 一级菜单：重置所有应用设置（3.9）
                    ResetAction(
                        title = "重置所有应用设置",
                    ) {
                        session.preferences.resetAllAppSettings()
                        resetTick++
                        session.showToast("已重置所有应用设置")
                    }
                }
            )
        }
    ) { pad ->
        Column(
            Modifier
                .padding(pad)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            OutlinedTextField(
                value = settingsQuery,
                onValueChange = { settingsQuery = it },
                modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                singleLine = true,
                placeholder = { Text("搜索设置项，如「主题色」「缓存」") },
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                trailingIcon = if (settingsQuery.isNotBlank()) {{
                    IconButton(onClick = { settingsQuery = "" }) { Icon(Icons.Filled.Close, "清空") }
                }} else null,
                shape = RoundedCornerShape(28.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                ),
            )
            GroupCard {
                if (visibleEntries.isEmpty()) {
                    Text(
                        "没有找到相关设置",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth().padding(18.dp),
                    )
                } else visibleEntries.forEachIndexed { index, (title, subtitle, route) ->
                    SettingMenuRow(title, subtitle, entryIcons[route]) {
                        if (route == "cardRedemptions") showCardRedemptions = true else nav.navigate(route)
                    }
                    if (index < visibleEntries.lastIndex) {
                        HorizontalDivider(
                            Modifier.padding(start = 66.dp, end = 18.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                        )
                    }
                }
            }
        }
    }
}

/** 独立数据管理：按项删除，并选择应用完全退出后需要自动清理的项目。 */
