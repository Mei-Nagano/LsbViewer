package sb.linux.client.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import sb.linux.client.ui.theme.LocalAppSpacing
import sb.linux.client.domain.gacha.GachaAction
import sb.linux.client.domain.gacha.GachaPoolRow
import sb.linux.client.domain.gacha.GachaTitleItem
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
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(LocalAppSpacing.current.pageHorizontal),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(42.dp).clip(MaterialTheme.shapes.large)
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
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(Modifier.height(IntrinsicSize.Min)) {
            Box(Modifier.width(4.dp).fillMaxHeight().background(accent))
            Column(
                Modifier.weight(1f).padding(horizontal = LocalAppSpacing.current.pageHorizontal, vertical = 12.dp),
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
    Surface(shape = MaterialTheme.shapes.small, color = accent.copy(alpha = 0.14f)) {
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
    Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
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
    Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)) {
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

/** 收藏行内的单个称号：左侧稀有度色条 + 徽章 + 数量/装备状态 + 源站操作按钮。 */
@Composable
internal fun GachaTitleRow(
    item: GachaTitleItem,
    enabled: Boolean,
    onAction: (GachaAction) -> Unit,
    onGift: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = LocalAppSpacing.current.pageHorizontal, vertical = 5.dp)
            .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 色条与文字块等高，稀有度靠色条表达，不再额外占一行标签。
        Box(
            Modifier.width(3.dp).fillMaxHeight()
                .clip(CircleShape).background(rarityAccent(item.badge.rarity))
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
                    shape = CircleShape,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.heightIn(min = 34.dp),
                ) { Text(action.label, style = MaterialTheme.typography.labelMedium) }
            }
            TextButton(
                enabled = enabled && item.count > 0,
                onClick = onGift,
                shape = CircleShape,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                modifier = Modifier.heightIn(min = 34.dp),
            ) { Text("赠送", style = MaterialTheme.typography.labelMedium) }
        }
    }
}

/** 称号池概率行：稀有度标签 + 数量 + 概率，概率右对齐便于纵向比较。 */
