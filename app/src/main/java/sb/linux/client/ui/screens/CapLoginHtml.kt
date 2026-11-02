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
import sb.linux.client.platform.verification.CapNetworkBridge
import sb.linux.client.domain.account.LoginVerification

internal fun sb.linux.client.domain.account.LoginVerification.Cap.widgetHtml(): String {
    val endpointAttr = endpoint.escapeHtmlAttribute()
    val scriptAttr = scriptUrl.escapeHtmlAttribute()
    val fieldAttr = fieldName.escapeHtmlAttribute()
    val fieldJs = org.json.JSONObject.quote(fieldName)
    val scriptUrlJs = org.json.JSONObject.quote(scriptUrl)
    val moduleJs = if (scriptModule) "true" else "false"
    val wasmJs = org.json.JSONObject.quote(wasmDataUrl.ifBlank { wasmUrl })
    val componentScript = when {
        // 经典脚本可以安全内联：同源执行，也不受跨域脚本的 CORS 限制。
        widgetScript.isNotBlank() && !scriptModule ->
            "<script>${widgetScript.escapeInlineScript()}</script>"
        // ES module 一律按脚本地址加载：内联 module 的 import.meta / 相对导入
        // 会按组件页地址解析，源站脚本反而加载不到自己的依赖。
        scriptUrl.isNotBlank() ->
            "<script${if (scriptModule) " type=\"module\"" else ""} src=\"$scriptAttr\"" +
                " onerror=\"window.__lsbCapLastFailure=window.__lsbCapLastFailure||'验证码组件下载失败'\"></script>"
        widgetScript.isNotBlank() ->
            "<script type=\"module\">${widgetScript.escapeInlineScript()}</script>"
        else -> ""
    }
    return """
        <!doctype html><html><head><meta charset="utf-8">
        <meta name="viewport" content="width=device-width,initial-scale=1,maximum-scale=1">
        <style>html,body,form{margin:0;padding:0;background:transparent;overflow:hidden}cap-widget{display:block;width:100%}</style>
        <script>
        (()=>{
          const pending=new Map();let sequence=0;
          window.__lsbCapLastFailure='';
          // 状态同时通过 JS Bridge 回传：自定义 scheme 跳转在没有用户手势时不一定触发。
          window.__lsbCapReport=(kind,payload)=>{try{
            const bridge=window.LsbCapStatus;if(!bridge)return;
            if(kind==='ready')bridge.ready();
            else if(kind==='error')bridge.error(String(payload||''));
            else bridge.token(String(payload||''));
          }catch(error){}};
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
          const NativeWorker=window.Worker;
          const nativeWorkers=new Map();let workerSequence=0;
          window.__lsbCapWorkerComplete=(id,result)=>{
            const worker=nativeWorkers.get(id);if(!worker||!worker.nativePending)return;
            if(result&&result.error){worker.fallback();return}
            worker.nativePending=false;clearTimeout(worker.nativeTimer);
            window.__lsbCapLastFailure='';
            try{worker.dispatchEvent(new MessageEvent('message',{data:result}))}catch(error){}
          };
          if(typeof NativeWorker==='function'){
            // 只接管 rsw / sha256-pow 的求解（交给原生线程更快）；
            // hashwx 等依赖 WASM 的挑战继续交给组件自己创建的 Blob Worker，
            // 否则消息被吞掉后组件会一直停在「正在验证」。
            window.Worker=class extends NativeWorker{
              constructor(url,options){
                super(url,options);
                this.lsbId='worker-'+(++workerSequence);
                this.nativePending=false;this.nativeTimer=0;this.lastMessage=null;
                nativeWorkers.set(this.lsbId,this)
              }
              postMessage(message,transfer){
                if(!message)return super.postMessage(message,transfer);
                this.lastMessage=message;
                if(message.kind==='rsw'&&typeof message.N==='string'&&typeof message.x==='string'){
                  this.nativePending=true;
                  this.nativeTimer=setTimeout(()=>{if(this.nativePending)this.fallback()},15000);
                  try{window.${CapNetworkBridge.NAME}.solveRsw(this.lsbId,message.N,message.x,Number(message.t)||0)}catch(error){this.fallback()}
                  return;
                }
                if(typeof message.salt==='string'&&typeof message.target==='string'){
                  this.nativePending=true;
                  this.nativeTimer=setTimeout(()=>{if(this.nativePending)this.fallback()},15000);
                  try{window.${CapNetworkBridge.NAME}.solvePow(this.lsbId,message.salt,message.target)}catch(error){this.fallback()}
                  return;
                }
                return super.postMessage(message,transfer);
              }
              fallback(){
                if(!this.nativePending)return;
                this.nativePending=false;clearTimeout(this.nativeTimer);
                const message=this.lastMessage,worker=this;
                const emit=(data)=>{try{worker.dispatchEvent(new MessageEvent('message',{data}))}catch(error){}};
                Promise.resolve().then(async()=>{
                  try{
                    if(message&&message.kind==='rsw'&&typeof message.N==='string'&&typeof message.x==='string'){
                      let y=BigInt('0x'+message.x),n=BigInt('0x'+message.N);
                      for(let i=0;i<(Number(message.t)||0);i++)y=y*y%n;
                      window.__lsbCapLastFailure='';emit({found:true,y:y.toString(16)});return;
                    }
                    const target=message&&message.target,salt=message&&message.salt;
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
                      if(matched){window.__lsbCapLastFailure='';emit({found:true,nonce});return}
                    }
                  }catch(error){
                    window.__lsbCapLastFailure='CAP 求解失败：'+((error&&error.message)||String(error));
                    try{worker.dispatchEvent(new Event('error'))}catch(error){}
                  }
                });
              }
              terminate(){nativeWorkers.delete(this.lsbId);clearTimeout(this.nativeTimer);super.terminate()}
            };
          }
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
          const cap=document.getElementById('cap'); let sent=false,ready=false;
          const report=(kind,payload)=>{try{window.__lsbCapReport&&window.__lsbCapReport(kind,payload)}catch(error){}};
          const fail=(message)=>{
            report('error',message);
            location.href='lsbcap://error?message='+encodeURIComponent(message);
          };
          const done=(token)=>{
            if(sent||!token)return;sent=true;
            report('token',token);
            location.href='lsbcap://solved?token='+encodeURIComponent(token);
          };
          cap.addEventListener('solve',(event)=>done(event.detail&&event.detail.token));
          cap.addEventListener('error',(event)=>{
            const source=(event.detail&&event.detail.message)||'验证码加载失败';
            fail(window.__lsbCapLastFailure||source);
          });
          // 组件注册成功且真正渲染出高度才算加载完成：否则用户只会看到空白，
          // 无法判断验证码到底有没有加载。
          const checkReady=()=>{
            if(ready||sent)return true;
            if(!customElements.get('cap-widget'))return false;
            if(!cap||cap.getBoundingClientRect().height<8)return false;
            ready=true;report('ready');return true;
          };
          const readyTimer=setInterval(()=>{if(checkReady())clearInterval(readyTimer)},300);
          // 首轮加载失败（模块语法被拒、资源下载失败等）时，按相反的脚本类型
          // 从源站补加载一次，再退回模块方式；模块依赖按脚本自身地址解析。
          const addScript=(module)=>{
            if(!$scriptUrlJs)return;
            const script=document.createElement('script');
            script.src=$scriptUrlJs;
            if(module)script.type='module';
            script.onerror=()=>{window.__lsbCapLastFailure=window.__lsbCapLastFailure||'验证码组件下载失败'};
            document.head.appendChild(script);
          };
          const retryScript=()=>{
            if(customElements.get('cap-widget')||ready)return;
            const stage=window.__lsbCapRetryStage||0;
            if(stage>=2)return;
            window.__lsbCapRetryStage=stage+1;
            addScript(stage===0?!$moduleJs:true);
          };
          setTimeout(retryScript,1500);
          setTimeout(retryScript,5000);
          setInterval(()=>{const input=document.querySelector('input[name='+$fieldJs+']');if(input&&!sent)done(input.value)},400);
          setTimeout(()=>{
            if(checkReady()||sent)return;
            fail(window.__lsbCapLastFailure||(
              customElements.get('cap-widget')?'验证码组件未渲染，请重新加载':'验证码组件加载超时'
            ));
          },10000);
        })();
        </script></body></html>
    """.trimIndent()
}

internal fun handleCallback(
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
