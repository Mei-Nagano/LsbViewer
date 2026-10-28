package sb.linux.client.ui.components.layout

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import sb.linux.client.ui.theme.AppMenuDefaults
import sb.linux.client.ui.theme.LocalAppSizes
import sb.linux.client.ui.theme.LocalAppSpacing

@Composable
fun AppGroupLabel(title: String, modifier: Modifier = Modifier) {
    Text(title, modifier.padding(AppMenuDefaults.labelPadding),
        style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary)
}

@Composable
fun AppMenuGroup(title: String, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    AppSection(title, modifier, card = true, content = content)
}

@Composable
fun AppSection(
    title: String? = null, modifier: Modifier = Modifier, card: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(LocalAppSpacing.current.item),
    ) {
        title?.let { AppGroupLabel(it) }
        if (card) Surface(shape = AppMenuDefaults.cardShape, color = MaterialTheme.colorScheme.surfaceContainerLow) {
            Column(Modifier.fillMaxWidth(), content = content)
        } else content()
    }
}

@Composable
fun AppMenuRow(
    title: String, subtitle: String = "", icon: ImageVector? = null,
    modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified, enabled: Boolean = true,
    trailing: (@Composable () -> Unit)? = null, compact: Boolean = false,
    // 传 null 表示本行不自带点击动作，由调用方在外层提供可访问语义
    // （AppSwitchRow 用整行 toggleable 提供 Role.Switch + 状态，不再叠加第二个点击处理器）。
    onClick: (() -> Unit)? = null,
) {
    val foreground = if (tint == Color.Unspecified) MaterialTheme.colorScheme.onSurface else tint
    val alpha = if (enabled) 1f else AppMenuDefaults.disabledAlpha
    val minHeight = when {
        compact -> AppMenuDefaults.singleLineMinHeight
        subtitle.isBlank() -> AppMenuDefaults.singleLineMinHeight
        else -> AppMenuDefaults.twoLineMinHeight
    }
    Row(modifier.fillMaxWidth()
        .heightIn(min = minHeight)
        .then(if (onClick != null) Modifier.clickable(enabled = enabled, onClick = onClick) else Modifier)
        .padding(if (compact) AppMenuDefaults.compactRowPadding else AppMenuDefaults.rowPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppMenuDefaults.rowSpacing)) {
        if (icon != null) Icon(icon, null, Modifier.size(LocalAppSizes.current.icon), tint = foreground.copy(alpha = alpha))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AppMenuDefaults.titleSubtitleSpacing)) {
            Text(title, style = if (compact) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium, color = foreground.copy(alpha = alpha))
            if (subtitle.isNotBlank()) Text(subtitle, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha))
        }
        if (trailing != null) trailing() else Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha))
    }
}
