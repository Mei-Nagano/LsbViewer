package sb.linux.client.data

import android.content.Context
import android.net.ConnectivityManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async

/** 启动和回到前台识别代理；请求等待同一份结果，不阻塞 UI 线程。 */
internal object AppProxyDiscovery {
    @Volatile private var pending: Deferred<Unit>? = null

    @Synchronized fun refresh(context: Context, scope: CoroutineScope) {
        if (pending?.isActive == true) return
        val work = scope.async(start = CoroutineStart.LAZY) {
            val settings = AppSettings(context)
            val before = AppProxyConfig(settings.proxyType, settings.proxyHost, settings.proxyPort)
            val wasAutomatic = settings.proxyAutoDetected
            val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val system = runCatching { manager?.defaultProxy }.getOrNull()?.let { proxy ->
                AppProxyConfig(1, proxy.host.orEmpty(), proxy.port).takeIf { it.enabled }
            }
            val found = system ?: AppNetwork.detectLocalProxy(before.port)
            // 不覆盖识别期间用户手动保存的配置。
            val now = AppProxyConfig(settings.proxyType, settings.proxyHost, settings.proxyPort)
            if (now == before && settings.proxyAutoDetected == wasAutomatic) {
                if (found != null) settings.saveProxy(found.type, found.host, found.port, automaticallyDetected = true)
                else if (wasAutomatic) settings.saveProxy(0, before.host, before.port)
            }
        }
        pending = work
        work.start()
    }

    suspend fun awaitReady() { pending?.await() }
}
