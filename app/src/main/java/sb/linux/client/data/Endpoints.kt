package sb.linux.client.data

object Endpoints {
    const val BASE = "https://linux.sb"
    fun abs(u: String): String = if (u.startsWith("http")) u else BASE + u
}
