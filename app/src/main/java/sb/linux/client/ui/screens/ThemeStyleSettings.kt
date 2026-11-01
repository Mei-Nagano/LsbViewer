package sb.linux.client.ui.screens

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import sb.linux.client.ui.session.Session
import sb.linux.client.ui.theme.PaletteStyles
import sb.linux.client.ui.theme.colorSeed
import sb.linux.client.ui.theme.paletteStyleLabel
import sb.linux.client.ui.theme.AppMenuDefaults

internal fun LazyListScope.themeStyleSettings(session: Session, dark: Boolean) {
// ---------- 调色风格（单行横向滑动） ----------
item { GroupLabel("调色风格") }
item {
    GroupCard {
        Text(
            "同一主题色在不同调色风格下的观感差异，点按切换",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = AppMenuDefaults.contentHorizontal, vertical = 8.dp)
        )
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = AppMenuDefaults.contentHorizontal, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(PaletteStyles) { style ->
                PaletteStyleSwatch(
                    seed = colorSeed(session.themeColorKey),
                    style = style,
                    label = paletteStyleLabel(style),
                    contrast = session.themeContrast.toDouble(),
                    dark = dark,
                    selected = session.themeStyle == style.ordinal,
                    onClick = { session.preferences.saveThemeStyle(style.ordinal) },
                    modifier = Modifier.width(84.dp)
                )
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

// ---------- 取色（3.15：改为与其他设置一致的开关行样式） ----------
item { GroupLabel("取色") }
item {
    GroupCard {
        SwitchRow(
            "Material 动态取色",
            "跟随 Android 12+ 系统壁纸配色",
            checked = session.dynamicColor,
            onCheckedChange = { on ->
                // 开启动态取色 → 重置为默认主题色；关闭则保持当前色
                session.preferences.saveTheme(session.themeMode, on, if (on) "default" else session.themeColorKey)
            }
        )
        HorizontalDivider(Modifier.padding(horizontal = AppMenuDefaults.contentHorizontal), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        SwitchRow(
            "OLED 纯黑",
            "深色模式下使用纯黑背景",
            checked = session.oledDark,
            onCheckedChange = { session.preferences.saveOledDark(it) }
        )
    }
}

// ---------- 对比度（滑条 + 三档标签，两端与中间可点选，靠近档位时轻吸附） ----------
item { GroupLabel("对比度") }
item {
    GroupCard {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = AppMenuDefaults.contentHorizontal, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "对比度等级",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.weight(1f))
            Text(
                "拖动滑条或点选档位",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        val snapPoints = listOf(-1f, 0f, 1f)
        var dragging by remember { mutableStateOf(false) }
        var dragValue by remember { mutableStateOf(session.themeContrast) }
        Slider(
            value = if (dragging) dragValue else session.themeContrast,
            onValueChange = { dragging = true; dragValue = it },
            onValueChangeFinished = {
                dragging = false
                // 轻吸附：距离档位 0.25 以内时吸附到档位
                val snapped = snapPoints
                    .filter { kotlin.math.abs(it - dragValue) <= 0.25f }
                    .minByOrNull { kotlin.math.abs(it - dragValue) }
                    ?: dragValue
                session.preferences.saveThemeContrast(snapped)
            },
            valueRange = -1f..1f,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppMenuDefaults.contentHorizontal)
        )
        // 三档标签：点击直达（左中右三个特殊值的表达），当前档位高亮
        val current = if (dragging) dragValue else session.themeContrast
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = AppMenuDefaults.contentHorizontal, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            listOf("低" to -1f, "标准" to 0f, "高" to 1f).forEach { (label, value) ->
                val active = kotlin.math.abs(current - value) < 0.01f
                Text(
                    label,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (active) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.small)
                        .clickable { dragging = false; session.preferences.saveThemeContrast(value) }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
        Text(
            "仅对「主题色」派生方案生效，动态取色（跟随系统壁纸）不受此影响",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = AppMenuDefaults.contentHorizontal, vertical = 8.dp)
        )
    }
}

}
