package sb.linux.client.ui.screens
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.navigation.NavHostController
import sb.linux.client.ui.patterns.settings.SettingsScaffold
import sb.linux.client.ui.patterns.settings.SettingsHeader
import sb.linux.client.ui.theme.LocalAppSpacing

/**
 * 设置页适配：共用设置专用外壳与大标题。
 * 宽窗限宽、系统 inset 和主题保护面由 SettingsScaffold 负责。
 */
@Composable
internal fun SettingsPage(
    title: String,
    nav: NavHostController,
    onBack: () -> Unit = { nav.popBackStack() },
    headerActions: @Composable RowScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    val spacing = LocalAppSpacing.current
    val scroll = rememberScrollState()
    var titleHeight by remember(title) { mutableIntStateOf(Int.MAX_VALUE) }
    val collapsed by remember(scroll, titleHeight) { derivedStateOf { scroll.value >= titleHeight } }
    sb.linux.client.ui.patterns.settings.SettingsSearchHost(nav) {
    SettingsScaffold {
        Column(Modifier.fillMaxSize()) {
            sb.linux.client.ui.patterns.settings.SettingsTopBar(title, onBack, headerActions, showTitle = collapsed)
            Column(
                Modifier.fillMaxWidth().weight(1f).verticalScroll(scroll).padding(bottom = spacing.largeSection),
            ) {
                SettingsHeader(title, compact = true, modifier = Modifier.onSizeChanged { titleHeight = it.height })
                Spacer(Modifier.height(spacing.contentGap))
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(spacing.section), content = content)
            }
        }
    }
    }
}

@Composable
internal fun SettingsResetRow(title: String, message: String = "恢复默认设置？", onConfirm: () -> Unit) {
    var show by remember { mutableStateOf(false) }
    SettingMenuRow(title, "", showChevron = false) { show = true }
    if (show) sb.linux.client.ui.components.feedback.AppConfirmDialog(
        title = title,
        message = message,
        onDismiss = { show = false },
        onConfirm = { show = false; onConfirm() },
        confirmText = "确认", dismissText = "取消",
    )
}
