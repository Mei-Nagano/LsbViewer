@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package sb.linux.client.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import sb.linux.client.domain.topic.TopicCard
import sb.linux.client.ui.components.feedback.AppConfirmDialog
import sb.linux.client.ui.components.feedback.EmptyBox
import sb.linux.client.ui.components.input.LocalFilterField
import sb.linux.client.ui.components.navigation.AppPageScaffold
import sb.linux.client.ui.feature.community.BrowsingHistoryViewModel
import sb.linux.client.ui.feature.community.BrowsingHistoryViewModelFactory
import sb.linux.client.ui.patterns.topic.TopicCardView
import sb.linux.client.ui.session.Session
import sb.linux.client.ui.theme.LocalAppSpacing
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * 本地浏览历史：按真实日期分组，行内只显示当天时刻；左滑删除单条，清空需确认且说明只清理本地记录。
 * 游客同样可读；点击回到帖子（阅读位置仍由阅读锚点单独恢复）。
 */
@Composable
fun FootprintScreen(session: Session, nav: NavHostController) {
    val factory = remember(session.browsingHistoryRepository) {
        BrowsingHistoryViewModelFactory(session.browsingHistoryRepository)
    }
    val viewModel: BrowsingHistoryViewModel = viewModel(factory = factory)
    val topics = viewModel.topics
    var showClearDialog by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    val spacing = LocalAppSpacing.current

    // 从帖子返回时刷新（浏览时间可能变化）
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    AppPageScaffold(
        title = "浏览历史",
        onBack = { nav.popBackStack() },
        actions = {
            TextButton(onClick = { showClearDialog = true }, enabled = topics.isNotEmpty()) {
                Text("清空")
            }
        },
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            if (topics.isNotEmpty()) {
                LocalFilterField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = "搜索浏览历史",
                    modifier = Modifier.padding(
                        start = spacing.pageHorizontal,
                        end = spacing.pageHorizontal,
                        top = spacing.item,
                        bottom = spacing.item,
                    ),
                )
            }
            val filtered = remember(topics, query) {
                topics.filter { query.isBlank() || it.title.contains(query.trim(), ignoreCase = true) }
            }
            when {
                topics.isEmpty() -> EmptyBox("暂无浏览历史", "打开帖子后会自动记录，最多保留 200 条")
                filtered.isEmpty() -> EmptyBox("无匹配“$query”的记录")
                else -> HistoryList(filtered, nav, spacing, onRemove = {
                    viewModel.remove(it)
                    session.showToast("已删除该记录")
                })
            }
        }
    }

    if (showClearDialog) {
        AppConfirmDialog(
            title = "清空浏览历史",
            message = "确定清空全部 ${topics.size} 条本地浏览记录吗？只影响本机历史，不会取消源站收藏或清除阅读位置。",
            onDismiss = { showClearDialog = false },
            onConfirm = {
                viewModel.clear()
                showClearDialog = false
                session.showToast("浏览历史已清空")
            },
            confirmText = "清空", destructive = true, dismissText = "取消",
        )
    }
}

private val DayTimeFormat = DateTimeFormatter.ofPattern("M月d日")
private val DayTimeFormatWithYear = DateTimeFormatter.ofPattern("yyyy年M月d日")

@Composable
private fun HistoryList(
    topics: List<TopicCard>,
    nav: NavHostController,
    spacing: sb.linux.client.ui.theme.AppSpacing,
    onRemove: (Long) -> Unit,
) {
    val today = LocalDate.now()
    val grouped = topics.groupBy { visitedDate(it.visitedAt) }
    LazyColumn(contentPadding = PaddingValues(vertical = spacing.pageVertical)) {
        grouped.forEach { (date, entries) ->
            item(key = "day-$date") {
                Text(
                    dayLabel(date, today),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(
                        start = spacing.pageHorizontal, end = spacing.pageHorizontal,
                        top = spacing.contentGap, bottom = spacing.inline,
                    ),
                )
            }
            items(entries, key = { it.topicId }) { topic ->
                DismissibleHistory(onDismiss = { onRemove(topic.topicId) }, spacing = spacing) {
                    TopicCardView(
                        topic,
                        onClick = { nav.navigate("topic/${topic.topicId}") },
                        onForumClick = { fid -> if (fid > 0) nav.navigate("forum/$fid") },
                        showComments = false,
                        showForum = false,
                    )
                }
            }
        }
    }
}

private fun visitedDate(at: Long): LocalDate = if (at > 0) {
    Instant.ofEpochMilli(at).atZone(ZoneId.systemDefault()).toLocalDate()
} else LocalDate.MIN

private fun dayLabel(date: LocalDate, today: LocalDate): String = when (date) {
    today -> "今天"
    today.minusDays(1) -> "昨天"
    LocalDate.MIN -> "更早"
    else -> if (date.year == today.year) date.format(DayTimeFormat)
    else date.format(DayTimeFormatWithYear)
}

@Composable
private fun DismissibleHistory(
    onDismiss: () -> Unit,
    spacing: sb.linux.client.ui.theme.AppSpacing,
    content: @Composable () -> Unit,
) {
    val state = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) { onDismiss(); true } else false
        },
    )
    SwipeToDismissBox(
        state = state,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp, vertical = 5.dp)
                    .background(MaterialTheme.colorScheme.errorContainer, MaterialTheme.shapes.large),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Row(
                    Modifier.padding(end = 22.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(spacing.inline),
                ) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                    )
                    Text("删除", color = MaterialTheme.colorScheme.onErrorContainer)
                }
            }
        },
        content = { content() },
    )
}
