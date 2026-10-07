package sb.linux.client.domain.network

data class ProxySettings(
    val type: Int,
    val host: String,
    val port: Int,
)

interface ProxySettingsRepository {
    fun current(): ProxySettings
    fun save(settings: ProxySettings)
    fun resetNetworkSettings(): ProxySettings
}
