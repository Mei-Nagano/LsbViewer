package sb.linux.client.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import sb.linux.client.domain.topic.TitleBadge
import sb.linux.client.ui.theme.LocalDarkTheme
import sb.linux.client.ui.theme.sourceTitleColor
import sb.linux.client.ui.theme.sourceTitleSurface



@Composable
fun Badge(text: String, bg: Color, fg: Color, small: Boolean = false, modifier: Modifier = Modifier) {
    Surface(shape = CircleShape, color = bg, modifier = modifier) {
        Text(
            text,
            style = if (small) MaterialTheme.typography.labelSmall.copy(lineHeight = 12.sp)
            else MaterialTheme.typography.labelSmall.copy(lineHeight = 13.sp),
            fontWeight = FontWeight.Medium,
            color = fg,
            maxLines = 1,
            modifier = Modifier.padding(
                horizontal = if (small) 5.dp else 7.dp,
                vertical = if (small) 1.dp else 1.dp
            )
        )
    }
}

/** 称号稀有度配色（对齐源站 gacha-title-* 的深浅配色风格） */
fun titleRarityColor(rarity: String, dark: Boolean = false): Pair<Color, Color> {
    val r = rarity.uppercase().trim()
    if (dark) return when {
        r.contains("UR") -> Color(0xFFDFC6F7) to Color(0xFF2C1940)
        r.contains("SSR") -> Color(0xFFF6A04E) to Color(0xFF252019)
        r.contains("SR") -> Color(0xFF6AAEFF) to Color(0xFF19212A)
        r == "R" -> Color(0xFF9DB5CA) to Color(0xFF202B37)
        else -> Color(0xFF9399AB) to Color(0xFF202432)
    }
    return when {
        r.contains("UR") -> Color(0xFFA855F7) to Color(0xFFEFE3FF)   // 源站 UR 紫
        r.contains("SSR") -> Color(0xFFFF8F00) to Color(0xFFFFF0D6)  // 橙金
        r.contains("SR") -> Color(0xFF4A9EFF) to Color(0xFFEEF4FF)
        r == "R" -> Color(0xFF405D78) to Color(0xFFE8EFF6)
        else -> Color(0xFF6B7280) to Color(0xFFE9ECEF)               // 灰（N/默认）
    }
}

/** 按原站帖子称号徽章：浅底、无外边框、实心稀有度标签，隐藏图标。 */
@Composable
fun TitleBadgeView(title: TitleBadge, modifier: Modifier = Modifier, small: Boolean = false) {
    val dark = LocalDarkTheme.current
    val baseSurface = sourceTitleSurface(dark)
    val (fallback, soft) = titleRarityColor(title.rarity, dark)
    val fg = sourceTitleColor(title, if (dark) "dark-color" else "color", fallback, dark)
    val bg = sourceTitleColor(title, if (dark) "dark-bg" else "bg", soft, dark).compositeOver(baseSurface)
    val serialBg = sourceTitleColor(title, if (dark) "dark-serial-bg" else "color", fg, dark).compositeOver(bg)
    val openLink = LocalLinkHandler.current
    Surface(shape = MaterialTheme.shapes.extraSmall, color = bg,
        modifier = modifier.clickable(enabled = openLink != null) { openLink?.invoke(sb.linux.client.common.network.Endpoints.abs("/gacha")) }) {
        Row(Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title.name, color = fg, style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Normal, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val label = if (title.rarity.uppercase().startsWith("UR") && title.serial.isNotBlank())
                title.serial else title.rarity
            if (label.isNotBlank()) Surface(color = serialBg, shape = MaterialTheme.shapes.extraSmall) {
                Text(label, color = Color.White, style = MaterialTheme.typography.labelSmall.copy(lineHeight = 14.sp),
                    fontWeight = FontWeight.Bold, maxLines = 1,
                    modifier = Modifier.padding(horizontal = 3.dp))
            }
        }
    }
}
