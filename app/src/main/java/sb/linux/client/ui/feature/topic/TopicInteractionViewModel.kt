package sb.linux.client.ui.feature.topic

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import sb.linux.client.domain.topic.DanmakuItem
import sb.linux.client.domain.topic.DonationForm
import sb.linux.client.domain.topic.EssenceApplication
import sb.linux.client.domain.topic.EssencePointsTopUp
import sb.linux.client.domain.topic.PostEntry
import sb.linux.client.domain.topic.TopicActionResult
import sb.linux.client.domain.topic.TopicInteractionRepository
import sb.linux.client.domain.topic.TopicPageData
import sb.linux.client.domain.topic.TopicPoll
import sb.linux.client.domain.topic.TopicPollOption
import sb.linux.client.domain.topic.VirtualCard
import sb.linux.client.domain.community.FavoritesRepository
import sb.linux.client.domain.topic.TopicDanmakuCacheRepository

class TopicInteractionViewModel(
    private val repository: TopicInteractionRepository,
    private val favorites: FavoritesRepository,
    private val danmakuCache: TopicDanmakuCacheRepository,
) : ViewModel() {
    fun favoriteCommentIds(): Set<Long> = favorites.comments().map { it.replyId }.toSet()

    fun loadDonateFeedCached(topicId: Long, onComplete: (List<DanmakuItem>) -> Unit) {
        val cached = danmakuCache.get(topicId)
        if (cached != null) onComplete(cached)
        else loadDonateFeed(topicId) { result ->
            val items = result.getOrDefault(emptyList())
            danmakuCache.save(topicId, items)
            onComplete(items)
        }
    }
    fun loadDonateFeed(topicId: Long, onComplete: (Result<List<DanmakuItem>>) -> Unit) =
        runRequest({ repository.loadDonateFeed(topicId) }, onComplete)

    fun loadDonateForm(topicId: Long, onComplete: (Result<DonationForm>) -> Unit) =
        runRequest({ repository.loadDonateForm(topicId) }, onComplete)

    fun donate(
        topicId: Long,
        amount: String,
        message: String,
        onComplete: (Result<String?>) -> Unit,
    ) = runRequest({ repository.donate(topicId, amount, message) }, onComplete)

    fun joinLottery(page: TopicPageData, option: String, onComplete: (Result<TopicActionResult>) -> Unit) =
        runRequest({ repository.joinLottery(page, option) }, onComplete)

    fun applyForEssence(page: TopicPageData, application: EssenceApplication, onComplete: (Result<TopicActionResult>) -> Unit) =
        runRequest({ repository.applyForEssence(page, application) }, onComplete)

    fun topUpEssence(page: TopicPageData, topUp: EssencePointsTopUp, amount: Int, onComplete: (Result<TopicActionResult>) -> Unit) =
        runRequest({ repository.topUpEssence(page, topUp, amount) }, onComplete)

    fun vote(page: TopicPageData, poll: TopicPoll, selected: List<TopicPollOption>, reason: String, onComplete: (Result<TopicActionResult>) -> Unit) =
        runRequest({ repository.vote(page, poll, selected, reason) }, onComplete)

    fun buyVirtualCard(page: TopicPageData, card: VirtualCard, onComplete: (Result<TopicActionResult>) -> Unit) =
        runRequest({ repository.buyVirtualCard(page, card) }, onComplete)

    fun react(
        topicId: Long,
        page: TopicPageData,
        post: PostEntry,
        points: Int,
        reason: String,
        onComplete: (Result<String>) -> Unit,
    ) = runRequest({ repository.react(topicId, page, post, points, reason) }, onComplete)

    fun toggleFavorite(page: TopicPageData, wasFavorite: Boolean, onComplete: (Result<sb.linux.client.domain.topic.TopicFavoriteChange?>) -> Unit) =
        runRequest({ repository.toggleFavorite(page, wasFavorite) }, onComplete)

    fun toggleCommentFavorite(topicId: Long, topicTitle: String, post: PostEntry) =
        repository.toggleCommentFavorite(topicId, topicTitle, post)

    private fun <T> runRequest(request: suspend () -> T, onComplete: (Result<T>) -> Unit) {
        viewModelScope.launch {
            try {
                onComplete(Result.success(request()))
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                onComplete(Result.failure(error))
            }
        }
    }
}

class TopicInteractionViewModelFactory(
    private val repository: TopicInteractionRepository,
    private val favorites: FavoritesRepository,
    private val danmakuCache: TopicDanmakuCacheRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        TopicInteractionViewModel(repository, favorites, danmakuCache) as T
}
