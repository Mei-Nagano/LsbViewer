package sb.linux.client.ui.screens

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.text.font.FontFamily
import sb.linux.client.ui.session.Session
import sb.linux.client.ui.patterns.settings.SettingsActionRow
import sb.linux.client.ui.patterns.settings.SettingsChoiceRow
import sb.linux.client.ui.patterns.settings.SettingsSliderRow
import sb.linux.client.ui.patterns.settings.searchingFor

internal fun LazyListScope.themeFontSettings(
    session: Session,
    customFontFamily: FontFamily?,
    onChooseCustomFont: () -> Unit,
) {
    item { GroupLabel("字体", searchable = false) }
    item {
        GroupCard {
            val customLabel = session.customFontName.removeSuffix(".ttf").removeSuffix(".otf").ifBlank { "自定义" }
            SettingsChoiceRow("字体", listOf("default" to "默认", "custom" to customLabel), session.fontKey,
                onSelect = { key ->
                    if (key == "custom" && customFontFamily == null) onChooseCustomFont()
                    else session.preferences.saveFont(key)
                }, subtitle = "全局字体；支持导入 TTF、OTF 文件")
            SettingsActionRow("导入自定义字体", onChooseCustomFont)
            if (customFontFamily != null || searchingFor("移除自定义字体")) SettingsActionRow("移除自定义字体", {
                session.preferences.clearCustomFont()
                session.showToast("已移除自定义字体")
            }, enabled = customFontFamily != null, subtitle = customLabel)
            SettingsChoiceRow("字体粗细", listOf(-3 to "极细", -2 to "细体", -1 to "较细", 0 to "标准", 1 to "较粗", 2 to "粗体"),
                session.fontWeightLevel, session.preferences::saveFontWeightLevel,
                subtitle = "当前正文粗细 ${400 + session.fontWeightLevel * 100}")
        }
    }
    item {
        SettingsSliderRow("界面缩放", session.fontScale, session.preferences::saveFontScale,
            "${(session.fontScale * 100).toInt()}%", subtitle = "调整全局显示比例", valueRange = 0.85f..1.30f, steps = 8)
    }
}
