package sb.linux.client.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import sb.linux.client.ui.patterns.settings.SettingsListRow
import sb.linux.client.ui.patterns.settings.SettingsSliderRow
import sb.linux.client.ui.session.Session
import sb.linux.client.ui.theme.PaletteStyles
import sb.linux.client.ui.theme.colorSeed
import sb.linux.client.ui.theme.paletteStyleLabel

internal fun LazyListScope.themeStyleSettings(session: Session, dark: Boolean) {
    item { GroupLabel("调色风格", searchable = false) }
    item {
        var selecting by remember { mutableStateOf(false) }
        SettingsListRow("调色风格", "预览同一主题色的不同配色方案", onClick = { selecting = true }, trailing = {
            Text(paletteStyleLabel(PaletteStyles.first { it.ordinal == session.themeStyle }),
                color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
        })
        if (selecting) AlertDialog(onDismissRequest = { selecting = false }, title = { Text("调色风格") },
            text = { GroupCard(modifier = Modifier.verticalScroll(rememberScrollState())) {
                PaletteStyles.forEach { style ->
                    SettingsListRow(paletteStyleLabel(style), onClick = {
                        session.preferences.saveThemeStyle(style.ordinal); selecting = false
                    }, trailing = {
                        PaletteStyleSwatch(seed = colorSeed(session.themeColorKey), style = style, label = paletteStyleLabel(style),
                            contrast = session.themeContrast.toDouble(), dark = dark, selected = session.themeStyle == style.ordinal,
                            onClick = { session.preferences.saveThemeStyle(style.ordinal); selecting = false },
                            modifier = Modifier.width(72.dp))
                    })
                }
            } }, confirmButton = { TextButton(onClick = { selecting = false }) { Text("关闭") } })
    }
    item { GroupLabel("取色") }
    item {
        GroupCard {
            SwitchRow("Material 动态取色", "跟随 Android 12+ 系统壁纸配色", session.dynamicColor,
                onCheckedChange = { on -> session.preferences.saveTheme(session.themeMode, on, if (on) "default" else session.themeColorKey) })
            SwitchRow("OLED 纯黑", "深色模式下使用纯黑背景", session.oledDark,
                onCheckedChange = session.preferences::saveOledDark)
        }
    }
    item { GroupLabel("对比度") }
    item {
        var dragging by remember { mutableStateOf(false) }
        var dragValue by remember { mutableStateOf(session.themeContrast) }
        val current = if (dragging) dragValue else session.themeContrast
        SettingsSliderRow("对比度等级", current, { dragging = true; dragValue = it },
            valueText = when (current) { -1f -> "低"; 0f -> "标准"; 1f -> "高"; else -> "%.2f".format(current) },
            subtitle = "仅影响主题色派生配色；靠近低、标准、高档位时自动吸附", valueRange = -1f..1f,
            onValueChangeFinished = {
                val snapped = listOf(-1f, 0f, 1f).filter { kotlin.math.abs(it - dragValue) <= 0.25f }
                    .minByOrNull { kotlin.math.abs(it - dragValue) } ?: dragValue
                dragging = false
                session.preferences.saveThemeContrast(snapped)
            })
    }
}
