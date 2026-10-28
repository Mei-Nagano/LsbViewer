package sb.linux.client.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import sb.linux.client.common.network.Endpoints
import sb.linux.client.ui.session.Session

/** 保留原站广告 DOM、点击脚本、CSRF 表单及奖励弹窗，不自行发起奖励请求。 */
@Composable
fun SourcePromotionsScreen(session: Session, nav: NavHostController) {
    val context = LocalContext.current
    WebScreen(session, nav, sourceUrl = "/", title = "原站广告",
        onOpenWindow = { url ->
            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
                .onFailure { session.showToast("无法打开广告链接") }
        },
        onLoaded = { view, url ->
            if (Endpoints.isInternal(Uri.parse(url).host)) view.evaluateJavascript(PROMOTIONS_LAYOUT, null)
        })
}

// 只调整显示范围，保持原站事件绑定、表单、奖励状态和模态窗口所在的 DOM。
private const val PROMOTIONS_LAYOUT = """
(function(){
 var board=document.querySelector('.sidebar-promotions-card');
 if(!board)return;
 var parent=board.parentElement;
 while(parent){parent.setAttribute('data-app-promotions-shell','');parent=parent.parentElement;}
 if(document.getElementById('app-promotions-layout'))return;
 var style=document.createElement('style');style.id='app-promotions-layout';
 style.textContent='[data-app-promotions-shell]>:not([data-app-promotions-shell]):not(.sidebar-promotions-card):not(script):not(style):not(form):not([role="dialog"]):not([id*="modal"]):not([class*="modal"]):not(#toast){display:none!important}'+
 '.wrap,.sidebar,.sidebar-promotions-card{width:100%!important;max-width:none!important;margin:0!important;float:none!important}'+
 '[data-app-promotions-shell]{grid-template-columns:1fr!important}body{padding:12px!important}';
 document.head.appendChild(style);
})();
"""
