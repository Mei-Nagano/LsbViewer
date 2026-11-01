package sb.linux.client.ui.screens

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import sb.linux.client.ui.session.Session
import sb.linux.client.ui.theme.colorSeed
import sb.linux.client.ui.theme.isCustomColorKey
import sb.linux.client.ui.theme.AppMenuDefaults

internal fun LazyListScope.themeBackgroundPalette(
    section: String,
    session: Session,
    currentScheme: ColorScheme,
    backgroundImportBusy: Boolean,
    onOpenBackgroundPicker: () -> Unit,
    onSelectBackgroundImage: () -> Unit,
    onPickRole: (PreviewRole) -> Unit,
    onPickElement: (String) -> Unit,
    onOpenThemeColorPicker: () -> Unit,
) {
// ---------- 自定义全局背景（20、38） ----------
if (section == "background") {
item { GroupLabel("页面背景") }
item {
    GroupCard {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = AppMenuDefaults.contentHorizontal, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("自定义背景色", style = MaterialTheme.typography.bodyLarge)
                Text(
                    if (session.themeBackground == null) "当前跟随主题" else "已应用到所有页面底色",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (session.themeBackground != null) {
                TextButton(onClick = { session.preferences.saveThemeBackground(null) }) { Text("恢复") }
            }
            Box(
                Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(session.themeBackground ?: currentScheme.background)
                    .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                    .clickable { onOpenBackgroundPicker() },
            )
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = AppMenuDefaults.contentHorizontal), verticalAlignment = Alignment.CenterVertically) {
            TextButton(enabled = !backgroundImportBusy, onClick = { onSelectBackgroundImage() }) {
                Text(if (backgroundImportBusy) "正在处理图片…" else "选择背景图片")
            }
            if (session.backgroundImage.isNotEmpty()) TextButton(onClick = session.preferences::removeBackgroundImage) { Text("移除图片") }
        }
        if (session.backgroundImage.isNotEmpty()) {
            SwitchRow("启用背景图片", "本地保存，随设置备份；不上传图床", checked = session.backgroundImageEnabled,
                onCheckedChange = session.preferences::saveBackgroundImageEnabled)
            Column(Modifier.padding(horizontal = AppMenuDefaults.contentHorizontal)) {
                Text("背景图片不透明度 ${(session.backgroundImageOpacity * 100).toInt()}%")
                Slider(value = session.backgroundImageOpacity, onValueChange = session.preferences::saveBackgroundImageOpacity, valueRange = 0.05f..0.65f)
                Text("卡片、顶栏表面不透明度 ${(session.surfaceOpacity * 100).toInt()}%")
                Slider(value = session.surfaceOpacity, onValueChange = session.preferences::saveSurfaceOpacity, valueRange = 0.2f..1f)
            }
        }
        SwitchRow(
            "禁止页面染色（兼容旧版）",
            "新默认使用带轻微色调的 MD3 表面；开启后页面底色、普通卡片和弹窗保持中性色，按钮、选中状态、标签和高亮完整保留强调色。手动自定义背景不受影响",
            checked = session.keepBackgroundColor,
            onCheckedChange = session.preferences::saveKeepBackgroundColor,
        )
    }
}

}
if (section == "colors") {
// ---------- 实时预览（上部吸顶，始终可见；下半部分默认折叠 3.5） ----------
item {
    PostCardPreview(currentScheme) { key -> onPickElement(key) }
}
item {
    ElementSelectorList(
        session,
        currentScheme,
        onClickRole = { role -> onPickRole(role) },
        onClickElement = { key -> onPickElement(key) }
    )
}

// ---------- 主题色（单行横向滑动） ----------
item { GroupLabel("主题色") }
item {
    GroupCard {
        Text(
            "选择起点色由主题引擎生成配色；自定义色可「存为预选」，长按色块可移除",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = AppMenuDefaults.contentHorizontal, vertical = 8.dp)
        )
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = AppMenuDefaults.contentHorizontal, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(sb.linux.client.ui.theme.ThemeColorPresets) { preset ->
                val selected = session.themeColorKey == preset.key
                ColorSwatch(
                    color = preset.lightPrimary,
                    label = preset.label,
                    selected = selected,
                    onClick = {
                        session.preferences.saveTheme(
                            session.themeMode,
                            preset.key == "default" && session.dynamicColor,
                            preset.key
                        )
                    }
                )
            }
            // 用户保存过的自定义主题色预选
            items(session.themeCustomColors) { key ->
                val selected = session.themeColorKey == key
                ColorSwatch(
                    color = colorSeed(key),
                    label = "#" + key.removePrefix("custom#").take(4),
                    selected = selected,
                    onClick = { session.preferences.saveTheme(session.themeMode, false, key) },
                    onLongClick = {
                        session.preferences.removeThemeCustomColor(key)
                        session.showToast("已移除该自定义主题色")
                    }
                )
            }
            item {
                val isCustom = isCustomColorKey(session.themeColorKey)
                ColorSwatch(
                    color = if (isCustom) colorSeed(session.themeColorKey) else MaterialTheme.colorScheme.surfaceContainerHigh,
                    label = "自定义",
                    selected = isCustom,
                    custom = true,
                    onClick = { onOpenThemeColorPicker() }
                )
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

}
}
