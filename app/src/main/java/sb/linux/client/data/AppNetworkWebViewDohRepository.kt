package sb.linux.client.data

import sb.linux.client.domain.network.WebViewDohNetworkRepository
import sb.linux.client.domain.network.WebViewDohPolicy
import sb.linux.client.domain.network.WebViewDohTunnel

/** Supplies WebView with the same active proxy / DoH policy used by app networking. */
internal class AppNetworkWebViewDohRepository : WebViewDohNetworkRepository {
    override fun currentPolicy(): WebViewDohPolicy {
        val proxy = AppNetwork.proxyConfig()
        val context = AppNetwork.appContext()
        return WebViewDohPolicy(
            proxyUrl = proxy.proxyUrl.takeIf { proxy.enabled },
            dohActive = !proxy.enabled && AppNetwork.isDohActive(),
            dohUrl = context?.let { AppSettings(it).dohUrl }.orEmpty(),
        )
    }

    override fun openTunnel(): WebViewDohTunnel = LocalDnsTunnel(AppNetwork.dns)
}
