package sb.linux.client.data

import sb.linux.client.domain.account.UserPageContent
import sb.linux.client.domain.account.UserPageRepository

internal class UserPageService(
    private val client: LsbClient,
) : UserPageRepository {
    override suspend fun load(userId: Long, tab: String): UserPageContent {
        val html = client.get("/user/$userId?tab=$tab").html
        val profile = runCatching { UserAccountHtmlParser.parseUserProfile(html) }.getOrNull()
        return when (tab) {
            "replies" -> UserPageContent(profile, replies = parseReplies(html))
            "points_rewards" -> UserPageContent(profile, points = parsePoints(html))
            "notifications" -> {
                val notifications = parseNotifications(html)
                UserPageContent(profile, notifications = notifications)
            }
            else -> UserPageContent(profile, topics = parseTopics(html))
        }
    }

    private fun parseReplies(html: String) =
        runCatching { CommunityFeedHtmlParser.parseReplyList(html) }.getOrDefault(emptyList())

    private fun parsePoints(html: String) =
        runCatching { PointsHtmlParser.parsePointRows(html) }.getOrDefault(emptyList())

    private fun parseNotifications(html: String) =
        runCatching { NotificationHtmlParser.parseNotifications(html) }.getOrDefault(emptyList())

    private fun parseTopics(html: String) =
        runCatching { TopicListParser.parseTopicList(html).first }.getOrDefault(emptyList())

}
