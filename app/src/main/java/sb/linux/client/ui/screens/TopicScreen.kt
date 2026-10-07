package sb.linux.client.ui.screens

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import sb.linux.client.ui.session.Session
import sb.linux.client.ui.feature.ai.AiAssistantViewModel
import sb.linux.client.ui.feature.ai.AiAssistantViewModelFactory
import sb.linux.client.ui.feature.topic.TopicInteractionViewModel
import sb.linux.client.ui.feature.topic.TopicInteractionViewModelFactory
import sb.linux.client.ui.feature.topic.TopicReplyViewModel
import sb.linux.client.ui.feature.topic.TopicReplyViewModelFactory

internal const val FLOOR_PILL_ENABLED = false

@Composable
fun TopicScreen(session: Session, nav: NavHostController) {
    val entry = nav.currentBackStackEntry ?: return
    val topicId = entry.arguments?.getLong("tid") ?: return
    val initialPage = 1
    val initialFloor = entry.arguments?.getInt("floor") ?: 0
    val context = LocalContext.current
    val source = remember(session) { session.topicPageRepository }
    val floorLoader = remember(session, source) { session.topicFloorPageRepository }
    val interactionFactory = remember(
        session.topicInteractionRepository, session.favoritesRepository, session.danmakuCacheRepository,
    ) {
        TopicInteractionViewModelFactory(
            session.topicInteractionRepository, session.favoritesRepository, session.danmakuCacheRepository,
        )
    }
    val interactions: TopicInteractionViewModel = viewModel(
        factory = interactionFactory,
        key = "topic-interactions-$topicId",
    )
    val replyFactory = remember(session.topicReplyRepository, session.usageStatisticsRepository) {
        TopicReplyViewModelFactory(session.topicReplyRepository, session.usageStatisticsRepository)
    }
    val replyViewModel: TopicReplyViewModel = viewModel(
        factory = replyFactory,
        key = "topic-replies-$topicId",
    )
    val aiFactory = remember(session.aiAssistantRepository) {
        AiAssistantViewModelFactory(
            session.aiAssistantRepository, session.aiConfigurationRepository, session.aiSummaryHistoryRepository,
        )
    }
    val aiViewModel: AiAssistantViewModel = viewModel(
        factory = aiFactory,
        key = "topic-ai-$topicId",
    )
    val pinnedSnapshot = remember(topicId) {
        session.floatingTopicSnapshot?.takeIf {
            session.floatingTopicId == topicId && it.topic.topicId == topicId
        }
    }
    val aiSettings = aiViewModel.topicSettings()
    val ui = remember(topicId) { TopicScreenUiState(session, interactions.favoriteCommentIds()) }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val pageCoordinatorFactory = remember(
        topicId, source, floorLoader, session.topicPageCacheRepository,
        session.browseSettingsRepository, session.readingPositionRepository,
        session.cardRedemptionsRepository, session.topicViewsRepository,
        session.browsingHistoryRepository, session.usageStatisticsRepository,
    ) {
        TopicPageCoordinatorFactory(
            topicId = topicId,
            repository = source,
            floorPages = floorLoader,
            pageCache = session.topicPageCacheRepository,
            browseSettings = session.browseSettingsRepository,
            readingPositions = session.readingPositionRepository,
            redemptions = session.cardRedemptionsRepository,
            topicViews = session.topicViewsRepository,
            browsingHistory = session.browsingHistoryRepository,
            usageStatistics = session.usageStatisticsRepository,
            showToast = session::showToast,
        )
    }
    val pageCoordinator: TopicPageCoordinator = viewModel(
        factory = pageCoordinatorFactory,
        key = "topic-pages-$topicId",
    )
    val paging = pageCoordinator.state
    val actions = remember(
        session, topicId, scope, paging, ui, pageCoordinator,
        interactions, replyViewModel, aiViewModel, listState, context,
    ) {
        TopicScreenActions(
            session, topicId, scope, paging, ui, pageCoordinator,
            interactions, replyViewModel, aiViewModel, listState, context,
        )
    }
    val view = rememberTopicScreenViewState(session, topicId, paging, pageCoordinator, ui, listState, aiSettings)

    TopicScreenEffects(
        session = session,
        topicId = topicId,
        initialPage = initialPage,
        initialFloor = initialFloor,
        pinnedSnapshot = pinnedSnapshot,
        paging = paging,
        ui = ui,
        actions = actions,
        listState = listState,
        reply = view.reply,
        sortedPosts = view.sortedPosts,
        mainPost = view.mainPost,
        topicInfinite = view.infinite,
        repliesPerPage = view.repliesPerPage,
        searchHits = view.searchHits,
    )
    TopicScreenScaffold(
        session = session,
        topicId = topicId,
        initialPage = initialPage,
        nav = nav,
        scope = scope,
        paging = paging,
        ui = ui,
        actions = actions,
        listState = listState,
        view = view,
    )
}
