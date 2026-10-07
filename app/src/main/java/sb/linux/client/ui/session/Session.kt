package sb.linux.client.ui.session

import sb.linux.client.domain.community.HomeSessionObservation
import sb.linux.client.domain.community.KeywordFilterRepository
import sb.linux.client.domain.community.BrowsingHistoryRepository
import sb.linux.client.domain.community.UsageStatisticsRepository

import android.app.Application
import sb.linux.client.LsbApp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import sb.linux.client.common.filter.KeywordFilterSnapshot
import sb.linux.client.domain.account.CheckinInfo
import sb.linux.client.domain.account.CheckinSummary
import sb.linux.client.domain.account.CheckinSummaryRepository
import sb.linux.client.domain.ai.AiAssistantRepository
import sb.linux.client.domain.ai.AiConfigurationRepository
import sb.linux.client.domain.ai.AiSummaryHistoryRepository
import sb.linux.client.domain.settings.DataManagementRepository
import sb.linux.client.domain.settings.GeneralSettingsRepository
import sb.linux.client.domain.settings.BrowseSettings
import sb.linux.client.domain.settings.BrowseSettingsRepository
import sb.linux.client.domain.settings.AutoClearItemsRepository
import sb.linux.client.domain.media.ImageUploadRepository
import sb.linux.client.domain.media.ImageHostSettingsRepository
import sb.linux.client.domain.linkpreview.LinkPreviewRepository
import sb.linux.client.domain.update.UpdateRepository
import sb.linux.client.domain.network.DohSettingsRepository
import sb.linux.client.domain.network.ProxyDetectionRepository
import sb.linux.client.domain.network.ProxySettingsRepository
import sb.linux.client.domain.network.NetworkRecoveryRepository
import sb.linux.client.domain.backup.WebDavBackupRepository
import sb.linux.client.domain.backup.BackupSettingsRepository
import sb.linux.client.domain.collection.TopicCollectionPageRepository
import sb.linux.client.domain.community.HomeBrowseCache
import sb.linux.client.domain.community.ForumRepository
import sb.linux.client.domain.search.SearchCache
import sb.linux.client.domain.search.SearchRepository
import sb.linux.client.platform.verification.CapVerificationGateway
import sb.linux.client.domain.topic.NewTopicDraftRepository
import sb.linux.client.domain.topic.TopicDanmakuCacheRepository
import sb.linux.client.domain.topic.TopicPageCacheRepository
import sb.linux.client.domain.topic.TopicReadingPositionRepository
import sb.linux.client.domain.topic.TopicViewsRepository
import sb.linux.client.di.SessionRepositories

class Session(app: Application) : AndroidViewModel(app), SessionState by SessionUiState() {
    private val appContainer = app as LsbApp
    internal val searchRepository: SearchRepository = appContainer.searchRepository
    internal val searchCache: SearchCache = appContainer.searchCache
    internal val forumRepository: ForumRepository = appContainer.forumRepository
    private val repositories by lazy { SessionRepositories(this, appContainer.client) }
    private val accountLifecycle by lazy { SessionAccountLifecycleCoordinator(this) }
    internal val accountSessionRepository get() = repositories.accountSessionRepository
    internal val topicCollectionRepository: TopicCollectionPageRepository get() = repositories.topicCollectionRepository
    internal val keywordFilterRepository: KeywordFilterRepository get() = repositories.keywordFilterRepository
    internal val browsingHistoryRepository: BrowsingHistoryRepository get() = repositories.browsingHistoryRepository
    internal val usageStatisticsRepository: UsageStatisticsRepository get() = repositories.sourceUsageRepository
    internal val account by lazy {
        AccountSession(accountSessionRepository, accountLifecycle::applySnapshot)
    }
    internal val preferences by lazy { SessionPreferences(getApplication(), this) }
    internal val cache by lazy { SessionCacheOwner(getApplication(), this) }
    internal val aiSummaryHistoryRepository: AiSummaryHistoryRepository
        get() = cache.aiSummaryHistoryRepository
    internal val dataManagementRepository: DataManagementRepository
        get() = cache.dataManagementRepository
    internal val generalSettingsRepository: GeneralSettingsRepository get() = repositories.generalSettingsRepository
    internal val browseSettingsRepository: BrowseSettingsRepository get() = repositories.browseSettingsRepository
    internal val autoClearItemsRepository: AutoClearItemsRepository get() = repositories.autoClearItemsRepository
    internal fun applyBrowseSettings(settings: BrowseSettings) = preferences.applyBrowseSettings(settings)
    internal val aiAssistantRepository: AiAssistantRepository get() = repositories.aiAssistantRepository
    internal val aiConfigurationRepository: AiConfigurationRepository get() = repositories.aiConfigurationRepository
    internal val imageUploadRepository: ImageUploadRepository get() = repositories.imageUploadRepository
    internal val imageHostSettingsRepository: ImageHostSettingsRepository get() = repositories.imageHostSettingsRepository
    internal val linkPreviewRepository: LinkPreviewRepository get() = repositories.linkPreviewRepository
    internal val updateRepository: UpdateRepository get() = repositories.updateRepository
    internal val dohSettingsRepository: DohSettingsRepository get() = repositories.dohSettingsRepository
    internal val proxyDetectionRepository: ProxyDetectionRepository get() = repositories.proxyDetectionRepository
    internal val proxySettingsRepository: ProxySettingsRepository get() = repositories.proxySettingsRepository
    internal val networkRecoveryRepository: NetworkRecoveryRepository get() = repositories.networkRecoveryRepository
    internal val webDavBackupRepository: WebDavBackupRepository get() = repositories.webDavBackupRepository
    internal val backupSettingsRepository: BackupSettingsRepository get() = repositories.backupSettingsRepository

    internal val privateMessageRepository get() = repositories.privateMessageRepository
    internal val privateMessageHistory get() = repositories.privateMessageHistory
    internal val checkinRepository get() = repositories.checkinRepository
    internal val checkinSummaryRepository: CheckinSummaryRepository
        get() = repositories.checkinSummaryRepository
    internal val capVerificationGateway: CapVerificationGateway get() = repositories.capVerificationGateway
    internal val gachaCenterRepository get() = repositories.gachaCenterRepository
    internal val cardRedemptionsRepository get() = repositories.cardRedemptionsRepository
    internal val homeBrowseCache: HomeBrowseCache get() = cache.homeBrowseCache
    internal val topicPageCacheRepository: TopicPageCacheRepository get() = cache.topicPageCacheRepository
    internal val topicViewsRepository: TopicViewsRepository get() = cache.topicViewsRepository
    internal val readingPositionRepository: TopicReadingPositionRepository get() = cache.readingPositionRepository
    internal val danmakuCacheRepository: TopicDanmakuCacheRepository get() = cache.danmakuCacheRepository
    internal val newTopicDraftRepository: NewTopicDraftRepository get() = cache.newTopicDraftRepository
    internal val gachaMarketRepository get() = repositories.gachaMarketRepository
    internal val gachaProfileRepository get() = repositories.gachaProfileRepository
    internal val gachaOperationRepository get() = repositories.gachaOperationRepository
    internal val homeRepository get() = repositories.homeRepository
    internal val favoritesRepository get() = repositories.favoritesRepository
    internal val userPageRepository get() = repositories.userPageRepository
    internal val userLookupRepository get() = repositories.userLookupRepository
    internal val notificationRepository get() = repositories.notificationRepository
    internal val identityRepository get() = repositories.identityRepository
    internal val inviteRepository get() = repositories.inviteRepository
    internal val leaderboardRepository get() = repositories.leaderboardRepository
    internal val reportRepository get() = repositories.reportRepository
    internal val profileSettingsRepository get() = repositories.profileSettingsRepository
    internal val topicInteractionRepository get() = repositories.topicInteractionRepository
    internal val topicReplyRepository get() = repositories.topicReplyRepository
    internal val topicPageRepository get() = repositories.topicPageRepository
    internal val topicFloorPageRepository get() = repositories.topicFloorPageRepository
    internal val accountAuthenticationRepository get() = repositories.accountAuthenticationRepository
    internal val newTopicRepository get() = repositories.newTopicRepository

    /** 首页请求携带代次，防止登出或切换账号之前的旧响应重新覆盖身份。 */
    val sessionVersion: Long get() = account.version

    fun recoverSession() = account.recover()

    internal fun observeHomeSession(observation: HomeSessionObservation, version: Long) {
        setKeywordFilterSnapshot(keywordFilter.copy(policy = observation.keywordPolicy))
        observation.snapshot?.let { account.accept(it, version) }
            ?: account.recover()
    }

    private val preferenceLoader by lazy {
        SessionPreferenceLoader(getApplication(), this, preferences.themePreferenceController)
    }

    /** 从磁盘重新读取全部偏好到内存状态（导入设置备份后调用，立即生效无需重启） */
    fun reloadPrefs() = preferenceLoader.reload()
    init { SessionStartupCoordinator(this).start() }

    // ---------------- 通知未读状态（跟随源站） ----------------

    /** 更新源站未读通知数（首页 .notify-badge 解析结果；进入通知页 GET 后源站已全部标记已读，置 0） */
    fun updateNotifUnread(count: Int) {
        account.updateUnread(count)
    }

    /** 从源站拉取当前账号屏蔽设置；网络失败时保留账号级本地缓存。 */
    fun refreshKeywordFilter(force: Boolean = false) {
        viewModelScope.launch {
            runCatching { keywordFilterRepository.refresh(force) }
        }
    }

    internal fun updateKeywordFilter(snapshot: KeywordFilterSnapshot) {
        setKeywordFilterSnapshot(snapshot)
        blockedWords = (snapshot.settings.presets + snapshot.settings.custom).toSet()
        blockedUsers = snapshot.settings.users.toSet()
    }
    /** 通过全局对话框展示源站验证页，完成后返回结果供请求重试 */
    suspend fun verifyWithUi(url: String, challengeHtml: String): Boolean = withContext(Dispatchers.Main) {
        val deferred = CompletableDeferred<Boolean>()
        setPendingVerification(PendingVerification(url, challengeHtml, deferred))
        // 超时给足时间完成 CF Turnstile / 手动答题，超时才让上层重试
        val ok = withTimeoutOrNull(150_000) { deferred.await() } ?: false
        if (pendingVerification?.deferred === deferred) setPendingVerification(null)
        ok
    }

    fun completeVerification(request: PendingVerification, result: Boolean) {
        if (pendingVerification?.deferred !== request.deferred) return
        // 先移除 Dialog，确保成功回调后的网络重试不会继续遮挡底栏和页面点击。
        setPendingVerification(null)
        if (!request.deferred.isCompleted) request.deferred.complete(result)
    }

    fun refreshSession() {
        account.refresh()
    }

    private val checkinCoordinator by lazy {
        SessionCheckinCoordinator(this, viewModelScope)
    }

    fun applyCheckinInfoPublic(info: CheckinInfo) = checkinCoordinator.apply(info)

    fun loadCheckinInfo(force: Boolean = false) = checkinCoordinator.load(force)

    fun onCheckinDone() = checkinCoordinator.onCheckinDone()

    internal fun applyCheckinSnapshot(info: CheckinInfo) {
        val text = buildString {
            if (info.streak > 0) append("连续 ${info.streak} 天")
            if (info.total > 0) {
                if (isNotEmpty()) append(" · ")
                append("累计 ${info.total} 天")
            }
        }
        updateCheckinUi(text, info.checkedToday || !info.canCheckin)
        checkinSummaryRepository.save(CheckinSummary(info.streak, info.total))
    }

    internal fun markCheckinComplete() {
        updateCheckinUi(checkinText, checkedToday = true)
    }
    fun onLoggedOut() {
        accountLifecycle.onLoggedOut()
    }
}
