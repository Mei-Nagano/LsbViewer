package sb.linux.client.ui.screens

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import sb.linux.client.domain.community.BrowsingHistoryRepository
import sb.linux.client.domain.community.UsageStatisticsRepository
import sb.linux.client.domain.gacha.CardRedemptionsRepository
import sb.linux.client.domain.settings.BrowseSettingsRepository
import sb.linux.client.domain.topic.TopicFloorPageRepository
import sb.linux.client.domain.topic.TopicPageCacheRepository
import sb.linux.client.domain.topic.TopicPageRepository
import sb.linux.client.domain.topic.TopicReadingPositionRepository
import sb.linux.client.domain.topic.TopicViewsRepository

internal class TopicPageCoordinatorFactory(
    private val topicId: Long,
    private val repository: TopicPageRepository,
    private val floorPages: TopicFloorPageRepository,
    private val pageCache: TopicPageCacheRepository,
    private val browseSettings: BrowseSettingsRepository,
    private val readingPositions: TopicReadingPositionRepository,
    private val redemptions: CardRedemptionsRepository,
    private val topicViews: TopicViewsRepository,
    private val browsingHistory: BrowsingHistoryRepository,
    private val usageStatistics: UsageStatisticsRepository,
    private val showToast: (String) -> Unit,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = createCoordinator(SavedStateHandle())

    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T =
        createCoordinator(extras.createSavedStateHandle())

    @Suppress("UNCHECKED_CAST")
    private fun <T : ViewModel> createCoordinator(handle: SavedStateHandle): T =
        TopicPageCoordinator(
            topicId, repository, floorPages, pageCache, browseSettings, readingPositions,
            redemptions, topicViews, browsingHistory, usageStatistics, showToast, handle,
        ) as T
}
