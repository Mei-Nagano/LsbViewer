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
import sb.linux.client.ui.session.Session
import sb.linux.client.ui.Avatar
import sb.linux.client.ui.Badge
import sb.linux.client.ui.EmptyBox
import sb.linux.client.ui.ErrorBox
import sb.linux.client.ui.LoadingBox
import sb.linux.client.ui.LoginRequiredBox

@Composable
internal fun CollectionList(
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
