package sb.linux.client.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import sb.linux.client.ui.patterns.settings.SettingsListRow
import sb.linux.client.ui.session.Session
import sb.linux.client.ui.theme.ThemeColorPresets
import sb.linux.client.ui.theme.colorSeed

@Composable
internal fun ThemeColorSelection(session: Session, onCustom: () -> Unit) {
    var open by remember { mutableStateOf(false) }
    val key = session.themeColorKey
    val label = ThemeColorPresets.firstOrNull { it.key == key }?.label ?: "#${key.removePrefix("custom#")}"
    SettingsListRow("主题色", "由起点色生成配色；支持保存自定义预选", onClick = { open = true }, trailing = {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ColorDot(colorSeed(key))
            Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        }
    })
    if (open) AlertDialog(onDismissRequest = { open = false }, title = { Text("主题色") },
        text = {
            GroupCard(modifier = Modifier.verticalScroll(rememberScrollState())) {
                ThemeColorPresets.forEach { preset ->
                    SettingsListRow(preset.label, if (key == preset.key) "当前选择" else "", trailing = { ColorDot(preset.lightPrimary) }, onClick = {
                        session.preferences.saveTheme(session.themeMode, preset.key == "default" && session.dynamicColor, preset.key)
                        open = false
                    })
                }
                session.themeCustomColors.forEach { custom ->
                    SettingsListRow("#${custom.removePrefix("custom#")}", if (key == custom) "当前选择" else "自定义预选", onClick = {
                        session.preferences.saveTheme(session.themeMode, false, custom); open = false
                    }, trailing = {
                        Row {
                            ColorDot(colorSeed(custom))
                            IconButton(onClick = {
                                session.preferences.removeThemeCustomColor(custom)
                                session.showToast("已移除该自定义主题色")
                            }) { Icon(Icons.Filled.Delete, "移除预选") }
                        }
                    })
                }
                SettingsListRow("自定义主题色", "选择颜色并存为预选", onClick = { open = false; onCustom() })
            }
        }, confirmButton = { TextButton(onClick = { open = false }) { Text("关闭") } })
}

@Composable
private fun ColorDot(color: Color) {
    Box(Modifier.size(28.dp).clip(CircleShape).background(color).border(1.dp, MaterialTheme.colorScheme.outline, CircleShape))
}
