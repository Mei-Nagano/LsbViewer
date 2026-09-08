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

    /** 在应用联网策略准备完成后加载内嵌页面，保留 base URL 供跨域组件校验来源。 */
    fun loadHtml(view: WebView, baseUrl: String, html: String) {
        context = view.context.applicationContext
        scope.launch {
            if (prepareSafely()) view.loadDataWithBaseURL(baseUrl, html, "text/html", "utf-8", baseUrl)
            else view.loadData("<html><meta charset='utf-8'><body>网页网络设置暂不可用，请更新系统 WebView 后重试，或关闭 DoH。</body></html>", "text/html", "utf-8")
        }
    }

    /** 视频域名已在 DoH 代理配置中按域名直连；其他 WebView 仍保持原有网络策略。 */
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
        val proxy = AppNetwork.proxyConfig()
        val active = !proxy.enabled && AppNetwork.isDohActive()
        val key = when {
            proxy.enabled -> "proxy:${proxy.proxyUrl}"
            active -> "doh:${AppSettings(ctx).dohUrl}:video-bypass-v2"
            else -> "off"
        }
        if (key == applied) return true
        return try {
            // 部分旧版系统 WebView 没有进程级代理能力。DoH 本身允许系统 DNS 兜底，
            // 此处也继续直连，让验证码至少可以显示；显式代理配置则不能静默绕过。
            if (!WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)) return !proxy.enabled
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

    /** 视频页面和媒体分片绕过论坛 DoH 隧道，交给系统网络、VPN或系统级代理。 */
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
