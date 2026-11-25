package sb.linux.client.data

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.launch
import sb.linux.client.domain.account.UserPageContent
import sb.linux.client.domain.account.UserPageRepository
import sb.linux.client.domain.community.NotificationItem
import sb.linux.client.domain.community.PointRow

/**
 * 用户主页各标签的数据入口。
 *
 * 积分明细与通知历史要顺着源站分页逐页读取，因此单独走 [loadIncrementally]：
 * 先给出已有快照，再逐页补数据，命中已知记录就提前收工，不必每次进来都从头爬一遍。
 * 翻页任务挂在会话作用域里，按 (userId, tab) 复用：中途离开页面不会把已爬的进度扔掉，
 * 回到页面直接接着看正在跑或已经跑完的结果。
 */
internal class UserPageService(
    private val client: LsbClient,
    private val identity: () -> Long,
    private val records: UserRecordCacheStore,
    private val scope: CoroutineScope,
    /** 真正读到源站通知页时回调：源站在那次请求里就把通知标为已读。 */
    private val onNotificationsRead: () -> Unit,
) : UserPageRepository {
    private data class Entry(val content: UserPageContent, val time: Long)

    /** 会话级翻页任务：内容随分页推进，running 为 false 表示这一轮已经结束。 */
    private class RecordLoad(val state: MutableStateFlow<RecordLoadState>) {
        var job: Job? = null
    }

    private data class RecordLoadState(
        val content: UserPageContent? = null,
        val running: Boolean = true,
        /** 本轮是否已经真的拿到过源站响应（而不是只回放了快照）。 */
        val fromSource: Boolean = false,
        val error: Throwable? = null,
    )

    private val cache = linkedMapOf<Pair<Long, String>, Entry>()
    private val loads = mutableMapOf<Pair<Long, String>, RecordLoad>()
    private val mutexes = mutableMapOf<Pair<Long, String>, kotlinx.coroutines.sync.Mutex>()
    private var cacheOwner = identity()

    @Synchronized override fun cached(userId: Long, tab: String): UserPageContent? {
        if (cacheOwner != identity()) { cache.clear(); cacheOwner = identity() }
        cache[userId to tab]?.let { return it.content }
        // 内存里没有：读上次落盘的快照顶住界面。快照带着落盘时间，仍在新鲜窗口内就算命中。
        val snapshot = records.load(userId, tab) ?: return null
        cache[userId to tab] = Entry(snapshot.content, elapsedAt(snapshot.savedAt))
        return snapshot.content
    }

    @Synchronized override fun clearAll() {
        val jobs = loads.values.mapNotNull { it.job }
        loads.clear()
        cache.clear()
        jobs.forEach { it.cancel() }
        scope.launch(Dispatchers.IO) { records.clearAll() }
    }

    /** 把墙钟落盘时间换算到 elapsedRealtime 时间轴；跨重启换算结果很旧，自然视为过期。 */
    private fun elapsedAt(savedAt: Long) =
        android.os.SystemClock.elapsedRealtime() - (System.currentTimeMillis() - savedAt)

    @Synchronized private fun fresh(userId: Long, tab: String): UserPageContent? {
        val content = cached(userId, tab) ?: return null
        val time = cache[userId to tab]?.time ?: return null
        if (!content.complete) return null
        if (android.os.SystemClock.elapsedRealtime() - time >= FRESH_MS) return null
        return content
    }

    @Synchronized private fun mutexFor(userId: Long, tab: String): kotlinx.coroutines.sync.Mutex =
        mutexes.getOrPut(userId to tab) {
            // 只在没人持有时回收，别把正在爬长历史的锁换掉
            if (mutexes.size >= MAX_MUTEXES) {
                mutexes.entries.firstOrNull { !it.value.isLocked }?.let { mutexes.remove(it.key) }
            }
            kotlinx.coroutines.sync.Mutex()
        }

    @Synchronized private fun ensureLoad(userId: Long, tab: String, forceRefresh: Boolean): RecordLoad {
        val key = userId to tab
        val active = loads[key]
        if (!forceRefresh && active != null && active.job?.isActive == true) return active
        active?.job?.cancel()
        if (loads.size >= MAX_LOADS) {
            loads.entries.firstOrNull { it.value.job?.isActive != true }?.let { loads.remove(it.key) }
        }
        val load = RecordLoad(MutableStateFlow(RecordLoadState(cached(userId, tab))))
        loads[key] = load
        load.job = scope.launch(Dispatchers.IO) {
            var failure: Throwable? = null
            try {
                when (tab) {
                    TAB_POINTS -> crawlPoints(userId, load)
                    TAB_NOTIFICATIONS -> crawlNotifications(userId, load)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                failure = error
            } finally {
                load.state.value = load.state.value.copy(running = false, error = failure)
            }
        }
        return load
    }

    private fun recordFlow(
        userId: Long,
        tab: String,
        forceRefresh: Boolean,
        emitCached: Boolean,
    ): Flow<UserPageContent> = flow {
        if (!forceRefresh) fresh(userId, tab)?.let { emit(it); return@flow }
        val load = ensureLoad(userId, tab, forceRefresh)
        var snapshot = load.state.value
        var emitted: UserPageContent? = null
        while (true) {
            val content = snapshot.content
            if (content != null && content !== emitted && (emitCached || snapshot.fromSource)) {
                emit(content)
                emitted = content
            }
            if (!snapshot.running) {
                snapshot.error?.let { throw it }
                return@flow
            }
            snapshot = load.state.first { it != snapshot }
        }
    }

    override fun loadIncrementally(userId: Long, tab: String, forceRefresh: Boolean): Flow<UserPageContent> = when (tab) {
        // 通知列表的界面状态本来就从 cachedNotifications 起步，这里不回放旧快照：
        // 「收到源站响应」是清未读角标的唯一依据，不能和「只是读了缓存」混在一起。
        TAB_POINTS -> recordFlow(userId, tab, forceRefresh, emitCached = true)
        TAB_NOTIFICATIONS -> recordFlow(userId, tab, forceRefresh, emitCached = false)
        else -> flow { emit(load(userId, tab, forceRefresh)) }
    }

    /** 积分明细：按时间倒序累计，命中旧记录时接上旧历史的剩余部分并停止翻页。 */
    private suspend fun crawlPoints(userId: Long, load: RecordLoad) {
        val mutex = mutexFor(userId, TAB_POINTS)
        mutex.lock()
        try {
            val owner = identity()
            // 拿到锁之后再读旧快照，避免用加锁前的过期副本做衔接
            val old = cached(userId, TAB_POINTS)
            val oldRows = old?.points.orEmpty()
            val rows = mutableListOf<PointRow>()
            var latest = old ?: UserPageContent(null, complete = false)
            var complete = false
            var unsavedPages = 0
            SourceRecordPages(client, identity).load(userId, TAB_POINTS) { response, document, page, total ->
                val batch = PointsHtmlParser.parsePointRows(document)
                rows.addAll(batch)
                val profile = if (page == 1) UserAccountHtmlParser.parseUserProfile(response.html) else latest.profile
                val offset = if (batch.all { it.timestamp > 0 }) uniqueOverlap(oldRows, batch) else null
                val stop = offset != null && coversAll(old)
                val visibleRows = when {
                    stop -> rows.dropLast(batch.size) + oldRows.drop(offset!!)
                    old != null && rows.isNotEmpty() && rows.all { it.timestamp > 0 } &&
                        rows.zipWithNext().all { (a, b) -> a.timestamp >= b.timestamp } ->
                        rows + oldRows.filter { it.timestamp < rows.last().timestamp }
                    else -> rows.toList()
                }
                complete = stop
                latest = UserPageContent(profile, points = visibleRows, loadedPages = page,
                    totalPages = total, complete = stop)
                publish(userId, TAB_POINTS, owner, latest)
                load.state.value = RecordLoadState(latest, running = true, fromSource = true)
                if (++unsavedPages >= DISK_EVERY_PAGES || stop) {
                    unsavedPages = 0
                    persist(userId, TAB_POINTS, owner, latest)
                }
                !stop
            }
            if (!complete) latest = latest.copy(points = rows.toList(), complete = true)
            publish(userId, TAB_POINTS, owner, latest)
            // 收尾这一帧也要推给订阅方，否则界面最后看到的仍是 complete = false 的上一次拼接结果
            load.state.value = RecordLoadState(latest, running = true, fromSource = true)
            persist(userId, TAB_POINTS, owner, latest)
        } finally { mutex.unlock() }
    }

    /** 通知历史：整页与旧快照连续衔接才算读到已知处；同内容的不同记录照样保留，不做批内去重。 */
    private suspend fun crawlNotifications(userId: Long, load: RecordLoad) {
        val mutex = mutexFor(userId, TAB_NOTIFICATIONS)
        mutex.lock()
        try {
            val owner = identity()
            val old = cached(userId, TAB_NOTIFICATIONS)
            val oldItems = old?.notifications.orEmpty()
            val oldKeys = oldItems.map(::notificationKey)
            val oldCoversAll = coversAll(old)
            val items = mutableListOf<NotificationItem>()
            var latest = old ?: UserPageContent(null, complete = false)
            var complete = false
            var unsavedPages = 0
            SourceRecordPages(client, identity).load(userId, TAB_NOTIFICATIONS) { response, document, page, total ->
                val batch = NotificationHtmlParser.parseNotifications(document)
                items.addAll(batch)
                val profile = if (page == 1) UserAccountHtmlParser.parseUserProfile(response.html) else latest.profile
                val offset = uniqueOverlap(oldKeys, batch.map(::notificationKey))
                val stop = offset != null && oldCoversAll
                val visibleItems = if (offset != null) items.dropLast(batch.size) + oldItems.drop(offset) else items.toList()
                complete = stop
                latest = UserPageContent(profile, notifications = visibleItems, loadedPages = page,
                    totalPages = total, complete = stop)
                publish(userId, TAB_NOTIFICATIONS, owner, latest)
                if (page == 1) onNotificationsRead()
                load.state.value = RecordLoadState(latest, running = true, fromSource = true)
                if (++unsavedPages >= DISK_EVERY_PAGES || stop) {
                    unsavedPages = 0
                    persist(userId, TAB_NOTIFICATIONS, owner, latest)
                }
                !stop
            }
            if (!complete) latest = latest.copy(notifications = items.toList(), complete = true)
            publish(userId, TAB_NOTIFICATIONS, owner, latest)
            // 收尾这一帧也要推给订阅方，否则界面最后看到的仍是 complete = false 的上一次拼接结果
            load.state.value = RecordLoadState(latest, running = true, fromSource = true)
            persist(userId, TAB_NOTIFICATIONS, owner, latest)
        } finally { mutex.unlock() }
    }

    /** 旧快照是否已经读到最后一页；页数未知时不能算完整。 */
    private fun coversAll(old: UserPageContent?): Boolean =
        old != null && (old.complete || (old.totalPages > 0 && old.loadedPages >= old.totalPages))

    /** 只复用整页连续且唯一的旧记录序列，保留相同记录的出现次数。 */
    private fun uniqueOverlap(old: List<Any?>, batch: List<Any?>): Int? {
        if (batch.isEmpty() || old.size < batch.size) return null
        val matches = (0..old.size - batch.size).filter { offset -> old.subList(offset, offset + batch.size) == batch }
        return matches.singleOrNull()
    }

    /**
     * 通知的去重键。不含「未读」标记：源站在打开通知页时就会把记录标为已读，
     * 把它算进键里会让同一批通知对不上旧快照。
     */
    private fun notificationKey(item: NotificationItem): String =
        listOf(item.link, item.fromUser, item.typeText, item.timeText, item.content).joinToString("\u0001")

    /** 只更新内存状态，让界面立刻看到进度。 */
    private fun publish(userId: Long, tab: String, owner: Long, content: UserPageContent) = synchronized(this) {
        check(owner == identity()) { "账号已切换，请重新加载" }
        if (cacheOwner != owner) { cache.clear(); cacheOwner = owner }
        cache[userId to tab] = Entry(content, android.os.SystemClock.elapsedRealtime())
        while (cache.size > MAX_ENTRIES) cache.remove(cache.keys.first())
    }

    /** 落盘较慢，由翻页协程（IO 线程）在若干页之后与结束时调用。 */
    private fun persist(userId: Long, tab: String, owner: Long, content: UserPageContent) {
        check(owner == identity()) { "账号已切换，请重新加载" }
        records.save(userId, tab, content)
    }

    override suspend fun load(userId: Long, tab: String, forceRefresh: Boolean): UserPageContent {
        if (tab == TAB_POINTS || tab == TAB_NOTIFICATIONS) return loadIncrementally(userId, tab, forceRefresh).last()
        if (!forceRefresh) fresh(userId, tab)?.let { return it }
        val mutex = mutexFor(userId, tab)
        mutex.lock()
        try {
            if (!forceRefresh) fresh(userId, tab)?.let { return it }
            val owner = identity()
            val content = loadFresh(userId, tab)
            check(owner == identity()) { "账号已切换，请重新加载" }
            publish(userId, tab, owner, content)
            return content
        } finally { mutex.unlock() }
    }

    private suspend fun loadFresh(userId: Long, tab: String): UserPageContent {
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

    private companion object {
        const val TAB_POINTS = "points_rewards"
        const val TAB_NOTIFICATIONS = "notifications"
        const val FRESH_MS = 5 * 60 * 1000L
        const val MAX_ENTRIES = 24
        const val MAX_LOADS = 8
        const val MAX_MUTEXES = 32
        const val DISK_EVERY_PAGES = 5
    }
}
