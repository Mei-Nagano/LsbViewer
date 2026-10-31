package sb.linux.client.ui.screens
import sb.linux.client.common.network.Endpoints

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavHostController
import sb.linux.client.ui.session.Session
import sb.linux.client.platform.webview.WebViewDoh
import sb.linux.client.platform.webview.LinkWindowClient
import sb.linux.client.ui.*
import sb.linux.client.ui.theme.*
import sb.linux.client.ui.content.*
import sb.linux.client.ui.patterns.topic.*
import sb.linux.client.ui.patterns.user.*
import sb.linux.client.ui.components.feedback.*
import sb.linux.client.ui.components.layout.*
import sb.linux.client.ui.components.navigation.*

/** 应用内 WebView 页面：常规设置选择「应用内打开」时，站内未映射页面与外部链接在此展示 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebScreen(
    session: Session, nav: NavHostController, sourceUrl: String? = null, title: String? = null,
    onBack: (() -> Unit)? = null,
    onLoaded: ((android.webkit.WebView, String) -> Unit)? = null,
    onOpenWindow: ((String) -> Unit)? = null,
) {
    val entry = nav.currentBackStackEntry ?: return
    val rawUrl = sourceUrl ?: entry.arguments?.getString("url").orEmpty()
    val url = sb.linux.client.common.network.Endpoints.abs(rawUrl)
    var pageTitle by remember { mutableStateOf(url) }
    // WebView 内页面可能发生跳转，记录当前实际地址供「在浏览器打开」使用
    var currentUrl by remember { mutableStateOf(url) }
    // 加载进度与主文档错误：只影响应用侧外壳，不改写源站 DOM
    var loading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var webView by remember { mutableStateOf<android.webkit.WebView?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val linkHandler = LocalLinkHandler.current
    val chooseFiles = rememberWebFileChooser()

    AppPageScaffold(
        title = title ?: pageTitle,
        onBack = { if (onBack != null) onBack() else nav.popBackStack() },
        actions = {
            // 在浏览器打开：调起系统浏览器查看当前页面
            IconButton(onClick = {
                runCatching {
                    context.startActivity(
                        android.content.Intent(
                            android.content.Intent.ACTION_VIEW,
                            android.net.Uri.parse(currentUrl)
                        )
                    )
                }
            }) {
                Icon(Icons.Filled.OpenInNew, "在浏览器打开")
            }
        },
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                android.webkit.WebView(ctx).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.userAgentString = sb.linux.client.common.network.ForumUserAgent.DEFAULT
                    settings.setSupportMultipleWindows(true)
                    webChromeClient = LinkWindowClient(openLink = { target ->
                        if (onOpenWindow != null) onOpenWindow(target)
                        else if (linkHandler != null) linkHandler(target)
                        else nav.navigate("web?url=${android.net.Uri.encode(target)}")
                    }, chooseFiles = chooseFiles, onProgress = { progress ->
                        loading = progress < 100
                    })
                    // 注入已保存的登录会话，私信等需要登录的页面在应用内即可直接看到（避免跳登录拦截）
                    session.accountAuthenticationRepository.exportWebCookies()
                    webViewClient = object : android.webkit.WebViewClient() {
                        override fun onPageStarted(view: android.webkit.WebView, url: String, favicon: android.graphics.Bitmap?) {
                            loading = true
                            loadError = null
                        }

                        override fun onPageFinished(view: android.webkit.WebView, url: String) {
                            loading = false
                            onLoaded?.invoke(view, url)
                        }

                        override fun onReceivedError(
                            view: android.webkit.WebView,
                            request: android.webkit.WebResourceRequest,
                            error: android.webkit.WebResourceError,
                        ) {
                            if (request.isForMainFrame) {
                                loading = false
                                loadError = error.description?.toString()?.takeIf { it.isNotBlank() }
                                    ?: "网页加载失败"
                            }
                        }
                        override fun shouldOverrideUrlLoading(
                            view: android.webkit.WebView,
                            request: android.webkit.WebResourceRequest,
                        ): Boolean {
                            val destination = request.url
                            val fromSource = Endpoints.isInternal(android.net.Uri.parse(view.url.orEmpty()).host)
                            if (request.isForMainFrame && request.hasGesture() && fromSource &&
                                destination.scheme in setOf("http", "https") && !Endpoints.isInternal(destination.host) && linkHandler != null
                            ) {
                                linkHandler(destination.toString())
                                return true
                            }
                            return false
                        }
                        override fun doUpdateVisitedHistory(
                            view: android.webkit.WebView?,
                            u: String?,
                            isReload: Boolean,
                        ) {
                            pageTitle = view?.title?.takeIf { it.isNotBlank() } ?: u ?: url
                            currentUrl = u ?: url
                        }
                    }
                    WebViewDoh.load(this, url)
                }.also { webView = it }
            },
            modifier = Modifier.fillMaxSize(),
        )
            if (loading) {
                LinearProgressIndicator(Modifier.fillMaxWidth().align(Alignment.TopCenter))
            }
            loadError?.let { message ->
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.align(Alignment.TopCenter).padding(12.dp),
                ) {
                    Row(
                        Modifier.padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = { loadError = null; webView?.reload() }) { Text("重试") }
                    }
                }
            }
        }
    }
}

