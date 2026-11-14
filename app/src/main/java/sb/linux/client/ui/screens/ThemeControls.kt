package sb.linux.client.ui.screens
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import sb.linux.client.ui.session.ThemeModePref
import sb.linux.client.ui.patterns.settings.SettingsChoiceRow

@Composable
internal fun DayNightToggle(mode: ThemeModePref, onSelect: (ThemeModePref) -> Unit) {
    SettingsChoiceRow(
        title = "深浅模式", subtitle = "选择应用的显示模式", icon = Icons.Filled.BrightnessAuto,
        options = listOf(ThemeModePref.SYSTEM to "跟随系统", ThemeModePref.LIGHT to "浅色", ThemeModePref.DARK to "深色"),
        selectedValue = mode, onSelect = onSelect,
    )
}

/** 主题色预设方形色板（ColorBlendr 风格，适应性排布） */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ColorSwatch(
    color: Color,
    label: String,
    selected: Boolean,
    custom: Boolean = false,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
) {
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    val borderWidth = if (selected) 2.dp else 1.dp
    Column(
        Modifier.width(56.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .size(44.dp)
                .clip(MaterialTheme.shapes.large)
                .border(borderWidth, borderColor, MaterialTheme.shapes.large)
                .background(color)
                .combinedClickable(onClick = onClick, onLongClick = onLongClick),
            contentAlignment = Alignment.Center
        ) {
            if (custom) {
                Icon(
                    Icons.Filled.Add,
                    "自定义取色",
                    modifier = Modifier.size(20.dp),
                    tint = if (selected) MaterialTheme.colorScheme.onPrimary else Color.Black.copy(alpha = 0.55f)
                )
            } else if (onLongClick != null) {
                Text(
                    "长按移除",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Black.copy(alpha = 0.35f),
                    maxLines = 1,
                    fontSize = 8.sp
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** 读取 SAF 文档的显示名（用于自定义字体文件名展示） */
internal fun queryDisplayName(context: android.content.Context, uri: android.net.Uri): String =
    runCatching {
        context.contentResolver.query(uri, null, null, null, null)?.use { c ->
            val idx = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (idx >= 0 && c.moveToFirst()) c.getString(idx) else null
        } ?: uri.lastPathSegment
    }.getOrNull() ?: "自定义字体"

/** 字体选项块（24）：以「字A」预览该字体实际观感 */
@Composable
internal fun FontSwatch(
    label: String,
    fontFamily: FontFamily?,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    custom: Boolean = false,
) {
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    val borderWidth = if (selected) 2.dp else 1.dp
    Column(
        Modifier.width(64.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .size(48.dp)
                .clip(MaterialTheme.shapes.large)
                .border(borderWidth, borderColor, MaterialTheme.shapes.large)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .combinedClickable(onClick = onClick, onLongClick = onLongClick),
            contentAlignment = Alignment.Center
        ) {
            if (custom) {
                Icon(
                    Icons.Filled.Add,
                    "导入自定义字体",
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    "字A",
                    fontFamily = fontFamily,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * 外观设置（独立分类页）：快捷切换 / 实时预览(帖子卡片元素级改色) / 主题色 / 调色风格 / 取色 / 对比度。
 * 基于 ColorBlendr 同款 HCT 主题引擎：主题色作为「种子色」，配合调色风格与对比度
 * 生成完整 MD3 配色，预览卡点按元素可独立覆盖对应颜色，只作用于本应用。
 */
