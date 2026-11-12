package sb.linux.client.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import sb.linux.client.ui.components.layout.AppGroupLabel
import sb.linux.client.ui.theme.LocalAppSpacing

/**
 * 「我的」页分组与设置页保持一致：
 * 分组标题 + 无卡片、无分隔线的扁平条目，条目之间只留白。
 */
@Composable
internal fun MenuGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    val spacing = LocalAppSpacing.current
    Column(
        Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(spacing.item),
    ) {
        AppGroupLabel(title)
        content()
    }
}

/** 单个入口：复用设置页条目样式（左侧圆形图标 + 主标题，无右侧箭头）。 */
@Composable
internal fun MenuRow(icon: ImageVector, label: String, onClick: () -> Unit) {
    SettingMenuRow(title = label, subtitle = "", icon = icon, onClick = onClick)
}
