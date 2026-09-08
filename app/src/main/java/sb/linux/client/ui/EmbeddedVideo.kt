package sb.linux.client.ui

import android.graphics.Color
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import org.jsoup.nodes.Element
import sb.linux.client.data.WebViewDoh
import java.net.URI

/** 源站正文当前支持的可信视频播放器。 */
internal enum class EmbeddedVideoPlatform {
    DOUYIN,
    BILIBILI,
    YOUTUBE,
}

private val EmbeddedVideoPlatform.displayName: String
    get() = when (this) {
        EmbeddedVideoPlatform.DOUYIN -> "抖音"
        EmbeddedVideoPlatform.BILIBILI -> "哔哩哔哩"
        EmbeddedVideoPlatform.YOUTUBE -> "YouTube"
    }

internal data class EmbeddedVideo(
    val platform: EmbeddedVideoPlatform,
    val playerUrl: String,
    val sourceUrl: String,
)

/**
 * 识别源站 nb-editor 输出的播放器 iframe。只允许已知 HTTPS 播放器域名，
 * 避免把普通帖子中的任意 iframe 交给 WebView 执行。
 */
internal fun parseEmbeddedVideo(element: Element): EmbeddedVideo? {
    if (!element.tagName().equals("iframe", ignoreCase = true)) return null
    val source = element.absUrl("src").ifBlank { element.attr("src").trim() }
    val uri = runCatching { URI(source) }.getOrNull() ?: return null
    if (!uri.scheme.equals("https", ignoreCase = true) || uri.userInfo != null || uri.port !in setOf(-1, 443)) return null
    val query = uri.rawQuery.orEmpty()
    return when {
        uri.host.equals("player.bilibili.com", ignoreCase = true) &&
            uri.path == "/player.html" -> {
            val id = queryValue(query, "bvid") ?: queryValue(query, "aid") ?: return null
            val pathId = if (queryValue(query, "bvid") != null) id else "av$id"
            EmbeddedVideo(
                platform = EmbeddedVideoPlatform.BILIBILI,
                playerUrl = source,
                sourceUrl = "https://www.bilibili.com/video/$pathId",
            )
        }
        uri.host.equals("open.douyin.com", ignoreCase = true) &&
            uri.path == "/player/video" -> {
            val id = queryValue(query, "vid")?.takeIf { it.all(Char::isDigit) } ?: return null
            EmbeddedVideo(
                platform = EmbeddedVideoPlatform.DOUYIN,
                playerUrl = source,
                sourceUrl = "https://www.douyin.com/video/$id",
            )
        }
        uri.host?.lowercase() in YOUTUBE_PLAYER_HOSTS && uri.path.orEmpty().startsWith("/embed/") -> {
            val id = uri.path.orEmpty().removePrefix("/embed/").substringBefore('/')
                .takeIf { it.matches(YOUTUBE_VIDEO_ID) } ?: return null
            EmbeddedVideo(
                platform = EmbeddedVideoPlatform.YOUTUBE,
                playerUrl = buildYouTubePlayerUrl(source, uri),
                sourceUrl = "https://www.youtube.com/watch?v=$id",
            )
        }
        else -> null
    }
}

private fun queryValue(query: String, name: String): String? = query.split('&')
    .mapNotNull { part -> part.split('=', limit = 2).takeIf { it.size == 2 } }
    .firstOrNull { it[0] == name }
    ?.get(1)
    ?.takeIf(String::isNotBlank)

private fun applyPlayerPageFixes(view: WebView, platform: EmbeddedVideoPlatform) {
    val script = when (platform) {
        EmbeddedVideoPlatform.BILIBILI -> BILIBILI_VIEWPORT_FIX_JS
        EmbeddedVideoPlatform.YOUTUBE -> EMBEDDED_IFRAME_VIEWPORT_FIX_JS
        EmbeddedVideoPlatform.DOUYIN -> return
    }
    view.evaluateJavascript(script, null)
}

private fun updateVideoAspectRatio(
    view: WebView,
    platform: EmbeddedVideoPlatform,
    onAspectRatio: (Float) -> Unit,
) {
    if (platform != EmbeddedVideoPlatform.DOUYIN) return
    view.evaluateJavascript(
        "(function(){var v=document.querySelector('video');" +
            "return v&&v.videoWidth>0&&v.videoHeight>0?v.videoWidth+','+v.videoHeight:'';})()",
    ) { raw ->
        val dimensions = raw.trim('"').split(',')
        val width = dimensions.getOrNull(0)?.toFloatOrNull() ?: return@evaluateJavascript
        val height = dimensions.getOrNull(1)?.toFloatOrNull() ?: return@evaluateJavascript
        if (width > 0f && height > 0f) onAspectRatio((width / height).coerceIn(0.42f, 2.2f))
    }
}

/**
 * 哔哩哔哩移动播放器作为顶层 WebView 加载时会把根节点算成 0 高；使用实际视口像素，
 * 百分比和 vh 在该页面初始化阶段仍会继承这个错误高度。
 */
private const val BILIBILI_VIEWPORT_FIX_JS =
    "(function(){" +
        "function applyHeight(){" +
        "var height=window.innerHeight+'px';" +
        "var nodes=[document.documentElement,document.body,document.getElementById('w-player')];" +
        "nodes.forEach(function(node){if(node){" +
        "node.style.setProperty('height',height,'important');" +
        "node.style.setProperty('min-height',height,'important');" +
        "}});" +
        "}" +
        "applyHeight();" +
        "if(!window.__lsbViewportFixInstalled){" +
        "window.__lsbViewportFixInstalled=true;" +
        "window.addEventListener('resize',applyHeight);" +
        "}" +
        "})();"

private const val EMBEDDED_IFRAME_VIEWPORT_FIX_JS =
    "(function(){" +
        "function applyHeight(){" +
        "var height=window.innerHeight+'px';" +
        "[document.documentElement,document.body].forEach(function(node){if(node){" +
        "node.style.setProperty('height',height,'important');" +
        "node.style.setProperty('min-height',height,'important');" +
        "}});" +
        "Array.prototype.forEach.call(document.querySelectorAll('iframe'),function(frame){" +
        "frame.style.setProperty('height',height,'important');" +
        "frame.style.setProperty('min-height',height,'important');" +
        "});" +
        "}" +
        "applyHeight();" +
        "if(!window.__lsbIframeViewportFixInstalled){" +
        "window.__lsbIframeViewportFixInstalled=true;" +
        "window.addEventListener('resize',applyHeight);" +
        "}" +
        "})();"

/** 使用系统/VPN网络播放嵌入视频，避免大流量媒体分片经过论坛专用 DoH 隧道。 */
@Composable
internal fun EmbeddedVideoPlayer(video: EmbeddedVideo, modifier: Modifier = Modifier) {
    val linkHandler = LocalLinkHandler.current
    val uriHandler = LocalUriHandler.current
    var loading by remember(video.playerUrl) { mutableStateOf(true) }
    var failed by remember(video.playerUrl) { mutableStateOf(false) }
    var videoAspectRatio by remember(video.playerUrl) {
        mutableFloatStateOf(if (video.platform == EmbeddedVideoPlatform.DOUYIN) 324f / 672f else 16f / 9f)
    }
    var webView by remember(video.playerUrl) { mutableStateOf<WebView?>(null) }
    var fullscreen by remember(video.playerUrl) { mutableStateOf<FullscreenVideoSession?>(null) }
    val closeFullscreen = {
        fullscreen?.let { session ->
            fullscreen = null
            session.restoreOrientation()
            runCatching { session.callback.onCustomViewHidden() }
        }
        Unit
    }
    DisposableEffect(video.playerUrl) {
        onDispose {
            fullscreen?.let {
                it.restoreOrientation()
                runCatching { it.callback.onCustomViewHidden() }
            }
            webView?.let { view ->
                view.stopLoading()
                view.destroy()
            }
            webView = null
        }
    }
    val containerModifier = if (video.platform == EmbeddedVideoPlatform.DOUYIN && videoAspectRatio < 1f) {
        modifier.fillMaxWidth().widthIn(max = 324.dp)
    } else {
        modifier.fillMaxWidth()
    }
    Column(containerModifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.secondaryContainer,
            ) {
                Text(
                    video.platform.displayName,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
            Spacer(Modifier.weight(1f))
            TextButton(
                onClick = {
                    loading = true
                    failed = false
                    webView?.reload()
                },
                enabled = webView != null,
            ) {
                Text("刷新")
            }
        }
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(videoAspectRatio)
                .clip(RoundedCornerShape(10.dp)),
        ) {
            Box(contentAlignment = Alignment.Center) {
                AndroidView(
                    factory = { context ->
                        WebView(context).apply {
                            webView = this
                            setBackgroundColor(Color.BLACK)
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.cacheMode = WebSettings.LOAD_DEFAULT
                            settings.mediaPlaybackRequiresUserGesture = true
                            settings.allowFileAccess = false
                            settings.allowContentAccess = false
                            settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                            if (video.platform == EmbeddedVideoPlatform.YOUTUBE) {
                                // YouTube 会把带 wv/Version 标记的嵌入请求识别成异常客户端并要求登录。
                                settings.userAgentString = browserCompatibleUserAgent(settings.userAgentString)
                            }
                            CookieManager.getInstance().setAcceptCookie(true)
                            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                            webChromeClient = object : WebChromeClient() {
                                override fun onShowCustomView(
                                    view: android.view.View,
                                    callback: CustomViewCallback,
                                ) {
                                    closeFullscreen()
                                    (view.parent as? android.view.ViewGroup)?.removeView(view)
                                    val activity = view.context.findActivity()
                                    val previousOrientation = activity?.requestedOrientation
                                    val orientation = if (videoAspectRatio >= 1f) {
                                        FullscreenVideoOrientation.LANDSCAPE
                                    } else {
                                        FullscreenVideoOrientation.PORTRAIT
                                    }
                                    activity?.requestedOrientation = orientation.requestedOrientation
                                    fullscreen = FullscreenVideoSession(
                                        view = view,
                                        callback = callback,
                                        orientation = orientation,
                                        activity = activity,
                                        previousOrientation = previousOrientation,
                                    )
                                }

                                override fun onHideCustomView() {
                                    closeFullscreen()
                                }
                            }
                            webViewClient = object : WebViewClient() {
                                override fun onPageCommitVisible(view: WebView, url: String) {
                                    // 播放器外壳可见就移除遮罩，不等待全部统计脚本和媒体连接结束。
                                    applyPlayerPageFixes(view, video.platform)
                                    updateVideoAspectRatio(view, video.platform) { videoAspectRatio = it }
                                    loading = false
                                }
                                override fun onPageFinished(view: WebView, url: String) {
                                    applyPlayerPageFixes(view, video.platform)
                                    updateVideoAspectRatio(view, video.platform) { videoAspectRatio = it }
                                    if (video.platform == EmbeddedVideoPlatform.DOUYIN) {
                                        view.postDelayed({
                                            updateVideoAspectRatio(view, video.platform) { videoAspectRatio = it }
                                        }, 600L)
                                    }
                                    loading = false
                                }

                                override fun onReceivedError(
                                    view: WebView,
                                    request: WebResourceRequest,
                                    error: WebResourceError,
                                ) {
                                    if (request.isForMainFrame) {
                                        loading = false
                                        failed = true
                                    }
                                }
                            }
                            if (video.platform == EmbeddedVideoPlatform.YOUTUBE) {
                                // 使用论坛真实来源，保持与源站正文中的 YouTube 嵌入请求一致。
                                WebViewDoh.loadHtml(
                                    this,
                                    YOUTUBE_EMBED_BASE_URL,
                                    buildYouTubeEmbedDocument(video.playerUrl),
                                )
                            } else {
                                WebViewDoh.loadDirect(this, video.playerUrl)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                )
                if (loading) CircularProgressIndicator()
                if (failed) {
                    TextButton(onClick = {
                        if (linkHandler != null) linkHandler(video.sourceUrl)
                        else runCatching { uriHandler.openUri(video.sourceUrl) }
                    }) {
                        Text("播放器加载失败，点击打开视频")
                    }
                }
            }
        }
    }
    fullscreen?.let { session ->
        FullscreenVideoDialog(session, onDismiss = closeFullscreen)
    }
}
