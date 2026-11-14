package sb.linux.client.platform.webview

import android.webkit.JsPromptResult
import android.webkit.JsResult
import android.webkit.WebView

/** 保留网页待返回的结果；宿主关闭或用户操作都只能完成一次。 */
internal class WebJsDialog(
    val owner: WebView,
    val kind: Kind,
    val url: String,
    val message: String,
    val defaultValue: String = "",
    private val result: JsResult,
) {
    enum class Kind { ALERT, CONFIRM, PROMPT, BEFORE_UNLOAD }
    private var completed = false

    fun finish(confirmed: Boolean, text: String = defaultValue) {
        if (completed) return
        completed = true
        if (!confirmed) result.cancel()
        else if (kind == Kind.PROMPT) (result as JsPromptResult).confirm(text)
        else result.confirm()
    }
}
