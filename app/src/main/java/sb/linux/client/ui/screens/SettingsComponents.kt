package sb.linux.client.ui.screens

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.vector.ImageVector

@Composable
internal fun GroupLabel(title: String) { sb.linux.client.ui.components.layout.AppGroupLabel(title) }

/** 设置分组：标题与内容直接展示。 */
@Composable
internal fun GroupCard(
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    sb.linux.client.ui.components.layout.AppSection(title = title, content = content)
}

/** 带开关的设置行：保留旧入口名，行为统一由 `AppSwitchRow` 拥有。 */
@Composable
internal fun SwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    sb.linux.client.ui.components.input.AppSwitchRow(
        title = title, checked = checked, onCheckedChange = onCheckedChange,
        subtitle = subtitle, enabled = enabled,
    )
}

/** 顶栏重置图标：点击后走统一确认弹窗，确认才交给调用方执行。 */
@Composable
internal fun ResetAction(title: String, onConfirm: () -> Unit) {
    var show by remember { mutableStateOf(false) }
    IconButton(onClick = { show = true }) {
        Icon(Icons.Filled.RestartAlt, "重置")
    }
    if (show) sb.linux.client.ui.components.feedback.AppConfirmDialog(
        title = title,
        message = "恢复默认设置？",
        onDismiss = { show = false },
        onConfirm = { show = false; onConfirm() },
        confirmText = "重置", dismissText = "取消",
    )
}

/**
 * 应用设置（区别于源站个人设置）：分类菜单入口，点进子页面再进行具体设置。
 */
@Composable
internal fun SettingMenuRow(title: String, subtitle: String, icon: ImageVector? = null, onClick: () -> Unit) {
    sb.linux.client.ui.components.layout.AppMenuRow(title, subtitle, icon, onClick = onClick)
}
