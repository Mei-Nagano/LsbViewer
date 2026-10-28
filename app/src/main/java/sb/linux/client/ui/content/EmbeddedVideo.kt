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
import java.net.URI

/** 源站正文当前支持的可信视频播放器。 */
internal enum class EmbeddedVideoPlatform {
    DOUYIN,
    BILIBILI,
    YOUTUBE,
}

internal val EmbeddedVideoPlatform.displayName: String
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

internal fun applyPlayerPageFixes(view: WebView, platform: EmbeddedVideoPlatform) {
    val script = when (platform) {
        EmbeddedVideoPlatform.BILIBILI -> BILIBILI_VIEWPORT_FIX_JS
        EmbeddedVideoPlatform.YOUTUBE -> EMBEDDED_IFRAME_VIEWPORT_FIX_JS
        EmbeddedVideoPlatform.DOUYIN -> return
    }
    view.evaluateJavascript(script, null)
}

internal fun updateVideoAspectRatio(
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
