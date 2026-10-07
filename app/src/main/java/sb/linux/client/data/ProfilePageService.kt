package sb.linux.client.data

import java.io.File
import sb.linux.client.domain.profile.ProfileSettingsForm
import sb.linux.client.domain.profile.ProfileSettingsPage
import sb.linux.client.domain.profile.ProfileSettingsRepository
import sb.linux.client.domain.profile.ProfileSettingsLoadResult
import sb.linux.client.domain.profile.ProfileFormSubmissionResult
import sb.linux.client.domain.profile.ProfileSettingsUpload

internal class ProfilePageService(private val client: LsbClient) : ProfileSettingsRepository {
    private val sourceForms = mutableMapOf<String, ProfileSourceForm>()

    override suspend fun load(): ProfileSettingsLoadResult {
        sourceForms.clear()
        val response = client.get("/profile")
        if (response.url.contains("/login")) {
            return ProfileSettingsLoadResult(error = "请先登录后管理个人信息")
        }
        val parsed = ProfilePageHtmlParser.parseProfilePage(response.html)
        sourceForms.putAll(parsed.sourceForms)
        val page = parsed.page
        val empty = page.forms.isEmpty() && page.info.isEmpty()
        return ProfileSettingsLoadResult(
            page = page,
            error = if (empty) "未解析到设置内容，源站页面结构可能已变化" else null,
        )
    }

    override suspend fun submit(
        form: ProfileSettingsForm,
        fields: Map<String, String>,
        upload: ProfileSettingsUpload?,
    ): ProfileFormSubmissionResult {
        val source = sourceForms[form.id] ?: error("个人资料表单已失效，请重新加载")
        val payload = form.hidden + fields + ("_csrf" to client.csrf())
        val file = upload?.let { File(it.path) }
        val response = if (file != null && source.hasFile) {
            client.postMultipart(source.action, payload, source.fileFieldName, file, upload.mimeType)
        } else {
            client.postForm(source.action, payload)
        }
        val rejected = response.url.contains("form_error") || response.url.contains("error")
        val message = if (rejected) {
            HtmlTextParser.extractError(response.html).ifBlank { "提交失败" }
        } else {
            "✓ ${form.submitText}成功"
        }
        return ProfileFormSubmissionResult(message, shouldRefresh = !response.url.contains("form_error"))
    }
}
