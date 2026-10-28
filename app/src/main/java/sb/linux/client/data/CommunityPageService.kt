package sb.linux.client.data

import sb.linux.client.domain.community.ReportForm
import sb.linux.client.domain.community.NotificationRepository
import sb.linux.client.domain.community.IdentityRepository
import sb.linux.client.domain.community.InviteRepository
import sb.linux.client.domain.community.LeaderboardRepository
import sb.linux.client.domain.community.ReportRepository
import sb.linux.client.domain.community.IdentityCenterData
import sb.linux.client.domain.community.InviteCenter
import sb.linux.client.domain.community.LeaderRow
import sb.linux.client.domain.community.NotificationItem

internal class CommunityPageService(
    private val client: LsbClient,
    private val updateUnread: (Int) -> Unit,
) : NotificationRepository, IdentityRepository, InviteRepository, LeaderboardRepository, ReportRepository {
    private val reportSubmissionData = mutableMapOf<Pair<Long, String>, ReportFormParseResult>()
    override suspend fun loadIdentity(): IdentityCenterData =
        CommunityAccountHtmlParser.parseIdentityCenter(client.get("/identity_center").html)

    override suspend fun loadInvitations(): InviteCenter =
        CommunityAccountHtmlParser.parseInviteCenter(client.get("/invite_center").html)

    override suspend fun loadLeaderboard(type: String): List<LeaderRow> =
        CommunityFeedHtmlParser.parseLeaderboard(client.get("/leaderboard?type=$type").html)

    override suspend fun loadNotifications(userId: Long): List<NotificationItem> {
        val html = client.get("/user/$userId?tab=notifications").html
        val items = NotificationHtmlParser.parseNotifications(html)
        updateUnread(0)
        return items
    }

    override suspend fun loadReport(reportId: Long, type: String): ReportForm {
        val parsed = ReportHtmlParser.parseReport(
            client.get("/content_report/$reportId?type=$type").html,
        )
        reportSubmissionData[reportId to type] = parsed
        return parsed.form
    }

    override suspend fun submitReport(
        reportId: Long,
        type: String,
        reason: String,
        details: String,
    ): String? {
        val info = reportSubmissionData[reportId to type]
            ?: error("举报表单已失效，请重新加载")
        val response = client.postForm(
            "/content_report/$reportId?type=$type",
            mapOf(
                "_csrf" to client.csrf(),
                "target_type" to info.targetType.ifBlank { type },
                "target_id" to info.targetId.ifBlank { reportId.toString() },
                "reason_type" to reason,
                "reason_details" to details,
            ),
        )
        return if (response.url.contains("form_error")) {
            HtmlTextParser.extractError(response.html).ifBlank { "提交失败" }
        } else {
            null
        }
    }
}
