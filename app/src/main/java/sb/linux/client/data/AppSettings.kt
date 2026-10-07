package sb.linux.client.data

import sb.linux.client.domain.network.DohServer

import android.content.Context
import android.content.SharedPreferences
import sb.linux.client.domain.ai.AiConfigPreset
import sb.linux.client.domain.gacha.CardRedemptionRecord
import sb.linux.client.domain.community.CommentFavorite
import sb.linux.client.domain.community.UsageEvent
import sb.linux.client.domain.community.DirectMessageThread
import sb.linux.client.domain.community.PointRow
import sb.linux.client.domain.topic.TopicCard
import sb.linux.client.domain.topic.VirtualCard

/** Compatibility facade for the existing settings API. Storage is split by domain. */
class AppSettings(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("lsb_app_settings", Context.MODE_PRIVATE)
    private val themePrefs: SharedPreferences = context.getSharedPreferences("lsb_prefs", Context.MODE_PRIVATE)
    private val aiRecords = context.getSharedPreferences("lsb_ai_summaries", Context.MODE_PRIVATE)
    private val drafts = context.getSharedPreferences("lsb_topic_drafts", Context.MODE_PRIVATE)
    private val readingRecords = context.getSharedPreferences("lsb_reading_positions", Context.MODE_PRIVATE)

    private val browse = AppSettingsBrowseStore(prefs)
    private val general = AppSettingsGeneralStore(prefs)
    private val ai = AppSettingsAiStore(prefs)
    private val account = AppSettingsAccountStore(prefs)
    private val history = AppSettingsHistoryStore(prefs)
    private val cards = AppSettingsCardStore(prefs)
    private val usage = AppSettingsUsageStore(prefs)
    private val favorites = AppSettingsFavoriteStore(prefs)
    private val commentFavorites = AppSettingsCommentFavoriteStore(prefs)
    private val messages = AppSettingsPrivateMessageStore(prefs)

    var infiniteScroll by browse::infiniteScroll
    var homeKeepCache by browse::homeKeepCache
    var collapseNewTopics by browse::collapseNewTopics
    var danmakuOn by browse::danmakuOn
    var threadDialogEnabled by browse::threadDialogEnabled
    var unfavoriteConfirm by browse::unfavoriteConfirm
    var sidebarTwoColumns by browse::sidebarTwoColumns
    var sidebarShowOnlineUsers by browse::sidebarShowOnlineUsers
    var topicsPerPage by browse::topicsPerPage
    var commentSortOrder by browse::commentSortOrder
    var commentsPerPage by browse::commentsPerPage
    var tableDisplayMode by browse::tableDisplayMode
    var showUid by browse::showUid
    var smartDecodeEnabled by browse::smartDecodeEnabled
    var autoClearItems by browse::autoClearItems
    var homeSortDrawerOpen by browse::homeSortDrawerOpen
    var scrollModeOverrides by browse::scrollModeOverrides
    var linkOpenMode by general::linkOpenMode
    var linkPreviewEnabled by general::linkPreviewEnabled
    var proxyType by general::proxyType
    var proxyHost by general::proxyHost
    var proxyPort by general::proxyPort
    fun saveProxy(type: Int, host: String, port: Int) = general.saveProxy(type, host, port)
    var dohEnabled by general::dohEnabled
    var dohUrl by general::dohUrl
    fun dohServers() : List<DohServer> = general.dohServers()
    fun saveDohServers(servers: List<DohServer>) = general.saveDohServers(servers)
    var dohDisableOnVpn by general::dohDisableOnVpn
    var dohAutoWithoutVpn by general::dohAutoWithoutVpn
    var useBuiltInImageHost by general::useBuiltInImageHost
    var imageHostUrl by general::imageHostUrl
    var imageHostToken by general::imageHostToken
    var imageHostField by general::imageHostField
    var updateCheckMode by general::updateCheckMode
    var updateCheckIntervalHours by general::updateCheckIntervalHours
    var lastUpdateCheck by general::lastUpdateCheck
    var tabletMode by general::tabletMode
    var bottomBarStyle by general::bottomBarStyle
    var bottomBarItems by general::bottomBarItems
    var replyBarStyle by general::replyBarStyle
    var webdavUrl by general::webdavUrl
    var webdavUser by general::webdavUser
    var webdavPass by general::webdavPass
    var webdavDir by general::webdavDir
    var backupItems by general::backupItems
    fun resetBrowse() = general.resetBrowse()
    fun resetAi() = general.resetAi()
    fun resetGeneral() = general.resetGeneral()
    fun resetNetwork() = general.resetNetwork()
    fun resetWebdav() = general.resetWebdav()
    var aiEnabled by ai::aiEnabled
    var aiAutoRun by ai::aiAutoRun
    var aiUrl by ai::aiUrl
    var aiKey by ai::aiKey
    var aiModel by ai::aiModel
    var aiPrompt by ai::aiPrompt
    fun aiConfigPresets() : List<AiConfigPreset> = ai.aiConfigPresets()
    fun saveCurrentAiPreset(name: String) = ai.saveCurrentAiPreset(name)
    fun applyAiPreset(preset: AiConfigPreset) = ai.applyAiPreset(preset)
    fun removeAiPreset(name: String) = ai.removeAiPreset(name)
    var aiTemp by ai::aiTemp
    var aiIncludeComments by ai::aiIncludeComments
    var blockedWords by ai::blockedWords
    var blockedUsers by ai::blockedUsers
    fun checkinDoneToday() : Boolean = account.checkinDoneToday()
    fun markCheckinDone() = account.markCheckinDone()
    fun saveCheckinSummary(streak: Int, total: Int) = account.saveCheckinSummary(streak, total)
    fun checkinSummaryToday() : Pair<Int, Int>? = account.checkinSummaryToday()
    fun clearCheckinCache() = account.clearCheckinCache()
    var rememberPassword by account::rememberPassword
    var savedUsername by account::savedUsername
    var savedPassword by account::savedPassword
    fun clearSavedPassword() = account.clearSavedPassword()
    fun addHistory( topicId: Long, title: String, authorId: Long, authorName: String, forumId: Long, forumName: String, avatarUrl: String, titleColor: String, ) = history.addHistory(topicId, title, authorId, authorName, forumId, forumName, avatarUrl, titleColor)
    fun historyList() : List<sb.linux.client.domain.topic.TopicCard> = history.historyList()
    fun removeHistory(topicId: Long) = history.removeHistory(topicId)
    fun clearHistory() = history.clearHistory()
    fun recordCardRedemptions(topicId: Long, topicTitle: String, card: VirtualCard?) = cards.recordCardRedemptions(topicId, topicTitle, card)
    fun cardRedemptions() : List<CardRedemptionRecord> = cards.cardRedemptions()
    fun clearCardRedemptions() = cards.clearCardRedemptions()
    fun recordUsageEvent(type: String, topicId: Long = 0, title: String = "", value: Int = 0) = usage.recordUsageEvent(type, topicId, title, value)
    fun usageEvents() : List<UsageEvent> = usage.usageEvents()
    var usageAiSummary by usage::usageAiSummary
    var sourceUsageJson by usage::sourceUsageJson
    var builtInImageHostProvider by usage::builtInImageHostProvider
    var confirmGachaPull by usage::confirmGachaPull
    fun saveSourcePoints(rows: List<PointRow>) = usage.saveSourcePoints(rows)
    fun sourcePoints() : List<PointRow> = usage.sourcePoints()
    val sourcePointsSyncedAt by usage::sourcePointsSyncedAt
    fun clearUsageStats() = usage.clearUsageStats()
    fun addFavorite( topicId: Long, title: String, authorId: Long, authorName: String, forumId: Long, forumName: String, avatarUrl: String, titleColor: String, ) = favorites.addFavorite(topicId, title, authorId, authorName, forumId, forumName, avatarUrl, titleColor)
    fun favoriteList() : List<sb.linux.client.domain.topic.TopicCard> = favorites.favoriteList()
    fun mergeFavorites(cards: List<TopicCard>) = favorites.mergeFavorites(cards)
    fun removeFavorite(topicId: Long) = favorites.removeFavorite(topicId)
    fun clearFavorites() = favorites.clearFavorites()
    fun addCommentFavorite( replyId: Long, topicId: Long, topicTitle: String, floor: Int, authorId: Long, authorName: String, avatarUrl: String, content: String, ) = commentFavorites.addCommentFavorite(replyId, topicId, topicTitle, floor, authorId, authorName, avatarUrl, content)
    fun commentFavoriteList() : List<CommentFavorite> = commentFavorites.commentFavoriteList()
    fun removeCommentFavorite(replyId: Long) = commentFavorites.removeCommentFavorite(replyId)
    fun clearCommentFavorites() = commentFavorites.clearCommentFavorites()
    fun addPmMessage( partnerId: Long, partnerName: String, avatarUrl: String, incoming: Boolean, content: String, ts: Long = 0L, quoteText: String = "", synthetic: Boolean = false, ) = messages.addPmMessage(partnerId, partnerName, avatarUrl, incoming, content, ts, quoteText, synthetic)
    fun pmHasContent(partnerId: Long, content: String) : Boolean = messages.pmHasContent(partnerId, content)
    fun pmThread(partnerId: Long) : List<sb.linux.client.domain.community.PmMessage> = messages.pmThread(partnerId)
    fun savePmThread(thread: DirectMessageThread) = messages.savePmThread(thread)
    fun markPmRead(partnerId: Long) = messages.markPmRead(partnerId)
    fun exportJson(): String = exportBackupJson(prefs, themePrefs, aiRecords, drafts, readingRecords)
    fun exportJson(items: Set<String>): String = exportBackupJson(items, prefs, themePrefs, aiRecords, drafts, readingRecords)
    fun importJson(text: String): String? = importBackupJson(text, prefs, themePrefs, aiRecords, drafts, readingRecords)
}
