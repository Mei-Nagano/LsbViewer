package sb.linux.client.ui.screens
import sb.linux.client.ui.patterns.settings.SettingsActionRow
import sb.linux.client.ui.patterns.settings.settingsSearchAnchor
import sb.linux.client.ui.patterns.settings.searchingFor
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import sb.linux.client.ui.session.Session
import sb.linux.client.ui.feature.network.NetworkSettingsViewModel
import sb.linux.client.ui.feature.network.NetworkSettingsViewModelFactory
import sb.linux.client.ui.theme.AppMenuDefaults
import sb.linux.client.ui.theme.LocalAppSpacing
import sb.linux.client.ui.patterns.settings.SettingsChoiceRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NetworkSettingsDetailScreen(session: Session, nav: NavHostController, section: String) {
    val factory = remember(session.proxySettingsRepository, session.proxyDetectionRepository) {
        NetworkSettingsViewModelFactory(session.proxySettingsRepository, session.proxyDetectionRepository)
    }
    val viewModel: NetworkSettingsViewModel = viewModel(factory = factory)
    val state = viewModel.state
    val saved = session.proxySettingsRepository.current()
    val exit = rememberSettingsExit(section == "proxy" && (state.proxyType != saved.type ||
        (state.proxyType != 0 && (state.proxyHost.trim() != saved.host || state.proxyPort != saved.port.toString())))) { nav.popBackStack() }
    LaunchedEffect(state.feedback) {
        state.feedback?.let {
            session.showToast(it)
            viewModel.consumeFeedback()
        }
    }
    DisposableEffect(viewModel) {
        onDispose { viewModel.cancelDetection() }
    }

    val title = when (section) { "proxy" -> "应用代理"; "dns" -> "加密 DNS"; else -> "链接设置" }
    SettingsPage(title, nav, onBack = exit) {
        if (section == "proxy") {
            GroupCard {
                Text("应用代理", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                Text(
                    "启动或回到应用时自动检测并保存系统、本地代理。代理生效时，应用联网和网页浏览优先使用代理；关闭后恢复已保存的 DoH 设置。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                SettingsChoiceRow("代理类型", listOf(0 to "关闭", 1 to "HTTP", 2 to "SOCKS5"),
                    state.proxyType, viewModel::setProxyType)
                if (state.proxyType != 0 || searchingFor("代理主机", "代理端口")) {
                    SettingsTextField(
                        value = state.proxyHost, onValueChange = viewModel::setProxyHost,
                        label = { Text("代理主机") }, searchLabel = "代理主机", placeholder = { Text("127.0.0.1") },
                        description = "代理服务器的 IP 或主机名",
                        singleLine = true, isError = state.proxyError != null, modifier = Modifier.fillMaxWidth(),
                    )
                    SettingsTextField(
                        value = state.proxyPort,
                        onValueChange = viewModel::setProxyPort,
                        label = { Text("代理端口") }, searchLabel = "代理端口", placeholder = { Text("7890") },
                        description = "代理服务器监听的端口",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true, isError = state.proxyError != null, modifier = Modifier.fillMaxWidth(),
                    )
                }
                state.proxyError?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
                SettingsActionRow("重新检测本地代理", viewModel::detect, enabled = !state.detecting,
                    subtitle = if (state.detecting) "正在检测…" else "立即识别当前代理")
                SettingsActionRow("保存代理设置", viewModel::save)
            }
        }
        if (section == "dns") DohSettingsContent(session)
        if (section == "links") LinkSettingsContent(session)
    }
}
