package sb.linux.client.ui

import androidx.compose.foundation.ExperimentalFoundationApi
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



@Composable
fun Badge(text: String, bg: Color, fg: Color, small: Boolean = false) {
    Surface(shape = RoundedCornerShape(50), color = bg) {
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

/** 源站称号系统徽章（对齐源站 2026 新样式）：
 *  名称 = 稀有度色纯文本（无胶囊背景）+ 稀有度 = 细边框小标签；图标不再展示（源站已 display:none）。
 *  small=true 时缩小字号并收紧名称宽度，避免卡片/评论里称号过宽而换行。 */
@Composable
fun TitleBadgeView(title: TitleBadge, modifier: Modifier = Modifier, small: Boolean = false) {
    val (fg, _) = titleRarityColor(title.rarity)
    val style = if (small) MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp)
    else MaterialTheme.typography.labelSmall
    // UR 使用源站特化：紫色软背景 + 紫色描边 + 独立三位序列号。
    if (title.rarity.uppercase().contains("UR")) {
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = Color(0xFFEFE3FF).copy(alpha = 0.8f),
            modifier = modifier.border(1.dp, Color(0xFF9333EA), RoundedCornerShape(6.dp))
        ) {
            Row(
                Modifier.padding(horizontal = if (small) 5.dp else 7.dp, vertical = 1.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(title.name, style = style, fontWeight = FontWeight.SemiBold, color = Color(0xFF7E22CE), maxLines = 1)
                if (title.serial.isNotBlank()) {
                    Text(title.serial.padStart(3, '0'), style = style, fontWeight = FontWeight.Bold, color = Color(0xFF9333EA))
                }
            }
        }
        return
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(if (small) 3.dp else 4.dp),
        modifier = modifier
    ) {
        Text(
            title.name,
            style = style,
            fontWeight = FontWeight.Medium,
            color = fg,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            // 小尺寸下收紧名称宽度，过长称号截断而不再撑大/换行
            modifier = if (small) Modifier.widthIn(max = 64.dp) else Modifier
        )
        // 稀有度：细边框小标签（源站 .gacha-title-rarity：1px 边框 + 透明背景 + 稀有度色）
        if (title.rarity.isNotBlank()) {
            Text(
                title.rarity,
                style = style,
                fontWeight = FontWeight.SemiBold,
                color = fg,
                maxLines = 1,
                modifier = Modifier
                    .border(
                        width = 1.dp,
                        color = fg.copy(alpha = 0.55f),
                        shape = RoundedCornerShape(4.dp)
                    )
                    .padding(horizontal = 3.dp)
            )
        }
    }
}
