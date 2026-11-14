package sb.linux.client.data

import sb.linux.client.domain.account.UserPageContent
import sb.linux.client.domain.account.UserPageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.last
import sb.linux.client.domain.community.PointRow

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
        return cache[userId to tab]?.content
    }

    @Synchronized private fun fresh(userId: Long, tab: String): UserPageContent? = cached(userId, tab)
        ?.takeIf { it.complete && android.os.SystemClock.elapsedRealtime() - (cache[userId to tab]?.time ?: 0) < 300_000 }

    private fun save(userId: Long, tab: String, owner: Long, content: UserPageContent) = synchronized(this) {
        check(owner == identity()) { "账号已切换，请重新加载" }
        if (cacheOwner != owner) { cache.clear(); cacheOwner = owner }
        cache[userId to tab] = Entry(content, android.os.SystemClock.elapsedRealtime())
        while (cache.size > 24) cache.remove(cache.keys.first())
    }

    override fun loadIncrementally(userId: Long, tab: String, forceRefresh: Boolean): Flow<UserPageContent> = flow {
        if (tab != "points_rewards") { emit(load(userId, tab, forceRefresh)); return@flow }
        if (!forceRefresh) fresh(userId, tab)?.let { emit(it); return@flow }
        cached(userId, tab)?.let { emit(it.copy(loadedPages = 0, complete = false)) }
        mutex.lock()
        try {
            if (!forceRefresh) fresh(userId, tab)?.let { emit(it); return@flow }
            val owner = identity()
            val old = cached(userId, tab)
            val rows = mutableListOf<PointRow>()
            var latest = old ?: UserPageContent(null, complete = false)
            var connected = false
            SourceRecordPages(client, identity).load(userId, tab) { response, page, total ->
                val batch = PointsHtmlParser.parsePointRows(response.html)
                rows.addAll(batch)
                val profile = if (page == 1) UserAccountHtmlParser.parseUserProfile(response.html) else latest.profile
                val offset = if (old?.complete == true) uniqueOverlap(old.points, batch) else null
                connected = offset != null
                val visibleRows = when {
                    offset != null -> rows.dropLast(batch.size) + old!!.points.drop(offset)
                    old != null && rows.isNotEmpty() && rows.all { it.timestamp > 0 } &&
                        rows.zipWithNext().all { (a, b) -> a.timestamp >= b.timestamp } ->
                        rows + old.points.filter { it.timestamp < rows.last().timestamp }
                    else -> rows.toList()
                }
                latest = UserPageContent(profile, points = visibleRows, loadedPages = page,
                    totalPages = total, complete = connected)
                check(owner == identity()) { "账号已切换，请重新加载" }
                // 完整旧历史在刷新中断时仍保留；首次读取则缓存已经取得的分页。
                if (latest.complete || old?.complete != true) save(userId, tab, owner, latest)
                emit(latest)
                !connected
            }
            if (!connected) latest = latest.copy(points = rows.toList(), complete = true)
            save(userId, tab, owner, latest)
            emit(latest)
        } finally { mutex.unlock() }
    }

    /** 只复用整页连续且唯一的旧记录序列，保留相同交易的出现次数。 */
    private fun uniqueOverlap(old: List<PointRow>, batch: List<PointRow>): Int? {
        if (batch.size < 2 || batch.any { it.timestamp <= 0 } || old.size < batch.size) return null
        val matches = (0..old.size - batch.size).filter { offset -> old.subList(offset, offset + batch.size) == batch }
        return matches.singleOrNull()
    }

    override suspend fun load(userId: Long, tab: String, forceRefresh: Boolean): UserPageContent {
        if (tab == "points_rewards") return loadIncrementally(userId, tab, forceRefresh).last()
        if (!forceRefresh) fresh(userId, tab)?.let { return it }
        mutex.lock()
        try {
            if (!forceRefresh) fresh(userId, tab)?.let { return it }
            val owner = identity()
            val content = loadFresh(userId, tab)
            check(owner == identity()) { "账号已切换，请重新加载" }
            save(userId, tab, owner, content)
            return content
        } finally { mutex.unlock() }
    }

    private suspend fun loadFresh(userId: Long, tab: String): UserPageContent {
        if (tab == "notifications") {
            val pages = SourceRecordPages(client, identity).load(userId, tab)
            val profile = UserAccountHtmlParser.parseUserProfile(pages.first().html)
            return UserPageContent(profile, notifications = pages.flatMap { NotificationHtmlParser.parseNotifications(it.html) })
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
