package sb.linux.client.data

import java.net.InetSocketAddress
import java.net.Proxy

data class AppProxyConfig(val type: Int, val host: String, val port: Int) {
    val enabled: Boolean get() = type in 1..2 && host.isNotBlank() && port in 1..65535
    val proxyUrl: String get() {
        val formattedHost = if (host.contains(':') && !host.startsWith('[')) "[$host]" else host
        return "${if (type == 2) "socks5" else "http"}://$formattedHost:$port"
    }

    fun asJavaProxy(): Proxy = Proxy(
        if (type == 2) Proxy.Type.SOCKS else Proxy.Type.HTTP,
        InetSocketAddress.createUnresolved(host, port),
    )
}
