package sb.linux.client.data

import sb.linux.client.domain.network.DetectedProxy
import sb.linux.client.domain.network.ProxyDetectionRepository

internal class AppProxyDetectionRepository : ProxyDetectionRepository {
    override suspend fun detect(preferredPort: Int?): DetectedProxy? =
        AppNetwork.detectLocalProxy(preferredPort)?.let { DetectedProxy(it.type, it.host, it.port) }
}
