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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import sb.linux.client.domain.topic.TitleBadge
import sb.linux.client.ui.theme.sourceTitleColor



@Composable
fun Badge(text: String, bg: Color, fg: Color, small: Boolean = false, modifier: Modifier = Modifier) {
    Surface(shape = RoundedCornerShape(50), color = bg, modifier = modifier) {
        Text(
            text,
            style = if (small) MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, lineHeight = 12.sp)
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
fun titleRarityColor(rarity: String): Pair<Color, Color> {
    val r = rarity.uppercase().trim()
    return when {
        r.contains("UR") -> Color(0xFFA855F7) to Color(0xFFEFE3FF)   // 源站 UR 紫
        r.contains("SSR") -> Color(0xFFFF8F00) to Color(0xFFFFF0D6)  // 橙金
        r.contains("SR") -> Color(0xFF9C27B0) to Color(0xFFF3E5F5)   // 紫
        r.contains("R") -> Color(0xFF1E88E5) to Color(0xFFE3F2FD)    // 蓝
        else -> Color(0xFF6B7280) to Color(0xFFE9ECEF)               // 灰（N/默认）
    }
}

/** 按原站帖子称号徽章：浅底、无外边框、实心稀有度标签，隐藏图标。 */
@Composable
fun TitleBadgeView(title: TitleBadge, modifier: Modifier = Modifier, small: Boolean = false) {
    val dark = MaterialTheme.colorScheme.background.let { it.red + it.green + it.blue < 1.5f }
    val (fallback, soft) = titleRarityColor(title.rarity)
    val fg = sourceTitleColor(title, if (dark) "dark-color" else "color", fallback, dark)
    val bg = sourceTitleColor(title, if (dark) "dark-bg" else "bg", soft, dark)
    val serialBg = sourceTitleColor(title, if (dark) "dark-serial-bg" else "color", fg, dark)
    val openLink = LocalLinkHandler.current
    Surface(shape = RoundedCornerShape(4.dp), color = bg,
        modifier = modifier.clickable(enabled = openLink != null) { openLink?.invoke("https://linux.sb/gacha") }) {
        Row(Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title.name, color = fg, fontSize = 11.sp, lineHeight = 16.5.sp,
                fontWeight = FontWeight.Normal, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val label = if (title.rarity.uppercase().startsWith("UR") && title.serial.isNotBlank())
                title.serial else title.rarity
            if (label.isNotBlank()) Surface(color = serialBg, shape = RoundedCornerShape(4.dp)) {
                Text(label, color = Color.White, fontSize = 10.sp, lineHeight = 14.sp,
                    fontWeight = FontWeight.Bold, maxLines = 1,
                    modifier = Modifier.padding(horizontal = 3.dp))
            }
        }
    }
}
