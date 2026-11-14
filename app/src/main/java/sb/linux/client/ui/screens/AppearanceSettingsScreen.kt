package sb.linux.client.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
                Triple("主题设置", "深浅模式、主题色、配色角色与对比度", "themeColors"),
                Triple("壁纸", "背景颜色、图片与透明度", "themeBackground"),
                Triple("字体", "导入字体，调整字体粗细与界面缩放", "themeFonts"),
            )
            entries.forEachIndexed { index, (title, summary, route) ->
                SettingMenuRow(title, summary, shape = settingsItemShape(index, entries.size)) { nav.navigate(route) }
            }
        }
        GroupCard("布局") {
            SwitchRow("隐藏状态栏", "隐藏系统时间和电量；窄屏保留顶部安全空间，宽屏不留白，边缘滑动可临时显示",
                checked = session.hideStatusBar, onCheckedChange = session.preferences::saveHideStatusBar)
            SettingsChoiceRow(
                title = "宽屏列表与详情", subtitle = "宽屏时自动并排显示列表与详情，或始终使用单页",
                options = listOf(false to "自动并排", true to "单页显示"),
                selectedValue = session.singleContentColumn,
                onSelect = { session.preferences.saveTabletMode(!it) },
            )
            SettingMenuRow("导航设置", "选择底部入口并调整显示顺序") { nav.navigate("themeLayout") }
        }
        SettingsResetRow("重置外观设置") { session.preferences.resetThemeSettings() }
    }
}
