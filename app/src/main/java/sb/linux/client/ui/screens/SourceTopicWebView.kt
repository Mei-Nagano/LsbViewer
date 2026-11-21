package sb.linux.client.ui.screens

import android.graphics.Color as AndroidColor
import android.webkit.ConsoleMessage
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import org.json.JSONObject
import sb.linux.client.common.network.Endpoints
import sb.linux.client.domain.topic.TopicPageData
import sb.linux.client.platform.webview.WebViewDoh
import sb.linux.client.platform.webview.LinkWindowClient
import sb.linux.client.ui.LocalShowUid
import java.io.ByteArrayInputStream
import kotlin.math.roundToInt

@Composable
internal fun SourceTopicWebView(page: TopicPageData, view: TopicScreenViewState, ui: TopicScreenUiState,
    loggedIn: Boolean, resources: sb.linux.client.domain.topic.SourceTopicResourceRepository,
    onAction: (String) -> Unit, nativePanels: @Composable () -> Unit) {
    val density = LocalDensity.current
    val colors = MaterialTheme.colorScheme
    val screenHeight = LocalConfiguration.current.screenHeightDp
    fun css(color: androidx.compose.ui.graphics.Color) = "#%06x".format(color.toArgb() and 0xffffff)
    val theme = "--lsb-text:${css(colors.onSurface)};--lsb-muted:${css(colors.onSurfaceVariant)};" +
        "--lsb-panel:${css(colors.surface)};--lsb-bg:${css(colors.surfaceContainerLow)};--lsb-line:${css(colors.outlineVariant)};" +
        "--lsb-accent:${css(colors.primary)};--lsb-accent-soft:${css(colors.primaryContainer)};" +
        "--lsb-on-accent-soft:${css(colors.onPrimaryContainer)};--lsb-screen-height:${screenHeight}px;"
    val shell = remember(page.contentStyles, theme) {
        SourceTopicDocument.shell(page.contentStyles, colors.surface.luminance() < 0.5f, theme)
    }
    val geometry = ui.sourceGeometry
    val allHiddenState = LocalAllSignaturesHidden.current
    val allHidden = allHiddenState.value
    val restoreVersion = LocalSignatureRestoreVersion.current.value
    LaunchedEffect(restoreVersion) {
        if (ui.sourceSignatureRestoreVersion != restoreVersion) {
            ui.sourceHiddenSignatures = emptySet()
            ui.sourceSignatureRestoreVersion = restoreVersion
        }
    }
    val showUid = LocalShowUid.current
    var hideTarget by remember(page.topicId) { mutableStateOf<Long?>(null) }
    var browser by remember { mutableStateOf<WebView?>(ui.sourceBrowser) }
    val windows = rememberSourceWebWindows()
    val chooseFiles = rememberWebFileChooser()
    val currentAction by rememberUpdatedState(onAction)
    val update = remember(page, view.mainPost, view.reply.flatTree, view.reply.pagedReplies,
        view.threadEnabled, ui.topicSearchOpen, ui.topicSearchQuery, ui.favoriteCommentIds, ui.isFav,
        ui.collapsedReplyIds, loggedIn, allHidden, ui.sourceHiddenSignatures, ui.sourceSlotHeight, geometry.pixelsPerCss, showUid) {
        SourceTopicDocument.update(page, view, ui, loggedIn, allHidden, ui.sourceHiddenSignatures,
            ui.sourceSlotHeight / geometry.pixelsPerCss, showUid)
    }
    val currentUpdate by rememberUpdatedState(update)
    fun collectMetrics(web: WebView) {
        web.evaluateJavascript("JSON.stringify(window.lsbContent?window.lsbContent.metrics():null)") { result ->
            runCatching {
                val encoded = org.json.JSONTokener(result).nextValue() as? String
                if (encoded == null || encoded == "null") { ui.sourceLoadError = true; return@runCatching }
                ui.sourceLoadError = false
                geometry.update(JSONObject(encoded), web.width)
            }
        }
    }
    Box(Modifier.fillMaxWidth().height(with(density) { geometry.height.toDp() }.coerceAtLeast(1.dp))
        .onGloballyPositioned {
            geometry.topInRoot = it.localToRoot(Offset.Zero).y
            view.mainPost?.let { post -> geometry.posts[post.id]?.let { position ->
                ui.bodyTop = geometry.topInRoot + position.body
                ui.bodyHeight = position.bodyHeight
            } }
        }) {
        AndroidView(modifier = Modifier.fillMaxWidth().height(with(density) { geometry.height.toDp() }.coerceAtLeast(1.dp)),
            factory = { context ->
                val retained = ui.sourceBrowser ?: SourceContentWebView(
                    android.content.MutableContextWrapper(context.applicationContext),
                ).also { ui.sourceBrowser = it }
                (retained.parent as? android.view.ViewGroup)?.removeView(retained)
                (retained.context as android.content.MutableContextWrapper).baseContext = context
                retained.apply {
                browser = this
                setBackgroundColor(AndroidColor.TRANSPARENT)
                isVerticalScrollBarEnabled = false
                isHorizontalScrollBarEnabled = false
                settings.javaScriptEnabled = true
                settings.javaScriptCanOpenWindowsAutomatically = true
                settings.domStorageEnabled = false
                settings.allowFileAccess = false
                settings.allowContentAccess = false
                settings.setSupportMultipleWindows(true)
                settings.mediaPlaybackRequiresUserGesture = true
                setOnScrollChangeListener { _, _, y, _, _ -> if (y != 0) scrollTo(0, 0) }
                webChromeClient = object : LinkWindowClient(windows::show, windows::close,
                    showJsDialog = windows::showJsDialog,
                    chooseFiles = chooseFiles, onTitle = windows::title) {
                    override fun onConsoleMessage(message: ConsoleMessage): Boolean {
                        if (message.message() == "lsb-content-layout") { post { collectMetrics(this@apply) }; return true }
                        return false
                    }
                }
                webViewClient = object : WebViewClient() {
                    override fun shouldInterceptRequest(web: WebView, request: WebResourceRequest): WebResourceResponse? {
                        val url = request.url.toString()
                        if (!url.startsWith(SOURCE_TOPIC_ASSET)) {
                            if (request.isForMainFrame || request.method != "GET" ||
                                request.url.scheme != "https" || !Endpoints.isForumHost(request.url.host) ||
                                request.url.path?.startsWith("/app/") != true) return null
                            return try {
                                val resource = resources.load(url, request.requestHeaders)
                                WebResourceResponse(resource.mimeType, resource.encoding, resource.status,
                                    resource.reason, resource.headers, resource.body)
                            } catch (_: java.io.IOException) {
                                WebResourceResponse("text/plain", "UTF-8", 502, "Resource unavailable",
                                    emptyMap(), ByteArrayInputStream(ByteArray(0)))
                            }
                        }
                        val path = url.removePrefix(SOURCE_TOPIC_ASSET).substringBefore('?')
                        if (path.contains("..")) return WebResourceResponse("text/plain", "UTF-8", ByteArrayInputStream(ByteArray(0)))
                        val asset = if (path.startsWith("katex/")) path else "source-topic/$path"
                        val mime = when { path.endsWith(".js") -> "application/javascript"; path.endsWith(".css") -> "text/css"; path.endsWith(".woff2") -> "font/woff2"; else -> "text/plain" }
                        return runCatching { WebResourceResponse(mime, "UTF-8", context.assets.open(asset)) }.getOrElse {
                            WebResourceResponse("text/plain", "UTF-8", ByteArrayInputStream(ByteArray(0)))
                        }
                    }
                    override fun shouldOverrideUrlLoading(web: WebView, request: WebResourceRequest): Boolean {
                        if (!request.isForMainFrame) return false
                        val uri = request.url
                        if (uri.scheme == "lsb-action" && !request.hasGesture()) return true
                        if (uri.scheme == "lsb-action" && uri.host == "hide-signature") {
                            val id = uri.lastPathSegment?.toLongOrNull() ?: return true
                            hideTarget = id
                        } else currentAction(uri.toString())
                        return true
                    }
                    override fun onPageFinished(web: WebView, url: String) {
                        ui.sourceReady = true
                        web.tag = currentUpdate
                        web.evaluateJavascript(currentUpdate) { collectMetrics(web) }
                    }
                }
                onResume()
                if (ui.sourceLoadedShell != shell || !ui.sourceReady) {
                    ui.sourceLoadedShell = shell
                    WebViewDoh.loadHtml(this, Endpoints.abs("/topic/${page.topicId}"), shell)
                }
            } },
            update = { web ->
                val textZoom = (100 * density.fontScale * density.density / web.resources.displayMetrics.density).roundToInt()
                if (web.settings.textZoom != textZoom) web.settings.textZoom = textZoom
                if (ui.sourceLoadedShell != shell) {
                    ui.sourceLoadedShell = shell; ui.sourceReady = false; geometry.ready = false; ui.sourceLoadError = false
                    WebViewDoh.loadHtml(web, Endpoints.abs("/topic/${page.topicId}"), shell)
                } else if (ui.sourceReady && web.tag != update) { geometry.ready = false; web.tag = update; web.evaluateJavascript(update) { collectMetrics(web) } }
            }, onRelease = { web ->
                windows.close(web)
                browser = null
                web.onPause()
                web.webChromeClient = null
                web.webViewClient = WebViewClient()
                (web.context as android.content.MutableContextWrapper).baseContext = web.context.applicationContext
            })
        Box(Modifier.fillMaxWidth().offset { IntOffset(0, geometry.slotTop.roundToInt()) }
            .onSizeChanged { ui.sourceSlotHeight = it.height.toFloat() }) { nativePanels() }
    }
    if (hideTarget != null) androidx.compose.material3.AlertDialog(
        onDismissRequest = { hideTarget = null }, title = { androidx.compose.material3.Text("隐藏所有签名？") },
        text = { androidx.compose.material3.Text("可隐藏应用内所有帖子和评论的签名，或只隐藏当前签名。\n\n$SIGNATURE_RECOVERY_HINT") },
        confirmButton = { androidx.compose.material3.TextButton(onClick = { allHiddenState.value = true; hideTarget = null }) { androidx.compose.material3.Text("隐藏所有") } },
        dismissButton = { androidx.compose.material3.TextButton(onClick = { ui.sourceHiddenSignatures = ui.sourceHiddenSignatures + hideTarget!!; hideTarget = null }) { androidx.compose.material3.Text("仅隐藏此条") } })
    windows.Content()
    if (ui.sourceLoadError) sb.linux.client.ui.components.feedback.ErrorBox("正文网页加载失败，请检查网络设置后重试") {
        browser?.let { web -> ui.sourceReady = false; ui.sourceLoadError = false; WebViewDoh.loadHtml(web, Endpoints.abs("/topic/${page.topicId}"), shell) }
    }
    LaunchedEffect(ui.highlightPostId) {
        ui.highlightPostId?.let { browser?.evaluateJavascript("window.lsbContent&&window.lsbContent.highlightPost('$it')", null) }
    }
    SideEffect {
        view.mainPost?.let { post -> geometry.posts[post.id]?.let { position ->
            ui.bodyTop = geometry.topInRoot + position.body
            ui.bodyHeight = position.bodyHeight
            geometry.headings.forEach { (index, top) -> ui.headingPositions[index] = top - position.body }
        } }
    }
}
