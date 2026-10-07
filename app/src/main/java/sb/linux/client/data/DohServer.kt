package sb.linux.client.data

import sb.linux.client.domain.network.DohServer
import sb.linux.client.domain.network.DohBenchmark

object DohServers {
    /** 内置候选不因单个网络的检测结果被删除，是否可用由用户所在网络决定。 */
    val defaults = listOf(
        "dns.t53.de", "dns.telekom.de", "dns.neeb.it", "dns.nick-slowinski.de",
        "dns.vaioswolke.xyz", "us-kan-w-p-1.nashkan.net", "us-nyc-w-p-1.nashkan.net",
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
        } + kept.filter { it.id !in known && DohEndpointPolicy.isValid(it.url) }.distinctBy { it.id }.map { it.copy(builtIn = false) }
        // 兼容旧版单地址配置，不擅自更换用户正在使用的 DNS。
        return if (DohEndpointPolicy.isValid(activeUrl) && result.none { it.url == activeUrl }) {
            val existingIds = result.map { it.id }.toSet()
            var id = "legacy:$activeUrl"
            while (id in existingIds) id += ":migrated"
            result + DohServer(id, "原有 DoH 配置", activeUrl, "从旧版本保留的服务器")
        } else result
    }
}

/** 首选运行期已恢复的端点，其次用户配置，再按内置顺序提供有限的故障切换候选。 */
internal fun dohCandidateUrls(
    activeUrl: String,
    runtimeUrl: String?,
    servers: List<DohServer>,
    limit: Int = 3,
): List<String> = (listOfNotNull(runtimeUrl, activeUrl) + servers.map(DohServer::url))
    .map(String::trim)
    .filter(DohEndpointPolicy::isValid)
    .distinct()
    .take(limit)
