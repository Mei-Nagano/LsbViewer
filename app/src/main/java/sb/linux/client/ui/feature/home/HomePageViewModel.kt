package sb.linux.client.ui.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import sb.linux.client.domain.community.HomeBrowseCache
import sb.linux.client.domain.community.HomeBrowseRepository
import sb.linux.client.domain.community.HomePage
import sb.linux.client.domain.community.HomeSidebar
import sb.linux.client.domain.community.FollowingView
import sb.linux.client.domain.community.FollowingUser
import sb.linux.client.domain.topic.TopicCard
import sb.linux.client.domain.topic.TopicPageCacheRepository
import sb.linux.client.domain.topic.TopicViewsRepository
import sb.linux.client.ui.session.HomeTabState

data class HomePageUiState(
    val loading: Boolean = false,
    val loadingMore: Boolean = false,
    val error: String? = null,
    val sidebar: HomeSidebar? = null,
    val pendingTopics: List<TopicCard> = emptyList(),
    val scrollToTopVersion: Int = 0,
)

class HomePageViewModel(
    private val repository: HomeBrowseRepository,
    private val cache: HomeBrowseCache,
    private val topicPageCache: TopicPageCacheRepository,
    private val topicViews: TopicViewsRepository,
    private val current: HomeTabState,
    private val currentSort: String,
    private val homeInfinite: Boolean,
    private val pageSize: () -> Int,
    private val showToast: (String) -> Unit,
    private val followingView: FollowingView = FollowingView.TOPICS,
    private val accountUserId: Long = 0,
) : ViewModel() {
    private val accountFeed = currentSort in setOf("following", "footprint")
    private val mutableState = MutableStateFlow(HomePageUiState(sidebar = cache.loadHomeSidebar()))
    val uiState = mutableState.asStateFlow()

    fun viewsFor(topicId: Long): Int = topicViews.get(topicId)

    fun clearHomeCaches() {
        cache.clearHomeCache()
        topicPageCache.clearAll()
    }

    fun setPendingTopics(topics: List<TopicCard>) =
        mutableState.update { it.copy(pendingTopics = topics) }

    fun loadCachedHome(allowed: Boolean): List<TopicCard> =
        if (allowed && !accountFeed) cache.loadHomeCache(currentSort) else emptyList()

    fun acceptPendingTopics(currentTopics: List<TopicCard>): List<TopicCard> {
        val merged = (mutableState.value.pendingTopics + currentTopics).distinctBy(TopicCard::topicId)
        if (!accountFeed) cache.saveHomeCache(currentSort, merged)
        mutableState.update { it.copy(pendingTopics = emptyList()) }
        return merged
    }

    fun load(page: Int, append: Boolean = false, foldNew: Boolean = false) {
        val generation = current.generation
        mutableState.update {
            it.copy(loading = !append, loadingMore = append, error = null)
        }
        viewModelScope.launch {
            try {
                val loaded = loadPage(page)
                if (generation != current.generation) return@launch
                applyLoadedPage(loaded, page, append, foldNew)
            } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled
            } catch (error: Exception) {
                if (generation != current.generation) return@launch
                mutableState.update { it.copy(error = error.message ?: "加载失败") }
                if (current.topics.isNotEmpty()) showToast(error.message ?: "加载失败")
            } finally {
                if (generation == current.generation) mutableState.update { it.copy(loading = false, loadingMore = false) }
            }
        }
    }

    fun goLocalPage(target: Int, filterTopics: (List<TopicCard>) -> List<TopicCard>) {
        val generation = current.generation
        viewModelScope.launch {
            val requiredCount = target * pageSize().coerceAtLeast(1)
            mutableState.update { it.copy(error = null) }
            while (filterTopics(current.topics).size < requiredCount &&
                current.sourcePage < current.sourceTotalPages
            ) {
                mutableState.update { it.copy(loadingMore = true) }
                try {
                    val nextPage = current.sourcePage + 1
                    val loaded = loadPage(nextPage)
                    if (generation != current.generation) return@launch
                    current.topics = (current.topics + loaded.topics).distinctBy(TopicCard::topicId)
                    current.sourcePage = loaded.page.coerceAtLeast(nextPage)
                    current.sourceTotalPages = loaded.totalPages.coerceAtLeast(nextPage)
                } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled
                } catch (error: Exception) {
                    if (generation != current.generation) return@launch
                    mutableState.update { it.copy(error = error.message ?: "加载失败") }
                    break
                }
            }
            mutableState.update { it.copy(loadingMore = false, scrollToTopVersion = it.scrollToTopVersion + 1) }
            current.page = target.coerceAtLeast(1)
        }
    }

    private suspend fun loadPage(page: Int): HomePage = if (currentSort == "following")
        repository.loadFollowing(followingView, page, accountUserId)
    else repository.load(currentSort, page)

    private fun applyLoadedPage(loaded: HomePage, requestedPage: Int, append: Boolean, foldNew: Boolean) {
        val items = loaded.topics
        val pending = mutableState.value.pendingTopics
        current.topics = when {
            append -> (current.topics + items).distinctBy(TopicCard::topicId)
            foldNew && current.topics.isNotEmpty() -> {
                val oldIds = current.topics.mapTo(mutableSetOf(), TopicCard::topicId)
                mutableState.update {
                    it.copy(pendingTopics = (items.filter { topic -> topic.topicId !in oldIds } + pending)
                        .distinctBy(TopicCard::topicId))
                }
                val latest = items.associateBy(TopicCard::topicId)
                current.topics.map { latest[it.topicId] ?: it }
            }
            else -> items
        }
        current.followingUsers = if (append)
            (current.followingUsers + loaded.followingUsers).distinctBy(FollowingUser::userId)
        else loaded.followingUsers
        current.emptyText = loaded.emptyText
        if (!foldNew) current.sourcePage = loaded.page.coerceAtLeast(if (append) requestedPage else 1)
        current.sourceTotalPages = loaded.totalPages
        if (!foldNew) current.page = if (homeInfinite) current.sourcePage else 1
        current.totalPages = loaded.totalPages
        current.loaded = true
        if (!append && !foldNew) {
            mutableState.update { it.copy(pendingTopics = emptyList(), scrollToTopVersion = it.scrollToTopVersion + 1) }
        }
        if (!append && !accountFeed) cache.saveHomeCache(currentSort, items)
        if (!append) refreshSidebar(loaded.sidebar)
    }

    private fun refreshSidebar(parsed: HomeSidebar?) {
        parsed ?: return
        val old = mutableState.value.sidebar
        val merged = if (old == null) parsed else old.copy(
            forums = parsed.forums.ifEmpty { old.forums },
            hotTopics = if (parsed.hotTopicPeriods.isNotEmpty()) parsed.hotTopics else parsed.hotTopics.ifEmpty { old.hotTopics },
            hotTopicPeriods = parsed.hotTopicPeriods.ifEmpty { old.hotTopicPeriods },
            statsText = parsed.statsText.ifBlank { old.statsText },
            newUsers = parsed.newUsers.ifEmpty { old.newUsers },
            announcements = parsed.announcements.ifEmpty { old.announcements },
            dailyStars = parsed.dailyStars.ifEmpty { old.dailyStars },
            onlineUsers = parsed.onlineUsers.takeIf {
                it.count.isNotBlank() || it.items.isNotEmpty()
            } ?: old.onlineUsers,
        )
        mutableState.update { it.copy(sidebar = merged) }
        cache.saveHomeSidebar(merged)
    }
}

class HomePageViewModelFactory(
    private val repository: HomeBrowseRepository,
    private val cache: HomeBrowseCache,
    private val topicPageCache: TopicPageCacheRepository,
    private val topicViews: TopicViewsRepository,
    private val current: HomeTabState,
    private val currentSort: String,
    private val homeInfinite: Boolean,
    private val pageSize: () -> Int,
    private val showToast: (String) -> Unit,
    private val followingView: FollowingView = FollowingView.TOPICS,
    private val accountUserId: Long = 0,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        HomePageViewModel(
            repository, cache, topicPageCache, topicViews, current, currentSort, homeInfinite, pageSize, showToast,
            followingView, accountUserId,
        ) as T
}
