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
        Row(
            Modifier.fillMaxWidth().padding(horizontal = AppMenuDefaults.contentHorizontal),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("字体大小", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.weight(1f))
            Text("${(session.fontScale * 100).toInt()}%", style = MaterialTheme.typography.labelMedium)
        }
        Slider(
            value = session.fontScale,
            onValueChange = session.preferences::saveFontScale,
            valueRange = 0.85f..1.30f,
            steps = 8,
            modifier = Modifier.fillMaxWidth().padding(horizontal = AppMenuDefaults.contentHorizontal),
        )
        Text("字体粗细", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(horizontal = AppMenuDefaults.contentHorizontal))
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(horizontal = AppMenuDefaults.contentHorizontal, vertical = 8.dp)) {
            listOf("标准", "较粗", "粗体").forEachIndexed { index, label ->
                SegmentedButton(
                    selected = session.fontWeightLevel == index,
                    onClick = { session.preferences.saveFontWeightLevel(index) },
                    shape = SegmentedButtonDefaults.itemShape(index, 3),
                ) { Text(label) }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}
item { Spacer(Modifier.height(24.dp)) }
}
