package sb.linux.client.ui.session

import android.app.Application
import android.content.Context
import coil.Coil
import sb.linux.client.domain.settings.DataManagementRepository

internal class SessionDataCleanupController(
    private val context: Application,
    private val session: Session,
    private val cache: SessionCacheOwner,
) : DataManagementRepository {
    override fun autoClearItems(): Set<String> = session.autoClearItemsRepository.items()

    override fun saveAutoClearItems(items: Set<String>) {
        session.autoClearItemsRepository.save(items)
    }

    override fun clearAutoClearItems() {
        val items = autoClearItems()
        if (items.isNotEmpty()) clearDataItems(items, notify = false)
    }

    /** 保留登录态、设置与登录信息，只清理本地缓存和临时文件。 */
    fun clearLocalCache() {
        session.topicPageCacheRepository.clearAll()
        cache.clearAiSummaryCache()
        session.danmakuCacheRepository.clear()
        session.topicViewsRepository.clear()
        session.homeBrowseCache.clearHomeCache()
        session.homeBrowseCache.saveHomeSidebar(null)
        runCatching {
            context.getSharedPreferences("lsb_home_cache", Context.MODE_PRIVATE)
                .edit().clear().apply()
            context.getSharedPreferences("lsb_views", Context.MODE_PRIVATE)
                .edit().clear().apply()
            context.getSharedPreferences("lsb_sidebar", Context.MODE_PRIVATE)
                .edit().clear().apply()
            context.cacheDir.listFiles()?.forEach { file ->
                if (file.name.startsWith("avatar_upload_") || file.name.endsWith(".png") ||
                    file.name.endsWith(".html") || file.name.endsWith(".md")) {
                    file.delete()
                }
            }
        }
        runCatching { Coil.imageLoader(context).diskCache?.clear() }
        session.showToast("本地缓存已清除")
    }

    /** 清理数据管理页所选项目，可用于启动时执行用户已配置的自动清理。 */
    fun clearDataItems(items: Set<String>, notify: Boolean) {
        if ("home" in items) {
            session.homeBrowseCache.clearHomeCache()
            session.homeBrowse.homeTabs.forEach { tab ->
                tab.topics = emptyList()
                tab.loaded = false
            }
            session.homeBrowseCache.saveHomeSidebar(null)
        }
        if ("topic" in items) {
            session.topicPageCacheRepository.clearAll()
            session.danmakuCacheRepository.clear()
            session.topicViewsRepository.clear()
            context.getSharedPreferences("lsb_views", Context.MODE_PRIVATE)
                .edit().clear().apply()
        }
        if ("images" in items) runCatching {
            Coil.imageLoader(context).apply {
                diskCache?.clear()
                memoryCache?.clear()
            }
            session.uiInvalidation?.clearBodyImageLayoutCache()
        }
        if ("reading_positions" in items) session.readingPositionRepository.clear()
        if ("ai_history" in items) session.aiSummaryHistoryRepository.clear()
        if ("ai_config" in items) session.aiConfigurationRepository.reset()
        if ("link_previews" in items) session.linkPreviewRepository.clearCache()
        if ("background_image" in items) session.preferences.removeBackgroundImage()
        if ("history" in items) session.browsingHistoryRepository.clear()
        if ("favorites" in items) session.favoritesRepository.clearTopics()
        if ("comment_favorites" in items) session.favoritesRepository.clearComments()
        if ("card_redemptions" in items) session.cardRedemptionsRepository.clear()
        if (notify) session.showToast("已清理所选数据")
    }

    override suspend fun clearDataItems(items: Set<String>) = clearDataItems(items, notify = true)
}
