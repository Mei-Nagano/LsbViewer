package sb.linux.client.platform.verification

import android.content.Context
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
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
    private val loadWebView: (WebView, String) -> Unit,
) : VerificationWebRepository {
    override suspend fun solveUam(request: UamVerificationWebRequest): UamVerificationWebResult =
        withContext(Dispatchers.Main) {
            var webView: WebView? = null
            try {
                val view = WebView(context.applicationContext)
                webView = view
                view.settings.javaScriptEnabled = true
                view.settings.domStorageEnabled = true
                view.settings.cacheMode = WebSettings.LOAD_DEFAULT
                view.settings.userAgentString = request.userAgent
                CookieManager.getInstance().setAcceptCookie(true)
                exportCookies(request.cookies)
                view.webViewClient = WebViewClient()
                loadWebView(view, request.url)
                val passed = withTimeoutOrNull(30_000) { waitForUamPass(view, request.challengeScript) } ?: false
                UamVerificationWebResult(passed, importCookies(request.url))
            } catch (_: Exception) {
                UamVerificationWebResult(false, null)
            } finally {
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

    private suspend fun waitForUamPass(webView: WebView, challengeScript: String): Boolean {
        for (attempt in 0 until 25) {
            delay(1000)
            val result = CompletableDeferred<String>()
            runCatching {
                webView.evaluateJavascript(challengeScript) { value -> result.complete(value ?: "true") }
            }.onFailure { result.complete("true") }
            val value = withTimeoutOrNull(2000) { result.await() } ?: "true"
            if (!value.contains("true", ignoreCase = true)) return true
            if (attempt == 12) runCatching { webView.reload() }
        }
        return false
    }
}
