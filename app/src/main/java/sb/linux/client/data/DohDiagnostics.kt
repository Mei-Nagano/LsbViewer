package sb.linux.client.data

import java.net.InetAddress
import java.net.UnknownHostException

/** DoH 连通性诊断：直连所选解析器，避免系统 DNS 回退造成误报。 */
object DohDiagnostics {
    fun lookup(host: String = "linux.sb"): List<InetAddress> {
        val context = AppNetwork.appContext() ?: throw UnknownHostException("网络组件尚未初始化")
        val settings = AppSettings(context)
        check(settings.dohEnabled) { "DoH 尚未启用" }
        check(!settings.dohDisableOnVpn || !AppNetwork.isVpnActive(context)) {
            "VPN 已开启，当前按设置停用 DoH；未发送测试请求"
        }
        val url = settings.dohUrl.trim()
        require(DohEndpointPolicy.isValid(url)) { "请输入有效的 HTTPS DoH 地址" }
        return AppNetwork.resolver(url).lookup(host)
    }
}
