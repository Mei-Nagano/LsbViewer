package sb.linux.client.ui.screens
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import sb.linux.client.ui.session.Session
import sb.linux.client.ui.feature.community.UsageStatsViewModel
import sb.linux.client.ui.feature.community.UsageStatsViewModelFactory
import sb.linux.client.ui.EmptyBox
import sb.linux.client.ui.MarkdownText

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun UsageStatsScreen(session: Session, nav: NavHostController) {
    val userId = session.account.loginState.userId
    val factory = remember(
        session.usageStatisticsRepository,
        session.favoritesRepository,
        session.aiConfigurationRepository,
        session.aiAssistantRepository,
    ) {
        UsageStatsViewModelFactory(
            session.usageStatisticsRepository,
            session.favoritesRepository,
            session.aiConfigurationRepository,
            session.aiAssistantRepository,
        )
    }
    val viewModel: UsageStatsViewModel = viewModel(factory = factory)
    val state = viewModel.state
    LaunchedEffect(userId) { viewModel.refresh(userId) }
    var days by rememberSaveable { mutableIntStateOf(30) }
    LaunchedEffect(days) { viewModel.setDays(days) }
    LaunchedEffect(state.feedback) {
        state.feedback?.let {
            session.showToast(it)
            viewModel.consumeFeedback()
        }
    }
    val events = state.visibleEvents
    val counts = state.counts
    val sourceTotals = state.sourceTotals
    val sourcePoints = state.visiblePoints
    val sourceIncome = state.sourceIncome
    val sourceExpense = state.sourceExpense
    val gachaSpent = state.gachaSpent
    val favorites = state.favorites
    val aiSummary = state.aiSummary
    val aiBusy = state.aiBusy
    val aiError = state.aiError
    val syncingPoints = state.syncingPoints
    var confirmClear by remember { mutableStateOf(false) }

    if (confirmClear) AlertDialog(
        onDismissRequest = { confirmClear = false },
        title = { Text("清空使用统计") },
        text = { Text("将删除操作统计和已保存的 AI 分析，不影响浏览历史与收藏。") },
        confirmButton = { TextButton(onClick = {
            viewModel.clear(userId)
            confirmClear = false
        }) { Text("清空") } },
        dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("取消") } },
    )

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("使用统计") },
            navigationIcon = { IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } },
            actions = { IconButton(onClick = { confirmClear = true }) { Icon(Icons.Filled.Delete, "清空统计") } },
        )
    }) { pad ->
        LazyColumn(
            modifier = Modifier.padding(pad).fillMaxSize(),
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    listOf(7 to "7 天", 30 to "30 天", 0 to "全部").forEachIndexed { index, (value, label) ->
                        SegmentedButton(
                            selected = days == value, onClick = { days = value; viewModel.setDays(value) },
                            shape = SegmentedButtonDefaults.itemShape(index, 3), modifier = Modifier.weight(1f),
                        ) { Text(label) }
                    }
                }
            }
            item {
                FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        "浏览" to (counts["view"] ?: 0), "收藏（源站）" to (sourceTotals?.favorites ?: "未同步"),
                        "发帖（源站）" to (sourceTotals?.topics ?: "未同步"), "回帖（源站）" to (sourceTotals?.replies ?: "未同步"),
                        "积分支出" to sourceExpense, "抽卡消耗" to gachaSpent,
                        "淘帖" to (sourceTotals?.collections ?: "未同步"),
                    ).forEach { (label, value) ->
                        Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceContainerLow, modifier = Modifier.width(104.dp)) {
                            Column(Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(value.toString(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
            item {
                GroupCard {
                    Column(Modifier.padding(horizontal = 18.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("源站积分流水", style = MaterialTheme.typography.titleMedium)
                        Text("当前范围 ${sourcePoints.size} 条 · 收入 $sourceIncome · 支出 $sourceExpense")
                        Text("发帖、回帖、收藏为个人主页当前总数；积分按所选日期汇总完整流水。浏览次数来自本机。同步全部分页，成功后统一更新。", style = MaterialTheme.typography.bodySmall)
                        Button(
                            enabled = !syncingPoints && session.account.loginState.loggedIn,
                            onClick = { viewModel.sync(userId) },
                        ) { Text(if (syncingPoints) "同步中…" else "同步个人主页") }
                    }
                }
            }
            item {
                GroupCard {
                    Column(Modifier.padding(horizontal = 18.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("AI 使用分析", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        when {
                            aiBusy -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp); Text("正在分析…")
                            }
                            aiError != null -> Text(aiError.orEmpty(), color = MaterialTheme.colorScheme.error)
                            aiSummary.isNotBlank() -> MarkdownText(aiSummary)
                            else -> Text("根据统计、浏览历史和收藏内容生成兴趣与参与习惯总结。", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Button(onClick = viewModel::summarize, enabled = !aiBusy && state.aiConfigured, modifier = Modifier.fillMaxWidth()) {
                            Text(if (aiSummary.isBlank()) "生成 AI 总结" else "重新生成")
                        }
                        if (!state.aiConfigured) Text("请先在 AI 总结设置中配置 API。", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            if (events.isNotEmpty()) {
                item { GroupLabel("最近活动") }
                items(events.take(30), key = { "${it.at}-${it.type}-${it.topicId}" }) { event ->
                    val label = when (event.type) {
                        "view" -> "浏览帖子"; "favorite" -> "收藏帖子"; "topic" -> "发布帖子"
                        "reply" -> "发表回复"; "coin" -> "投币 ${event.value} 积分"
                        "gacha" -> "抽取称号 ${event.value} 积分"; else -> "打赏 ${event.value} 积分"
                    }
                    ListItem(
                        headlineContent = { Text(label) },
                        supportingContent = { if (event.title.isNotBlank()) Text(event.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        trailingContent = { Text(java.text.SimpleDateFormat("MM-dd HH:mm", androidx.compose.ui.platform.LocalConfiguration.current.locales[0]).format(java.util.Date(event.at)), style = MaterialTheme.typography.labelSmall) },
                        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                        modifier = Modifier.clip(RoundedCornerShape(14.dp)),
                    )
                }
            } else item { EmptyBox("该时间范围内暂无统计；新版开始记录后会显示在这里") }
        }
    }
}

/** 设置菜单行：图标 + 标题 + 副标题 + 右箭头 */
