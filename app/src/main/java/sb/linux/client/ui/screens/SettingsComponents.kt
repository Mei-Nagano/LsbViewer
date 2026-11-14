package sb.linux.client.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewQuilt
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
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Wallpaper
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import sb.linux.client.ui.components.layout.AppMenuRow
import sb.linux.client.ui.patterns.settings.SettingsCard

@Composable
internal fun GroupLabel(title: String) { sb.linux.client.ui.components.layout.AppGroupLabel(title) }

/** 设置分组：标签在卡片外，控件在统一圆角保护面内。 */
@Composable
internal fun GroupCard(
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    sb.linux.client.ui.components.layout.AppSection(title = title) {
        SettingsCard(content = content)
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
    sb.linux.client.ui.components.input.AppSwitchRow(
        title = title, checked = checked, onCheckedChange = onCheckedChange,
        subtitle = subtitle, enabled = enabled,
        prominent = true,
        icon = icon ?: settingsRowIcon(title),
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
 * 设置条目：直接图标、较粗标题、辅助说明与右箭头，整行在圆角卡片内。
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
    shape: Shape = MaterialTheme.shapes.extraLarge,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    SettingsCard(modifier, shape) {
        AppMenuRow(
            title = title,
            subtitle = subtitle,
            icon = icon ?: settingsRowIcon(title),
            prominent = true,
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

/** 设置条目图标：按现有条目文案映射，文案本身保持不变。 */
internal fun settingsRowIcon(title: String): ImageVector = when (title) {
    "版本更新", "检查更新时机" -> Icons.Filled.SystemUpdate
    "空间占用", "立即清理", "自动清理" -> Icons.Filled.Storage
    "本地备份", "WebDAV 云备份" -> Icons.Filled.CloudUpload
    "列表与分页" -> Icons.Filled.FormatListBulleted
    "正文与评论", "总结行为" -> Icons.Filled.AutoStories
    "帖子交互" -> Icons.Filled.TouchApp
    "侧边栏" -> Icons.Filled.Menu
    "屏蔽规则" -> Icons.Filled.Block
    "应用代理" -> Icons.Filled.Language
    "加密 DNS" -> Icons.Filled.Dns
    "链接设置" -> Icons.Filled.Link
    "接口与配置方案", "总结历史" -> Icons.Filled.AutoAwesome
    "阅读与交互" -> Icons.Filled.AutoStories
    "外观与布局" -> Icons.Filled.Palette
    "主题设置" -> Icons.Filled.Palette
    "壁纸" -> Icons.Filled.Wallpaper
    "字体" -> Icons.Filled.TextFields
    "导航与布局" -> Icons.AutoMirrored.Filled.ViewQuilt
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
    else -> when {
        title.contains("图片") || title.contains("图床") -> Icons.Filled.Image
        title.contains("缓存") || title.contains("清理") -> Icons.Filled.Storage
        title.contains("AI") || title.contains("总结") -> Icons.Filled.AutoAwesome
        title.contains("收藏") -> Icons.Filled.Star
        title.contains("历史") || title.contains("阅读位置") -> Icons.Filled.History
        title.contains("评论") || title.contains("对话") || title.contains("弹幕") -> Icons.Filled.Forum
        title.contains("侧边栏") || title.contains("在线用户") -> Icons.Filled.Menu
        title.contains("滚动") || title.contains("刷新") -> Icons.Filled.FormatListBulleted
        title.contains("UID") || title.contains("用户") -> Icons.Filled.Person
        title.contains("字体") -> Icons.Filled.TextFields
        title.contains("屏蔽") -> Icons.Filled.Block
        else -> Icons.Filled.Settings
    }
}
