package sb.linux.client.ui.screens

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import sb.linux.client.ui.session.Session
import sb.linux.client.ui.theme.AppMenuDefaults
import sb.linux.client.ui.patterns.settings.SettingsChoiceRow
import sb.linux.client.ui.patterns.settings.SettingsSlider
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AspectRatio
import kotlin.math.roundToInt

internal fun LazyListScope.themeFontSettings(
    session: Session,
    customFontFamily: FontFamily?,
    onChooseCustomFont: () -> Unit,
) {
// ---------- 字体（24）：全局应用字体，支持导入自定义字体文件 ----------
item { GroupLabel("字体") }
item {
    GroupCard {
        Text(
            "选择应用内全局字体；「自定义」从文件导入 ttf/otf 字体，导入后长按可移除",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = AppMenuDefaults.contentHorizontal, vertical = 8.dp)
        )
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = AppMenuDefaults.contentHorizontal, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "default") {
                FontSwatch(
                    label = "默认",
                    fontFamily = null,
                    selected = session.fontKey == "default",
                    onClick = { session.preferences.saveFont("default") }
                )
            }
            item(key = "custom") {
                FontSwatch(
                    label = session.customFontName
                        .removeSuffix(".ttf").removeSuffix(".otf")
                        .ifBlank { "自定义" },
                    fontFamily = customFontFamily,
                    selected = session.fontKey == "custom",
                    custom = customFontFamily == null,
                    onClick = {
                        if (customFontFamily == null) {
                            onChooseCustomFont(
                                
                            )
                        } else {
                            session.preferences.saveFont("custom")
                        }
                    },
                    onLongClick = if (customFontFamily != null) {
                        {
                            session.preferences.clearCustomFont()
                            session.showToast("已移除自定义字体")
                        }
                    } else null
                )
            }
        }
        SettingsChoiceRow("字体粗细", listOf(-3 to "极细", -2 to "细体", -1 to "较细", 0 to "标准", 1 to "较粗", 2 to "粗体"),
            session.fontWeightLevel, session.preferences::saveFontWeightLevel)
        SettingsSlider(value = session.fontWeightLevel.toFloat(),
            onValueChange = { session.preferences.saveFontWeightLevel(it.roundToInt()) },
            valueRange = -3f..2f, steps = 4,
            modifier = Modifier.padding(horizontal = AppMenuDefaults.contentHorizontal, vertical = 12.dp))
        Text("当前正文粗细 ${(400 + session.fontWeightLevel * 100)}", style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(horizontal = AppMenuDefaults.contentHorizontal, vertical = 8.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
item {
    GroupCard {
        ListItem(
            headlineContent = { Text("界面缩放") },
            supportingContent = { Text("调整全局显示比例") },
            leadingContent = { Icon(Icons.Filled.AspectRatio, null) },
            trailingContent = { Text("${(session.fontScale * 100).toInt()}%", style = MaterialTheme.typography.titleSmall) },
            colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        )
        SettingsSlider(
            value = session.fontScale, onValueChange = session.preferences::saveFontScale,
            valueRange = 0.85f..1.30f, steps = 8,
            modifier = Modifier.padding(horizontal = AppMenuDefaults.contentHorizontal, vertical = 12.dp),
        )
    }
}
item { Spacer(Modifier.height(24.dp)) }
}
