package sb.linux.client.ui.screens
import sb.linux.client.ui.patterns.settings.SettingsActionRow
import sb.linux.client.ui.patterns.settings.settingsSearchAnchor
import sb.linux.client.ui.patterns.settings.searchingFor
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import sb.linux.client.ui.session.Session
import sb.linux.client.ui.AppUpdatePromptDialog
import androidx.lifecycle.viewmodel.compose.viewModel
import sb.linux.client.ui.feature.settings.GeneralSettingsViewModel
import sb.linux.client.ui.feature.settings.GeneralSettingsViewModelFactory
import sb.linux.client.ui.theme.AppMenuDefaults
import sb.linux.client.ui.patterns.settings.SettingsChoiceRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun UpdateSettingsScreen(session: Session, nav: NavHostController) {
    val factory = remember(session.generalSettingsRepository) {
        GeneralSettingsViewModelFactory(session.generalSettingsRepository)
    }
    val viewModel: GeneralSettingsViewModel = viewModel(factory = factory)
    val state = viewModel.state
    val context = LocalContext.current
    val (doCheck, updateState) = rememberUpdateChecker(session)
    val updateMode = state.settings.updateCheckMode
    val intervalText = state.intervalText

    fun openExternal(url: String) {
        runCatching {
            context.startActivity(
                android.content.Intent(
                    android.content.Intent.ACTION_VIEW,
                    android.net.Uri.parse(url),
                ).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    SettingsPage("版本更新", nav) {
            Text("当前版本 v${currentVersionName(context)}")
            // ---------- 检查更新 ----------
            GroupCard {
                SettingsChoiceRow("检查更新时机", listOf(0 to "每次启动", 1 to "按间隔", 2 to "从不"),
                    updateMode, viewModel::setUpdateCheckMode)
                // 按间隔时的间隔小时数输入
                if (updateMode == 1 || searchingFor("检查间隔（1 - 720 小时）")) {
                    SettingsTextField(
                        value = intervalText,
                        onValueChange = viewModel::setIntervalText,
                        label = { Text("检查间隔（1 - 720 小时）") }, searchLabel = "检查间隔（1 - 720 小时）",
                        suffix = { Text("小时") },
                        singleLine = true,
                        shape = MaterialTheme.shapes.large,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                SettingsActionRow("立即检查更新", { doCheck(false) }, enabled = !updateState.checking,
                    subtitle = if (updateState.checking) "检查中…" else "从 GitHub Releases 获取最新版本")
                updateState.msg?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
    }

    // 发现新版本对话框
    updateState.update?.let { rel ->
        AppUpdatePromptDialog(
            releaseName = rel.name,
            releaseVersion = rel.tag,
            currentVersion = currentVersionName(context),
            notes = rel.notes,
            onDownload = { openExternal(rel.url); updateState.dismiss() },
            onDismiss = updateState.dismiss,
            onSkipVersion = updateState.skipVersion,
            onNeverPrompt = { updateState.neverPrompt(); viewModel.setUpdateCheckMode(2) },
        )
    }
}
