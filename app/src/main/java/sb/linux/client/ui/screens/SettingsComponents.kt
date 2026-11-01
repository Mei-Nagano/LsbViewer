package sb.linux.client.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

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
 * 设置条目：左侧 MD3 图标，中间主标题与灰色说明。
 * 主标题 labelLarge、说明 bodyMedium；条目之间由父级留白，不使用分隔线。
 */
@Composable
internal fun SettingMenuRow(
    title: String,
    subtitle: String,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icon ?: settingsRowIcon(title),
                null,
                Modifier.size(22.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (subtitle.isNotBlank()) Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** 设置条目图标：按现有条目文案映射，文案本身保持不变。 */
private fun settingsRowIcon(title: String): ImageVector = when (title) {
    "阅读与交互" -> Icons.Filled.AutoStories
    "外观与布局" -> Icons.Filled.Palette
    "发帖与图片上传" -> Icons.Filled.Image
    "网络与链接" -> Icons.Filled.Language
    "AI 助手" -> Icons.Filled.AutoAwesome
    "存储与备份" -> Icons.Filled.Storage
    "应用与账号" -> Icons.Filled.ManageAccounts
    "我的内容" -> Icons.Filled.CollectionsBookmark
    "个人资料与账号" -> Icons.Filled.Person
    "我的隐私" -> Icons.Filled.Lock
    "社区活跃度" -> Icons.Filled.TrendingUp
    "浏览历史" -> Icons.Filled.History
    "附件管理" -> Icons.Filled.AttachFile
    "我的收藏" -> Icons.Filled.Star
    "AI 总结历史" -> Icons.Filled.SmartToy
    "发卡兑换记录" -> Icons.Filled.CardGiftcard
    "已导出的帖子" -> Icons.Filled.FileDownload
    "用户屏蔽名单" -> Icons.Filled.Block
    else -> Icons.Filled.Settings
}
