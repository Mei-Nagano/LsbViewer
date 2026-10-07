package sb.linux.client.ui.feature.community

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import sb.linux.client.domain.community.CommentFavorite
import sb.linux.client.domain.community.FavoritesRepository
import sb.linux.client.domain.topic.TopicCard

data class FavoritesUiState(
    val topics: List<TopicCard> = emptyList(),
    val comments: List<CommentFavorite> = emptyList(),
    val syncing: Boolean = false,
)

class FavoritesViewModel(private val repository: FavoritesRepository) : ViewModel() {
    private val mutableState = MutableStateFlow(FavoritesUiState())
    val uiState = mutableState.asStateFlow()

    fun load() = mutableState.update {
        it.copy(topics = repository.topics(), comments = repository.comments())
    }

    fun syncFromSource(userId: Long, loggedIn: Boolean) {
        if (!loggedIn || userId <= 0 || mutableState.value.syncing) return
        viewModelScope.launch {
            mutableState.update { it.copy(syncing = true) }
            try { repository.syncTopics(userId) } catch (_: Exception) { }
            load()
            mutableState.update { it.copy(syncing = false) }
        }
    }

    fun removeTopic(topicId: Long) { repository.removeTopic(topicId); load() }
    fun removeComment(replyId: Long) { repository.removeComment(replyId); load() }
    fun clearTopics() { repository.clearTopics(); load() }
    fun clearComments() { repository.clearComments(); load() }
}

class FavoritesViewModelFactory(private val repository: FavoritesRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = FavoritesViewModel(repository) as T
}
