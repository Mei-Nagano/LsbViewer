package sb.linux.client.data

import sb.linux.client.domain.account.UserPageContent
import sb.linux.client.domain.account.UserPageRepository

internal class UserPageService(
    private val client: LsbClient,
    private val identity: () -> Long,
) : UserPageRepository {
    private data class Entry(val content: UserPageContent, val time: Long)
    private val cache = linkedMapOf<Pair<Long, String>, Entry>()
    private var cacheOwner = identity()
    private val mutex = kotlinx.coroutines.sync.Mutex()

    @Synchronized override fun cached(userId: Long, tab: String): UserPageContent? {
        if (cacheOwner != identity()) { cache.clear(); cacheOwner = identity() }
        return cache[userId to tab]?.takeIf { android.os.SystemClock.elapsedRealtime() - it.time < 300_000 }?.content
    }

    override suspend fun load(userId: Long, tab: String, forceRefresh: Boolean): UserPageContent {
        if (!forceRefresh) cached(userId, tab)?.let { return it }
        mutex.lock()
        try {
            if (!forceRefresh) cached(userId, tab)?.let { return it }
            val owner = identity()
            val content = loadFresh(userId, tab)
            check(owner == identity()) { "账号已切换，请重新加载" }
            synchronized(this) {
                check(owner == identity()) { "账号已切换，请重新加载" }
                if (cacheOwner != owner) { cache.clear(); cacheOwner = owner }
                cache[userId to tab] = Entry(content, android.os.SystemClock.elapsedRealtime())
                while (cache.size > 24) cache.remove(cache.keys.first())
            }
            return content
        } finally { mutex.unlock() }
    }

    private suspend fun loadFresh(userId: Long, tab: String): UserPageContent {
        if (tab in setOf("points_rewards", "notifications")) {
            val pages = SourceRecordPages(client, identity).load(userId, tab)
            val profile = UserAccountHtmlParser.parseUserProfile(pages.first().html)
            return if (tab == "points_rewards") UserPageContent(profile,
                points = pages.flatMap { PointsHtmlParser.parsePointRows(it.html) })
            else UserPageContent(profile, notifications = pages.flatMap { NotificationHtmlParser.parseNotifications(it.html) })
        }
        val html = client.get("/user/$userId?tab=$tab").html
        val profile = runCatching { UserAccountHtmlParser.parseUserProfile(html) }.getOrNull()
        return when (tab) {
            "replies" -> UserPageContent(profile, replies = parseReplies(html))
            else -> UserPageContent(profile, topics = parseTopics(html))
        }
    }

    private fun parseReplies(html: String) =
        runCatching { CommunityFeedHtmlParser.parseReplyList(html) }.getOrDefault(emptyList())

    private fun parseTopics(html: String) =
        runCatching { TopicListParser.parseTopicList(html).first }.getOrDefault(emptyList())

}
