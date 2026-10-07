package sb.linux.client.domain.network

data class DohServer(
    val id: String,
    val name: String,
    val url: String,
    val note: String = "",
    val builtIn: Boolean = false,
)

data class DohBenchmark(val timesMs: List<Long>, val addresses: List<String>, val attempts: Int) {
    val medianMs: Long? get() = timesMs.sorted().let {
        when {
            it.isEmpty() -> null
            it.size % 2 == 1 -> it[it.size / 2]
            else -> it[it.size / 2 - 1] + (it[it.size / 2] - it[it.size / 2 - 1]) / 2
        }
    }
}

data class DohConfiguration(
    val servers: List<DohServer>,
    val selectedUrl: String,
    val enabled: Boolean,
    val bypassVpn: Boolean,
    val autoWithoutVpn: Boolean,
)

interface DohSettingsRepository {
    val defaultServers: List<DohServer>
    fun configuration(): DohConfiguration
    fun setEnabled(enabled: Boolean)
    fun selectServer(url: String)
    fun setBypassVpn(enabled: Boolean)
    fun setAutoWithoutVpn(enabled: Boolean)
    fun saveServers(servers: List<DohServer>)
    fun isValidEndpoint(url: String): Boolean
    suspend fun benchmark(url: String): DohBenchmark
}
