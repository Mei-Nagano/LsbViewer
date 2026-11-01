package sb.linux.client.ui.screens

import android.graphics.Color
import android.net.Uri
import android.webkit.CookieManager
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import sb.linux.client.ui.components.feedback.AppInlineLoading
import sb.linux.client.domain.account.LoginVerification
import sb.linux.client.platform.webview.WebViewDoh
import sb.linux.client.common.network.ForumUserAgent
import sb.linux.client.platform.verification.CapNetworkBridge
import sb.linux.client.platform.verification.CapVerificationGateway

/** 在原生表单中承载源站 CAP 组件，并只向上层返回一次性令牌。 */
@Composable
internal fun CapVerificationWidget(
    verification: sb.linux.client.domain.account.LoginVerification.Cap,
    revision: Int,
    gateway: CapVerificationGateway,
    onToken: (String) -> Unit,
    onStatus: (String) -> Unit,
    onError: (String) -> Unit,
) {
    val latestToken by rememberUpdatedState(onToken)
    val latestStatus by rememberUpdatedState(onStatus)
    val latestError by rememberUpdatedState(onError)
    var prepared by remember(verification, revision) {
        mutableStateOf(verification.takeIf { it.widgetScript.isNotBlank() })
    }
    var preparing by remember(verification, revision) { mutableStateOf(prepared == null) }
    LaunchedEffect(verification, revision) {
        if (prepared != null) return@LaunchedEffect
        try {
            prepared = gateway.prepare(verification)
        } catch (error: Exception) {
            latestError(error.message ?: "验证码资源加载失败")
        } finally {
            preparing = false
        }
    }
    val ready = prepared
    if (ready == null) {
        Box(Modifier.fillMaxWidth().height(82.dp), contentAlignment = Alignment.Center) {
            if (preparing) AppInlineLoading()
        }
        return
    }
    key(revision, ready) {
        var loading by remember { mutableStateOf(true) }
        var webView by remember { mutableStateOf<WebView?>(null) }
        var networkBridge by remember { mutableStateOf<CapNetworkBridge?>(null) }
        DisposableEffect(Unit) {
            onDispose {
                networkBridge?.close()
                networkBridge = null
                webView?.stopLoading()
                webView?.removeJavascriptInterface(CapNetworkBridge.NAME)
                webView?.destroy()
                webView = null
            }
        }
        Box(Modifier.fillMaxWidth().height(82.dp), contentAlignment = Alignment.Center) {
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        webView = this
                        setBackgroundColor(Color.TRANSPARENT)
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.cacheMode = WebSettings.LOAD_NO_CACHE
                        settings.allowFileAccess = false
                        settings.allowContentAccess = false
                        settings.userAgentString = ForumUserAgent.DEFAULT
                        CookieManager.getInstance().setAcceptCookie(true)
                        CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                        gateway.exportCookies()
                        networkBridge = gateway.createBridge(ready) { script ->
                            post { runCatching { evaluateJavascript(script, null) } }
                        }
                        addJavascriptInterface(networkBridge!!, CapNetworkBridge.NAME)
                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean =
                                handleCallback(request.url, latestToken, latestStatus, latestError)

                            override fun onPageFinished(view: WebView, url: String) {
                                loading = false
                                latestStatus("请完成人机验证")
                            }

                            override fun onReceivedError(
                                view: WebView,
                                request: WebResourceRequest,
                                error: WebResourceError,
                            ) {
                                if (request.isForMainFrame) latestError("验证码页面加载失败，请重试")
                            }
                        }
                        WebViewDoh.loadHtml(this, ready.pageUrl, ready.widgetHtml())
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )
            if (loading) AppInlineLoading(size = 32.dp)
        }
    }
}
