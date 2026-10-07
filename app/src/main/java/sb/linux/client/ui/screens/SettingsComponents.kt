package sb.linux.client.ui.screens
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
internal fun GroupLabel(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 12.dp, top = 10.dp, bottom = 2.dp)
    )
}

/** 设置分组卡片容器：标题行可折叠，展开/收起带动画，标题可带图标 */
@Composable
internal fun GroupCard(
    title: String? = null,
    initiallyExpanded: Boolean = true,
    icon: ImageVector? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    var expanded by rememberSaveable(title) { mutableStateOf(initiallyExpanded) }
    // 箭头随展开状态旋转，比换图标的跳变更连贯
    val arrowRotation by animateFloatAsState(if (expanded) 180f else 0f, label = "groupArrow")
    Surface(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(Modifier.padding(vertical = 6.dp)) {
            if (title != null) ListItem(
                headlineContent = { Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold) },
                leadingContent = icon?.let { vector -> { SettingIcon(vector) } },
                trailingContent = {
                    Icon(
                        Icons.Filled.ExpandMore,
                        if (expanded) "收起" else "展开",
                        Modifier.rotate(arrowRotation),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                modifier = Modifier.clickable { expanded = !expanded },
            )
            if (title == null) content()
            else AnimatedVisibility(expanded) { Column(content = content) }
        }
    }
}

/** 设置项图标：主色图标 + 淡主色圆底，让长列表可以靠图标定位 */
@Composable
internal fun SettingIcon(icon: ImageVector) {
    Box(
        Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(11.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
    }
}

/** 带开关的设置行 */
@Composable
internal fun SwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    // 禁用时标题与副标题一起变淡，只淡标题会让人以为副标题还可操作
    val alpha = if (enabled) 1f else .38f
    ListItem(
        headlineContent = { Text(title, color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)) },
        supportingContent = {
            Text(subtitle, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha))
        },
        trailingContent = { Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clickable(enabled = enabled) { onCheckedChange(!checked) },
    )
}

/** 人类可读的字节大小 */
@Composable
internal fun ResetAction(title: String, onConfirm: () -> Unit) {
    var show by remember { mutableStateOf(false) }
    IconButton(onClick = { show = true }) {
        Icon(Icons.Filled.RestartAlt, "重置")
    }
    if (show) {
        AlertDialog(
            onDismissRequest = { show = false },
            title = { Text(title) },
            text = { Text("恢复默认设置？") },
            confirmButton = {
                TextButton(onClick = { show = false; onConfirm() }) { Text("重置") }
            },
            dismissButton = { TextButton(onClick = { show = false }) { Text("取消") } }
        )
    }
}

/**
 * 应用设置（区别于源站个人设置）：分类菜单入口，点进子页面再进行具体设置。
 */
@Composable
internal fun SettingMenuRow(title: String, subtitle: String, icon: ImageVector? = null, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            SettingIcon(icon)
            Spacer(Modifier.width(14.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** 浏览设置：列表与分页、正文与评论、文本选择操作。缓存分析归入数据管理。 */
