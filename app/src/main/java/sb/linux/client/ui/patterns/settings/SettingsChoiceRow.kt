package sb.linux.client.ui.patterns.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import sb.linux.client.ui.components.layout.AppMenuRow
import sb.linux.client.ui.theme.LocalAppSizes
import sb.linux.client.ui.theme.LocalAppSpacing

/** 行与当前值都可打开菜单，浮层位置跟随右侧当前值。选值策略仍由调用方决定。 */
@Composable
internal fun <T> SettingsChoiceRow(
    title: String,
    options: List<Pair<T, String>>,
    selectedValue: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String = "",
    icon: ImageVector? = settingsRowIcon(title),
    enabled: Boolean = true,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val label = options.firstOrNull { it.first == selectedValue }?.second ?: "未选择"
    SettingsListRow(
        title = title,
        subtitle = subtitle,
        icon = icon,
        modifier = modifier,
        enabled = enabled,
        onClick = { expanded = true },
        trailing = {
            Box {
                Text(label, Modifier.widthIn(max = 128.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
                SettingsChoiceMenu(
                    expanded = expanded,
                    onDismiss = { expanded = false },
                    options = options,
                    selectedValue = selectedValue,
                    onSelect = { value -> expanded = false; onSelect(value) },
                )
            }
        },
    )
}

/** 可供带自由输入的模型或方案选择共用；选中状态由颜色、勾号和语义同时表达。 */
@Composable
internal fun <T> SettingsChoiceMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    options: List<Pair<T, String>>,
    selectedValue: T,
    onSelect: (T) -> Unit,
    trailingContent: (@Composable (T) -> Unit)? = null,
) {
    val spacing = LocalAppSpacing.current
    val sizes = LocalAppSizes.current
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        modifier = Modifier.widthIn(min = 168.dp, max = 280.dp).heightIn(max = 360.dp),
        shape = MaterialTheme.shapes.extraLarge,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        shadowElevation = spacing.item,
    ) {
        if (options.isEmpty()) DropdownMenuItem(text = { Text("暂无可选项") }, enabled = false, onClick = {})
        options.forEach { (value, label) ->
            val active = value == selectedValue
            val foreground = if (active) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface
            DropdownMenuItem(
                text = { Text(label, style = MaterialTheme.typography.bodyLarge, color = foreground) },
                onClick = { onSelect(value) },
                leadingIcon = {
                    if (active) Icon(Icons.Filled.Check, null, Modifier.size(sizes.icon), tint = foreground)
                    else Spacer(Modifier.size(sizes.icon))
                },
                trailingIcon = trailingContent?.let { { it(value) } },
                contentPadding = PaddingValues(horizontal = spacing.pageHorizontal, vertical = spacing.contentGap),
                modifier = Modifier.padding(horizontal = spacing.inline)
                    .clip(MaterialTheme.shapes.large)
                    .background(if (active) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent)
                    .semantics { selected = active },
            )
        }
    }
}
