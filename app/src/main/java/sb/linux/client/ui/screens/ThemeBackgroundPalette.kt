package sb.linux.client.ui.screens

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import sb.linux.client.ui.session.Session
import sb.linux.client.ui.patterns.settings.SettingsListRow
import sb.linux.client.ui.patterns.settings.SettingsSliderRow
import sb.linux.client.ui.patterns.settings.searchingFor

internal fun LazyListScope.themeBackgroundPalette(
    section: String, session: Session, currentScheme: ColorScheme,
    backgroundImportBusy: Boolean, onOpenBackgroundPicker: () -> Unit,
    onSelectBackgroundImage: () -> Unit, onPickRole: (PreviewRole) -> Unit,
    onPickElement: (String) -> Unit, onOpenThemeColorPicker: () -> Unit,
) {
    if (section == "background") {
        item { GroupLabel("页面背景") }
        item {
            GroupCard {
                SettingsListRow("自定义背景色", if (session.themeBackground == null) "当前跟随主题" else "已应用到所有页面底色",
                    onClick = onOpenBackgroundPicker, trailing = {
                        Box(Modifier.size(32.dp).clip(CircleShape).background(session.themeBackground ?: currentScheme.background)
                            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape))
                    })
                if (session.themeBackground != null || searchingFor("恢复背景色")) {
                    SettingsListRow("恢复背景色", "重新跟随主题底色", enabled = session.themeBackground != null,
                        onClick = { session.preferences.saveThemeBackground(null) })
                }
                SettingsListRow("选择背景图片", if (backgroundImportBusy) "正在处理图片…" else "从设备选择图片并保存到应用内",
                    enabled = !backgroundImportBusy, onClick = onSelectBackgroundImage)
                if (session.backgroundImage.isNotEmpty() || searchingFor("移除图片")) {
                    SettingsListRow("移除图片", "移除已保存的背景图片", enabled = session.backgroundImage.isNotEmpty() && !backgroundImportBusy,
                        onClick = session.preferences::removeBackgroundImage)
                }
                if (session.backgroundImage.isNotEmpty() || searchingFor("启用背景图片", "背景图片不透明度", "卡片、顶栏表面不透明度")) {
                    SwitchRow("启用背景图片", "使用本地保存的图片，图片可随设置备份", session.backgroundImageEnabled,
                        onCheckedChange = session.preferences::saveBackgroundImageEnabled)
                    SettingsSliderRow("背景图片不透明度", session.backgroundImageOpacity, session.preferences::saveBackgroundImageOpacity,
                        "${(session.backgroundImageOpacity * 100).toInt()}%", valueRange = 0.05f..0.65f)
                    SettingsSliderRow("卡片、顶栏表面不透明度", session.surfaceOpacity, session.preferences::saveSurfaceOpacity,
                        "${(session.surfaceOpacity * 100).toInt()}%", valueRange = 0.2f..1f)
                }
                SwitchRow("禁止页面染色", "页面、卡片和弹窗保持中性色，按钮与高亮保留强调色；自定义背景色不受影响",
                    session.keepBackgroundColor, onCheckedChange = session.preferences::saveKeepBackgroundColor)
            }
        }
    }
    if (section == "colors") {
        item { PostCardPreview(currentScheme, onPickElement) }
        item { ElementSelectorList(session, currentScheme, onPickRole, onPickElement) }
        item { GroupLabel("主题色", searchable = false) }
        item { ThemeColorSelection(session, onOpenThemeColorPicker) }
    }
}
