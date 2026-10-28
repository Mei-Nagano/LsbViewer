package sb.linux.client.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import sb.linux.client.domain.community.BrowsingHistoryRepository
import sb.linux.client.domain.gacha.CardRedemptionsRepository
import sb.linux.client.domain.settings.BrowseSettingsRepository
import sb.linux.client.domain.topic.PostEntry
import sb.linux.client.domain.topic.TopicFloorPage
import sb.linux.client.domain.topic.TopicFloorPageRepository
import sb.linux.client.domain.topic.TopicPageData
import sb.linux.client.domain.topic.TopicPageCacheRepository
import sb.linux.client.domain.topic.TopicPageRepository
import sb.linux.client.domain.topic.TopicReadingAnchor
import sb.linux.client.domain.topic.TopicReadingPositionRepository
import sb.linux.client.domain.topic.TopicViewsRepository

/** Coordinates source pages with the screen's retained paging state. */
internal class TopicPageCoordinator(
    private val topicId: Long,
    private val source: TopicPageRepository,
    private val floorPages: TopicFloorPageRepository,
    private val pageCache: TopicPageCacheRepository,
    private val browseSettings: BrowseSettingsRepository,
    private val readingPositions: TopicReadingPositionRepository,
    private val redemptions: CardRedemptionsRepository,
    private val topicViews: TopicViewsRepository,
    private val browsingHistory: BrowsingHistoryRepository,
    private val showToast: (String) -> Unit,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    internal val state = TopicPagingState(savedStateHandle)
    private val threadLoader = sb.linux.client.domain.topic.TopicThreadLoader(source)

    fun restoreCachedPages(maxPage: Int): TopicPageData? = runCatching {
        val pages = (1..maxPage).map { pageCache.get(topicId, it) ?: return null }
        pages.drop(1).fold(pages.first()) { combined, page ->
            combined.copy(
                posts = (combined.posts + page.posts).distinctBy { it.id },
                page = page.page,
                totalPages = page.totalPages,
            )
        }
    }.getOrNull()

    fun commentsPerPage(): Int = browseSettings.current().commentsPerPage.coerceAtLeast(1)

    fun topicInfiniteScrollEnabled(): Boolean {
        val settings = browseSettings.current()
        return settings.scrollModeOverrides["topic"] ?: settings.infiniteScroll
    }

    fun saveReadingPosition(floor: Int, listIndex: Int, scrollOffset: Int, postId: Long = 0) {
        readingPositions.save(topicId, floor, listIndex, scrollOffset, postId)
    }

    /** 版本化锚点：只在旧四段记录仍有效时返回，页面据此优先按楼层恢复。 */
    fun readingAnchor(): TopicReadingAnchor? = readingPositions.getAnchor(topicId)

    /** 旧四段记录（楼层/列表下标/像素偏移）；无锚点时作为可解释的回退。 */
    fun readingPosition(): sb.linux.client.domain.topic.TopicReadingPosition? =
        readingPositions.get(topicId)

    fun loadFloor(floor: Int, onComplete: (Result<TopicFloorPage>) -> Unit) =
        runRequest({ floorPages.load(topicId, floor) }, onComplete)

    fun loadThread(
        startPost: PostEntry,
        availablePages: Int,
        onComplete: (Result<List<PostEntry>>) -> Unit,
    ) = runRequest({ threadLoader.load(topicId, startPost, availablePages) }, onComplete)

    fun load(page: Int, append: Boolean = false, force: Boolean = false) {
        viewModelScope.launch {
            if (append) state.loadingMore.value = true
            else {
                state.loading.value = true
                state.error.value = null
            }
            try {
                if (force) pageCache.clearTopic(topicId)
                val loaded = source.load(topicId, page)
                val current = state.page.value
                val missing = if (append && current != null) {
                    buildList {
                        for (number in (current.page + 1) until page) {
                            addAll(source.load(topicId, number).posts)
                        }
                    }
                } else emptyList()
                state.page.value = if (append && current != null) current.copy(
                    posts = (current.posts + missing + loaded.posts).associateBy { it.id }.values.toList(),
                    page = loaded.page,
                    totalPages = loaded.totalPages,
                ) else loaded
                if (!append) redemptions.record(topicId, loaded.title, loaded.virtualCard)
                state.loadedMaxPage.value = if (append) {
                    maxOf(state.loadedMaxPage.value, loaded.page.coerceAtLeast(1))
                } else loaded.page.coerceAtLeast(1)
                if (!append) {
                    state.localReplyPage.value = 1
                    state.reverseStalledAtPage.value = 0
                }
                if (!append) loaded.viewsText.toIntOrNull()?.let { topicViews.save(topicId, it) }
                if (!append && page == 1) recordTopicVisit(loaded)
            } catch (error: Exception) {
                if (!append) state.error.value = error.message
                else showToast(error.message ?: "加载失败")
            } finally {
                state.loading.value = false
                state.loadingMore.value = false
            }
        }
    }

    suspend fun loadUntil(postCount: Int) {
        while ((state.page.value?.posts?.size ?: 0) < postCount && hasNextPage()) {
            state.loadingMore.value = true
            val previousPage = state.page.value?.page ?: 1
            val previousCount = state.page.value?.posts?.size ?: 0
            try {
                val loaded = source.load(topicId, previousPage + 1)
                merge(loaded, distinct = true)
                state.loadedMaxPage.value = maxOf(state.loadedMaxPage.value, loaded.page.coerceAtLeast(1))
            } catch (error: Exception) {
                showToast(error.message ?: "加载失败")
                break
            }
            if (hasNotAdvanced(previousPage, previousCount)) break
        }
        state.loadingMore.value = false
    }

    fun loadAllRemaining() {
        viewModelScope.launch {
            state.loadingMore.value = true
            try {
                while (hasNextPage() && (state.page.value?.page ?: 1) < 100) {
                    val previousPage = state.page.value?.page ?: 1
                    val previousCount = state.page.value?.posts?.size ?: 0
                    merge(source.load(topicId, previousPage + 1), distinct = true)
                    state.loadedMaxPage.value = maxOf(
                        state.loadedMaxPage.value,
                        state.page.value?.page?.coerceAtLeast(1) ?: 1,
                    )
                    if (hasNotAdvanced(previousPage, previousCount)) {
                        state.reverseStalledAtPage.value = previousPage
                        break
                    }
                }
            } catch (error: Exception) {
                showToast(error.message ?: "加载失败")
            } finally {
                state.loadingMore.value = false
            }
        }
    }

    fun loadAllForSearch() {
        if (state.loading.value || state.loadingMore.value) return
        viewModelScope.launch {
            state.loadingMore.value = true
            try {
                var page = 1
                var lastPage = state.page.value?.totalPages ?: 1
                while (page <= lastPage) {
                    val loaded = source.load(topicId, page)
                    check(loaded.page == page) { "源站未返回第 $page 页，已保留当前搜索结果" }
                    val current = state.page.value
                    state.page.value = current?.copy(
                        posts = (current.posts + loaded.posts).associateBy { it.id }.values.toList(),
                        page = maxOf(current.page, loaded.page),
                        totalPages = loaded.totalPages,
                    ) ?: loaded
                    lastPage = loaded.totalPages
                    state.loadedMaxPage.value = maxOf(state.loadedMaxPage.value, page)
                    page++
                }
                state.allSearchPagesLoaded.value = true
            } catch (error: Exception) {
                showToast(error.message ?: "加载失败，可重试搜索全帖")
            } finally {
                state.loadingMore.value = false
            }
        }
    }

    private fun hasNextPage(): Boolean =
        (state.page.value?.page ?: 1) < (state.page.value?.totalPages ?: 1)

    private fun hasNotAdvanced(previousPage: Int, previousCount: Int): Boolean =
        (state.page.value?.page ?: 1) <= previousPage &&
            (state.page.value?.posts?.size ?: 0) <= previousCount

    private fun merge(loaded: TopicPageData, distinct: Boolean) {
        val current = state.page.value
        val posts = current?.posts.orEmpty() + loaded.posts
        state.page.value = current?.copy(
            posts = if (distinct) posts.distinctBy(PostEntry::id) else posts,
            page = loaded.page,
            totalPages = loaded.totalPages,
        ) ?: loaded
    }

    private fun recordTopicVisit(page: TopicPageData) {
        val originalPoster = page.posts.firstOrNull { it.floor == 0 } ?: page.posts.firstOrNull()
        browsingHistory.record(
            topicId = topicId,
            title = page.title,
            authorId = originalPoster?.authorId ?: 0,
            authorName = originalPoster?.authorName ?: "",
            forumId = 0,
            forumName = "",
            avatarUrl = originalPoster?.avatarUrl ?: "",
            titleColor = "",
        )
    }

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
