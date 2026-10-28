package sb.linux.client.ui.navigation

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import sb.linux.client.ui.theme.LocalAppSizes
import sb.linux.client.ui.theme.LocalAppSpacing

/**
 * 主目的地导航（DESIGN_SYSTEM D06 `AppPrimaryNavigation`）。
 *
 * 手机使用标准 NavigationBar，宽窗使用同一目的地模型的 NavigationRail；
 * 仅有 2 个有效目的地时使用顶部 PrimaryTabRow（根内分区应改用分段控件）。
 * 所有形态共享选中态、标签与点击事件，不维护第二套导航数据。
 */
@Composable
internal fun AppBottomNavigation(
    destinations: List<AppDestination>,
    selectedId: String,
    onSelect: (String) -> Unit,
) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
        destinations.forEach { destination ->
            val selected = destination.id == selectedId
            NavigationBarItem(
                selected = selected,
                onClick = { onSelect(destination.id) },
                icon = {
                    Icon(
                        if (selected) destination.selectedIcon else destination.unselectedIcon,
                        contentDescription = null,
                    )
                },
                // 标签就是无障碍名称，图标不重复朗读。
                label = { Text(destination.label) },
            )
        }
    }
}

@Composable
internal fun AppNavigationRail(
    destinations: List<AppDestination>,
    selectedId: String,
    onSelect: (String) -> Unit,
    header: @Composable ColumnScope.() -> Unit = {},
) {
    val sizes = LocalAppSizes.current
    NavigationRail(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxHeight().width(sizes.railWidth),
        header = header,
    ) {
        destinations.forEach { destination ->
            val selected = destination.id == selectedId
            NavigationRailItem(
                selected = selected,
                onClick = { onSelect(destination.id) },
                icon = {
                    Icon(
                        if (selected) destination.selectedIcon else destination.unselectedIcon,
                        contentDescription = null,
                    )
                },
                label = { Text(destination.label, style = MaterialTheme.typography.labelMedium) },
            )
        }
    }
}

/** 只有 2 个有效根目的地时的兼容布局：顶部 PrimaryTabRow，取消底部导航。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AppTopPrimaryTabs(
    destinations: List<AppDestination>,
    selectedId: String,
    onSelect: (String) -> Unit,
) {
    val spacing = LocalAppSpacing.current
    val selectedIndex = destinations.indexOfFirst { it.id == selectedId }.coerceAtLeast(0)
    PrimaryTabRow(
        selectedTabIndex = selectedIndex,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        destinations.forEachIndexed { index, destination ->
            val selected = index == selectedIndex
            Tab(
                selected = selected,
                onClick = { onSelect(destination.id) },
                text = { Text(destination.label) },
                icon = {
                    Icon(
                        if (selected) destination.selectedIcon else destination.unselectedIcon,
                        contentDescription = null,
                    )
                },
                modifier = Modifier.padding(horizontal = spacing.inline),
            )
        }
    }
}
