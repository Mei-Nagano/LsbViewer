package sb.linux.client.di

import android.app.Application
import sb.linux.client.data.AiSummaryHistoryStore
import sb.linux.client.data.HomeCacheStore
import sb.linux.client.data.NewTopicDraftStore
import sb.linux.client.data.ReadingPositionStore
import sb.linux.client.data.TopicDanmakuCacheStore
import sb.linux.client.data.TopicPageCacheStore
import sb.linux.client.data.TopicViewsCacheStore
import sb.linux.client.domain.ai.AiSummaryHistoryRepository
import sb.linux.client.domain.community.HomeBrowseCache
import sb.linux.client.domain.topic.NewTopicDraftRepository
import sb.linux.client.domain.topic.TopicDanmakuCacheRepository
import sb.linux.client.domain.topic.TopicPageCacheRepository
import sb.linux.client.domain.topic.TopicReadingPositionRepository
import sb.linux.client.domain.topic.TopicViewsRepository

/** Creates the app-scoped cache and local-store adapters for one Session. */
internal class SessionCacheRepositories(
    private val app: Application,
    private val topicViews: MutableMap<Long, Int>,
    private val aiSummaryCache: MutableMap<Long, String>,
) {
    internal val newTopicDraftRepository: NewTopicDraftRepository by lazy {
        NewTopicDraftStore()
    }
    internal val homeBrowseCache: HomeBrowseCache by lazy { HomeCacheStore(app) }
    internal val topicPageCacheRepository: TopicPageCacheRepository by lazy { TopicPageCacheStore() }
    internal val topicViewsRepository: TopicViewsRepository by lazy { TopicViewsCacheStore(app, topicViews) }
    internal val readingPositionRepository: TopicReadingPositionRepository by lazy { ReadingPositionStore(app) }
    internal val danmakuCacheRepository: TopicDanmakuCacheRepository by lazy { TopicDanmakuCacheStore() }
    internal val aiSummaryHistoryRepository: AiSummaryHistoryRepository by lazy {
        AiSummaryHistoryStore(
            app.getSharedPreferences("lsb_ai_summaries", android.content.Context.MODE_PRIVATE),
            aiSummaryCache,
        )
    }
}
