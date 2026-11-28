package sb.linux.client.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import sb.linux.client.common.error.LsbException
import sb.linux.client.common.network.Endpoints
import sb.linux.client.domain.topic.*
import java.net.URI

internal class InviteRelayService(private val client: LsbClient) : InviteRelayRepository {

    override suspend fun loadView(url: String): InviteRelayViewData {
        val target = internalUrl(url)
        val json = try {
            client.getAjaxJsonForRepository(target)
        } catch (error: LsbException) {
            if (error.message == "AUTH_REQUIRED") throw LsbException("请先登录论坛，再参与邀请码接力")
            throw error
        }
        val ok = json.optBoolean("ok") || json.optInt("ok") == 1
        if (!ok) {
            throw LsbException(json.optString("message").ifBlank { "操作失败，请重试" })
        }
        val title = json.optString("title").trim().ifBlank { "邀请码接力" }
        val html = json.optString("html")
        return InviteRelayHtmlParser.parseViewHtml(target, title, html)
    }

    override suspend fun submitForm(
        actionUrl: String,
        fields: List<Pair<String, String>>,
        files: List<InviteRelayFileUpload>,
    ): InviteRelaySubmitResult = withContext(Dispatchers.IO) {
        val target = internalUrl(actionUrl)
        val bodyBuilder = MultipartBody.Builder().setType(MultipartBody.FORM)
        fields.forEach { (k, v) -> bodyBuilder.addFormDataPart(k, v) }
        files.forEach { upload ->
            val media = upload.mimeType.toMediaTypeOrNull() ?: "application/octet-stream".toMediaType()
            bodyBuilder.addFormDataPart(upload.fieldName, upload.fileName, upload.bytes.toRequestBody(media))
        }
        val req = Request.Builder()
            .url(target)
            .header("User-Agent", LsbClient.UA)
            .header("X-Requested-With", "XMLHttpRequest")
            .header("Accept", "application/json")
            .post(bodyBuilder.build())
            .build()

        var responseCode = 0
        var text = ""
        repeat(3) {
            client.http.newCall(req).execute().use { r ->
                responseCode = r.code
                text = r.body?.string() ?: ""
                if (r.request.url.encodedPath == "/login" || r.code == 401) {
                    val msg = runCatching { JSONObject(text).optString("message") }.getOrNull().orEmpty()
                    throw LsbException(msg.ifBlank { "请先登录论坛，再参与邀请码接力" })
                }
                if (ChallengePagePolicy.isChallenge(text)) {
                    if (!client.recoverAccess(r.request.url.toString(), text)) {
                        throw LsbException("已取消人机验证")
                    }
                } else {
                    val json = runCatching { JSONObject(text.trim()) }.getOrNull()
                        ?: throw LsbException("提交失败，请重试")
                    val ok = responseCode in 200..299 && (json.optBoolean("ok") || json.optInt("ok") == 1)
                    if (!ok) {
                        throw LsbException(json.optString("message").ifBlank { "操作失败，请重试" })
                    }
                    val topicId = json.optLong("topic_id", 0L)
                    val panelHtml = json.optString("panel_html")
                    val viewUrl = json.optString("view_url").trim().let {
                        if (it.isBlank()) target else Endpoints.abs(it)
                    }
                    val notice = json.optString("notice").trim()
                        .ifBlank { json.optString("tip").trim() }
                        .ifBlank { "已保存" }
                    return@withContext InviteRelaySubmitResult(
                        topicId = topicId,
                        viewUrl = viewUrl,
                        notice = notice,
                        updatedPanel = InviteRelayHtmlParser.parsePanelHtml(panelHtml, topicId),
                    )
                }
            }
        }
        throw LsbException("提交失败，请重试")
    }

    private fun internalUrl(url: String): String = Endpoints.abs(url).also {
        val uri = URI(it)
        check(uri.scheme == "https" && Endpoints.isForumHost(uri.host)) { "请求地址无效" }
    }
}
