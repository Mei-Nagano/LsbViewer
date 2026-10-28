package sb.linux.client.ui.components.input

import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import sb.linux.client.ui.components.layout.AppMenuRow

/**
 * 设置开关行的唯一入口：标题、说明、右置开关，整行可点。
 *
 * 可访问语义（UI_DEVELOPMENT_GUIDE 5.2 第 7 条）：
 * - 可操作节点只有一个：整行通过 `toggleable` 提供 `Role.Switch`、name（标题与说明）、
 *   checked、enabled 与切换动作，辅助技术能在一个节点里同时读到名称与状态；
 * - 内部 `Switch` 只负责视觉：`onCheckedChange = null` 且清空自身语义，
 *   避免再产生一个「无名可勾选节点」；
 * - 整行与开关区域都只会切换一次，禁用时整行不可操作。
 */
@Composable
fun AppSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String = "",
    enabled: Boolean = true,
    compact: Boolean = false,
) {
    AppMenuRow(
        title = title,
        subtitle = subtitle,
        modifier = modifier.toggleable(
            value = checked,
            enabled = enabled,
            role = Role.Switch,
            onValueChange = onCheckedChange,
        ),
        enabled = enabled,
        compact = compact,
        trailing = {
            Switch(
                checked = checked,
                onCheckedChange = null,
                enabled = enabled,
                modifier = Modifier.clearAndSetSemantics { },
            )
        },
    )
}
