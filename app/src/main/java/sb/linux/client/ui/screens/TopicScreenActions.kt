package sb.linux.client.ui.screens

import android.content.Context
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.ColorScheme
import kotlinx.coroutines.CoroutineScope
import sb.linux.client.domain.topic.PostEntry
import sb.linux.client.ui.session.Session
import sb.linux.client.ui.feature.ai.AiAssistantViewModel
import sb.linux.client.ui.feature.topic.TopicInteractionViewModel
import sb.linux.client.ui.feature.topic.TopicReplyViewModel

/** Stable action surface for the topic screen; responsibilities live in focused delegates. */
internal class TopicScreenActions(
    private val session: Session,
    private val topicId: Long,
    scope: CoroutineScope,
    private val state: TopicPagingState,
    private val ui: TopicScreenUiState,
    pageCoordinator: TopicPageCoordinator,
    val interactionViewModel: TopicInteractionViewModel,
    val replyViewModel: TopicReplyViewModel,
    aiAssistantViewModel: AiAssistantViewModel,
    listState: LazyListState,
    context: Context,
) {
    private val pageActions = TopicScreenPageActions(
        session, topicId, scope, state, ui, pageCoordinator, interactionViewModel, listState,
    )
    private val interactionActions = TopicInteractionActions(
        session, topicId, state, ui, interactionViewModel,
        onReloadPage = { load(it) },
        onLoadDanmaku = ::loadDanmaku,
    )
    private val exportActions = TopicExportActions(session, topicId, scope, state, ui, context)
    private val aiActions = TopicAiSummaryActions(topicId, state, ui, aiAssistantViewModel)

    fun load(page: Int, append: Boolean = false, force: Boolean = false) =
        pageActions.load(page, append, force)

    fun loadAllRemaining() = pageActions.loadAllRemaining()
    fun loadAllForSearch() = pageActions.loadAllForSearch()
    fun restoreCachedPages(maxPage: Int) = pageActions.restoreCachedPages(maxPage)
    fun loadDanmaku() = pageActions.loadDanmaku()
    fun goLocalReplyPage(target: Int, hasMainPost: Boolean) = pageActions.goLocalReplyPage(target, hasMainPost)
    fun jumpFloor(floor: Int) = pageActions.jumpFloor(floor)
    fun jumpToFloor(floor: Int, sortedPosts: List<PostEntry>) = pageActions.jumpToFloor(floor, sortedPosts)
    fun scrollToComments(hasMainPost: Boolean) = pageActions.scrollToComments(hasMainPost)
    fun openThread(startPost: PostEntry) = pageActions.openThread(startPost)
    fun toggleCommentFavorite(post: PostEntry) = interactionActions.toggleCommentFavorite(post)
    fun react(post: PostEntry, points: Int, reason: String = "") = interactionActions.react(post, points, reason)
    fun toggleFavorite() = interactionActions.toggleFavorite()
    fun requestFavorite() = interactionActions.requestFavorite()

    fun export(
        kind: String,
        scopeMode: String,
        fromFloor: Int,
        toFloor: Int,
        multiPage: Boolean,
        selectedIds: Set<Long>,
        colorScheme: ColorScheme,
    ) = exportActions.export(kind, scopeMode, fromFloor, toFloor, multiPage, selectedIds, colorScheme)

    fun runAi(force: Boolean = false) = aiActions.run(force)
    fun cachedAiSummary(): String? = aiActions.cachedSummary()
    fun shouldAutoRunAiSummary(): Boolean = aiActions.shouldAutoRun()
}
