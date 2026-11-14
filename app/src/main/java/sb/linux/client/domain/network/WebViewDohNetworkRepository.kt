package sb.linux.client.domain.network

import java.io.Closeable

data class WebViewDohPolicy(
    val proxyUrl: String?,
    val dohActive: Boolean,
    val dohUrl: String,
    val forWebsites: Boolean = true,
)

interface WebViewDohTunnel : Closeable {
    val port: Int
}

interface WebViewDohNetworkRepository {
    suspend fun awaitReady()
    fun currentPolicy(): WebViewDohPolicy
    fun openTunnel(): WebViewDohTunnel
}
