package sb.linux.client.data

import sb.linux.client.domain.profile.PersonalSignaturePage
import sb.linux.client.domain.profile.PersonalSignatureRepository

/** 只处理源站已核对的个人签名表单，不解析页面侧栏或其他账号设置。 */
internal class PersonalSignatureService(private val client: LsbClient) : PersonalSignatureRepository {
    override suspend fun load(): PersonalSignaturePage = parse(client.get(PATH))

    override suspend fun save(signature: String, hidden: Boolean): PersonalSignaturePage {
        val fields = buildMap {
            put("_csrf", client.csrf())
            put("signature", signature)
            // 源站 unchecked checkbox 不参与表单提交。
            if (hidden) put("hidden", "1")
        }
        val response = client.postForm(PATH, fields)
        if (response.code !in 200..299 || response.url.contains("form_error")) {
            error(HtmlTextParser.extractError(response.html).ifBlank { "签名保存失败，请重试" })
        }
        val validationError = SourceHtmlSupport.sourceDocument(response.html)
            .selectFirst(".form-error-panel, .form_error, .error-message")?.text().orEmpty()
        check(validationError.isBlank()) { validationError }
        // POST 可以重定向，重新读取原表单确认服务端实际保存值。
        val saved = load()
        check(saved.signature.trim() == signature.trim() && saved.hidden == hidden) {
            "源站未确认保存结果，请重新加载后核对"
        }
        return saved
    }

    private fun parse(response: LsbClient.Resp): PersonalSignaturePage {
        check(!response.url.contains("/login")) { "登录已失效，请重新登录" }
        val doc = SourceHtmlSupport.sourceDocument(response.html)
        val form = doc.selectFirst(".post-signature-page form[action=$PATH]")
            ?: error("未解析到个人签名表单，源站页面结构可能已变化")
        val signature = form.selectFirst("textarea[name=signature]")
            ?: error("源站签名字段不可用")
        val hidden = form.selectFirst("input[type=checkbox][name=hidden]")
            ?: error("源站签名显示设置不可用")
        val panel = form.parent()
        return PersonalSignaturePage(
            signature = signature.wholeText(),
            hidden = hidden.hasAttr("checked"),
            maxLength = signature.attr("maxlength").toIntOrNull()?.takeIf { it > 0 },
            description = panel?.selectFirst("> p:not(.muted)")?.text().orEmpty(),
            signatureHint = signature.parent()?.selectFirst("span")?.text().orEmpty(),
            hiddenHint = hidden.parent()?.selectFirst("span")?.text().orEmpty(),
        )
    }

    private companion object { const val PATH = "/post_signature" }
}
