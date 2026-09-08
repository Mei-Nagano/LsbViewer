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
import androidx.compose.runtime.LaunchedEffect
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
import sb.linux.client.data.CapNetworkBridge
import sb.linux.client.data.LoginVerification
import sb.linux.client.data.LsbClient
import sb.linux.client.data.WebViewDoh

/** 在原生表单中承载源站 CAP 组件，并只向上层返回一次性令牌。 */
@Composable
internal fun CapVerificationWidget(
    verification: LoginVerification.Cap,
    revision: Int,
    client: LsbClient,
    onToken: (String) -> Unit,
    onStatus: (String) -> Unit,
    onError: (String) -> Unit,
) {
    val latestToken by rememberUpdatedState(onToken)
    val latestStatus by rememberUpdatedState(onStatus)
    val latestError by rememberUpdatedState(onError)
    var prepared by remember(verification, revision) {
        mutableStateOf(verification.takeIf { it.widgetScript.isNotBlank() })
    }
    var preparing by remember(verification, revision) { mutableStateOf(prepared == null) }
    LaunchedEffect(verification, revision) {
        if (prepared != null) return@LaunchedEffect
        try {
            prepared = client.prepareVerification(verification) as LoginVerification.Cap
        } catch (error: Exception) {
            latestError(error.message ?: "验证码资源加载失败")
        } finally {
            preparing = false
        }
    }
    val ready = prepared
    if (ready == null) {
        Box(Modifier.fillMaxWidth().height(82.dp), contentAlignment = Alignment.Center) {
            if (preparing) CircularProgressIndicator()
        }
        return
    }
    key(revision, ready) {
        var loading by remember { mutableStateOf(true) }
        var webView by remember { mutableStateOf<WebView?>(null) }
        var networkBridge by remember { mutableStateOf<CapNetworkBridge?>(null) }
        DisposableEffect(Unit) {
            onDispose {
                networkBridge?.close()
                networkBridge = null
                webView?.stopLoading()
                webView?.removeJavascriptInterface(CapNetworkBridge.NAME)
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
                        client.exportWebCookies()
                        networkBridge = CapNetworkBridge(
                            client = client,
                            endpoint = ready.endpoint,
                            pageUrl = ready.pageUrl,
                        ) { script -> post { runCatching { evaluateJavascript(script, null) } } }
                        addJavascriptInterface(networkBridge!!, CapNetworkBridge.NAME)
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
                        WebViewDoh.loadHtml(this, ready.pageUrl, ready.widgetHtml())
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
    val wasmJs = org.json.JSONObject.quote(wasmDataUrl.ifBlank { wasmUrl })
    val componentScript = if (widgetScript.isNotBlank()) {
        "<script>${widgetScript.escapeInlineScript()}</script>"
    } else {
        "<script type=\"module\" src=\"$scriptAttr\"></script>"
    }
    return """
        <!doctype html><html><head><meta charset="utf-8">
        <meta name="viewport" content="width=device-width,initial-scale=1,maximum-scale=1">
        <style>html,body,form{margin:0;padding:0;background:transparent;overflow:hidden}cap-widget{display:block;width:100%}</style>
        <script>
        (()=>{
          const pending=new Map();let sequence=0;
          window.__lsbCapLastFailure='';
          window.__lsbCapComplete=(id,result)=>{
            const entry=pending.get(id);if(!entry)return;
            if(result.error){
              // native DoH 请求失败时交给已配置 DoH 隧道的 WebView 再试一次；
              // 这条回退仍由 CAP 的同源/CORS 规则保护，不改变请求目标。
              fetch(entry.input,entry.options).then(response=>{
                if(pending.get(id)!==entry)return;pending.delete(id);window.__lsbCapLastFailure='';entry.resolve(response);
              }).catch(error=>{
                if(pending.get(id)!==entry)return;pending.delete(id);
                const nativeDetail=result.detail?result.detail:'未知异常';
                const webDetail=(error&&error.message)||String(error);
                window.__lsbCapLastFailure='CAP '+entry.stage+' 网络失败：native='+nativeDetail+'；WebView='+webDetail;
                entry.reject(new Error(window.__lsbCapLastFailure));
              });
              return;
            }
            pending.delete(id);
            window.__lsbCapLastFailure='';
            entry.resolve(new Response(result.body||'',{status:result.status||500,headers:{'Content-Type':result.contentType||'application/json'}}));
          };
          window.CAP_CUSTOM_FETCH=(input,options={})=>new Promise((resolve,reject)=>{
            const id='cap-'+Date.now()+'-'+(++sequence);const headers={};
            const requestUrl=new URL(input,location.href).href;
            const stage=new URL(requestUrl).pathname.endsWith('/redeem')?'redeem':'challenge';
            try{new Headers(options.headers||{}).forEach((value,name)=>headers[name]=value)}catch(error){}
            pending.set(id,{resolve,reject,input,options,stage});
            try{window.${CapNetworkBridge.NAME}.request(id,requestUrl,options.method||'GET',JSON.stringify(headers),typeof options.body==='string'?options.body:'')}
            catch(error){pending.delete(id);window.__lsbCapLastFailure='CAP bridge 调用失败：'+((error&&error.message)||String(error));reject(error)}
          });
          const nativeWorkers=new Map();let workerSequence=0;
          window.__lsbCapWorkerComplete=(id,result)=>{
            const worker=nativeWorkers.get(id);if(!worker)return;
            if(!worker.nativePending)return;
            if(result.error){worker.fallback();return}
            worker.nativePending=false;clearTimeout(worker.nativeTimer);
            window.__lsbCapLastFailure='';
            worker.emit('message',{data:result});
          };
          window.Worker=class{
            constructor(){
              this.listeners={message:new Set(),error:new Set()};this.id='worker-'+(++workerSequence);
              this.nativePending=false;this.nativeTimer=0;this.lastMessage=null;
              nativeWorkers.set(this.id,this)
            }
            addEventListener(type,listener){this.listeners[type]?.add(listener)}
            removeEventListener(type,listener){this.listeners[type]?.delete(listener)}
            postMessage(message){
              if(!message)return;
              this.lastMessage=message;
              if(message.kind==='rsw'&&typeof message.N==='string'&&typeof message.x==='string'){
                this.nativePending=true;
                this.nativeTimer=setTimeout(()=>{if(this.nativePending)this.fallback()},15000);
                try{window.${CapNetworkBridge.NAME}.solveRsw(this.id,message.N,message.x,Number(message.t)||0)}catch(error){this.fallback();return}
                return;
              }
              if(typeof message.salt==='string'&&typeof message.target==='string'){
                this.nativePending=true;
                this.nativeTimer=setTimeout(()=>{if(this.nativePending)this.fallback()},15000);
                try{window.${CapNetworkBridge.NAME}.solvePow(this.id,message.salt,message.target)}catch(error){this.fallback();return}
                return;
              }
            }
            fallback(){
              if(!this.nativePending)return;
              this.nativePending=false;clearTimeout(this.nativeTimer);
              const message=this.lastMessage;
              Promise.resolve().then(async()=>{
                try{
                  if(message?.kind==='rsw'&&typeof message.N==='string'&&typeof message.x==='string'){
                    let y=BigInt('0x'+message.x),n=BigInt('0x'+message.N);
                    for(let i=0;i<Number(message.t)||0;i++)y=y*y%n;
                    window.__lsbCapLastFailure='';this.emit('message',{data:{found:true,y:y.toString(16)}});return;
                  }
                  const target=message?.target,salt=message?.salt;
                  if(typeof target!=='string'||typeof salt!=='string')throw new Error('unsupported challenge');
                  const bits=target.length*4,fullBytes=Math.floor(bits/8),partialBits=bits%8;
                  const padded=target.length%2===0?target:target+'0',targetBytes=[];
                  for(let i=0;i<padded.length;i+=2)targetBytes.push(parseInt(padded.slice(i,i+2),16));
                  const mask=partialBits?255<<(8-partialBits)&255:0,encoder=new TextEncoder();
                  for(let nonce=0;;nonce++){
                    const hash=new Uint8Array(await crypto.subtle.digest('SHA-256',encoder.encode(salt+nonce)));
                    let matched=true;
                    for(let i=0;i<fullBytes;i++)if(hash[i]!==targetBytes[i]){matched=false;break}
                    if(matched&&partialBits)matched=(hash[fullBytes]&mask)===(targetBytes[fullBytes]&mask);
                    if(matched){window.__lsbCapLastFailure='';this.emit('message',{data:{found:true,nonce}});return}
                  }
                }catch(error){window.__lsbCapLastFailure='CAP 求解失败：'+(error?.message||String(error));this.emit('error',{message:window.__lsbCapLastFailure})}
              });
            }
            emit(type,event){this.listeners[type]?.forEach(listener=>{try{listener(event)}catch(error){}});if(type==='message'&&this.onmessage)this.onmessage(event);if(type==='error'&&this.onerror)this.onerror(event)}
            terminate(){nativeWorkers.delete(this.id);clearTimeout(this.nativeTimer);this.listeners.message.clear();this.listeners.error.clear()}
          };
          if($wasmJs)window.CAP_CUSTOM_WASM_URL=$wasmJs;
        })();
        </script>
        $componentScript</head><body>
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
          cap.addEventListener('error',(event)=>{
            const source=(event.detail&&event.detail.message)||'验证码加载失败';
            const message=window.__lsbCapLastFailure||source;
            location.href='lsbcap://error?message='+encodeURIComponent(message);
          });
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

private fun String.escapeInlineScript(): String = replace("</script", "<\\/script", ignoreCase = true)
