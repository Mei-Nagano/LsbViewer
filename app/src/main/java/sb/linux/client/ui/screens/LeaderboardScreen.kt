@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package sb.linux.client.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import sb.linux.client.domain.community.LeaderRow
import sb.linux.client.ui.components.feedback.EmptyBox
import sb.linux.client.ui.components.feedback.ErrorBox
import sb.linux.client.ui.components.feedback.LoadingBox
import sb.linux.client.ui.components.navigation.AppPageScaffold
import sb.linux.client.ui.feature.community.LeaderboardViewModel
import sb.linux.client.ui.feature.community.LeaderboardViewModelFactory
import sb.linux.client.ui.patterns.user.UserAvatar
import sb.linux.client.ui.session.Session
import sb.linux.client.ui.theme.LocalAppSpacing

/**
 * 用户榜单：类型切换保留来源返回；前三名用主题容器着色（不写死金银铜），
 * 其余名次为平面列表行；头像 48dp 触区。
 */
@Composable
fun LeaderboardScreen(session: Session, nav: NavHostController) {
    val entry = nav.currentBackStackEntry ?: return
    val initialType = entry.arguments?.getString("type") ?: "points"
    val factory = remember(session.leaderboardRepository, initialType) {
        LeaderboardViewModelFactory(session.leaderboardRepository, initialType)
    }
    val viewModel: LeaderboardViewModel = viewModel(factory = factory)
    val state by viewModel.uiState.collectAsState()
    val type = state.type
    val rows = state.rows
    val spacing = LocalAppSpacing.current
    LaunchedEffect(viewModel) { viewModel.load() }

    AppPageScaffold(
        title = "用户榜单",
        onBack = { nav.popBackStack() },
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            ScrollableTabRow(
                selectedTabIndex = LeaderTabs.indexOfFirst { it.first == type }.coerceAtLeast(0),
                edgePadding = spacing.pageHorizontal,
                containerColor = MaterialTheme.colorScheme.surface,
            ) {
                LeaderTabs.forEach { (tabType, label) ->
                    Tab(
                        selected = type == tabType,
                        onClick = { viewModel.selectType(tabType) },
                        text = { Text(label) },
                    )
                }
            }
            when {
                state.loading -> LoadingBox()
                state.error != null -> ErrorBox(state.error!!) { viewModel.load() }
                rows.isEmpty() -> EmptyBox("暂无数据")
                else -> LeaderboardList(rows, nav, spacing)
            }
        }
    }
}

@Composable
private fun LeaderboardList(
    rows: List<LeaderRow>,
    nav: NavHostController,
    spacing: sb.linux.client.ui.theme.AppSpacing,
) {
    LazyColumn(contentPadding = PaddingValues(top = spacing.pageVertical, bottom = spacing.pageVertical)) {
        if (rows.size >= 3) {
            item(key = "podium") {
                Row(
                    Modifier.fillMaxWidth().padding(
                        start = spacing.pageHorizontal,
                        end = spacing.pageHorizontal,
                        bottom = spacing.contentGap,
                    ),
                    horizontalArrangement = Arrangement.spacedBy(spacing.item),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    // 视觉顺序：2 / 1 / 3，高度差由主题容器承担而不是写死像素
                    listOf(rows[1], rows[0], rows[2]).forEach { row ->
                        PodiumCard(row, nav, Modifier.weight(1f), spacing)
                    }
                }
            }
        }
        itemsIndexed(rows.drop(3), key = { _, r -> "${r.rank}-${r.userId}" }) { index, row ->
            ListItem(
                leadingContent = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "${row.rank}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.width(28.dp),
                        )
                        UserAvatar(row.userId, row.avatarUrl, 40, online = row.online, minTouchTarget = true)
                    }
                },
                headlineContent = { Text(row.username, style = MaterialTheme.typography.bodyLarge, maxLines = 1) },
                supportingContent = if (row.userGroup.isNotBlank()) {
                    { Text(row.userGroup, style = MaterialTheme.typography.bodySmall) }
                } else null,
                trailingContent = {
                    Text(
                        row.valueText,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                modifier = Modifier.clickable { nav.navigate("user/${row.userId}") },
            )
            if (index != rows.drop(3).lastIndex) {
                HorizontalDivider(
                    Modifier.padding(start = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                )
            }
        }
    }
}

@Composable
private fun PodiumCard(
    row: LeaderRow,
    nav: NavHostController,
    modifier: Modifier,
    spacing: sb.linux.client.ui.theme.AppSpacing,
) {
    val container = when (row.rank) {
        1 -> MaterialTheme.colorScheme.tertiaryContainer
        2 -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val onContainer = when (row.rank) {
        1 -> MaterialTheme.colorScheme.onTertiaryContainer
        2 -> MaterialTheme.colorScheme.onSecondaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(
        shape = MaterialTheme.shapes.large,
        color = container,
        modifier = modifier
            .heightIn(min = if (row.rank == 1) 184.dp else 160.dp)
            .clip(MaterialTheme.shapes.large)
            .clickable { nav.navigate("user/${row.userId}") },
    ) {
        Column(
            Modifier.padding(vertical = spacing.contentGap, horizontal = spacing.item),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier.size(30.dp).clip(CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "${row.rank}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = onContainer,
                )
            }
            Spacer(Modifier.height(spacing.item))
            UserAvatar(row.userId, row.avatarUrl, 44, online = row.online)
            Spacer(Modifier.height(spacing.inline))
            Text(
                row.username,
                style = MaterialTheme.typography.labelLarge,
                color = onContainer,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                row.valueText,
                style = MaterialTheme.typography.labelSmall,
                color = onContainer.copy(alpha = 0.8f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
