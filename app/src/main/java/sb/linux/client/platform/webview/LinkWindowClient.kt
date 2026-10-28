package sb.linux.client.platform.webview

import android.os.Message
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient

/** 将用户点开的 target=_blank / window.open 链接交回应用统一链接入口。 */
internal class LinkWindowClient(
    private val openLink: (String) -> Unit,
    private val chooseFiles: ((android.webkit.ValueCallback<Array<android.net.Uri>>, FileChooserParams) -> Boolean)? = null,
    private val onProgress: ((Int) -> Unit)? = null,
) : WebChromeClient() {
    override fun onProgressChanged(view: WebView, newProgress: Int) {
        onProgress?.invoke(newProgress)
    }

    override fun onShowFileChooser(
        view: WebView,
        callback: android.webkit.ValueCallback<Array<android.net.Uri>>,
        params: FileChooserParams,
    ): Boolean = chooseFiles?.invoke(callback, params) ?: false
    override fun onCreateWindow(view: WebView, isDialog: Boolean, isUserGesture: Boolean, resultMsg: Message): Boolean {
        if (!isUserGesture) return false
        val transport = resultMsg.obj as? WebView.WebViewTransport ?: return false
        val popup = WebView(view.context)
        popup.webViewClient = object : WebViewClient() {
            private var opened = false
            private fun open(url: String): Boolean {
                val uri = android.net.Uri.parse(url)
                if (uri.scheme?.lowercase() !in setOf("http", "https")) return true
                if (!opened) {
                    opened = true
                    openLink(url)
                    popup.post { popup.destroy() }
                }
                return true
            }
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean = open(request.url.toString())
            @Suppress("DEPRECATION")
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean = open(url)
        }
        transport.webView = popup
        resultMsg.sendToTarget()
        return true
    }
}
