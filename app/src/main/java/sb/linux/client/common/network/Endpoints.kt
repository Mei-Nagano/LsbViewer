package sb.linux.client.common.network

object Endpoints {
    const val BASE = "https://linux.sb"
    const val HOST = "linux.sb"

    fun abs(url: String): String = if (url.startsWith("http")) url else BASE + url

    /** Matches the forum host and its subdomains without accepting lookalike suffixes. */
    fun isInternal(host: String?): Boolean {
        val value = host?.trim()?.lowercase().orEmpty()
        return value == HOST || value.endsWith(".$HOST")
    }
}
