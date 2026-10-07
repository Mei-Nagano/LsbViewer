package sb.linux.client.ui.session

import android.app.Application
import androidx.compose.runtime.mutableStateMapOf
import sb.linux.client.di.SessionCacheRepositories
import sb.linux.client.domain.ai.AiSummaryHistoryRepository
import sb.linux.client.domain.topic.NewTopicDraftRepository
import sb.linux.client.domain.topic.TopicDanmakuCacheRepository
import sb.linux.client.domain.topic.TopicPageCacheRepository
import sb.linux.client.domain.topic.TopicReadingPositionRepository
import sb.linux.client.domain.topic.TopicViewsRepository
import sb.linux.client.domain.settings.DataManagementRepository

internal class SessionCacheOwner(
    private val app: Application,
    private val session: Session,
) {
    // SnapshotStateMap keeps cached reading-count updates observable to home cards.
    private val topicViews = mutableStateMapOf<Long, Int>()
    private val aiSummaryCache = mutableStateMapOf<Long, String>()
    private val repositories by lazy { SessionCacheRepositories(app, topicViews, aiSummaryCache) }

    internal val newTopicDraftRepository: NewTopicDraftRepository get() = repositories.newTopicDraftRepository
    internal val homeBrowseCache get() = repositories.homeBrowseCache
    internal val topicPageCacheRepository: TopicPageCacheRepository get() = repositories.topicPageCacheRepository
    internal val topicViewsRepository: TopicViewsRepository get() = repositories.topicViewsRepository
    internal val readingPositionRepository: TopicReadingPositionRepository
        get() = repositories.readingPositionRepository
    internal val danmakuCacheRepository: TopicDanmakuCacheRepository get() = repositories.danmakuCacheRepository
    internal val aiSummaryHistoryRepository: AiSummaryHistoryRepository
        get() = repositories.aiSummaryHistoryRepository

    internal fun clearAiSummaryCache() = aiSummaryCache.clear()
    // ---------------- 清除本地缓存 ----------------

    /**
     * 清除全部本地缓存：内存缓存 + 持久化的首页/阅读量/侧边栏缓存 +
     * 临时文件 + Coil 图片磁盘缓存。保留登录态、设置与登录信息。
     */
    private val dataCleanupController by lazy {
        SessionDataCleanupController(app, session, this)
    }
    internal val dataManagementRepository: DataManagementRepository get() = dataCleanupController

    fun clearLocalCache() = dataCleanupController.clearLocalCache()

    /** 数据管理页的按项清理；键同时用于自动清理设置。 */
    fun clearDataItems(items: Set<String>, notify: Boolean = true) =
        dataCleanupController.clearDataItems(items, notify)

}
