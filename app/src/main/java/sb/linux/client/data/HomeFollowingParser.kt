package sb.linux.client.data

import org.jsoup.nodes.Document
import sb.linux.client.domain.community.FollowingUser

internal object HomeFollowingParser {
    fun parseUsers(document: Document): List<FollowingUser> {
        val onlineIds = SourceHtmlSupport.onlineIdsOf(document)
        return document.select(".main-panel .post-list > li.user-follow-user-item").mapNotNull { item ->
            val name = item.selectFirst("a.user-follow-user-name[href]") ?: return@mapNotNull null
            val uid = SourceHtmlSupport.idFrom(name.attr("href"))
            if (uid <= 0) return@mapNotNull null
            val meta = item.selectFirst(".user-follow-user-meta")
            val time = meta?.selectFirst("time[datetime], [data-performance-time]")
            val followedAt = time?.attr("data-performance-time")?.toLongOrNull()
                ?: time?.attr("datetime")?.let { runCatching { java.time.OffsetDateTime.parse(it).toEpochSecond() }.getOrNull() }
            val followText = meta?.children()?.firstOrNull { it.normalName() != "a" && it.text().trim().endsWith("关注") }
                ?.text()?.trim().orEmpty()
            val followers = meta?.selectFirst("[data-user-follow-count-for], a")?.text()?.trim().orEmpty()
            FollowingUser(
                userId = uid,
                username = name.text().trim(),
                avatarUrl = SourceHtmlSupport.absUrl(SourceHtmlSupport.avatarOf(item)),
                group = SourceHtmlSupport.groupBadgeOf(item)?.name.orEmpty(),
                meta = listOf(followers, followText).filter(String::isNotBlank).joinToString(" · "),
                titleBadge = SourceHtmlSupport.gachaTitleOf(item),
                online = uid in onlineIds || SourceHtmlSupport.onlineOf(item),
                following = item.selectFirst(".user-follow-inline-button")?.hasClass("is-following") == true,
                canFollow = item.selectFirst(".user-follow-inline-form button:not([disabled])") != null,
                groupBadge = SourceHtmlSupport.groupBadgeOf(item), followersText = followers,
                followTimeText = followText, followedAt = followedAt, capturedAt = System.currentTimeMillis() / 1000,
            )
        }.distinctBy(FollowingUser::userId)
    }
}
