package sb.linux.client.platform.webview

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
import sb.linux.client.domain.network.WebViewDohNetworkRepository
import sb.linux.client.domain.network.WebViewDohTunnel

/** 所有在线 WebView 在加载前等待同一份代理设置生效；不用 GET 拦截伪装 DNS。 */
object WebViewDoh {
    private val scope = MainScope()
    private val mutex = Mutex()
    @Volatile private var context: Context? = null
    @Volatile private var networkRepository: WebViewDohNetworkRepository? = null
    private var tunnel: WebViewDohTunnel? = null
    private var applied: String? = null
    private var overriding = false

    fun configure(repository: WebViewDohNetworkRepository) {
        networkRepository = repository
    }

    fun load(view: WebView, url: String) {
        context = view.context.applicationContext
        scope.launch {
            if (prepareSafely()) view.loadUrl(url)
            else view.loadData("<html><meta charset='utf-8'><body>网页网络设置暂不可用，请更新系统 WebView 后重试，或关闭 DoH。</body></html>", "text/html", "utf-8")
        }
    }

    /** 在应用联网策略准备完成后加载内嵌页面，保留 base URL 供跨域组件校验来源。 */
    fun loadHtml(view: WebView, baseUrl: String, html: String) {
        context = view.context.applicationContext
        scope.launch {
            if (prepareSafely()) view.loadDataWithBaseURL(baseUrl, html, "text/html", "utf-8", baseUrl)
            else view.loadData("<html><meta charset='utf-8'><body>网页网络设置暂不可用，请更新系统 WebView 后重试，或关闭 DoH。</body></html>", "text/html", "utf-8")
        }
    }

    /** 视频域名绕过 DoH 隧道，其他网页仍等待同一份网络策略。 */
    fun loadDirect(view: WebView, url: String, headers: Map<String, String> = emptyMap()) {
        context = view.context.applicationContext
        scope.launch {
            if (prepareSafely()) {
                if (headers.isEmpty()) view.loadUrl(url) else view.loadUrl(url, headers)
            }
            else view.loadData(
                "<html><meta charset='utf-8'><body>播放器网络设置暂不可用，请更新系统 WebView 后重试。</body></html>",
                "text/html",
                "utf-8",
            )
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

    private suspend fun prepare(): Boolean = mutex.withLock { prepareLocked() }

    private suspend fun prepareLocked(): Boolean {
        val ctx = context ?: return true
        val repository = networkRepository ?: return false
        val policy = repository.currentPolicy()
        val proxyUrl = policy.proxyUrl
        val active = policy.dohActive
        val key = when {
            proxyUrl != null -> "proxy:$proxyUrl"
            active -> "doh:${policy.dohUrl}:video-bypass-v4"
            else -> "off"
        }
        if (key == applied) return true
        return try {
            // 无法执行启用的网络策略时停止加载，避免悄悄改用系统 DNS。
            if (!WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)) return proxyUrl == null && !active
            if (proxyUrl != null) {
                suspendCancellableCoroutine<Unit> { continuation ->
                    ProxyController.getInstance().setProxyOverride(
                        ProxyConfig.Builder().addProxyRule(proxyUrl).build(),
                        androidx.core.content.ContextCompat.getMainExecutor(ctx),
                    ) { if (continuation.isActive) continuation.resume(Unit) }
                }
                overriding = true
                withContext(Dispatchers.IO) { tunnel?.close() }
                tunnel = null
            } else if (active) {
                // 配置变化后断开旧连接，后续连接使用当前 DNS 策略。
                if (tunnel != null && key != applied) {
                    withContext(Dispatchers.IO) { tunnel?.close() }
                    tunnel = null
                }
                val bridge = tunnel ?: withContext(Dispatchers.IO) { repository.openTunnel() }.also { tunnel = it }
                suspendCancellableCoroutine<Unit> { continuation ->
                    ProxyController.getInstance().setProxyOverride(
                        ProxyConfig.Builder()
                            .addProxyRule("http://127.0.0.1:${bridge.port}", ProxyConfig.MATCH_HTTPS)
                            .apply { VIDEO_DOH_BYPASS_RULES.forEach(::addBypassRule) }
                            .build(),
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

    /** 视频页面与媒体资源使用系统网络，避开 DoH 隧道。显式应用代理仍优先。 */
    private val VIDEO_DOH_BYPASS_RULES = listOf(
        "bilibili.com", "*.bilibili.com",
        "bilivideo.com", "*.bilivideo.com", "bilivideo.cn", "*.bilivideo.cn",
        "hdslb.com", "*.hdslb.com",
        "biliapi.com", "*.biliapi.com", "biliapi.net", "*.biliapi.net",
        "douyin.com", "*.douyin.com", "douyinvod.com", "*.douyinvod.com",
        "byteimg.com", "*.byteimg.com", "bytecdn.cn", "*.bytecdn.cn",
        "bytegoofy.com", "*.bytegoofy.com", "bytedance.com", "*.bytedance.com",
        "youtube.com", "*.youtube.com", "youtube-nocookie.com", "*.youtube-nocookie.com",
        "googlevideo.com", "*.googlevideo.com", "ytimg.com", "*.ytimg.com",
        "ggpht.com", "*.ggpht.com", "gstatic.com", "*.gstatic.com",
    )
}
