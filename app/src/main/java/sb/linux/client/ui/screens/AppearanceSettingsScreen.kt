package sb.linux.client.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewQuilt
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import sb.linux.client.ui.session.Session
import sb.linux.client.ui.patterns.settings.settingsItemShape
import sb.linux.client.ui.patterns.settings.SettingsChoiceRow
import sb.linux.client.ui.theme.LocalAppSpacing

/** 外观入口页保留短列表；具体配置由现有主题控件拥有。 */
@Composable
fun ThemeSettingsScreen(session: Session, nav: NavHostController) {
    val spacing = LocalAppSpacing.current
    SettingsPage("外观", nav) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.inline)) {
            val entries = listOf(
                Triple("主题设置", "自定义主题配色与显示模式", "themeColors"),
                Triple("壁纸", "为应用设置背景图片", "themeBackground"),
                Triple("字体", "自定义字体、字号与粗细", "themeFonts"),
            )
            entries.forEachIndexed { index, (title, summary, route) ->
                SettingMenuRow(title, summary, shape = settingsItemShape(index, entries.size)) { nav.navigate(route) }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(spacing.inline)) {
            GroupLabel("布局")
            GroupCard {
                SettingsChoiceRow(
                    title = "宽屏列表与详情", subtitle = "宽屏时可同时查看列表与详情",
                    icon = Icons.AutoMirrored.Filled.ViewQuilt,
                    options = listOf(false to "自动并排", true to "单页显示"),
                    selectedValue = session.singleContentColumn,
                    onSelect = { session.preferences.saveTabletMode(!it) },
                )
            }
            SettingMenuRow("导航设置", "自定义导航入口与顺序", Icons.AutoMirrored.Filled.ViewQuilt) { nav.navigate("themeLayout") }
        }
        SettingsResetRow("重置外观设置") { session.preferences.resetThemeSettings() }
    }
}
