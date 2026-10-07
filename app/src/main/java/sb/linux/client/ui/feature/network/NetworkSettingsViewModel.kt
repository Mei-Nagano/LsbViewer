package sb.linux.client.ui.feature.network

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import sb.linux.client.domain.network.ProxyDetectionRepository
import sb.linux.client.domain.network.ProxySettings
import sb.linux.client.domain.network.ProxySettingsRepository

data class NetworkSettingsUiState(
    val proxyType: Int,
    val proxyHost: String,
    val proxyPort: String,
    val proxyError: String? = null,
    val detecting: Boolean = false,
    val feedback: String? = null,
)

class NetworkSettingsViewModel(
    private val settingsRepository: ProxySettingsRepository,
    private val detectionRepository: ProxyDetectionRepository,
) : ViewModel() {
    var state by mutableStateOf(settingsRepository.current().toUiState())
        private set
    private var detectionJob: Job? = null

    fun setProxyType(value: Int) = update { copy(proxyType = value.coerceIn(0, 2), proxyError = null) }
    fun setProxyHost(value: String) = update { copy(proxyHost = value, proxyError = null) }
    fun setProxyPort(value: String) = update { copy(proxyPort = value.filter(Char::isDigit).take(5), proxyError = null) }
    fun consumeFeedback() = update { copy(feedback = null) }

    fun reset() {
        detectionJob?.cancel()
        state = settingsRepository.resetNetworkSettings().toUiState().copy(feedback = "已重置网络设置")
    }

    fun save() {
        val host = state.proxyHost.trim()
        val port = state.proxyPort.toIntOrNull()
        val validHost = host.isNotBlank() && !host.contains("://") && !host.contains('/') && host.none(Char::isWhitespace)
        if (state.proxyType != 0 && (!validHost || port == null || port !in 1..65535)) {
            update { copy(proxyError = "请输入有效的代理主机和 1–65535 端口") }
            return
        }
        val current = settingsRepository.current()
        settingsRepository.save(
            ProxySettings(
                state.proxyType,
                if (state.proxyType == 0) current.host else host,
                if (state.proxyType == 0) current.port else port!!,
            ),
        )
        update {
            copy(
                proxyError = null,
                feedback = if (proxyType == 0) "应用代理已关闭" else "应用代理已保存并生效",
            )
        }
    }

    fun detect() {
        detectionJob?.cancel()
        update { copy(detecting = true, proxyError = null, feedback = null) }
        detectionJob = viewModelScope.launch {
            try {
                val found = detectionRepository.detect(state.proxyPort.toIntOrNull())
                if (found == null) {
                    update { copy(proxyError = "未检测到本地 HTTP 或 SOCKS5 代理，请确认代理软件已开启本地监听") }
                } else {
                    settingsRepository.save(ProxySettings(found.type, found.host, found.port))
                    update {
                        copy(
                            proxyType = found.type,
                            proxyHost = found.host,
                            proxyPort = found.port.toString(),
                            feedback = "已检测并启用 ${if (found.type == 2) "SOCKS5" else "HTTP"} 代理 ${found.host}:${found.port}",
                        )
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                update { copy(proxyError = "未检测到本地 HTTP 或 SOCKS5 代理，请确认代理软件已开启本地监听") }
            } finally {
                update { copy(detecting = false) }
            }
        }
    }

    fun cancelDetection() { detectionJob?.cancel() }

    private fun update(change: NetworkSettingsUiState.() -> NetworkSettingsUiState) {
        state = state.change()
    }

    private fun ProxySettings.toUiState() = NetworkSettingsUiState(type, host, port.toString())
}

class NetworkSettingsViewModelFactory(
    private val settings: ProxySettingsRepository,
    private val detection: ProxyDetectionRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        NetworkSettingsViewModel(settings, detection) as T
}
