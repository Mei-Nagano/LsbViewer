package sb.linux.client.ui

import android.webkit.CookieManager
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

private const val VERIFY_CHALLENGE_JS =
    "(function(){try{var t=(document.title||'');var b=(document.body?document.body.innerText:'');" +
    "var el=!!(document.querySelector('#challenge-form')||document.querySelector('.ui-uam-box')||" +
    "document.querySelector('.cf-browser-verification')||document.querySelector('#cf-challenge-running')||" +
    "document.querySelector('.challenge-container')||document.querySelector('[class*=\"turnstile\"]')||" +
    "document.querySelector('iframe[src*=\"challenges.cloudflare.com\"]'));" +
    "return (t.indexOf('Checking your Browser')>=0||t.indexOf('Just a moment')>=0||" +
    "t.indexOf('Attention Required')>=0||t.indexOf('Verifying your request')>=0||" +
    "b.indexOf('SECURITY_VERIFICATION')>=0||b.indexOf('X-FL-UA-Step')>=0||el);" +
    "}catch(e){return false;}})()"

@Composable
fun VerificationDialog(
    url: String,
    initialHtml: String = "",
    onSucceeded: () -> Unit,
    onCancel: () -> Unit,
) {
    var finished by remember(url) { mutableStateOf(false) }
    var pageLoading by remember(url) { mutableStateOf(true) }
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
        ),
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    "安全验证",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "正在加载源站验证页面，验证完成后会自动继续。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 300.dp, max = 480.dp)
                ) {
                    AndroidView(
                        factory = { ctx ->
                            WebView(ctx).apply {
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                settings.cacheMode = WebSettings.LOAD_NO_CACHE
                                settings.loadWithOverviewMode = true
                                settings.useWideViewPort = true
                                settings.javaScriptCanOpenWindowsAutomatically = true
                                // CF 的 cf_clearance 与 UA 强绑定，必须与 OkHttp 一致，验证后回传 Cookie 才有效
                                settings.userAgentString = sb.linux.client.data.LsbClient.UA
                                CookieManager.getInstance().setAcceptCookie(true)
                                setDownloadListener { _, _, _, _, _ -> }
                                // 轮询验证状态：绝不做主动 reload —— reload 会重置 CF Turnstile / UAM 的
                                // 会话与挑战状态，反而导致"一直转圈 / 秒数不动"。只等它自己在页面内完成。
                                var blankReloaded = false
                                fun poll(view: WebView) {
                                    if (finished) return
                                    view.evaluateJavascript(VERIFY_CHALLENGE_JS) { result ->
                                        if (finished) return@evaluateJavascript
                                        val still = result.orEmpty().contains("true", ignoreCase = true)
                                        if (!still) {
                                            // 挑战已消失（CF iframe 移除 / UAM 跳转到真实内容）→ 等待稳定后判定成功
                                            view.postDelayed({
                                                if (!finished) {
                                                    finished = true
                                                    onSucceeded()
                                                }
                                            }, 800)
                                        } else {
                                            view.postDelayed({ if (!finished) poll(view) }, 2000)
                                        }
                                    }
                                }
                                webViewClient = object : WebViewClient() {
                                    override fun onPageStarted(view: WebView, pageUrl: String, favicon: android.graphics.Bitmap?) {
                                        pageLoading = true
                                    }

                                    override fun onReceivedError(
                                        view: WebView,
                                        request: WebResourceRequest,
                                        error: WebResourceError,
                                    ) {
                                        if (request.isForMainFrame && !finished) {
                                            view.postDelayed({ if (!finished) view.reload() }, 800)
                                        }
                                    }

                                    override fun onPageFinished(view: WebView, pageUrl: String) {
                                        pageLoading = false
                                        if (finished) return
                                        // 给挑战页 JS 一点时间构建 DOM，再判断是否真空白，避免开局误判重置
                                        view.postDelayed({
                                            if (finished) return@postDelayed
                                            view.evaluateJavascript("document.body ? document.body.innerText.length : 0") { len ->
                                                if (finished) return@evaluateJavascript
                                                val blank = len?.trim().isNullOrEmpty() || len?.trim() == "0"
                                                if (blank) {
                                                    if (!blankReloaded) {
                                                        blankReloaded = true
                                                        if (!finished) view.reload()
                                                    } else if (initialHtml.isNotBlank()) {
                                                        view.loadDataWithBaseURL(
                                                            pageUrl, initialHtml, "text/html", "utf-8", pageUrl
                                                        )
                                                    } else {
                                                        finished = true
                                                        onCancel()
                                                    }
                                                } else {
                                                    poll(view)
                                                }
                                            }
                                        }, 1500)
                                    }
                                }
                                sb.linux.client.data.WebViewDoh.load(this, url)
                            }
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                    if (pageLoading) {
                        CircularProgressIndicator(
                            Modifier
                                .align(Alignment.Center)
                                .size(28.dp)
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "若页面长时间白屏，可点击取消后重试。",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = cancel) { Text("取消") }
                }
            }
        }
    }
}
