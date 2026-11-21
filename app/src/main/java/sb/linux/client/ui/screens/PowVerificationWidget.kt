package sb.linux.client.ui.screens

import android.graphics.Color
import android.webkit.CookieManager
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import sb.linux.client.common.network.ForumUserAgent
import sb.linux.client.domain.account.LoginVerification
import sb.linux.client.domain.topic.SourceResourceVerificationRequired
import sb.linux.client.platform.verification.AccountVerificationGateway
import sb.linux.client.platform.webview.WebViewDoh
import java.io.ByteArrayInputStream
import java.io.IOException

/** 源站原组件负责交互和计算，应用只接收结果。 */
@Composable
internal fun PowVerificationWidget(
    verification: LoginVerification.Pow,
    gateway: AccountVerificationGateway,
    onProof: (String) -> Unit,
    onRefresh: (() -> Unit)? = null,
    revision: Int = 0,
) {
    var reload by remember(verification) { mutableIntStateOf(0) }
    var recoveryAttempted by remember(verification) { mutableStateOf(false) }
    val currentProof by rememberUpdatedState(onProof)
    key(verification, reload) {
        val scope = rememberCoroutineScope()
        var browser by remember { mutableStateOf<WebView?>(null) }
        var pendingLoad by remember { mutableStateOf<Job?>(null) }
        var loading by remember { mutableStateOf(true) }
        var recovering by remember { mutableStateOf(false) }
        var error by remember { mutableStateOf<String?>(null) }
        var height by remember { mutableIntStateOf(100) }
        var disposed by remember { mutableStateOf(false) }
        var appliedRevision by remember { mutableIntStateOf(revision) }
        LaunchedEffect(revision, loading) {
            if (!loading && appliedRevision != revision) {
                appliedRevision = revision
                currentProof("")
                browser?.evaluateJavascript("window.bbsHumanVerification&&window.bbsHumanVerification.reset(document.querySelector('form[data-lsb-pow-form]'))", null)
            }
        }
        LaunchedEffect(Unit) { currentProof("") }
        DisposableEffect(Unit) {
            onDispose {
                disposed = true
                pendingLoad?.cancel()
                currentProof("")
                browser?.apply {
                    stopLoading()
                    removeJavascriptInterface("LsbPowStatus")
                    destroy()
                }
                browser = null
            }
        }
        Column {
            Box(Modifier.fillMaxWidth().height(height.dp)) {
                AndroidView(
                    factory = { context ->
                        WebView(context).apply {
                            browser = this
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
                            fun failed(message: String) { post {
                                if (!disposed && !recovering) {
                                    loading = false
                                    error = message
                                    currentProof("")
                                }
                            } }
                            fun recover(challenge: SourceResourceVerificationRequired) { post {
                                if (disposed || recovering) return@post
                                if (recoveryAttempted) {
                                    failed("安全验证后组件仍被拦截，请重新加载")
                                    return@post
                                }
                                recoveryAttempted = true
                                recovering = true
                                loading = true
                                error = null
                                currentProof("")
                                scope.launch {
                                    try {
                                        if (gateway.recoverVerification(challenge)) {
                                            if (!disposed) reload++
                                        } else {
                                            error = "安全验证未完成，请重新加载"
                                        }
                                    } catch (cancelled: CancellationException) { throw cancelled
                                    } catch (_: Exception) {
                                        error = "安全验证未能完成，请重新加载"
                                    } finally {
                                        recovering = false
                                        loading = false
                                    }
                                }
                            } }
                            addJavascriptInterface(PowStatusBridge(onChange = { ready, result, contentHeight ->
                                post {
                                    if (!disposed && !recovering) {
                                        if (ready) { loading = false; error = null }
                                        if (contentHeight > 0) height = contentHeight.coerceIn(64, 500)
                                        currentProof(result)
                                    }
                                }
                            }, onError = ::failed), "LsbPowStatus")
                            fun coreResource(request: WebResourceRequest): Boolean {
                                val path = request.url.path.orEmpty()
                                return path == "/human_verification_refresh" ||
                                    path.startsWith("/app/") && (path.endsWith(".js") || path.endsWith(".mjs") || path.endsWith(".css"))
                            }
                            fun unavailable() = WebResourceResponse("text/plain", "UTF-8", 502, "Resource unavailable",
                                emptyMap(), ByteArrayInputStream(ByteArray(0)))
                            webViewClient = object : WebViewClient() {
                                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean = true
                                override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? =
                                    try { gateway.loadPowResource(request) }
                                    catch (challenge: SourceResourceVerificationRequired) {
                                        recover(challenge)
                                        unavailable()
                                    } catch (_: IOException) {
                                        if (coreResource(request)) failed("验证资源加载失败：" + request.url.lastPathSegment)
                                        unavailable()
                                    }
                                override fun onReceivedError(view: WebView, request: WebResourceRequest, cause: WebResourceError) {
                                    if (request.isForMainFrame || coreResource(request)) failed("验证组件加载失败（" + cause.errorCode + "），请重新加载")
                                }
                                override fun onReceivedHttpError(view: WebView, request: WebResourceRequest, response: WebResourceResponse) {
                                    if (coreResource(request)) failed("验证资源返回 HTTP " + response.statusCode + "，请重新加载")
                                }
                            }
                            pendingLoad = WebViewDoh.loadHtml(this, verification.pageUrl, verification.widgetHtml()) {
                                failed("验证页面的网络设置未能生效，请检查 DoH 或代理设置")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }
            if (loading) Text(if (recovering) "请先完成源站安全验证…" else "人机验证加载中…",
                style = MaterialTheme.typography.bodySmall)
            error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = {
                    currentProof("")
                    recoveryAttempted = false
                    if (onRefresh != null) onRefresh() else reload++
                }) { Text("重新加载人机验证") }
            }
        }
    }
}
