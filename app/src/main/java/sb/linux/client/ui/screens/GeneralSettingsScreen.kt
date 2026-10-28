package sb.linux.client.ui.screens
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneralSettingsScreen(session: Session, nav: NavHostController) {
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

    SettingsPage("应用与账号", nav) {
            SettingMenuRow("个人资料与账号", "源站个人设置") { nav.navigate("settings") }
            Text("当前版本 v${currentVersionName(context)}")
            // ---------- 检查更新 ----------
            GroupCard {
                Column(Modifier.padding(horizontal = 18.dp, vertical = 10.dp)) {
                    Text("检查更新时机", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(10.dp))
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            selected = updateMode == 0,
                            onClick = { viewModel.setUpdateCheckMode(0) },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3),
                            modifier = Modifier.weight(1f)
                        ) { Text("每次启动", maxLines = 1) }
                        SegmentedButton(
                            selected = updateMode == 1,
                            onClick = { viewModel.setUpdateCheckMode(1) },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3),
                            modifier = Modifier.weight(1f)
                        ) { Text("按间隔", maxLines = 1) }
                        SegmentedButton(
                            selected = updateMode == 2,
                            onClick = { viewModel.setUpdateCheckMode(2) },
                            shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3),
                            modifier = Modifier.weight(1f)
                        ) { Text("从不", maxLines = 1) }
                    }
                    // 按间隔时的间隔小时数输入
                    if (updateMode == 1) {
                        Spacer(Modifier.height(10.dp))
                        OutlinedTextField(
                            value = intervalText,
                            onValueChange = viewModel::setIntervalText,
                            label = { Text("检查间隔（1 - 720 小时）") },
                            suffix = { Text("小时") },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = { doCheck(false) },
                        enabled = !updateState.checking,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(if (updateState.checking) "检查中…" else "立即检查更新") }
                    updateState.msg?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            SettingsResetRow("重置所有应用设置") {
                session.preferences.resetAllAppSettings()
                session.showToast("已重置所有应用设置")
            }
    }

    // 发现新版本对话框
    updateState.update?.let { rel ->
        AppUpdatePromptDialog(
            releaseName = rel.name,
            currentVersion = currentVersionName(context),
            notes = rel.notes,
            onDownload = { openExternal(rel.url); updateState.dismiss() },
            onDismiss = updateState.dismiss,
        )
    }
}

// ==================== 已导出的帖子（3.17） ====================

/** 本地已导出为 HTML 的帖子列表（本地文件读取，不重新解析源站） */
