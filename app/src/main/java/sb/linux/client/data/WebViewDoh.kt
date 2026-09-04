package sb.linux.client.data

import android.content.Context
import android.webkit.WebView
import androidx.webkit.ProxyConfig
import androidx.webkit.ProxyController
import androidx.webkit.WebViewFeature
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

/** 所有在线 WebView 在加载前等待同一份代理设置生效；不用 GET 拦截伪装 DNS。 */
object WebViewDoh {
    private val scope = MainScope()
    private val mutex = Mutex()
    @Volatile private var context: Context? = null
    private var tunnel: LocalDnsTunnel? = null
    private var applied: String? = null
    private var overriding = false

    fun load(view: WebView, url: String) {
        context = view.context.applicationContext
        scope.launch {
            if (prepareSafely()) view.loadUrl(url)
            else view.loadData("<html><meta charset='utf-8'><body>网页网络设置暂不可用，请更新系统 WebView 后重试，或关闭 DoH。</body></html>", "text/html", "utf-8")
        }
    }

    fun refreshIfInitialized() {
        if (context != null) scope.launch { prepareSafely() }
    }

    /**
     * MainScope 里逃出去的异常会走默认处理器，在 Android 上直接杀进程。
     * 设置变更和网络回调都会走到这里，所以这一层必须兜住所有 Throwable。
     */
    private suspend fun prepareSafely(): Boolean = try { prepare() }
    catch (e: kotlinx.coroutines.CancellationException) { throw e }
    catch (_: Throwable) { false }

    private suspend fun prepare(): Boolean = mutex.withLock {
        val ctx = context ?: return@withLock true
        val proxy = AppNetwork.proxyConfig()
        val active = !proxy.enabled && AppNetwork.isDohActive()
        val key = when {
            proxy.enabled -> "proxy:${proxy.proxyUrl}"
            active -> AppSettings(ctx).dohUrl
            else -> "off"
        }
        if (key == applied) return@withLock true
        try {
            if (!WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)) return@withLock !active && !proxy.enabled
            if (proxy.enabled) {
                suspendCancellableCoroutine<Unit> { continuation ->
                    ProxyController.getInstance().setProxyOverride(
                        ProxyConfig.Builder().addProxyRule(proxy.proxyUrl).build(),
                        androidx.core.content.ContextCompat.getMainExecutor(ctx),
                    ) { if (continuation.isActive) continuation.resume(Unit) }
                }
                overriding = true
                withContext(Dispatchers.IO) { tunnel?.close() }
                tunnel = null
            } else if (active) {
                val bridge = tunnel ?: withContext(Dispatchers.IO) { LocalDnsTunnel(AppNetwork.dns) }.also { tunnel = it }
                suspendCancellableCoroutine<Unit> { continuation ->
                    ProxyController.getInstance().setProxyOverride(
                        ProxyConfig.Builder().addProxyRule("http://127.0.0.1:${bridge.port}", ProxyConfig.MATCH_HTTPS).build(),
                        androidx.core.content.ContextCompat.getMainExecutor(ctx),
                    ) { if (continuation.isActive) continuation.resume(Unit) }
                }
                overriding = true
            } else if (overriding) {
                suspendCancellableCoroutine<Unit> { continuation ->
                    ProxyController.getInstance().clearProxyOverride(androidx.core.content.ContextCompat.getMainExecutor(ctx)) {
                        if (continuation.isActive) continuation.resume(Unit)
                    }
                }
                overriding = false
                withContext(Dispatchers.IO) { tunnel?.close() }
                tunnel = null
            }
            applied = key
            true
        } catch (e: kotlinx.coroutines.CancellationException) { throw e }
        catch (_: Exception) { false }
    }
}
