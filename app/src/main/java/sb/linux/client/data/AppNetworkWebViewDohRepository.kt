package sb.linux.client.data

import sb.linux.client.domain.network.WebViewDohNetworkRepository
import sb.linux.client.domain.network.WebViewDohPolicy
import sb.linux.client.domain.network.WebViewDohTunnel

/** Supplies WebView with the same active proxy / DoH policy used by app networking. */
internal class AppNetworkWebViewDohRepository : WebViewDohNetworkRepository {
    override suspend fun awaitReady() = AppNetwork.awaitProxyDetection()
    override fun currentPolicy(): WebViewDohPolicy {
        val proxy = AppNetwork.proxyConfig()
        val context = AppNetwork.appContext()
        return WebViewDohPolicy(
            proxyUrl = proxy.proxyUrl.takeIf { proxy.enabled },
            dohActive = !proxy.enabled && AppNetwork.isDohActive(),
            dohUrl = context?.let { AppSettings(it).dohUrl }.orEmpty(),
            forWebsites = context?.let { AppSettings(it).dohForWebsites } ?: true,
        )
    }

    override fun openTunnel(): WebViewDohTunnel = LocalDnsTunnel(object : okhttp3.Dns {
      override fun lookup(hostname: String): List<java.net.InetAddress> {
        val policy = currentPolicy()
        return if (policy.dohActive && (sb.linux.client.common.network.Endpoints.isInternal(hostname) ||
            hostname.equals("challenges.cloudflare.com", ignoreCase = true) || policy.forWebsites))
            AppNetwork.dns.lookup(hostname) else okhttp3.Dns.SYSTEM.lookup(hostname)
      }
    })
}
