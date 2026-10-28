package sb.linux.client.di

import androidx.lifecycle.viewModelScope
import sb.linux.client.data.AccountAuthenticationService
import sb.linux.client.data.AppSettings
import sb.linux.client.data.LsbClient
import sb.linux.client.data.account.AccountSessionRepositoryImpl
import sb.linux.client.data.AiClient
import sb.linux.client.data.CheckinPageService
import sb.linux.client.data.CommunityPageService
import sb.linux.client.data.DohSettingsRepositoryImpl
import sb.linux.client.data.AppProxyDetectionRepository
import sb.linux.client.data.AppSettingsProxySettingsRepository
import sb.linux.client.data.AppSettingsImageHostSettingsRepository
import sb.linux.client.data.AppNetworkRecoveryRepository
import sb.linux.client.data.AppSettingsAiConfigurationRepository
import sb.linux.client.data.AppSettingsCheckinSummaryRepository
import sb.linux.client.data.AppSettingsAutoClearItemsRepository
import sb.linux.client.data.AppSettingsGeneralSettingsRepository
import sb.linux.client.data.AppSettingsBrowseSettingsRepository
import sb.linux.client.data.AppSettingsBackupSettingsRepository
import sb.linux.client.data.LocalBrowsingHistoryRepository
import sb.linux.client.data.FavoriteSourceService
import sb.linux.client.data.AppSettingsCardRedemptionsRepository
import sb.linux.client.data.HomePageService
import sb.linux.client.data.ImageHostClient
import sb.linux.client.data.LinkPreviewClient
import sb.linux.client.data.LocalFavoritesRepository
import sb.linux.client.data.NewTopicFormLoader
import sb.linux.client.data.NewTopicPageService
import sb.linux.client.data.NewTopicSubmitter
import sb.linux.client.data.PrivateMessageService
import sb.linux.client.data.ProfilePageService
import sb.linux.client.data.TopicReplyService
import sb.linux.client.data.UpdateChecker
import sb.linux.client.data.UserByNameService
import sb.linux.client.data.UserPageService
import sb.linux.client.data.remote.WebDavClient
import sb.linux.client.data.filter.AccountKeywordFilterRepositoryImpl
import sb.linux.client.repository.SourceKeywordFilterRepository
import sb.linux.client.repository.SourceTopicCollectionRepository
import sb.linux.client.service.KeywordFilterService
import sb.linux.client.service.TopicCollectionService
import sb.linux.client.ui.session.Session
import sb.linux.client.platform.verification.CapVerificationGateway
import sb.linux.client.data.ClientCapVerificationNetworkRepository
import sb.linux.client.data.topic.TopicFloorPageLoader
import sb.linux.client.data.topic.TopicInteractionDataSource
import sb.linux.client.data.topic.TopicPageDataSource
import sb.linux.client.domain.account.AccountAuthenticationRepository
import sb.linux.client.domain.account.AccountSessionRepository
import sb.linux.client.domain.ai.AiAssistantRepository
import sb.linux.client.domain.ai.AiConfigurationRepository
import sb.linux.client.domain.account.CheckinRepository
import sb.linux.client.domain.account.CheckinSummaryRepository
import sb.linux.client.domain.account.UserLookupRepository
import sb.linux.client.domain.account.UserPageRepository
import sb.linux.client.domain.collection.TopicCollectionPageRepository
import sb.linux.client.domain.community.FavoritesRepository
import sb.linux.client.domain.community.BrowsingHistoryRepository
import sb.linux.client.domain.community.HomeBrowseRepository
import sb.linux.client.domain.community.KeywordFilterRepository
import sb.linux.client.domain.community.IdentityRepository
import sb.linux.client.domain.community.InviteRepository
import sb.linux.client.domain.community.LeaderboardRepository
import sb.linux.client.domain.community.NotificationRepository
import sb.linux.client.domain.community.PrivateMessageRepository
import sb.linux.client.domain.community.ReportRepository
import sb.linux.client.domain.gacha.CardRedemptionsRepository
import sb.linux.client.domain.media.ImageUploadRepository
import sb.linux.client.domain.media.ImageHostSettingsRepository
import sb.linux.client.domain.linkpreview.LinkPreviewRepository
import sb.linux.client.domain.network.DohSettingsRepository
import sb.linux.client.domain.network.ProxyDetectionRepository
import sb.linux.client.domain.network.ProxySettingsRepository
import sb.linux.client.domain.network.NetworkRecoveryRepository
import sb.linux.client.domain.update.UpdateRepository
import sb.linux.client.domain.backup.WebDavBackupRepository
import sb.linux.client.domain.backup.BackupSettingsRepository
import sb.linux.client.domain.profile.ProfileSettingsRepository
import sb.linux.client.domain.settings.GeneralSettingsRepository
import sb.linux.client.domain.settings.BrowseSettingsRepository
import sb.linux.client.domain.settings.AutoClearItemsRepository
import sb.linux.client.domain.topic.NewTopicRepository
import sb.linux.client.domain.topic.TopicFloorPageRepository
import sb.linux.client.domain.topic.TopicInteractionRepository
import sb.linux.client.domain.topic.TopicPageRepository
import sb.linux.client.domain.topic.TopicReplyRepository

/** Lazy composition point for repositories still owned by the Session lifecycle. */
internal class SessionRepositories(
    private val session: Session,
    private val client: LsbClient,
) {
    private val settings by lazy { AppSettings(session.getApplication<android.app.Application>()) }

    internal val aiAssistantRepository: AiAssistantRepository get() = AiClient
    internal val capVerificationGateway by lazy {
        CapVerificationGateway(accountAuthenticationRepository, ClientCapVerificationNetworkRepository(client))
    }
    internal val aiConfigurationRepository: AiConfigurationRepository by lazy {
        AppSettingsAiConfigurationRepository(settings)
    }
    internal val generalSettingsRepository: GeneralSettingsRepository by lazy {
        AppSettingsGeneralSettingsRepository(settings)
    }
    internal val browseSettingsRepository: BrowseSettingsRepository by lazy {
        AppSettingsBrowseSettingsRepository(settings)
    }
    internal val autoClearItemsRepository: AutoClearItemsRepository by lazy {
        AppSettingsAutoClearItemsRepository(settings)
    }
    internal val browsingHistoryRepository: BrowsingHistoryRepository by lazy {
        LocalBrowsingHistoryRepository(settings)
    }
    internal val imageUploadRepository: ImageUploadRepository by lazy {
        ImageHostClient(session.getApplication())
    }
    internal val imageHostSettingsRepository: ImageHostSettingsRepository by lazy {
        AppSettingsImageHostSettingsRepository(settings)
    }
    internal val linkPreviewRepository: LinkPreviewRepository by lazy { LinkPreviewClient() }
    internal val updateRepository: UpdateRepository by lazy { UpdateChecker() }
    internal val dohSettingsRepository: DohSettingsRepository by lazy { DohSettingsRepositoryImpl(settings) }
    internal val proxyDetectionRepository: ProxyDetectionRepository by lazy { AppProxyDetectionRepository() }
    internal val proxySettingsRepository: ProxySettingsRepository by lazy { AppSettingsProxySettingsRepository(settings) }
    internal val networkRecoveryRepository: NetworkRecoveryRepository get() = AppNetworkRecoveryRepository
    internal val webDavBackupRepository: WebDavBackupRepository by lazy { WebDavClient() }
    internal val backupSettingsRepository: BackupSettingsRepository by lazy {
        AppSettingsBackupSettingsRepository(settings)
    }

    private val topicCollectionService by lazy {
        TopicCollectionService(SourceTopicCollectionRepository(client))
    }
    internal val topicCollectionRepository: TopicCollectionPageRepository
        get() = topicCollectionService
    internal val accountSessionRepository: AccountSessionRepository by lazy {
        AccountSessionRepositoryImpl(session.viewModelScope, client)
    }
    private val keywordFilterRemote by lazy { SourceKeywordFilterRepository(client) }
    private val keywordFilterService by lazy {
        KeywordFilterService(
            remote = keywordFilterRemote,
            prefs = session.getApplication<android.app.Application>()
                .getSharedPreferences("lsb_keyword_filter", android.content.Context.MODE_PRIVATE),
        )
    }
    internal val keywordFilterRepository: KeywordFilterRepository by lazy {
        AccountKeywordFilterRepositoryImpl(
            service = keywordFilterService,
            loadPolicy = keywordFilterRemote::loadPolicy,
            currentSnapshot = { session.keywordFilter },
            currentUserId = {
                session.account.loginState.userId.takeIf { session.account.loginState.loggedIn && it > 0L }
            },
            legacySettings = {
                sb.linux.client.common.filter.KeywordFilterSettings(
                    custom = settings.blockedWords.toList(),
                    users = settings.blockedUsers.toList(),
                )
            },
            clearLegacySettings = {
                settings.blockedWords = emptySet()
                settings.blockedUsers = emptySet()
            },
            updateSnapshot = session::updateKeywordFilter,
        )
    }
    internal val privateMessageService by lazy { PrivateMessageService(client) }
    internal val privateMessageRepository: PrivateMessageRepository get() = privateMessageService
    internal val checkinRepository: CheckinRepository by lazy {
        CheckinPageService(client, session::applyCheckinInfoPublic)
    }
    internal val checkinSummaryRepository: CheckinSummaryRepository by lazy {
        AppSettingsCheckinSummaryRepository(settings)
    }
    internal val cardRedemptionsRepository: CardRedemptionsRepository by lazy {
        AppSettingsCardRedemptionsRepository(settings)
    }
    internal val homeRepository: HomeBrowseRepository by lazy {
        HomePageService(client, { session.sessionVersion }, session::observeHomeSession, session::updateNotifUnread)
    }
    private val favoriteSourceService by lazy { FavoriteSourceService(client, settings::mergeFavorites) }
    internal val favoritesRepository: FavoritesRepository by lazy {
        LocalFavoritesRepository(settings, favoriteSourceService)
    }
    internal val userPageService by lazy { UserPageService(client) }
    internal val userPageRepository: UserPageRepository get() = userPageService
    internal val userByNameService by lazy { UserByNameService(client) }
    internal val userLookupRepository: UserLookupRepository get() = userByNameService
    internal val communityPageService by lazy {
        CommunityPageService(client, session::updateNotifUnread)
    }
    internal val notificationRepository: NotificationRepository get() = communityPageService
    internal val identityRepository: IdentityRepository get() = communityPageService
    internal val inviteRepository: InviteRepository get() = communityPageService
    internal val leaderboardRepository: LeaderboardRepository get() = communityPageService
    internal val reportRepository: ReportRepository get() = communityPageService
    internal val profilePageService by lazy { ProfilePageService(client) }
    internal val profileSettingsRepository: ProfileSettingsRepository get() = profilePageService
    internal val topicInteractionDataSource by lazy {
        TopicInteractionDataSource(client, settings, session.topicPageCacheRepository::clearTopic)
    }
    internal val topicInteractionRepository: TopicInteractionRepository get() = topicInteractionDataSource
    internal val topicReplyService by lazy { TopicReplyService(client) }
    internal val topicReplyRepository: TopicReplyRepository get() = topicReplyService
    internal val topicPageDataSource by lazy {
        TopicPageDataSource(client, session.topicPageCacheRepository)
    }
    internal val topicFloorPageLoader by lazy {
        TopicFloorPageLoader(client, topicPageDataSource)
    }
    internal val topicPageRepository: TopicPageRepository get() = topicPageDataSource
    internal val topicFloorPageRepository: TopicFloorPageRepository get() = topicFloorPageLoader
    internal val accountAuthenticationService by lazy {
        AccountAuthenticationService(client, settings, session::refreshSession)
    }
    internal val accountAuthenticationRepository: AccountAuthenticationRepository
        get() = accountAuthenticationService
    internal val userSummaryRepository by lazy { sb.linux.client.data.UserSummaryService(client) }
    internal val newTopicFormLoader by lazy { NewTopicFormLoader(client) }
    internal val newTopicSubmitter by lazy { NewTopicSubmitter(client) }
    internal val newTopicRepository: NewTopicRepository by lazy {
        NewTopicPageService(newTopicFormLoader, newTopicSubmitter)
    }

}
