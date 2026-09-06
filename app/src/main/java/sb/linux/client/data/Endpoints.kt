package sb.linux.client.data

object Endpoints {
    const val BASE = "https://linux.sb"
    const val HOST = "linux.sb"
    fun abs(u: String): String = if (u.startsWith("http")) u else BASE + u

    /**
     * 站内 host 判定：linux.sb 本域与其子域名（www、cdn 等）。
     * 必须用后缀点号比较，"linux.sb.example.com" 这类同前缀外部域名不能算站内。
     */
    fun isInternal(host: String?): Boolean {
        val value = host?.trim()?.lowercase().orEmpty()
        return value == HOST || value.endsWith(".$HOST")
    }
}
