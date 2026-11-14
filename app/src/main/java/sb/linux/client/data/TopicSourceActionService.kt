package sb.linux.client.data

import java.net.URI
import org.json.JSONObject
import sb.linux.client.common.network.Endpoints
import sb.linux.client.domain.account.*
import sb.linux.client.domain.topic.*

/** 操作来源限定为当前帖子公开的表单或按需面板，服务器决定最终权限。 */
internal class TopicSourceActionService(private val client: LsbClient, private val invalidate: (Long) -> Unit) {
    private val panels = linkedMapOf<Pair<Long, String>, List<AccountForm>>()

    suspend fun load(page: TopicPageData, url: String): AccountManagementPage {
        check(page.footerPanels.any { it.url == url }) { "源站未提供此操作面板" }
        val response = client.get(sourceUrl(url))
        check(response.code in 200..299 && !response.url.contains("/login")) { "操作面板不可用" }
        val doc = SourceHtmlSupport.sourceDocument(response.html)
        val forms = doc.select("form[action*=red_packet]").map { element ->
            val copy = element.clone()
            // JS 按红包类型启用这些字段，保留条件关系而不漏掉可切换的输入。
            copy.select("[data-red-packet-type-field]").forEach { field ->
                field.removeAttr("hidden"); field.select("[disabled]").removeAttr("disabled")
            }
            val parsed = AccountManagementParser.parseForm(copy, response.url)
            parsed.copy(fields = parsed.fields.map { field -> when (field.name) {
                "red_packet_approval_condition", "red_packet_settlement_days" -> field.copy(showWhenName = "red_packet_type", showWhenValue = "discussion")
                "red_packet_fixed_amount" -> field.copy(showWhenName = "red_packet_type", showWhenValue = "random", showWhenDifferent = true)
                "red_packet_total_amount" -> field.copy(showWhenName = "red_packet_type", showWhenValue = "random")
                else -> field
            } })
        }
        panels[page.topicId to page.csrf] = forms
        while (panels.size > 20) panels.remove(panels.keys.first())
        val notice = doc.selectFirst(".red-packet-add > p,.red-packet-topup > p")?.text().orEmpty()
        val nodes = listOfNotNull(notice.takeIf(String::isNotBlank)?.let { AccountNode.Text(it) }) + forms.map { AccountNode.Form(it) }
        check(nodes.isNotEmpty()) { "源站当前未提供红包操作权限" }
        return AccountManagementPage(url, nodes)
    }

    suspend fun submit(page: TopicPageData, form: AccountForm, fields: List<Pair<String, String>>): TopicActionResult {
        val available = page.posts.flatMap { it.sourceActions } + panels[page.topicId to page.csrf].orEmpty()
        val original = available.firstOrNull { it.id == form.id } ?: error("操作表单已失效，请刷新帖子")
        val actions = listOf(original.action) + original.buttons.filter { it.enabled }.map { it.action }.filter(String::isNotBlank)
        check(form.method == "post" && form.action in actions && original.buttons.any { it.enabled }) { "源站当前不允许此操作" }
        val verificationNames = when (val verification = original.verification) {
            is LoginVerification.Cap -> listOf(verification.fieldName)
            is LoginVerification.Native -> listOf("native_captcha_token", "native_captcha_answer")
            else -> emptyList()
        }
        val names = (original.hidden.map { it.first } + original.fields.map { it.name } + original.buttons.map { it.name } + verificationNames).toSet()
        check(fields.all { it.first in names }) { "操作字段不属于源站表单" }
        check(original.hidden.filterNot { it.first == "_csrf" || it.first in verificationNames }.all { it in fields }) { "操作目标已变化，请刷新帖子" }
        val type = fields.firstOrNull { it.first == "red_packet_type" }?.second
        val normalized = if (fields.any { it == "topic_special_type" to "red_packet" }) {
            fields.filterNot { it.first == "red_packet_distribution" } + ("red_packet_distribution" to if (type == "random") "random" else "fixed")
        } else fields
        val response = client.postFormPairs(sourceUrl(form.action), normalized.filterNot { it.first == "_csrf" } +
            ("_csrf" to original.hidden.firstOrNull { it.first == "_csrf" }?.second.orEmpty().ifBlank { page.csrf.ifBlank { client.csrf() } }))
        val doc = SourceHtmlSupport.sourceDocument(response.html)
        val error = doc.selectFirst(".form-error-panel,.form_error,.error-message,.alert-error")?.text().orEmpty()
        val json = if (response.html.trimStart().startsWith("{")) JSONObject(response.html) else null
        val success = response.code in 200..299 && !response.url.contains("form_error") && !response.url.contains("/login") &&
            error.isBlank() && (json == null || json.optBoolean("ok") || json.optInt("ok") == 1)
        if (success) { invalidate(page.topicId); panels.remove(page.topicId to page.csrf) }
        return TopicActionResult(if (success) json?.optString("message").orEmpty().ifBlank { "操作成功" }
            else error.ifBlank { json?.optString("message").orEmpty().ifBlank { HtmlTextParser.extractError(response.html).ifBlank { "操作失败" } } }, success)
    }

    private fun sourceUrl(raw: String): String = Endpoints.abs(raw).also {
        val uri = URI(it)
        check(uri.scheme == "https" && Endpoints.isInternal(uri.host)) { "操作必须使用源站地址" }
    }
}
