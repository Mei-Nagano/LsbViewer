package sb.linux.client.ui.content

import android.graphics.Color as AndroidColor
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import org.json.JSONObject
import java.io.ByteArrayInputStream

/** 仅加载本地 KaTeX 与转义后的公式字符串，不执行帖子 HTML 或访问网络。 */
@Composable
internal fun FormulaBlock(source: String, display: Boolean, modifier: Modifier = Modifier) {
    val foreground = MaterialTheme.colorScheme.onSurface.toArgb()
    var height by remember(source, display) { mutableIntStateOf(48) }
    val html = remember(source, display, foreground) {
        val expression = JSONObject.quote(source).replace("<", "\\u003c")
        val color = "#%06x".format(foreground and 0xffffff)
        """<!doctype html><html><head><meta name="viewport" content="width=device-width,initial-scale=1">
            <link rel="stylesheet" href="katex.min.css"><script src="katex.min.js"></script>
            <style>html,body{margin:0;padding:0;background:transparent;color:$color}body{padding:8px 0;overflow-x:auto}#formula{display:inline-block;max-width:100%;font-size:16px}</style>
            </head><body><div id="formula"></div><script>
            var node=document.getElementById('formula');try{katex.render($expression,node,{displayMode:$display,throwOnError:true,trust:false,strict:'ignore',maxExpand:1000,maxSize:20});}catch(e){node.textContent=$expression;}
            </script></body></html>""".trimIndent()
    }
    AndroidView(modifier = modifier.fillMaxWidth().height(height.dp), factory = { context ->
        WebView(context).apply {
            setBackgroundColor(AndroidColor.TRANSPARENT)
            settings.javaScriptEnabled = true
            settings.blockNetworkLoads = true
            settings.allowContentAccess = false
            settings.allowFileAccess = true
            @Suppress("DEPRECATION")
            settings.allowFileAccessFromFileURLs = false
            @Suppress("DEPRECATION")
            settings.allowUniversalAccessFromFileURLs = false
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest) = true
                override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
                    if (request.url.toString().startsWith("file:///android_asset/katex/")) return null
                    return WebResourceResponse("text/plain", "UTF-8", ByteArrayInputStream(ByteArray(0)))
                }
                override fun onPageFinished(view: WebView, url: String?) {
                    view.evaluateJavascript("Math.ceil(document.body.scrollHeight)") { result ->
                        height = result.toIntOrNull()?.coerceIn(32, 800) ?: 48
                    }
                }
            }
        }
    }, update = { view ->
        if (view.tag != html) {
            view.tag = html
            view.loadDataWithBaseURL("file:///android_asset/katex/", html, "text/html", "UTF-8", null)
        }
    }, onRelease = { it.stopLoading(); it.destroy() })
}
