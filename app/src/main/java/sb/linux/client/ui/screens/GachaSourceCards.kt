package sb.linux.client.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import sb.linux.client.domain.account.AccountNode
import sb.linux.client.domain.topic.TitleBadge
import sb.linux.client.ui.theme.LocalAppSpacing
import sb.linux.client.ui.theme.LocalDarkTheme
import sb.linux.client.ui.theme.sourceTitleColor
import sb.linux.client.ui.theme.sourceTitleSurface
import sb.linux.client.ui.titleRarityColor

/** 主信息为称号与操作；描述、数量、日期次之，颜色读取源站日夜变量，不跟随应用动态主题色。 */
@Composable
internal fun GachaSourceBadge(title: TitleBadge) {
    val colors = gachaBadgeColors(title)
    val dark = LocalDarkTheme.current
    val border = sourceTitleColor(title, if (dark) "dark-border" else "border", colors.first, dark)
    val serialBg = (if (title.rarity.startsWith("UR")) sourceTitleColor(title,
        if (dark) "dark-serial-bg" else "serial-bg", colors.first, dark) else colors.first)
        .compositeOver(colors.second)
    Surface(shape = RoundedCornerShape(6.dp), color = colors.second, border = BorderStroke(1.dp, border)) {
        Row(Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            if (title.icon.isNotBlank()) Text(title.icon, style = MaterialTheme.typography.labelLarge)
            Text(title.name, color = colors.first, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f, false))
            val serial = title.serial.ifBlank { title.rarity }
            Surface(color = serialBg, shape = RoundedCornerShape(4.dp)) {
                Text(serial, Modifier.padding(horizontal = 4.dp), color = if (title.rarity.startsWith("UR") && !dark) colors.first else Color.White,
                    style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun gachaBadgeColors(title: TitleBadge): Pair<Color, Color> {
    val dark = LocalDarkTheme.current
    val fallback = titleRarityColor(title.rarity, dark).first
    val fg = sourceTitleColor(title, if (dark) "dark-color" else "color", fallback, dark)
    val bg = sourceTitleColor(title, if (dark) "dark-bg" else "bg", fg.copy(alpha = 0.12f), dark)
        .compositeOver(sourceTitleSurface(dark))
    return fg to bg
}

@Composable
internal fun GachaSourceRarityStat(stat: AccountNode.RarityStat) {
    val colors = gachaBadgeColors(TitleBadge(name = stat.rarity, rarity = stat.rarity, sourceStyle = stat.style))
    val dark = LocalDarkTheme.current
    val border = sourceTitleColor(TitleBadge(name = stat.rarity, sourceStyle = stat.style), if (dark) "dark-border" else "border", colors.first, dark)
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = colors.second,
        border = BorderStroke(1.dp, border)) {
        Column(Modifier.padding(LocalAppSpacing.current.contentGap), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stat.rarity, color = colors.first, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(stat.count, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(stat.rate, color = colors.first, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
internal fun GachaAdaptiveGrid(count: Int, maxColumns: Int = 3, minimumWidth: androidx.compose.ui.unit.Dp = 210.dp, item: @Composable (Int) -> Unit) {
    val spacing = LocalAppSpacing.current
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val columns = ((maxWidth + spacing.item) / (minimumWidth + spacing.item)).toInt().coerceIn(1, maxColumns)
        val width = (maxWidth - spacing.item * (columns - 1)) / columns
        FlowRow(maxItemsInEachRow = columns, horizontalArrangement = Arrangement.spacedBy(spacing.item),
            verticalArrangement = Arrangement.spacedBy(spacing.item)) {
            repeat(count) { index -> Box(Modifier.width(width)) { item(index) } }
        }
    }
}

@Composable
internal fun GachaSourceCard(equipped: Boolean, content: @Composable () -> Unit) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
        color = if (equipped) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, if (equipped) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)) {
        Column(Modifier.padding(LocalAppSpacing.current.contentGap), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (equipped) Text("已佩戴", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            content()
        }
    }
}

@Composable
internal fun GachaSourcePills(values: List<Pair<String, Boolean>>) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        values.forEach { (label, available) ->
            Surface(shape = RoundedCornerShape(6.dp), color = if (available) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.errorContainer) {
                Text(label, Modifier.padding(horizontal = 6.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall,
                    color = if (available) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onErrorContainer)
            }
        }
    }
}

@Composable
internal fun GachaSourceNews(values: List<String>) {
    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("喜报", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelLarge)
            Text(values.joinToString("     "), Modifier.weight(1f).basicMarquee(iterations = Int.MAX_VALUE), style = MaterialTheme.typography.bodySmall, maxLines = 1)
        }
    }
}

@Composable
internal fun GachaSourceNotice(text: String) {
    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
        Text(text, Modifier.padding(LocalAppSpacing.current.contentGap), style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer)
    }
}
