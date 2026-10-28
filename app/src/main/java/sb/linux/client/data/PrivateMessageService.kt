package sb.linux.client.data

import sb.linux.client.domain.community.DirectMessageContact
import sb.linux.client.domain.community.DirectMessageThread
import sb.linux.client.domain.community.PrivateMessageRepository
import java.net.URLEncoder

/** 原站是私信的唯一数据源。 */
internal class PrivateMessageService(private val client: LsbClient) : PrivateMessageRepository {
    override suspend fun loadContacts(query: String): List<DirectMessageContact> {
        val path = if (query.isBlank()) "/direct_messages" else
            "/index.php?a=direct_messages&q=${URLEncoder.encode(query.trim(), "UTF-8") }"
        val response = client.get(path)
        check(!response.url.contains("login")) { "登录已失效" }
        check(response.code in 200..299) { "私信加载失败（${response.code}）" }
        check(response.html.contains("direct-messages")) { "未能识别原站私信页面" }
        return DirectMessageHtmlParser.parseDirectMessageContacts(response.html)
    }

    override suspend fun loadThread(partnerId: Long): DirectMessageThread {
        val response = client.get("/direct_messages/$partnerId")
        check(!response.url.contains("login")) { "登录已失效" }
        check(response.code in 200..299) { "会话加载失败（${response.code}）" }
        check(response.html.contains("direct-messages-thread")) { "未能识别原站会话，请刷新重试" }
        return DirectMessageHtmlParser.parseDirectMessageThread(response.html, partnerId)
    }

    override suspend fun toggleBlock(partnerId: Long): String? {
        val page = client.get("/direct_messages/$partnerId")
        check(!page.url.contains("login")) { "登录已失效" }
        val document = SourceHtmlSupport.sourceDocument(page.html)
        val form = document.select("form").firstOrNull { it.selectFirst("input[name=direct_messages_block_action]") != null }
            ?: error("未找到原站拉黑操作表单")
        val fields = form.select("input[type=hidden][name]").associate { it.attr("name") to it.attr("value") }
        check(!fields["_csrf"].isNullOrBlank()) { "操作表单已失效" }
        val response = client.postForm("/direct_messages/$partnerId", fields)
        check(!response.url.contains("login") && response.code in 200..299) { "拉黑操作失败，请刷新" }
        val failed = response.url.contains("form_error") || SourceHtmlSupport.sourceDocument(response.html)
            .selectFirst(".form-error-panel, .form_error, .error-message") != null
        return if (failed) HtmlTextParser.extractError(response.html) else null
    }

    override suspend fun sendMessage(partnerId: Long, body: String): String? {
        require(partnerId > 0 && body.isNotBlank() && body.codePointCount(0, body.length) <= 500) { "私信需要 1–500 字" }
        val page = client.get("/direct_messages/$partnerId")
        check(!page.url.contains("login")) { "登录已失效" }
        val document = SourceHtmlSupport.sourceDocument(page.html)
        val form = document.select("form").firstOrNull { it.selectFirst("textarea[name=content]") != null }
            ?: error("原站当前不允许发送私信")
        val fields = form.select("input[type=hidden][name]").associate { it.attr("name") to it.attr("value") }.toMutableMap()
        check(!fields["_csrf"].isNullOrBlank()) { "发送表单已失效，请刷新重试" }
        fields["content"] = body
        val response = client.postForm("/direct_messages/$partnerId", fields)
        check(!response.url.contains("login")) { "登录已失效，消息未发送" }
        check(response.code in 200..299) { "发送失败（${response.code}）" }
        val result = SourceHtmlSupport.sourceDocument(response.html)
        val failed = response.url.contains("form_error") || result.selectFirst(".form-error-panel, .form_error, .error-message") != null
        return if (failed) HtmlTextParser.extractError(response.html).ifBlank { "发送失败" } else null
    }
}
