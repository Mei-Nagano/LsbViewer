package sb.linux.client.data

import sb.linux.client.domain.network.ProxySettings
import sb.linux.client.domain.network.ProxySettingsRepository

internal class AppSettingsProxySettingsRepository(
    private val settings: AppSettings,
) : ProxySettingsRepository {
    override fun current() = ProxySettings(settings.proxyType, settings.proxyHost, settings.proxyPort)

    override fun save(settings: ProxySettings) {
        this.settings.saveProxy(settings.type, settings.host, settings.port)
    }

    override fun resetNetworkSettings(): ProxySettings {
        settings.resetNetwork()
        return current()
    }
}
