package sb.linux.client.ui.screens

import android.webkit.JavascriptInterface
import org.jsoup.Jsoup
import sb.linux.client.domain.account.LoginVerification

internal class PowStatusBridge(
    private val onChange: (Boolean, String, Int) -> Unit,
    private val onError: (String) -> Unit,
) {
    @JavascriptInterface
    fun changed(ready: Boolean, result: String, height: Int) = onChange(ready, result, height)

    @JavascriptInterface
    fun failed(message: String) = onError(message)
}

/** 保留源站初始化上下文，应用仅观察原组件结果。 */
internal fun LoginVerification.Pow.widgetHtml(): String {
    val page = Jsoup.parse(pageHtml, pageUrl)
    page.selectFirst("form[data-lsb-pow-form] input[name=_csrf]")!!.attr("value", csrf)
    val observer = """
        (()=>{
          let last='',lastError='',scriptError='';
          let observedForm=null;
          const fail=message=>{
            if(message!==lastError){lastError=message;LsbPowStatus.failed(message)}
          };
          document.addEventListener('submit',event=>{
            event.preventDefault();event.stopImmediatePropagation();
          },true);
          window.addEventListener('error',event=>{
            const target=event.target;
            if(target&&target.tagName==='SCRIPT')scriptError='脚本加载失败：'+target.src;
            else if(event.message)scriptError=event.message;
          },true);
          const sync=()=>{
            const form=document.querySelector('form[data-lsb-pow-form]');
            const widget=document.querySelector('[data-lsb-pow-widget]');
            if(!form||!widget){
              if(document.readyState==='complete')fail('源站验证组件未进入页面，请重新加载');
              return;
            }
            if(form!==observedForm){
              observedForm=form;
              new MutationObserver(sync).observe(form,{subtree:true,childList:true,attributes:true,characterData:true});
            }
            const api=window.bbsHumanVerification;
            const ready=!!api&&typeof api.ready==='function'&&typeof api.append==='function'&&typeof api.reset==='function';
            let result='';
            try{
              if(ready&&api.ready(form)){
                const fields=new FormData();api.append(form,fields);
                const challenge=fields.get('human_verification_pow_challenge');
                const proof=fields.get('human_verification_pow_proof');
                if(typeof challenge==='string'&&challenge&&typeof proof==='string'&&proof){
                  const expires=Number(challenge.split('.')[2]);
                  if(expires&&Date.now()/1000>=expires)api.reset(form);
                  else result=JSON.stringify({human_verification_pow_challenge:challenge,human_verification_pow_proof:proof});
                }
              }
            }catch(error){
              fail('源站验证组件初始化失败：'+(error.message||String(error)));return;
            }
            const height=Math.ceil(widget.getBoundingClientRect().height);
            const current=JSON.stringify([ready,result,height]);
            if(current!==last){last=current;LsbPowStatus.changed(ready,result,height)}
            if(ready)lastError='';
            else if(document.readyState==='complete')fail('源站验证脚本未能初始化'+(scriptError?'：'+scriptError:'，请重新加载'));
          };
          document.addEventListener('DOMContentLoaded',sync);
          window.addEventListener('load',sync);
          setInterval(sync,500);
        })();
    """.trimIndent()
    val layout = """
        html,body{margin:0!important;padding:0!important;min-height:0!important;background:transparent!important}
        [data-lsb-pow-container]{display:block!important;visibility:visible!important;opacity:1!important;position:static!important;float:none!important;transform:none!important;
          width:100%!important;max-width:none!important;height:auto!important;min-height:0!important;max-height:none!important;
          margin:0!important;padding:0!important;border:0!important;overflow:visible!important;background:transparent!important}
        [data-lsb-pow-container]>:not([data-lsb-pow-container]):not([data-lsb-pow-widget]){display:none!important}
        [data-lsb-pow-widget]{display:block!important;visibility:visible!important;margin:0!important}
    """.trimIndent()
    page.head().prependElement("script").html(observer)
    page.head().appendElement("style").html(layout)
    return page.outerHtml()
}
