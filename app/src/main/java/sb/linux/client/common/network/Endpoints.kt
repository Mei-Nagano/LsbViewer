package sb.linux.client.common.network

object Endpoints {
    val sourceHosts = listOf("linux.sb", "bing.you")
    @Volatile private var hostProvider: () -> String = { "linux.sb" }
    val HOST: String get() = normalizeHost(hostProvider())
    val BASE: String get() = "https://$HOST"

    fun configure(provider: () -> String) { hostProvider = provider }
    fun normalizeHost(host: String): String = host.trim().lowercase().takeIf { it in sourceHosts } ?: "linux.sb"
    fun isForumHost(host: String?): Boolean = host?.lowercase()?.removePrefix("www.") in sourceHosts

    /**
     * 站内地址一律升级为 HTTPS：应用内 WebView 的 DoH 只代理 HTTPS，
     * 明文站内链接会绕过 DoH（同时被 cleartext 策略拦截）。
     */
    fun abs(url: String): String {
        val raw = url.trim()
        if (raw.isEmpty()) return BASE
        // 协议相对写法按站内 HTTPS 处理
        if (raw.startsWith("//")) return abs("https:$raw")
        if (raw.startsWith("https://", true) || raw.startsWith("http://", true)) {
            val uri = runCatching { java.net.URI(raw) }.getOrNull() ?: return raw
            if (isForumHost(uri.host) && uri.rawUserInfo == null) {
                return "https://$HOST" + (uri.rawPath ?: "") +
                    (uri.rawQuery?.let { "?$it" } ?: "") + (uri.rawFragment?.let { "#$it" } ?: "")
            }
            return if (uri.scheme.equals("http", true) && isInternal(uri.host))
                "https:" + raw.substringAfter(':') else raw
        }
        return BASE + raw
    }

    /** Matches the forum host and its subdomains without accepting lookalike suffixes. */
    fun isInternal(host: String?): Boolean {
        val value = host?.trim()?.lowercase().orEmpty()
        return sourceHosts.any { value == it || value.endsWith(".$it") }
    }
}
