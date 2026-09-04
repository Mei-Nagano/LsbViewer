package sb.linux.client.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
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
import androidx.compose.material.icons.filled.Inbox
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
import sb.linux.client.data.GachaAction
import sb.linux.client.data.GachaPoolRow
import sb.linux.client.data.GachaTitleItem
import sb.linux.client.data.TitleBadge
import sb.linux.client.ui.TitleBadgeView
import sb.linux.client.ui.titleRarityColor

// ---------------- 称号页面共用视觉件 ----------------
// 稀有度色是强调色，不参与「禁止页面染色」的中性化；容器仍取主题表面色。

/** 稀有度强调色：深色主题下提亮源站配色，保证与深背景的对比度。 */
@Composable
internal fun rarityAccent(rarity: String): Color {
    val (base, _) = titleRarityColor(rarity)
    return if (MaterialTheme.colorScheme.surface.luminance() < 0.5f) lerp(base, Color.White, 0.3f) else base
}

/** 稀有度从高到低的排序权重；源站没列出的稀有度排在末尾。 */
internal fun rarityOrder(rarity: String): Int =
    listOf("UR+", "UR", "SSR", "SR", "R", "N").indexOf(rarity.uppercase()).let { if (it < 0) 99 else it }

/** 分组小标题：与「我的」页面的分组标题同一层级观感。 */
@Composable
internal fun GachaSectionTitle(text: String, trailing: String = "") {
    Row(
        Modifier.fillMaxWidth().padding(start = 4.dp, top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f),
        )
        if (trailing.isNotBlank()) {
            Text(trailing, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** 页面头部统计卡：图标容器 + 主文本 + 次文本，替代原来的裸文本。 */
@Composable
internal fun GachaStatCard(
    icon: ImageVector,
    primary: String,
    secondary: String = "",
    trailing: (@Composable () -> Unit)? = null,
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(42.dp).clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, null, Modifier.size(21.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
            }
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Text(primary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                if (secondary.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        secondary,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            trailing?.invoke()
        }
    }
}

/** 稀有度色条卡片：左侧 4dp 竖条标出稀有度，内容由调用方填充。 */
@Composable
internal fun RarityCard(
    rarity: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val accent = rarityAccent(rarity)
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(Modifier.height(IntrinsicSize.Min)) {
            Box(Modifier.width(4.dp).fillMaxHeight().background(accent))
            Column(
                Modifier.weight(1f).padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                content = content,
            )
        }
    }
}

/** 稀有度标签：描边小胶囊，池概率行与分组头共用。 */
@Composable
internal fun RarityPill(rarity: String) {
    val accent = rarityAccent(rarity)
    Surface(shape = RoundedCornerShape(6.dp), color = accent.copy(alpha = 0.14f)) {
        Text(
            rarity,
            Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = accent,
            maxLines = 1,
        )
    }
}

/** 计数小胶囊：持有数量、剩余数量等次要数字。 */
@Composable
internal fun GachaCountPill(text: String) {
    Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.surfaceContainerHighest) {
        Text(
            text,
            Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

/** 已装备标记：主色描边胶囊 + 勾选图标，替代原来的纯文本。 */
@Composable
internal fun EquippedPill() {
    Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)) {
        Row(
            Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Icon(Icons.Filled.CheckCircle, null, Modifier.size(12.dp), tint = MaterialTheme.colorScheme.primary)
            Text(
                "已装备",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
            )
        }
    }
}

/** 卡片内的空态/提示：不占满全屏，可放进滚动内容中间。 */
@Composable
internal fun GachaEmptyCard(text: String) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            Modifier.padding(vertical = 28.dp, horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                Modifier.size(48.dp).clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Inbox, null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** 未登录引导：与「我的」页面的未登录卡同一套结构。 */
@Composable
internal fun GachaLoginPrompt(onLogin: () -> Unit) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                Modifier.size(58.dp).clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.AutoMirrored.Filled.Login, null, Modifier.size(26.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Text("登录后可进入称号中心", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick = onLogin, shape = RoundedCornerShape(50)) { Text("去登录") }
        }
    }
}

/** 收藏行内的单个称号：左侧稀有度色条 + 徽章 + 数量/装备状态 + 源站操作按钮。 */
@Composable
private fun GachaTitleRow(
    item: GachaTitleItem,
    enabled: Boolean,
    onAction: (GachaAction) -> Unit,
    onGift: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 5.dp)
            .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 色条与文字块等高，稀有度靠色条表达，不再额外占一行标签。
        Box(
            Modifier.width(3.dp).fillMaxHeight()
                .clip(RoundedCornerShape(50)).background(rarityAccent(item.badge.rarity))
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TitleBadgeView(item.badge, modifier = Modifier.weight(1f, fill = false), small = true)
                if (item.count > 1) GachaCountPill("×${item.count}")
            }
            if (item.equipped) Text(
                "已装备",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
            item.actions.forEach { action ->
                TextButton(
                    enabled = enabled && action.enabled,
                    onClick = { onAction(action) },
                    shape = RoundedCornerShape(50),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.heightIn(min = 34.dp),
                ) { Text(action.label, style = MaterialTheme.typography.labelMedium) }
            }
            TextButton(
                enabled = enabled && item.count > 0,
                onClick = onGift,
                shape = RoundedCornerShape(50),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                modifier = Modifier.heightIn(min = 34.dp),
            ) { Text("赠送", style = MaterialTheme.typography.labelMedium) }
        }
    }
}

/** 称号池概率行：稀有度标签 + 数量 + 概率，概率右对齐便于纵向比较。 */
@Composable
internal fun GachaPoolRowView(row: GachaPoolRow) {
    Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
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
            Modifier.size(36.dp).clip(RoundedCornerShape(12.dp))
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
        shape = RoundedCornerShape(20.dp),
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun GachaNews(news: List<String>) {
    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            news.chunked((news.size + 2) / 3).forEachIndexed { row, values ->
                Text(
                    values.joinToString("     ✦     "),
                    Modifier.fillMaxWidth().basicMarquee(
                        iterations = Int.MAX_VALUE, initialDelayMillis = row * 600, velocity = (26 + row * 8).dp,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
    }
}

/** 全部称号：按稀有度分组，组头显示稀有度标签、种类数与展开状态。 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun GachaTitleGroups(titles: List<TitleBadge>) {
    var expanded by remember { mutableStateOf(setOf("SSR")) }
    titles.distinct().groupBy { it.rarity.uppercase() }.toSortedMap(compareBy { rarityOrder(it) }).forEach { (rarity, badges) ->
        val open = rarity in expanded
        Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
            Column(Modifier.fillMaxWidth()) {
                Row(
                    Modifier.fillMaxWidth()
                        .clickable { expanded = if (open) expanded - rarity else expanded + rarity }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    RarityPill(rarity)
                    Text(
                        "${badges.size} 种",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(
                        if (open) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        if (open) "收起" else "展开",
                        Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (open) {
                    FlowRow(
                        Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, bottom = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        badges.forEach { TitleBadgeView(it) }
                    }
                }
            }
        }
    }
}
