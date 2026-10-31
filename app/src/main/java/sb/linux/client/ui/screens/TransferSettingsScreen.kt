package sb.linux.client.ui.screens
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import sb.linux.client.ui.session.Session
import androidx.lifecycle.viewmodel.compose.viewModel
import sb.linux.client.ui.feature.backup.WebDavBackupViewModel
import sb.linux.client.ui.feature.backup.WebDavBackupViewModelFactory
import sb.linux.client.ui.feature.backup.BackupSettingsViewModel
import sb.linux.client.ui.feature.backup.BackupSettingsViewModelFactory
import sb.linux.client.ui.theme.AppMenuDefaults

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferSettingsScreen(session: Session, nav: NavHostController, cloud: Boolean = false) {
    val context = LocalContext.current
    val settingsFactory = remember(session.backupSettingsRepository) {
        BackupSettingsViewModelFactory(session.backupSettingsRepository)
    }
    val settingsViewModel: BackupSettingsViewModel = viewModel(factory = settingsFactory)
    val settingsState = settingsViewModel.state
    val davUrl = settingsState.settings.webDavUrl
    val davUser = settingsState.settings.webDavUsername
    val davPass = settingsState.settings.webDavPassword
    val davDir = settingsState.directoryText
    val backupItems = settingsState.settings.items
    val backupViewModelFactory = remember(session.webDavBackupRepository) {
        WebDavBackupViewModelFactory(session.webDavBackupRepository)
    }
    val backupViewModel: WebDavBackupViewModel = viewModel(factory = backupViewModelFactory)
    var msg by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(msg) { msg?.let { session.showToast(it.removePrefix("✓ ").trim()); msg = null } }

    var davBusy by remember { mutableStateOf(false) }
    var davPassVisible by remember { mutableStateOf(false) }

    val exportJsonLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { os ->
                    os.write(settingsViewModel.exportJson().toByteArray())
                }
                msg = "✓ 已导出为 JSON"
            }.onFailure { msg = "导出失败：${it.message}" }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText() ?: ""
                if (text.isBlank()) msg = "文件为空"
                else {
                    val err = settingsViewModel.importJson(text)
                    if (err == null) {
                        session.reloadPrefs()   // 导入后立即生效，无需重启
                        msg = "✓ 导入成功，已立即生效"
                    } else msg = "✗ $err"
                }
            }.onFailure { msg = "导入失败：${it.message}" }
        }
    }

    fun davFileUrl(): String? {
        if (davUrl.isBlank()) { msg = "✗ 请先填写 WebDAV 服务器地址"; return null }
        val base = davUrl.trim().trimEnd('/')
        val dir = davDir.trim().trim('/')
        return if (dir.isEmpty()) "$base/lsb_settings.json" else "$base/$dir/lsb_settings.json"
    }

    fun davBackup() {
        val url = davFileUrl() ?: return
        davBusy = true
        backupViewModel.upload(url, davUser, davPass, settingsViewModel.exportJson()) { result ->
            result.onSuccess { msg = "✓ 已备份到 WebDAV" }
                .onFailure { msg = "✗ WebDAV 备份失败：${it.message}" }
            davBusy = false
        }
    }

    fun davRestore() {
        val url = davFileUrl() ?: return
        davBusy = true
        backupViewModel.download(url, davUser, davPass) { result ->
            result.onSuccess { body ->
                val err = settingsViewModel.importJson(body)
                if (err == null) {
                    session.reloadPrefs()   // 恢复后立即生效，无需重启
                    msg = "✓ 已从 WebDAV 恢复，已立即生效"
                } else msg = "✗ $err"
            }.onFailure { msg = "✗ WebDAV 恢复失败：${it.message}" }
            davBusy = false
        }
    }

    SettingsPage(if (cloud) "WebDAV 云备份" else "本地备份", nav) {


            GroupCard {
                Text(
                    "选择要写入 JSON 和 WebDAV 的内容。导入时只恢复文件中存在的项目，未备份的本地数据不受影响。自定义字体文件本体不随备份迁移。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = AppMenuDefaults.contentHorizontal, vertical = 8.dp)
                )
                val backupOptions = listOf(
                    "settings" to "应用设置",
                    "history" to "浏览历史与阅读位置",
                    "favorites" to "帖子与评论收藏",
                    "ai" to "AI 配置、总结历史与总结收藏",
                    "cards" to "发卡兑换记录",
                )
                backupOptions.forEach { (key, label) ->
                    Row(
                        Modifier.fillMaxWidth().clickable {
                            settingsViewModel.setItemEnabled(key, key !in backupItems)
                        }.padding(horizontal = 10.dp, vertical = 1.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = key in backupItems,
                            onCheckedChange = { checked -> settingsViewModel.setItemEnabled(key, checked) },
                        )
                        Text(label, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                if (!cloud) Row(
                    Modifier.padding(horizontal = AppMenuDefaults.contentHorizontal, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            // 导出文件名带当前日期时间（3.3）
                            val now = java.text.SimpleDateFormat(
                                "yyyyMMdd_HHmmss", java.util.Locale.getDefault()
                            ).format(java.util.Date())
                            exportJsonLauncher.launch("lsb_settings_$now.json")
                        },
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.weight(1f)
                    ) { Text("导出设置") }
                    Button(
                        onClick = {
                            importLauncher.launch(arrayOf("application/json", "text/plain", "text/*", "application/octet-stream"))
                        },
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.weight(1f)
                    ) { Text("导入设置") }
                }
            }

            if (cloud) WebDavBackupCard(
                url = davUrl,
                onUrlChange = settingsViewModel::setWebDavUrl,
                username = davUser,
                onUsernameChange = settingsViewModel::setWebDavUsername,
                password = davPass,
                onPasswordChange = settingsViewModel::setWebDavPassword,
                passwordVisible = davPassVisible,
                onPasswordVisibleChange = { davPassVisible = it },
                directory = davDir,
                onDirectoryChange = settingsViewModel::setWebDavDirectory,
                busy = davBusy,
                onBackup = ::davBackup,
                onRestore = ::davRestore,
            )
            Spacer(Modifier.height(12.dp))
    }
}

/** 当前主题模式下是否处于暗色 */
