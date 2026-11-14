package sb.linux.client.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.navigation.NavHostController
import org.json.JSONObject
import sb.linux.client.common.network.Endpoints
import sb.linux.client.ui.session.Session

/** 使用原站完整编辑器，发布、草稿与附件均遵循原站规则。 */
@Composable
fun SourceEditorScreen(
    session: Session, nav: NavHostController, editId: Long = 0L,
    replyTopicId: Long = 0L, quote: String = "", onClose: (() -> Unit)? = null,
    sourceUrl: String? = null,
) {
    var quoteApplied by remember { mutableStateOf(false) }
    val close = { if (onClose != null) onClose() else { nav.popBackStack(); Unit } }
    val path = sourceUrl ?: when {
        replyTopicId > 0 -> "/topic/$replyTopicId#reply"
        editId > 0 -> "/topic_edit/$editId"
        else -> "/topic_edit"
    }
    WebScreen(session, nav, sourceUrl = path,
        title = when { replyTopicId > 0 -> "回复帖子"; editId > 0 -> "编辑帖子"; else -> "发表主题" },
        onBack = close,
        initialContentSelector = if (replyTopicId > 0) "#reply" else "form:has(input[name=title]), main",
        onLoaded = { view, url ->
            val uri = android.net.Uri.parse(url)
            if (replyTopicId > 0 && !quoteApplied && Endpoints.isInternal(uri.host) && uri.path == "/topic/$replyTopicId") {
                quoteApplied = true
                // 仅注入用户在应用中选择的引用内容，不自动提交。
                if (quote.isNotBlank()) view.evaluateJavascript("""
                    (function(){var form=document.getElementById('reply');
                    var field=form&&form.querySelector('textarea');
                    if(field&&!field.value){field.value=${JSONObject.quote(quote)};
                    field.dispatchEvent(new Event('input',{bubbles:true}));}})();
                """.trimIndent(), null)
            }
        })
}
