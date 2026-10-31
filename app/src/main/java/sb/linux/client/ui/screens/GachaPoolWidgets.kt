package sb.linux.client.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Recycling
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import sb.linux.client.domain.gacha.GachaAction
import sb.linux.client.domain.gacha.GachaPoolRow
import sb.linux.client.domain.gacha.GachaTitleItem
import sb.linux.client.ui.TitleBadgeView
import sb.linux.client.ui.titleRarityColor

@Composable
internal fun GachaPoolRowView(row: GachaPoolRow) {
    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            RarityPill(row.rarity)
            Text(
                row.countText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Text(row.rateText, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        }
    }
}

/** 卡片内一行：图标容器 + 标题 + 尾部件。收藏行与子页入口共用同一行式样。 */
@Composable
private fun GachaCardRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    trailing: @Composable RowScope.() -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(36.dp).clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
        }
        Spacer(Modifier.width(13.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        trailing()
    }
}

/**
 * 称号系统卡：收藏与各子页入口合并在同一张卡里。
 * 收藏收成一行「称号收藏」，默认折叠，点击展开在行下方；入口行沿用同一行式样。
 */
@Composable
internal fun GachaSystemCard(
    titles: List<GachaTitleItem>,
    enabled: Boolean,
    onAction: (GachaAction) -> Unit,
    nav: NavHostController,
) {
    val entries = listOf(
        Triple(Icons.Filled.Casino, "称号抽取", "gachaCenter"),
        Triple(Icons.Filled.LocalFireDepartment, "称号熔炼", "gachaOperation/forge"),
        Triple(Icons.Filled.Recycling, "称号回收", "gachaOperation/recycle"),
        Triple(Icons.Filled.AutoAwesome, "UR 合成", "gachaOperation/recipes"),
        Triple(Icons.Filled.Storefront, "称号交易", "gachaMarket"),
    )
    // 默认折叠：称号多时会把下面的入口行整个推走，需要看时再点开（旋转不丢展开状态）。
    var open by rememberSaveable { mutableStateOf(false) }
    var showAllTitles by rememberSaveable { mutableStateOf(false) }
    val divider = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column {
            GachaCardRow(Icons.Filled.MilitaryTech, "称号收藏", onClick = { open = !open }) {
                GachaCountPill(if (titles.isEmpty()) "暂无" else "${titles.size} 种")
                Spacer(Modifier.width(8.dp))
                Icon(
                    if (open) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    if (open) "收起" else "展开",
                    Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (open) {
                if (titles.isEmpty()) {
                    Text(
                        "还没有称号，去下面的称号抽取试试",
                        Modifier.padding(start = 65.dp, end = 16.dp, bottom = 12.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    // 已装备的排在前面，其余按稀有度从高到低。
                    val sortedTitles = titles.sortedWith(
                        compareByDescending<GachaTitleItem> { it.equipped }.thenBy { rarityOrder(it.badge.rarity) }
                    )
                    sortedTitles.take(if (showAllTitles) sortedTitles.size else 6)
                        .forEach { item ->
                            GachaTitleRow(item, enabled, onAction) {
                                nav.navigate("gachaOperation/gift?title=${android.net.Uri.encode(item.badge.name)}")
                            }
                        }
                    if (sortedTitles.size > 6) {
                        TextButton(
                            onClick = { showAllTitles = !showAllTitles },
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                        ) { Text(if (showAllTitles) "收起更多称号" else "查看其余 ${sortedTitles.size - 6} 种") }
                    }
                    Spacer(Modifier.height(4.dp))
                }
            }
            HorizontalDivider(color = divider)
            entries.forEachIndexed { index, (icon, label, path) ->
                GachaCardRow(icon, label, onClick = { nav.navigate(path) }) {
                    Icon(
                        Icons.Filled.ChevronRight, null,
                        Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (index != entries.lastIndex) {
                    HorizontalDivider(Modifier.padding(start = 65.dp), color = divider)
                }
            }
        }
    }
}
