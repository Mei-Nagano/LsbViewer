package sb.linux.client.ui.screens

import android.graphics.Color
import android.net.Uri
import android.webkit.CookieManager
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import sb.linux.client.data.LoginVerification
import sb.linux.client.data.LsbClient
import sb.linux.client.data.WebViewDoh

/** 在原生登录表单中承载源站 CAP 组件，并只向上层返回一次性令牌。 */
@Composable
internal fun CapLoginWidget(
    verification: LoginVerification.Cap,
    revision: Int,
    onToken: (String) -> Unit,
    onStatus: (String) -> Unit,
    onError: (String) -> Unit,
) {
    val latestToken by rememberUpdatedState(onToken)
    val latestStatus by rememberUpdatedState(onStatus)
    val latestError by rememberUpdatedState(onError)
    key(revision) {
        var loading by remember { mutableStateOf(true) }
        var webView by remember { mutableStateOf<WebView?>(null) }
        DisposableEffect(Unit) {
            onDispose {
                webView?.stopLoading()
                webView?.destroy()
                webView = null
            }
        }
        Box(Modifier.fillMaxWidth().height(82.dp), contentAlignment = Alignment.Center) {
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        webView = this
                        setBackgroundColor(Color.TRANSPARENT)
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.cacheMode = WebSettings.LOAD_NO_CACHE
                        settings.allowFileAccess = false
                        settings.allowContentAccess = false
                        settings.userAgentString = LsbClient.UA
                        CookieManager.getInstance().setAcceptCookie(true)
                        CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean =
                                handleCallback(request.url, latestToken, latestStatus, latestError)

                            override fun onPageFinished(view: WebView, url: String) {
                                loading = false
                                latestStatus("请完成人机验证")
                            }

                            override fun onReceivedError(
                                view: WebView,
                                request: WebResourceRequest,
                                error: WebResourceError,
                            ) {
                                if (request.isForMainFrame) latestError("验证码页面加载失败，请重试")
                            }
                        }
                        WebViewDoh.loadHtml(this, verification.pageUrl, verification.widgetHtml())
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )
            if (loading) CircularProgressIndicator()
        }
    }
}

private fun LoginVerification.Cap.widgetHtml(): String {
    val endpointAttr = endpoint.escapeHtmlAttribute()
    val scriptAttr = scriptUrl.escapeHtmlAttribute()
    val fieldAttr = fieldName.escapeHtmlAttribute()
    val fieldJs = org.json.JSONObject.quote(fieldName)
    val wasmJs = org.json.JSONObject.quote(wasmUrl)
    return """
        <!doctype html><html><head><meta charset="utf-8">
        <meta name="viewport" content="width=device-width,initial-scale=1,maximum-scale=1">
        <style>html,body,form{margin:0;padding:0;background:transparent;overflow:hidden}cap-widget{display:block;width:100%}</style>
        <script>if($wasmJs)window.CAP_CUSTOM_WASM_URL=$wasmJs;</script>
        <script type="module" src="$scriptAttr"></script></head><body>
        <form><cap-widget id="cap" required data-cap-api-endpoint="$endpointAttr"
          data-cap-hidden-field-name="$fieldAttr"
          data-cap-i18n-initial-state="点击进行人机验证"
          data-cap-i18n-verifying-label="正在验证…"
          data-cap-i18n-solved-label="验证完成"
          data-cap-i18n-error-label="验证失败"></cap-widget></form>
        <script>
        (()=>{
          const cap=document.getElementById('cap'); let sent=false;
          const done=(token)=>{if(sent||!token)return;sent=true;location.href='lsbcap://solved?token='+encodeURIComponent(token)};
          cap.addEventListener('solve',(event)=>done(event.detail&&event.detail.token));
          cap.addEventListener('error',(event)=>location.href='lsbcap://error?message='+encodeURIComponent((event.detail&&event.detail.message)||'验证码加载失败'));
          setInterval(()=>{const input=document.querySelector('input[name='+$fieldJs+']');if(input)done(input.value)},400);
          setTimeout(()=>{if(!customElements.get('cap-widget'))location.href='lsbcap://error?message='+encodeURIComponent('验证码组件加载超时')},12000);
        })();
        </script></body></html>
    """.trimIndent()
}

private fun handleCallback(
    uri: Uri,
    onToken: (String) -> Unit,
    onStatus: (String) -> Unit,
    onError: (String) -> Unit,
): Boolean {
    if (uri.scheme != "lsbcap") return false
    when (uri.host) {
        "solved" -> uri.getQueryParameter("token")?.takeIf { it.isNotBlank() }?.let {
            onToken(it)
            onStatus("验证完成")
        }
        "error" -> onError(uri.getQueryParameter("message").orEmpty().ifBlank { "验证码加载失败" })
    }
    return true
}

private fun String.escapeHtmlAttribute(): String = replace("&", "&amp;")
    .replace("\"", "&quot;")
    .replace("<", "&lt;")
    .replace(">", "&gt;")
