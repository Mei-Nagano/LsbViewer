package sb.linux.client.ui.patterns.settings

import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import sb.linux.client.ui.components.layout.AppMenuRow

/** 多选设置与开关使用同一文字层级，操作语义只由整行拥有。 */
@Composable
internal fun SettingsCheckRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    subtitle: String = "",
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    AppMenuRow(
        title = title, subtitle = subtitle, icon = icon, prominent = true, enabled = enabled,
        modifier = Modifier.toggleable(value = checked, enabled = enabled, role = Role.Checkbox, onValueChange = onCheckedChange),
        trailing = {
            Checkbox(checked = checked, onCheckedChange = null, enabled = enabled,
                modifier = Modifier.clearAndSetSemantics { })
        },
    )
}
