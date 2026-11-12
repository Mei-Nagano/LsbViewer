package sb.linux.client.ui.content

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
import androidx.compose.foundation.shape.CircleShape
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
import sb.linux.client.ui.components.feedback.AppInlineLoading
import sb.linux.client.platform.webview.WebViewDoh
import java.net.URI
import sb.linux.client.ui.LocalLinkHandler

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
                shape = CircleShape,
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
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(videoAspectRatio)
                .clip(MaterialTheme.shapes.medium),
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
                if (loading) AppInlineLoading(size = 32.dp)
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
