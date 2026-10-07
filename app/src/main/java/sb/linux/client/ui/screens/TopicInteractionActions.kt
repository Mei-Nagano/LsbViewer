package sb.linux.client.ui.screens

import sb.linux.client.domain.topic.PostEntry
import sb.linux.client.ui.session.Session
import sb.linux.client.ui.feature.topic.TopicInteractionViewModel

/** Coordinates topic and comment reaction results with the page UI state. */
internal class TopicInteractionActions(
    private val session: Session,
    private val topicId: Long,
    private val state: TopicPagingState,
    private val ui: TopicScreenUiState,
    private val viewModel: TopicInteractionViewModel,
    private val onReloadPage: (Int) -> Unit,
    private val onLoadDanmaku: () -> Unit,
) {
    fun toggleCommentFavorite(post: PostEntry) {
        val (ids, message) = viewModel.toggleCommentFavorite(
            topicId, state.page.value?.title.orEmpty(), post,
        )
        ui.favoriteCommentIds = ids
        session.showToast(message)
    }

    fun react(post: PostEntry, points: Int, reason: String = "") {
        val page = state.page.value ?: return
        viewModel.react(topicId, page, post, points, reason) { result ->
            result.onSuccess { message ->
                session.showToast(message)
                onReloadPage(page.page)
                if (points > 0) onLoadDanmaku()
            }.onFailure { session.showToast(it.message ?: "操作失败") }
        }
    }

    fun toggleFavorite() {
        val page = state.page.value ?: return
        val wasFavorite = ui.isFav
        viewModel.toggleFavorite(page, wasFavorite) { result ->
            result.onSuccess { change ->
                if (change == null) session.showToast("操作失败") else {
                    session.showToast(if (wasFavorite) "已取消收藏" else "已收藏")
                    ui.isFav = change.page.favorited
                    state.page.value = change.page
                }
            }.onFailure { session.showToast(it.message ?: "失败") }
        }
    }

    fun requestFavorite() {
        if (ui.isFav && session.unfavoriteConfirm) ui.showUnfavDialog = true else toggleFavorite()
    }
}
