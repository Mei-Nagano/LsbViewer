package sb.linux.client.data

import sb.linux.client.domain.account.CheckinInfo
import sb.linux.client.domain.account.CheckinRepository
import sb.linux.client.domain.account.CheckinResult

/** Owns the source forum's check-in page parsing and submission protocol. */
internal class CheckinPageService(
    private val client: LsbClient,
    private val applyInfo: (CheckinInfo) -> Unit,
) : CheckinRepository {
    override suspend fun load(): CheckinInfo {
        val response = client.get("/daily_checkin")
        return CheckinHtmlParser.parseCheckin(response.html).also(applyInfo)
    }

    override suspend fun submit(): CheckinResult {
        val csrf = client.csrf()
        val response = client.postForm("/daily_checkin", mapOf("_csrf" to csrf))
        return if (response.url.contains("form_error")) {
            CheckinResult(false, HtmlTextParser.extractError(response.html).ifBlank { "签到失败" })
        } else {
            val info = CheckinHtmlParser.parseCheckin(response.html)
            CheckinResult(true, info.message.ifBlank { "签到成功" })
        }
    }
}
