package sb.linux.client.data

import sb.linux.client.domain.network.DohBenchmark
import sb.linux.client.domain.network.DohServer
import sb.linux.client.domain.network.DohSettingsRepository

internal class DohSettingsRepositoryImpl(private val settings: AppSettings) : DohSettingsRepository {
    override val defaultServers: List<DohServer> get() = DohServers.defaults

    override fun configuration() = sb.linux.client.domain.network.DohConfiguration(
        servers = settings.dohServers(),
        selectedUrl = settings.dohUrl,
        enabled = settings.dohEnabled,
        bypassVpn = settings.dohDisableOnVpn,
        autoWithoutVpn = settings.dohAutoWithoutVpn,
    )

    override fun setEnabled(enabled: Boolean) { settings.dohEnabled = enabled }
    override fun selectServer(url: String) { settings.dohUrl = url }
    override fun setBypassVpn(enabled: Boolean) { settings.dohDisableOnVpn = enabled }
    override fun setAutoWithoutVpn(enabled: Boolean) { settings.dohAutoWithoutVpn = enabled }
    override fun saveServers(servers: List<DohServer>) { settings.saveDohServers(servers) }

    override fun isValidEndpoint(url: String): Boolean = DohEndpointPolicy.isValid(url)

    override suspend fun benchmark(url: String): DohBenchmark = AppNetwork.benchmark(url)
}
