package sb.linux.client.data

data class DohServer(val id: String, val name: String, val url: String, val note: String = "", val builtIn: Boolean = false)

object DohServers {
    /** 内置候选不因单个网络的检测结果被删除，是否可用由用户所在网络决定。 */
    val defaults = listOf(
        "us-kan-w-p-1.nashkan.net", "us-nyc-w-p-1.nashkan.net", "dns.neeb.it",
        "dns.nick-slowinski.de", "dns.telekom.de", "dns.t53.de", "dns.vaioswolke.xyz",
    ).map { host -> DohServer("builtin:$host", host, "https://$host/dns-query", builtIn = true) }

    /** 曾经内置、现已下线的端点。保存过的旧 builtin ID 被 merge 过滤，不让死地址以自定义身份回锅。 */
    private val retiredUrls = setOf(
        "https://dns.digitale-gesellschaft.ch/dns-query",
        "https://dns.aa.net.uk/dns-query",
        "https://us-kan-w-p-1.nashkan.net/dns-query",
        "https://us-nyc-w-p-1.nashkan.net/dns-query",
        "https://dns.neeb.it/dns-query",
        "https://dns.nick-slowinski.de/dns-query",
        "https://dns.vaioswolke.xyz/dns-query",
    ).minus(defaults.map { it.url }.toSet())

    fun isRetired(url: String): Boolean = url.trim() in retiredUrls

    fun merge(saved: List<DohServer>, activeUrl: String): List<DohServer> {
        val known = defaults.map { it.id }.toSet()
        // 不再内置的项目不混入默认列表；正在使用的地址由下方迁移保留。
        val kept = saved.filterNot { it.id !in known && it.id.startsWith("builtin:") }
        val result = defaults.map { original ->
            kept.lastOrNull { it.id == original.id }?.let { original.copy(name = it.name, note = it.note) } ?: original
        } + kept.filter { it.id !in known && AppNetwork.isValidEndpoint(it.url) }.distinctBy { it.id }.map { it.copy(builtIn = false) }
        // 兼容旧版单地址配置，不擅自更换用户正在使用的 DNS。
        return if (AppNetwork.isValidEndpoint(activeUrl) && result.none { it.url == activeUrl }) {
            val existingIds = result.map { it.id }.toSet()
            var id = "legacy:$activeUrl"
            while (id in existingIds) id += ":migrated"
            result + DohServer(id, "原有 DoH 配置", activeUrl, "从旧版本保留的服务器")
        } else result
    }
}

/** 只保留可比较的耗时；失败原因不对外呈现，底层异常文字对用户没有参考价值。 */
data class DohBenchmark(val timesMs: List<Long>, val addresses: List<String>, val attempts: Int) {
    val medianMs: Long? get() = timesMs.sorted().let {
        when { it.isEmpty() -> null; it.size % 2 == 1 -> it[it.size / 2]; else -> it[it.size / 2 - 1] + (it[it.size / 2] - it[it.size / 2 - 1]) / 2 }
    }
}
