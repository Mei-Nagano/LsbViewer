package sb.linux.client.ui.screens

import android.graphics.Color
import android.net.Uri
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import kotlinx.coroutines.delay
import sb.linux.client.ui.components.feedback.AppInlineLoading
import sb.linux.client.domain.account.LoginVerification
import sb.linux.client.platform.webview.WebViewDoh
import sb.linux.client.common.network.ForumUserAgent
import sb.linux.client.platform.verification.CapNetworkBridge
import sb.linux.client.platform.verification.CapVerificationGateway

/** 组件页通过它回传真实状态，避免只靠 `lsbcap://` 跳转判断是否加载成功。 */
private class CapStatusBridge(
    private val onReady: () -> Unit,
    private val onToken: (String) -> Unit,
    private val onError: (String) -> Unit,
) {
    @JavascriptInterface fun ready() = onReady()

    @JavascriptInterface fun token(token: String) = onToken(token)

    @JavascriptInterface fun error(message: String) = onError(message)

    companion object {
        const val NAME = "LsbCapStatus"
    }
}

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
        } catch (_: Exception) {
            // 本地资源准备失败时仍然交给组件页直接加载源站脚本，
            // 否则验证码区域会变成一块空白，用户看不到任何可操作内容。
            prepared = verification
        } finally {
            preparing = false
        }
    }
    val ready = prepared
    if (ready == null) {
        Box(Modifier.fillMaxWidth().height(82.dp), contentAlignment = Alignment.Center) {
            if (preparing) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AppInlineLoading(size = 18.dp)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "正在加载人机验证…",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        return
    }
    key(revision, ready) {
        // 加载中保持提示，直到组件页上报 ready / error；onPageFinished 只代表
        // HTML 文档结束，组件脚本和网络请求可能还在进行。
        var loading by remember { mutableStateOf(true) }
        var webView by remember { mutableStateOf<WebView?>(null) }
        var networkBridge by remember { mutableStateOf<CapNetworkBridge?>(null) }
        LaunchedEffect(Unit) {
            delay(12_000)
            if (loading) {
                loading = false
                latestError("人机验证加载超时，请重试")
            }
        }
        DisposableEffect(Unit) {
            onDispose {
                networkBridge?.close()
                networkBridge = null
                webView?.stopLoading()
                webView?.removeJavascriptInterface(CapNetworkBridge.NAME)
                webView?.removeJavascriptInterface(CapStatusBridge.NAME)
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
                        addJavascriptInterface(
                            CapStatusBridge(
                                onReady = {
                                    post {
                                        loading = false
                                        latestStatus("请完成人机验证")
                                    }
                                },
                                onToken = { token ->
                                    post {
                                        loading = false
                                        latestToken(token)
                                        latestStatus("验证完成")
                                    }
                                },
                                onError = { message ->
                                    post {
                                        loading = false
                                        latestError(message.ifBlank { "人机验证加载失败，请重试" })
                                    }
                                },
                            ),
                            CapStatusBridge.NAME,
                        )
                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean =
                                handleCallback(request.url, latestToken, latestStatus, latestError)

                            override fun onReceivedError(
                                view: WebView,
                                request: WebResourceRequest,
                                error: WebResourceError,
                            ) {
                                if (request.isForMainFrame) {
                                    loading = false
                                    latestError("验证码页面加载失败，请重试")
                                }
                            }
                        }
                        WebViewDoh.loadHtml(this, ready.pageUrl, ready.widgetHtml())
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )
            if (loading) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    AppInlineLoading(size = 20.dp)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "人机验证加载中…",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
