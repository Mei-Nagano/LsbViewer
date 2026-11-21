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
import sb.linux.client.platform.webview.WebViewWindowHost
import sb.linux.client.ui.*
import sb.linux.client.ui.theme.*
import sb.linux.client.ui.content.*
import sb.linux.client.ui.patterns.topic.*
import sb.linux.client.ui.patterns.user.*
import sb.linux.client.ui.components.feedback.*
import sb.linux.client.ui.components.layout.*
import sb.linux.client.ui.components.navigation.*
import sb.linux.client.ui.patterns.settings.SettingsScaffold
import sb.linux.client.ui.patterns.settings.SettingsCard

/** 应用内 WebView 页面：常规设置选择「应用内打开」时，站内未映射页面与外部链接在此展示 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebScreen(
    session: Session, nav: NavHostController, sourceUrl: String? = null, title: String? = null,
    onBack: (() -> Unit)? = null,
    onLoaded: ((android.webkit.WebView, String) -> Unit)? = null,
    settingsStyle: Boolean = false,
    initialContentSelector: String? = null,
) {
    val entry = nav.currentBackStackEntry ?: return
    val rawUrl = sourceUrl ?: entry.arguments?.getString("url").orEmpty()
    val url = sb.linux.client.common.network.Endpoints.abs(rawUrl)
    val initialPath = android.net.Uri.parse(url).path
    var pageTitle by remember { mutableStateOf(url) }
    // WebView 内页面可能发生跳转，记录当前实际地址供「在浏览器打开」使用
    var currentUrl by remember { mutableStateOf(url) }
    var canGoBack by remember { mutableStateOf(false) }
    // 加载进度与主文档错误：只影响应用侧外壳，不改写源站 DOM
    var loading by remember { mutableStateOf(true) }
    var loadProgress by remember { mutableIntStateOf(0) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var webView by remember { mutableStateOf<android.webkit.WebView?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val chooseFiles = rememberWebFileChooser()
    val windows = rememberSourceWebWindows()
    var initialLoadFinished by remember(url) { mutableStateOf(false) }
    androidx.activity.compose.BackHandler(enabled = canGoBack) { webView?.goBack() }

    WebPageScaffold(
        title = title ?: pageTitle,
        settingsStyle = settingsStyle,
        onBack = {
            if (windows.back()) Unit
            else if (webView?.canGoBack() == true) webView?.goBack()
            else if (onBack != null) onBack()
            else nav.popBackStack()
        },
        actions = {
            windows.WindowMenu()
            AppRefreshButton(onClick = {
                (windows.activeWebView ?: webView)?.let {
                    if (it === webView) { loading = true; loadProgress = 0; loadError = null }
                    it.reload()
                }
            }, enabled = webView != null, loading = windows.activeWebView == null && loading)
            // 在浏览器打开：调起系统浏览器查看当前页面
            IconButton(onClick = {
                runCatching {
                    context.startActivity(
                        android.content.Intent(
                            android.content.Intent.ACTION_VIEW,
                            android.net.Uri.parse(windows.activeWebView?.url ?: currentUrl)
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
                val primary = android.webkit.WebView(ctx).apply {
                    settings.javaScriptEnabled = true
                    settings.javaScriptCanOpenWindowsAutomatically = true
                    settings.domStorageEnabled = true
                    settings.userAgentString = sb.linux.client.common.network.ForumUserAgent.DEFAULT
                    settings.setSupportMultipleWindows(true)
                    val chrome = object : LinkWindowClient(showWindow = windows::show, closeWindow = windows::close,
                        showJsDialog = windows::showJsDialog,
                        chooseFiles = chooseFiles, onProgress = { progress ->
                        loadProgress = progress
                        loading = progress < 100
                    }, onTitle = windows::title, useNativeJsDialogs = true) {
                        override fun onConsoleMessage(message: android.webkit.ConsoleMessage): Boolean {
                            val line = message.message()
                            val source = android.net.Uri.parse(this@apply.url.orEmpty())
                            if (source.scheme == "https" && Endpoints.isForumHost(source.host) &&
                                line.startsWith(SOURCE_REWARD_TOAST_PREFIX)) {
                                val notice = try {
                                    org.json.JSONTokener(line.removePrefix(SOURCE_REWARD_TOAST_PREFIX)).nextValue() as? String
                                } catch (_: org.json.JSONException) { null }
                                if (!notice.isNullOrBlank()) session.showToast(notice)
                                return true
                            }
                            return super.onConsoleMessage(message)
                        }
                    }
                    webChromeClient = chrome
                    // 注入已保存的登录会话，私信等需要登录的页面在应用内即可直接看到（避免跳登录拦截）
                    session.accountAuthenticationRepository.exportWebCookies()
                    webViewClient = object : android.webkit.WebViewClient() {
                        override fun onPageStarted(view: android.webkit.WebView, url: String, favicon: android.graphics.Bitmap?) {
                            loading = true
                            loadProgress = 0
                            loadError = null
                        }

                        override fun onPageFinished(view: android.webkit.WebView, url: String) {
                            loading = false
                            onLoaded?.invoke(view, url)
                            val origin = android.net.Uri.parse(url)
                            if (origin.scheme == "https" && Endpoints.isForumHost(origin.host)) {
                                view.evaluateJavascript(sourceRewardToastScript, null)
                            }
                            val firstLoad = !initialLoadFinished
                            initialLoadFinished = true
                            if (firstLoad && initialContentSelector != null &&
                                Endpoints.isInternal(android.net.Uri.parse(url).host) &&
                                android.net.Uri.parse(url).path?.trimEnd('/') == initialPath?.trimEnd('/')) {
                                view.evaluateJavascript("""(function(){
                                    var selectors=${org.json.JSONObject.quote(initialContentSelector)}.split(',');
                                    var target=selectors.map(function(s){return document.querySelector(s.trim())}).find(Boolean);
                                    if(!target)return false;
                                    target.scrollIntoView({block:'start'});return true;
                                })()""", null)
                            }
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
                            // 站内明文跳转升级为 HTTPS，保证继续走 DoH 代理而不是系统 DNS。
                            if (request.isForMainFrame && request.method.equals("GET", ignoreCase = true) &&
                                destination.scheme.equals("http", ignoreCase = true) &&
                                Endpoints.isInternal(destination.host)
                            ) {
                                WebViewDoh.load(view, destination.buildUpon().scheme("https").build().toString())
                                return true
                            }
                            // 网页自身导航留在浏览器中，避免原生路由打断脚本及待显示弹窗。
                            return false
                        }
                        override fun doUpdateVisitedHistory(
                            view: android.webkit.WebView?,
                            u: String?,
                            isReload: Boolean,
                        ) {
                            pageTitle = view?.title?.takeIf { it.isNotBlank() } ?: u ?: url
                            currentUrl = u ?: url
                            canGoBack = view?.canGoBack() == true
                        }
                    }
                    WebViewDoh.load(this, url)
                }.also { webView = it }
                WebViewWindowHost(ctx).apply { show(listOf(primary), primary) }
            },
            update = { windows.updateHost(it, webView) },
            modifier = Modifier.fillMaxSize().padding(top = if (windows.activeWebView != null) 56.dp else 0.dp),
            onRelease = { host ->
                host.removeAllViews()
                webView?.let { windows.close(it); it.stopLoading(); it.destroy() }
                webView = null
            },
        )
            if (loading) {
                if (loadProgress > 0) LinearProgressIndicator(
                    progress = { loadProgress / 100f },
                    modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter))
                else LinearProgressIndicator(Modifier.fillMaxWidth().align(Alignment.TopCenter))
            }
            loadError?.let { message ->
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.align(Alignment.TopCenter).padding(LocalAppSpacing.current.contentGap),
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
                        TextButton(onClick = { loading = true; loadProgress = 0; loadError = null; webView?.reload() }) { Text("重试") }
                    }
                }
            }
            windows.InlineControls()
        }
    }
}

/** 设置网页仅替换应用外壳；滚动和业务表单仍由 WebView 自己处理。 */
@Composable
private fun WebPageScaffold(
    title: String,
    settingsStyle: Boolean,
    onBack: () -> Unit,
    actions: @Composable RowScope.() -> Unit,
    content: @Composable (PaddingValues) -> Unit,
) {
    if (!settingsStyle) {
        AppPageScaffold(title, onBack, actions = actions, content = content)
        return
    }
    SettingsScaffold {
        Column(Modifier.fillMaxSize()) {
            sb.linux.client.ui.patterns.settings.SettingsTopBar(title, onBack = onBack, actions = actions)
            SettingsCard(Modifier.weight(1f)) { content(PaddingValues()) }
        }
    }
}

