package sb.linux.client.common.network

object Endpoints {
    const val BASE = "https://linux.sb"
    const val HOST = "linux.sb"

    /**
     * 站内地址一律升级为 HTTPS：应用内 WebView 的 DoH 只代理 HTTPS，
     * 明文站内链接会绕过 DoH（同时被 cleartext 策略拦截）。
     */
    fun abs(url: String): String {
        val raw = url.trim()
        if (raw.isEmpty()) return BASE
        // 协议相对写法按站内 HTTPS 处理
        if (raw.startsWith("//")) return "https:$raw"
        if (raw.startsWith("https://", ignoreCase = true)) return raw
        if (raw.startsWith("http://", ignoreCase = true)) {
            val rest = raw.substring(7)
            val host = rest.substringBefore('/').substringBefore('?').substringBefore('#')
                .substringAfterLast('@').substringBefore(':')
            return if (isInternal(host)) "https://$rest" else raw
        }
        return BASE + raw
    }

    /** Matches the forum host and its subdomains without accepting lookalike suffixes. */
    fun isInternal(host: String?): Boolean {
        val value = host?.trim()?.lowercase().orEmpty()
        return value == HOST || value.endsWith(".$HOST")
    }
}
