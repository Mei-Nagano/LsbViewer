package sb.linux.client.ui.patterns.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import sb.linux.client.ui.patterns.user.Avatar
import sb.linux.client.ui.theme.LocalAppSizes
import sb.linux.client.ui.theme.LocalAppSpacing

internal data class SettingsCategory(
    val label: String,
    val description: String,
    val route: String,
    val icon: ImageVector,
)

/** 分类只负责数据展示和选择回调，不拥有导航或设置状态。 */
@Composable
internal fun SettingsCategoryGrid(categories: List<SettingsCategory>, onSelect: (SettingsCategory) -> Unit) {
    val spacing = LocalAppSpacing.current
    val pages = categories.chunked(8)
    if (pages.isEmpty()) return
    val pager = rememberPagerState { pages.size }
    SettingsCard {
        HorizontalPager(state = pager, modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top, beyondViewportPageCount = 1) { page ->
            SettingsCategoryPage(pages[page], onSelect)
        }
        if (pages.size > 1) Row(
            Modifier.fillMaxWidth().padding(bottom = spacing.item),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            pages.indices.forEach { page ->
                val selected = pager.currentPage == page
                val width by animateDpAsState(if (selected) spacing.section else spacing.item, label = "入口页指示")
                Box(Modifier.padding(horizontal = spacing.inline).size(width, spacing.item)
                    .clip(CircleShape).background(if (selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outlineVariant))
            }
        }
    }
}

@Composable
private fun SettingsCategoryPage(categories: List<SettingsCategory>, onSelect: (SettingsCategory) -> Unit) {
    val spacing = LocalAppSpacing.current
    val sizes = LocalAppSizes.current
        Column(
            Modifier.fillMaxWidth().padding(vertical = spacing.contentGap),
            verticalArrangement = Arrangement.spacedBy(spacing.item),
        ) {
            categories.chunked(4).forEach { row ->
                Row(Modifier.fillMaxWidth()) {
                    row.forEach { category ->
                        Column(
                            Modifier.weight(1f)
                                .clip(MaterialTheme.shapes.large)
                                .clickable(role = Role.Button, onClickLabel = category.description) { onSelect(category) }
                                .heightIn(min = sizes.navigationBarHeight)
                                .padding(horizontal = spacing.inline, vertical = spacing.contentGap),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(spacing.contentGap),
                        ) {
                            Icon(category.icon, null, Modifier.size(sizes.icon), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(category.label, style = MaterialTheme.typography.titleMedium,
                                fontWeight = MaterialTheme.typography.bodyLarge.fontWeight, textAlign = TextAlign.Center)
                        }
                    }
                    repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
}
