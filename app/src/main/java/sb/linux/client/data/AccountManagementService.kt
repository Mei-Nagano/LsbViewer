package sb.linux.client.data

import org.json.JSONObject
import sb.linux.client.common.network.Endpoints
import sb.linux.client.domain.account.*
import java.net.URI
import java.net.URLEncoder

internal class AccountManagementService(private val client: LsbClient) : AccountManagementRepository {
    override suspend fun load(url: String): AccountManagementPage = AccountManagementParser.parse(client.get(internalUrl(url)))

    override suspend fun submit(pageUrl: String, form: AccountForm, fields: List<Pair<String, String>>): AccountManagementPage {
        val action = internalUrl(form.action)
        if (form.method == "get") {
            val uri = URI(action)
            val target = URI(uri.scheme, uri.authority, uri.path, null, null).toString() + "?" +
                fields.joinToString("&") { "${encode(it.first)}=${encode(it.second)}" }
            return load(target)
        }
        check(form.method == "post") { "源站表单方式暂不支持" }
        val response = client.postFormPairs(action, fields)
        validateResponse(response)
        // 称号操作的响应页包含本次抽取／合成结果，优先保留；其他设置继续重新读取核对。
        val saved = if (URI(action).path.orEmpty().startsWith("/gacha") &&
            SourceHtmlSupport.sourceDocument(response.html).selectFirst(".gacha-center-page,.gacha-result") != null)
            AccountManagementParser.parse(response) else load(pageUrl)
        val verifyNames = when {
            URI(action).path == "/user_privacy" -> setOf("public[]", "direct_policy")
            URI(action).path == "/user_block" && form.fields.any { it.name == "block_ai" } -> setOf("block_ai")
            else -> emptySet()
        }
        if (verifyNames.isNotEmpty()) {
            val actualForm = findForms(saved.nodes).firstOrNull { it.action == form.action && it.fields.any { field -> field.name in verifyNames } }
                ?: error("源站未返回保存后的设置，请重新加载核对")
            val actual = actualForm.fields.filter { it.name in verifyNames }.flatMap { field ->
                when (field.type) {
                    "checkbox" -> if (field.checked) listOf(field.name to field.value) else emptyList()
                    "select" -> listOf(field.name to (field.options.firstOrNull { it.selected } ?: field.options.firstOrNull())?.value.orEmpty())
                    else -> listOf(field.name to field.value)
                }
            }.toSet()
            check(actual == fields.filter { it.first in verifyNames }.toSet()) { "源站未确认保存的设置，请重新加载核对" }
        }
        return saved
    }

    override suspend fun resolveRecipient(url: String, query: String): AccountRecipient {
        val path = internalUrl(url) + if ('?' in url) "&q=${encode(query)}" else "?q=${encode(query)}"
        val data = client.getAjaxJsonForRepository(path)
        check(data.optBoolean("ok") || data.optInt("ok") == 1) { data.optString("message").ifBlank { "未找到用户" } }
        val user = data.getJSONObject("user")
        val id = user.optLong("id")
        check(id > 0) { "源站未返回有效用户 ID" }
        return AccountRecipient(id, user.optString("username"))
    }

    override suspend fun uploadPromotionImage(url: String, fields: List<Pair<String, String>>, file: java.io.File): String {
        val data = JSONObject(client.postMultipart(internalUrl(url), fields.toMap(), "image_file", file, "image/jpeg").html)
        check(data.optBoolean("ok") || data.optInt("ok") == 1) { data.optString("message").ifBlank { "图片上传失败" } }
        return data.optString("url").takeIf(String::isNotBlank) ?: error("源站未返回图片地址")
    }

    private fun validateResponse(response: LsbClient.Resp) {
        check(!response.url.contains("/login")) { "登录已失效，请重新登录" }
        val doc = SourceHtmlSupport.sourceDocument(response.html)
        val error = doc.selectFirst(".form-error-panel,.form_error,.error-message,.alert-error")?.text().orEmpty()
        check(response.code in 200..299 && !response.url.contains("form_error") && error.isBlank()) {
            error.ifBlank { HtmlTextParser.extractError(response.html).ifBlank { "操作失败，请重新加载后核对" } }
        }
        if (response.html.trimStart().startsWith("{")) {
            val json = JSONObject(response.html)
            check(json.optBoolean("ok") || json.optInt("ok") == 1) { json.optString("message").ifBlank { "操作失败" } }
        }
    }

    private fun internalUrl(url: String): String {
        val result = Endpoints.abs(url)
        check(URI(result).host == Endpoints.HOST && URI(result).scheme == "https") { "管理操作必须使用源站地址" }
        return result
    }
    private fun encode(value: String): String = URLEncoder.encode(value, "UTF-8")
    private fun findForms(nodes: List<AccountNode>): List<AccountForm> = nodes.flatMap {
        when (it) {
            is AccountNode.Form -> listOf(it.value)
            is AccountNode.Gift -> listOf(it.form)
            is AccountNode.DrawActions -> it.forms
            is AccountNode.OwnedTitle -> it.actions + listOfNotNull(it.gift)
            is AccountNode.Group -> findForms(it.children)
            is AccountNode.Grid -> findForms(it.children)
            is AccountNode.TitleCard -> findForms(it.children)
            is AccountNode.Section -> findForms(it.children)
            is AccountNode.Actions -> findForms(it.children)
            is AccountNode.Storage -> listOfNotNull(it.form)
            is AccountNode.Invitation -> listOfNotNull(it.form)
            is AccountNode.Attachments -> listOfNotNull(it.batchForm) + it.files.flatMap { file -> file.actions.map { action -> action.form } }
            is AccountNode.Promotion -> findForms(it.actions)
            else -> emptyList()
        }
    }
}
