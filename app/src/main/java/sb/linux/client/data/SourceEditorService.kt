package sb.linux.client.data

import org.json.JSONObject
import org.jsoup.nodes.Element
import sb.linux.client.domain.account.AccountForm
import sb.linux.client.domain.topic.*
import sb.linux.client.common.network.Endpoints
import java.net.URI
import java.net.URLEncoder

/** 只读取当前源站字段；不携带未选帖型的默认值。 */
internal class SourceEditorService(private val client: LsbClient) : SourceEditorRepository {
    override suspend fun load(url: String): SourceEditorPage {
        val response = client.get(internalUrl(url))
        check(!response.url.contains("/login")) { "登录已失效，请重新登录" }
        val doc = SourceHtmlSupport.sourceDocument(response.html)
        val form = doc.selectFirst("form:has(textarea[name=body]):has(input[name=title])")
            ?: error("源站没有提供发帖表单")
        val extensions = form.select("[data-topic-extension]").mapNotNull { panel ->
            val toggle = panel.selectFirst("[data-topic-extension-toggle][name]") ?: return@mapNotNull null
            val extensionLabel = panel.selectFirst("summary")?.text()?.trim().orEmpty()
                .ifBlank { panel.selectFirst("> strong")?.text()?.trim().orEmpty() }
                .ifBlank { panel.selectFirst(".invite-relay-choice")?.text()?.trim().orEmpty() }
                .ifBlank { panel.selectFirst("strong")?.text()?.trim().orEmpty() }
                .ifBlank { "扩展选项" }
            val copy = panel.clone()
            copy.removeAttr("hidden")
            copy.select("[hidden]").forEach { it.removeAttr("hidden") }
            copy.select("[disabled]").forEach { it.removeAttr("disabled") }
            copy.select("[data-topic-extension-toggle], .invite-relay-choice").remove()
            val wrapper = Element("form").attr("method", "post").attr("action", response.url)
            wrapper.appendChild(copy)
            val parsed = AccountManagementParser.parseForm(wrapper, response.url)
            val fields = parsed.fields.map { field -> when (field.name) {
                "poll_max_choices" -> field.copy(showWhenName = "poll_allow_multiple", showWhenValue = "1")
                "virtual_card_auto_reply_content" -> field.copy(showWhenName = "virtual_card_auto_reply", showWhenValue = "1")
                "red_packet_approval_condition", "red_packet_settlement_days" -> field.copy(showWhenName = "red_packet_type", showWhenValue = "discussion")
                "red_packet_fixed_amount" -> field.copy(showWhenName = "red_packet_type", showWhenValue = "random", showWhenDifferent = true)
                "red_packet_total_amount" -> field.copy(showWhenName = "red_packet_type", showWhenValue = "random")
                else -> field
            } }
            val walletGuard = panel.selectFirst("[data-lottery-wallet-min][data-lottery-wallet-balance]")
            val minimum = walletGuard?.attr("data-lottery-wallet-min")?.toLongOrNull()
            val balance = walletGuard?.attr("data-lottery-wallet-balance")?.toLongOrNull()
            val unavailable = if (minimum != null && balance != null && balance < minimum)
                "源站要求发布抽奖帖至少持有 $minimum ${walletGuard.attr("data-lottery-wallet-name")}，当前余额 $balance" else ""
            SourceEditorExtension(extensionLabel,
                toggle.attr("name") to toggle.attr("value"), parsed.copy(fields = fields), toggle.hasAttr("checked"), unavailable)
        }
        val base = form.clone()
        base.select("[data-topic-extension],.attachment-uploader,[data-topic-extension-controls]").remove()
        val parsed = AccountManagementParser.parseForm(base, response.url)
        val upload = form.selectFirst(".attachment-uploader")
        val editor = form.selectFirst("[data-nb-editor]")
        val csrf = form.selectFirst("input[name=_csrf]")?.attr("value").orEmpty()
        return SourceEditorPage(response.url, parsed.copy(id = "topic-editor:$url"), extensions,
            AccountManagementParser.resolve(response.url, form.selectFirst("[data-nb-editor-mention-url]")?.attr("data-nb-editor-mention-url").orEmpty()),
            AccountManagementParser.resolve(response.url, upload?.attr("data-upload-url").orEmpty()),
            (upload?.attr("data-upload-max-mb")?.toLongOrNull() ?: 20) * 1024 * 1024,
            upload?.selectFirst("input[type=file]")?.attr("accept").orEmpty(),
            AccountManagementParser.resolve(response.url, editor?.attr("data-nb-editor-preview-url").orEmpty()),
            AccountManagementParser.resolve(response.url, editor?.attr("data-nb-editor-video-url").orEmpty()),
            editor?.attr("data-nb-editor-kind").orEmpty().ifBlank { "topic" },
            editor?.attr("data-nb-editor-topic").orEmpty().ifBlank { "0" },
            AccountManagementParser.resolve(response.url, editor?.attr("data-nb-editor-remote-url").orEmpty()),
            StickerPackHtmlParser.parsePicker(form, csrf))
    }

    override suspend fun submit(form: AccountForm, fields: List<Pair<String, String>>): Long {
        check(form.method == "post") { "源站编辑表单不支持此提交方式" }
        var pairs = fields
        if (pairs.any { it == "poll_enabled" to "1" }) {
            val options = pairs.filter { it.first == "poll_options[]" && it.second.isNotBlank() }
            check(options.size in 2..20 && options.all { it.second.length <= 30 }) { "投票需要2至20个选项，每项最多30字" }
            if (pairs.none { it == "poll_allow_multiple" to "1" }) pairs = pairs.filterNot { it.first == "poll_max_choices" }
        }
        if (pairs.any { it == "topic_special_type" to "red_packet" }) {
            val type = pairs.firstOrNull { it.first == "red_packet_type" }?.second
            if (type != "random") {
                val count = pairs.firstOrNull { it.first == "red_packet_count" }?.second?.toLongOrNull()
                val amount = pairs.firstOrNull { it.first == "red_packet_fixed_amount" }?.second?.toLongOrNull()
                check(count != null && amount != null) { "请填写红包数量和单个金额" }
                pairs = pairs.filterNot { it.first in setOf("red_packet_total_amount", "red_packet_distribution") } +
                    listOf("red_packet_distribution" to "fixed")
            } else pairs = pairs.filterNot { it.first == "red_packet_distribution" } + ("red_packet_distribution" to "random")
        }
        val response = client.postFormPairs(internalUrl(form.action), pairs)
        check(response.code in 200..299 && !response.url.contains("form_error") && !response.url.contains("/login")) {
            HtmlTextParser.extractError(response.html).ifBlank { "发帖失败，请重新加载表单" }
        }
        val id = Regex("/topic/(\\d+)").find(URI(response.url).path.orEmpty())?.groupValues?.get(1)?.toLongOrNull()
        return id ?: error("源站没有返回保存后的主题地址，输入已保留，请核对后再操作")
    }

    override suspend fun mentions(url: String, query: String): List<MentionUser> {
        val data = client.getAjaxJsonForRepository(internalUrl(url) + (if ('?' in url) "&" else "?") +
            "q=" + URLEncoder.encode(query, "UTF-8"))
        check(data.optInt("ok") == 1 || data.optBoolean("ok")) { data.optString("message", "用户建议暂时不可用") }
        val users = data.optJSONArray("users") ?: return emptyList()
        return (0 until users.length()).mapNotNull { index ->
            val user = users.optJSONObject(index) ?: return@mapNotNull null
            val name = user.optString("username").takeIf(String::isNotBlank) ?: return@mapNotNull null
            MentionUser(name, user.optString("group"), SourceHtmlSupport.absUrl(
                SourceHtmlSupport.sourceDocument(user.optString("avatar")).selectFirst("img[src]")?.attr("src").orEmpty()))
        }
    }

    override suspend fun upload(url: String, csrf: String, file: java.io.File, mime: String, originalName: String, original: Boolean): String {
        val response = client.postMultipart(internalUrl(url), mapOf("_csrf" to csrf,
            "attachment_original_name" to originalName) + if (original) mapOf("attachment_original" to "1") else emptyMap(), "attachment", file, mime, ajax = true)
        val data = JSONObject(response.html)
        check(response.code in 200..299 && (data.optInt("ok") == 1 || data.optBoolean("ok"))) {
            data.optString("message").ifBlank { "附件上传失败" }
        }
        return data.optString("markdown").takeIf(String::isNotBlank) ?: error("源站未返回附件内容")
    }

    override suspend fun preview(url: String, csrf: String, body: String, kind: String, topicId: String): String {
        val data = client.postAjax(internalUrl(url), mapOf("_csrf" to csrf, "body" to body, "kind" to kind, "topic_id" to topicId))
        check(data.optBoolean("ok") || data.optInt("ok") == 1) { data.optString("message").ifBlank { "预览失败" } }
        return data.optString("html")
    }

    override suspend fun video(url: String, csrf: String, share: String, kind: String): String {
        check(kind != "reply") { "源站回帖不支持插入视频" }
        val data = client.postAjax(internalUrl(url), mapOf("_csrf" to csrf, "share" to share, "kind" to kind))
        check(data.optBoolean("ok") || data.optInt("ok") == 1) { data.optString("message").ifBlank { "视频解析失败" } }
        val target = data.optString("url")
        check(URI(target).scheme == "https") { "源站未返回有效视频地址" }
        val label = when (data.optString("platform")) {
            "mp4" -> "MP4视频"; "bilibili" -> "哔哩哔哩视频"; "youtube" -> "YouTube视频"; "douyin" -> "抖音视频"
            else -> error("源站不支持此视频平台")
        }
        return "\n\n[$label]($target)\n\n"
    }

    override suspend fun remoteImage(url: String, csrf: String, imageUrl: String): SourceRemoteImage {
        check(URI(imageUrl).scheme in setOf("http", "https")) { "图片地址无效" }
        val data = client.postAjax(internalUrl(url), mapOf("_csrf" to csrf, "url" to imageUrl))
        check(data.optBoolean("ok") || data.optInt("ok") == 1) { data.optString("message").ifBlank { "图片转存失败" } }
        val mime = data.optString("type")
        check(mime in setOf("image/jpeg", "image/png", "image/gif", "image/webp")) { "源站未返回受支持图片" }
        return SourceRemoteImage(android.util.Base64.decode(data.getString("data"), android.util.Base64.DEFAULT), mime)
    }

    override suspend fun poll(url: String): TopicPoll {
        val response = client.get(internalUrl(url))
        return TopicPollParser.parse(SourceHtmlSupport.sourceDocument(response.html), null, null)
            ?: error("源站未返回投票内容")
    }

    private fun internalUrl(url: String): String = Endpoints.abs(url).also {
        check(URI(it).host == Endpoints.HOST && URI(it).scheme == "https") { "编辑器只允许使用源站接口" }
    }
}
