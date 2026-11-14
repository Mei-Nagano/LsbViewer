package sb.linux.client.platform.webview

import android.os.Message
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.JsResult
import android.webkit.JsPromptResult
import sb.linux.client.common.network.Endpoints

/** 保留真正的子窗口和 opener，由应用外壳展示，不把窗口拆成一次链接跳转。 */
internal open class LinkWindowClient(
    private val showWindow: (WebView, WebView) -> Unit,
    private val closeWindow: (WebView) -> Unit,
    private val showJsDialog: (WebJsDialog) -> Unit,
    private val chooseFiles: ((android.webkit.ValueCallback<Array<android.net.Uri>>, FileChooserParams) -> Boolean)? = null,
    private val onProgress: ((Int) -> Unit)? = null,
    private val onTitle: ((WebView, String) -> Unit)? = null,
    private val useNativeJsDialogs: Boolean = false,
) : WebChromeClient() {
    override fun onJsAlert(view: WebView, url: String, message: String, result: JsResult): Boolean {
        if (useNativeJsDialogs) return super.onJsAlert(view, url, message, result)
        showJsDialog(WebJsDialog(view, WebJsDialog.Kind.ALERT, url, message, result = result))
        return true
    }

    override fun onJsConfirm(view: WebView, url: String, message: String, result: JsResult): Boolean {
        if (useNativeJsDialogs) return super.onJsConfirm(view, url, message, result)
        showJsDialog(WebJsDialog(view, WebJsDialog.Kind.CONFIRM, url, message, result = result))
        return true
    }

    override fun onJsPrompt(view: WebView, url: String, message: String, defaultValue: String?, result: JsPromptResult): Boolean {
        if (useNativeJsDialogs) return super.onJsPrompt(view, url, message, defaultValue, result)
        showJsDialog(WebJsDialog(view, WebJsDialog.Kind.PROMPT, url, message, defaultValue.orEmpty(), result))
        return true
    }

    override fun onJsBeforeUnload(view: WebView, url: String, message: String, result: JsResult): Boolean {
        if (useNativeJsDialogs) return super.onJsBeforeUnload(view, url, message, result)
        showJsDialog(WebJsDialog(view, WebJsDialog.Kind.BEFORE_UNLOAD, url, message, result = result))
        return true
    }

    override fun onReceivedTitle(view: WebView, title: String) {
        onTitle?.invoke(view, title)
    }

    override fun onProgressChanged(view: WebView, newProgress: Int) {
        onProgress?.invoke(newProgress)
    }

    override fun onShowFileChooser(
        view: WebView,
        callback: android.webkit.ValueCallback<Array<android.net.Uri>>,
        params: FileChooserParams,
    ): Boolean = chooseFiles?.invoke(callback, params) ?: false
    override fun onCreateWindow(view: WebView, isDialog: Boolean, isUserGesture: Boolean, resultMsg: Message): Boolean {
        val transport = resultMsg.obj as? WebView.WebViewTransport ?: return false
        val popup = WebView(view.context).apply {
            settings.javaScriptEnabled = view.settings.javaScriptEnabled
            settings.javaScriptCanOpenWindowsAutomatically = true
            settings.domStorageEnabled = view.settings.domStorageEnabled
            settings.userAgentString = view.settings.userAgentString
            settings.allowFileAccess = view.settings.allowFileAccess
            settings.allowContentAccess = view.settings.allowContentAccess
            settings.textZoom = view.settings.textZoom
            settings.mediaPlaybackRequiresUserGesture = view.settings.mediaPlaybackRequiresUserGesture
            settings.mixedContentMode = view.settings.mixedContentMode
            settings.setSupportMultipleWindows(true)
            android.webkit.CookieManager.getInstance().let {
                it.setAcceptThirdPartyCookies(this, it.acceptThirdPartyCookies(view))
            }
            val chrome = LinkWindowClient(showWindow, closeWindow, showJsDialog, chooseFiles,
                onTitle = onTitle, useNativeJsDialogs = useNativeJsDialogs)
            webChromeClient = chrome
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                    val destination = request.url
                    if (request.isForMainFrame && request.method.equals("GET", true) &&
                        destination.scheme.equals("http", true) && Endpoints.isInternal(destination.host)) {
                        WebViewDoh.load(view, destination.buildUpon().scheme("https").build().toString())
                        return true
                    }
                    // about:blank、脚本写入、POST 和目标跳转由原窗口加载，沿用全局 DoH 代理。
                    return false
                }
            }
        }
        showWindow(view, popup)
        transport.webView = popup
        resultMsg.sendToTarget()
        return true
    }

    override fun onCloseWindow(window: WebView) { closeWindow(window) }
}
