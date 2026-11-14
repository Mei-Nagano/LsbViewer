package sb.linux.client.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import sb.linux.client.ui.components.layout.AppMenuRow
import sb.linux.client.ui.patterns.settings.SettingsCard
import sb.linux.client.ui.patterns.settings.settingsRowIcon
import sb.linux.client.ui.patterns.settings.SettingsGroup
import sb.linux.client.ui.patterns.settings.settingsCardShape

@Composable
internal fun GroupLabel(title: String, searchable: Boolean = true) {
    sb.linux.client.ui.components.layout.AppGroupLabel(title, searchable = searchable)
}

/** 同组列表项连接，分组标题和说明不参与卡片圆角。 */
@Composable
internal fun GroupCard(
    title: String? = null,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    sb.linux.client.ui.components.layout.AppSection(title = title) {
        SettingsGroup(modifier, content)
    }
}

/** 带开关的设置行：保留旧入口名，行为统一由 `AppSwitchRow` 拥有。 */
@Composable
internal fun SwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    onCheckedChange: (Boolean) -> Unit,
) {
    SettingsCard {
    sb.linux.client.ui.components.input.AppSwitchRow(
        title = title, checked = checked, onCheckedChange = onCheckedChange,
        subtitle = subtitle, enabled = enabled,
        prominent = true,
        icon = icon ?: settingsRowIcon(title),
    )
    }
}

/** 顶栏重置图标：点击后走统一确认弹窗，确认才交给调用方执行。 */
@Composable
internal fun ResetAction(title: String, onConfirm: () -> Unit) {
    var show by remember { mutableStateOf(false) }
    IconButton(onClick = { show = true }) {
        Icon(settingsRowIcon("重置"), "重置")
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
 * 设置条目：直接图标、全局正文的标题字重、辅助说明与右箭头，整行在圆角卡片内。
 * 普通菜单行和开关仍由公共组件负责操作与无障碍语义。
 */
@Composable
internal fun SettingMenuRow(
    title: String,
    subtitle: String,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier,
    value: String? = null,
    showChevron: Boolean = true,
    shape: Shape = settingsCardShape(),
    enabled: Boolean = true,
    titleWeight: FontWeight? = null,
    onClick: () -> Unit,
) {
    SettingsCard(modifier, shape) {
        AppMenuRow(
            title = title,
            subtitle = subtitle,
            icon = icon ?: settingsRowIcon(title),
            prominent = true,
            titleWeight = titleWeight,
            enabled = enabled,
            trailing = if (value != null || !showChevron) {
                {
                    value?.let {
                        Text(
                            it,
                            modifier = Modifier.widthIn(max = 112.dp),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            } else null,
            onClick = onClick,
        )
    }
}
