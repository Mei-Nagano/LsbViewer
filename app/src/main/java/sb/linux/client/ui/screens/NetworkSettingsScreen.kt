package sb.linux.client.ui.screens
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
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = AppMenuDefaults.contentHorizontal, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(LocalAppSpacing.current.contentGap),
                ) {
                    Text("应用代理", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                    Text(
                        "供翻墙软件的仅代理模式使用。覆盖应用请求、图片和在线网页；启用后不会使用 Cronet 直连回退。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    SettingsChoiceRow("代理类型", listOf(0 to "关闭", 1 to "HTTP", 2 to "SOCKS5"),
                        state.proxyType, viewModel::setProxyType)
                    if (state.proxyType != 0) {
                        SettingsTextField(
                            value = state.proxyHost, onValueChange = viewModel::setProxyHost,
                            label = { Text("代理主机") }, placeholder = { Text("127.0.0.1") },
                            singleLine = true, isError = state.proxyError != null, modifier = Modifier.fillMaxWidth(),
                        )
                        SettingsTextField(
                            value = state.proxyPort,
                            onValueChange = viewModel::setProxyPort,
                            label = { Text("代理端口") }, placeholder = { Text("7890") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true, isError = state.proxyError != null, modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    state.proxyError?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
                    OutlinedButton(
                        onClick = viewModel::detect,
                        enabled = !state.detecting,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(if (state.detecting) "正在检测…" else "自动检测本地代理") }
                    Button(onClick = viewModel::save, modifier = Modifier.fillMaxWidth()) { Text("保存代理设置") }
                }
            }
        }
        if (section == "dns") DohSettingsContent(session)
        if (section == "links") LinkSettingsContent(session)
    }
}
