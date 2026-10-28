package sb.linux.client.ui.theme

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import sb.linux.client.ui.components.feedback.AppNotice
import sb.linux.client.ui.components.input.AppSwitchRow

/**
 * 设计系统隔离预览（DESIGN_SYSTEM D10）。
 *
 * 只组合公共 token 与公共组件，不连接 Session、仓库或导航；用浅/深、长文案、
 * 禁用、错误与大字号检查同一模型，供实现阶段逐项评估，不代表已通过设备验收。
 */
@Composable
private fun DesignSystemSample(disabled: Boolean = false) {
    val spacing = LocalAppSpacing.current
    val reading = LocalReadingType.current
    Surface(color = MaterialTheme.colorScheme.surface) {
        Column(
            Modifier.fillMaxWidth().padding(spacing.pageHorizontal),
            verticalArrangement = Arrangement.spacedBy(spacing.item),
        ) {
            Text("页面标题 / titleLarge", style = MaterialTheme.typography.titleLarge)
            Text("帖子列表标题 / titleMedium", style = MaterialTheme.typography.titleMedium)
            Text(
                "正文阅读样本：这一段用于检查 18sp / 30sp 的行距与中文换行。" +
                    " 长英文与 URL 例如 https://linux.sb/t/topic/123456?page=2 不应破坏版式。",
                style = reading.body,
            )
            Text("辅助信息 / bodySmall · 12sp · 2 天前 · 板块 · 128 回复", style = MaterialTheme.typography.bodySmall)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Surface(
                shape = AppShapes.medium,
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(spacing.contentGap)) {
                    AppSwitchRow("整体开关行", checked = !disabled, enabled = !disabled, onCheckedChange = {}, subtitle = "说明文字随行高增长，不裁切")
                    AppSwitchRow("禁用开关", checked = false, enabled = false, onCheckedChange = {}, subtitle = "禁用原因应写在说明里")
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.item)) {
                Button(onClick = {}, enabled = !disabled) { Text("主要操作") }
                FilledTonalButton(onClick = {}, enabled = !disabled) { Text("次要操作") }
                TextButton(onClick = {}, enabled = !disabled) { Text("文字操作") }
            }
            AppNotice("加载失败：可在此区域重试", onAction = {})
            AppNotice("已有内容刷新失败，列表保持可读", error = false)
            Spacer(Modifier.height(spacing.section))
            Text("形状：4 / 8 / 12 / 16 / 28dp", style = MaterialTheme.typography.labelMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.inline)) {
                listOf(4, 8, 12, 16, 28).forEach { radius ->
                    Surface(
                        shape = RoundedCornerShape(radius.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.width(56.dp).height(32.dp),
                    ) {}
                }
            }
        }
    }
}

@Preview(name = "设计系统 · 浅色", showBackground = true, backgroundColor = 0xFFFAF8FF)
@Composable
private fun DesignSystemLightPreview() {
    LsbTheme(themeMode = ThemeMode.LIGHT, dynamicColor = false) { DesignSystemSample() }
}

@Preview(name = "设计系统 · 深色", showBackground = true, backgroundColor = 0xFF121318)
@Composable
private fun DesignSystemDarkPreview() {
    LsbTheme(themeMode = ThemeMode.DARK, dynamicColor = false) { DesignSystemSample() }
}

@Preview(name = "设计系统 · 大字号", showBackground = true, backgroundColor = 0xFFFAF8FF, fontScale = 2f)
@Composable
private fun DesignSystemLargeFontPreview() {
    LsbTheme(themeMode = ThemeMode.LIGHT, dynamicColor = false) { DesignSystemSample() }
}

@Preview(name = "设计系统 · 禁用", showBackground = true, backgroundColor = 0xFFFAF8FF)
@Composable
private fun DesignSystemDisabledPreview() {
    LsbTheme(themeMode = ThemeMode.LIGHT, dynamicColor = false) { DesignSystemSample(disabled = true) }
}
