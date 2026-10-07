package sb.linux.client.domain.network

data class DetectedProxy(val type: Int, val host: String, val port: Int)

interface ProxyDetectionRepository {
    suspend fun detect(preferredPort: Int? = null): DetectedProxy?
}
