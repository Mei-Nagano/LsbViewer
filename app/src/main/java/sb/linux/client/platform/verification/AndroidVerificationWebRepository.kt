package sb.linux.client.platform.verification

import android.content.Context
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebResourceError
import android.net.Uri
import sb.linux.client.common.network.AccessVerificationPage
import sb.linux.client.common.network.Endpoints
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import sb.linux.client.domain.account.UamVerificationWebRequest
import sb.linux.client.domain.account.UamVerificationWebResult
import sb.linux.client.domain.account.VerificationCookie
import sb.linux.client.domain.account.VerificationWebRepository

internal class AndroidVerificationWebRepository(
    private val context: Context,
    private val loadWebView: (WebView, String) -> kotlinx.coroutines.Job,
) : VerificationWebRepository {
    override suspend fun solveUam(request: UamVerificationWebRequest): UamVerificationWebResult =
        withContext(Dispatchers.Main) {
            var webView: WebView? = null
            var pendingLoad: kotlinx.coroutines.Job? = null
            try {
                val view = WebView(context.applicationContext)
                webView = view
                view.settings.javaScriptEnabled = true
                view.settings.domStorageEnabled = true
                view.settings.cacheMode = WebSettings.LOAD_DEFAULT
                view.settings.userAgentString = request.userAgent
                CookieManager.getInstance().setAcceptCookie(true)
                exportCookies(request.cookies)
                var navigation = 0
                var pageReady = false
                var failed = false
                view.webViewClient = object : WebViewClient() {
                    override fun onPageStarted(view: WebView, url: String, favicon: android.graphics.Bitmap?) {
                        navigation++
                        pageReady = false
                        failed = false
                    }
                    override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                        if (request.isForMainFrame) { failed = true; pageReady = false }
                    }
                    override fun onReceivedHttpError(view: WebView, request: WebResourceRequest, response: WebResourceResponse) {
                        if (request.isForMainFrame && response.statusCode >= 400) { failed = true; pageReady = false }
                    }
                    override fun onPageFinished(view: WebView, url: String) {
                        val uri = Uri.parse(url)
                        pageReady = !failed && uri.scheme == "https" && Endpoints.isForumHost(uri.host)
                    }
                }
                pendingLoad = loadWebView(view, request.url)
                val passed = withTimeoutOrNull(30_000) {
                    waitForUamPass(view, { pageReady }, { navigation })
                } ?: false
                UamVerificationWebResult(passed, importCookies(request.url))
            } catch (_: Exception) {
                UamVerificationWebResult(false, null)
            } finally {
                pendingLoad?.cancel()
                webView?.stopLoading()
                webView?.destroy()
            }
        }

    override fun exportCookies(cookies: List<VerificationCookie>): Boolean {
        return try {
            if (cookies.isEmpty()) return false
            val manager = CookieManager.getInstance()
            manager.setAcceptCookie(true)
            cookies.forEach { cookie ->
                val domain = cookie.domain.trimStart('.')
                val path = cookie.path.ifBlank { "/" }
                val pair = "${cookie.name}=${cookie.value}"
                runCatching { manager.setCookie("https://$domain$path", pair) }
                runCatching { manager.setCookie("http://$domain$path", pair) }
            }
            if (android.os.Build.VERSION.SDK_INT >= 21) runCatching { manager.flush() }
            true
        } catch (_: Exception) {
            false
        }
    }

    override fun importCookies(url: String): String? = CookieManager.getInstance().getCookie(url)

    override fun clearCookie(cookie: VerificationCookie) {
        val manager = CookieManager.getInstance()
        val domain = cookie.domain.trimStart('.')
        manager.setCookie("https://$domain/", "${cookie.name}=; Max-Age=0; Path=${cookie.path}; Domain=$domain; SameSite=Lax")
        manager.flush()
    }

    private suspend fun waitForUamPass(webView: WebView, pageReady: () -> Boolean, navigation: () -> Int): Boolean {
        repeat(25) {
            delay(1000)
            if (!pageReady()) return@repeat
            val currentNavigation = navigation()
            val result = CompletableDeferred<String>()
            webView.evaluateJavascript(AccessVerificationPage.STATE_SCRIPT) { value -> result.complete(value ?: "null") }
            val value = withTimeoutOrNull(2000) { result.await() }
            if (pageReady() && navigation() == currentNavigation && value == "\"ready\"") return true
        }
        return false
    }
}
