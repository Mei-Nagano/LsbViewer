package sb.linux.client.platform.webview

import android.content.Context
import android.view.View
import android.webkit.WebView
import android.widget.FrameLayout

/** 一个原生视图树管理浏览器窗口，后台网页不创建独立的 Compose 触摸区域。 */
internal class WebViewWindowHost(context: Context) : FrameLayout(context) {
    private var current: WebView? = null

    fun show(windows: List<WebView>, selected: WebView?) {
        for (index in childCount - 1 downTo 0) {
            if (getChildAt(index) !in windows) removeViewAt(index)
        }
        windows.forEach { web ->
            if (web.parent !== this) {
                addView(web, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
            }
            web.visibility = if (web === selected) View.VISIBLE else View.INVISIBLE
        }
        if (current !== selected) {
            if (current?.parent === this) current?.clearFocus()
            current = selected
            selected?.bringToFront()
            selected?.requestFocus()
        }
    }
}
