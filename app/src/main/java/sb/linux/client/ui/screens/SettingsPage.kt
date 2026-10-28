package sb.linux.client.ui.screens
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import sb.linux.client.ui.components.navigation.AppPageScaffold
import sb.linux.client.ui.theme.LocalAppSizes
import sb.linux.client.ui.theme.LocalAppSpacing
import sb.linux.client.ui.theme.LocalSurfaceOpacity

/**
 * 设置页适配：外壳统一走 [AppPageScaffold]，这里只保留设置页自己的
 * 「整页滚动 + 命名页边距」约定。`nav` 仅用于默认返回动作，
 * 不进入基础外壳组件（见 UI_DEVELOPMENT_GUIDE 5.2）。
 *
 * 宽窗按 DESIGN_SYSTEM D05 把表单限制在 640dp 内居中，页边距用 24dp；
 * 窄窗保持 16dp，字段不跨整个宽屏。
 */
@Composable
internal fun SettingsPage(
    title: String,
    nav: NavHostController,
    onBack: () -> Unit = { nav.popBackStack() },
    content: @Composable ColumnScope.() -> Unit,
) {
    val spacing = LocalAppSpacing.current
    val sizes = LocalAppSizes.current
    AppPageScaffold(title = title, onBack = onBack) { padding ->
        BoxWithConstraints(Modifier.padding(padding).fillMaxSize()) {
            val horizontal = if (maxWidth >= 600.dp) spacing.pageHorizontalWide else spacing.pageHorizontal
            // 背景图模式：设置表单也要有不透明保护面，避免字段与说明压在半透明表面上。
            val protect = LocalSurfaceOpacity.current < 0.999f
            Box(
                Modifier
                    .fillMaxSize()
                    .background(if (protect) androidx.compose.material3.MaterialTheme.colorScheme.surface else androidx.compose.ui.graphics.Color.Transparent),
                contentAlignment = Alignment.TopCenter,
            ) {
                Column(
                    Modifier
                        .widthIn(max = sizes.formMaxWidth)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = horizontal, vertical = spacing.pageVertical),
                    verticalArrangement = Arrangement.spacedBy(spacing.section),
                    content = content,
                )
            }
        }
    }
}

@Composable
internal fun SettingsResetRow(title: String, message: String = "恢复默认设置？", onConfirm: () -> Unit) {
    var show by remember { mutableStateOf(false) }
    HorizontalDivider()
    TextButton(onClick = { show = true }, modifier = Modifier.fillMaxWidth()) { Text(title) }
    if (show) sb.linux.client.ui.components.feedback.AppConfirmDialog(
        title = title,
        message = message,
        onDismiss = { show = false },
        onConfirm = { show = false; onConfirm() },
        confirmText = "确认", dismissText = "取消",
    )
}
