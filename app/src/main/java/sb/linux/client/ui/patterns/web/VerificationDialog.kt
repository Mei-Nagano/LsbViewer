package sb.linux.client.ui.patterns.web

import android.net.Uri
import android.webkit.CookieManager
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import sb.linux.client.common.network.AccessVerificationPage
import sb.linux.client.common.network.Endpoints
import sb.linux.client.common.network.ForumUserAgent
import sb.linux.client.platform.webview.WebViewDoh
import sb.linux.client.ui.components.feedback.AppInlineLoading
import sb.linux.client.ui.theme.LocalAppSpacing

@Composable
fun VerificationDialog(
    url: String,
    prepareCookies: () -> Unit = {},
    onSucceeded: () -> Unit,
    onCancel: () -> Unit,
) {
    var finished by remember(url) { mutableStateOf(false) }
    var released by remember(url) { mutableStateOf(false) }
    var pageLoading by remember(url) { mutableStateOf(true) }
    var error by remember(url) { mutableStateOf<String?>(null) }
    var browser by remember(url) { mutableStateOf<WebView?>(null) }
    var pendingLoad by remember(url) { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    val succeeded by rememberUpdatedState(onSucceeded)
    val cancel = {
        if (!finished) {
            finished = true
            onCancel()
        }
    }
    Dialog(
        onDismissRequest = cancel,
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
        ),
    ) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.fillMaxWidth().padding(LocalAppSpacing.current.pageHorizontal),
        ) {
            Column(Modifier.padding(LocalAppSpacing.current.pageHorizontal)) {
                Text("安全验证", style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(6.dp))
                Text("请在源站页面完成验证，通过后会自动继续。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))
                Box(Modifier.fillMaxWidth().heightIn(min = 300.dp, max = 480.dp)) {
                    AndroidView(
                        factory = { ctx ->
                            WebView(ctx).apply {
                                browser = this
                                released = false
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                settings.cacheMode = WebSettings.LOAD_NO_CACHE
                                settings.loadWithOverviewMode = true
                                settings.useWideViewPort = true
                                settings.userAgentString = ForumUserAgent.DEFAULT
                                CookieManager.getInstance().setAcceptCookie(true)
                                prepareCookies()
                                var navigation = 0
                                var failed = false
                                lateinit var poll: Runnable
                                poll = Runnable {
                                    if (finished || released || failed) return@Runnable
                                    val uri = Uri.parse(this.url.orEmpty())
                                    if (uri.scheme != "https" || !Endpoints.isForumHost(uri.host)) {
                                        error = "验证页面未能打开，请重试。"
                                        return@Runnable
                                    }
                                    val currentNavigation = navigation
                                    evaluateJavascript(AccessVerificationPage.STATE_SCRIPT) { state ->
                                        if (finished || released || failed || currentNavigation != navigation) return@evaluateJavascript
                                        if (state == "\"ready\"") {
                                            finished = true
                                            succeeded()
                                        } else {
                                            postDelayed(poll, 1000)
                                        }
                                    }
                                }
                                webViewClient = object : WebViewClient() {
                                    override fun onPageStarted(view: WebView, pageUrl: String, favicon: android.graphics.Bitmap?) {
                                        navigation++
                                        view.removeCallbacks(poll)
                                        failed = false
                                        pageLoading = true
                                        error = null
                                    }

                                    override fun onReceivedError(view: WebView, request: WebResourceRequest, failure: WebResourceError) {
                                        if (request.isForMainFrame && !finished) {
                                            failed = true
                                            pageLoading = false
                                            view.removeCallbacks(poll)
                                            error = "验证页面加载失败（" + failure.errorCode + "），请检查网络后重试。"
                                        }
                                    }

                                    override fun onReceivedHttpError(view: WebView, request: WebResourceRequest, response: WebResourceResponse) {
                                        // Cloudflare 的 403/503 挑战仍需完整显示，但不能被当作通过。
                                        if (request.isForMainFrame) failed = response.statusCode >= 400
                                    }

                                    override fun onPageFinished(view: WebView, pageUrl: String) {
                                        if (finished || released) return
                                        pageLoading = false
                                        view.removeCallbacks(poll)
                                        if (!failed) view.post(poll)
                                    }
                                }
                                // 必须由浏览器取得真实响应；缓存 HTML 不是一次源站导航。
                                pendingLoad = WebViewDoh.load(this, url)
                            }
                        },
                        onRelease = { view ->
                            released = true
                            pendingLoad?.cancel()
                            pendingLoad = null
                            browser = null
                            view.stopLoading()
                            view.webViewClient = WebViewClient()
                            view.destroy()
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                    if (pageLoading) AppInlineLoading(Modifier.align(Alignment.Center), size = 28.dp)
                }
                error?.let {
                    Spacer(Modifier.height(6.dp))
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = {
                        browser?.let { view ->
                            pageLoading = true
                            error = null
                            pendingLoad?.cancel()
                            pendingLoad = WebViewDoh.load(view, url)
                        }
                    }) { Text("重新加载") }
                    TextButton(onClick = cancel) { Text("取消") }
                }
            }
        }
    }
}
