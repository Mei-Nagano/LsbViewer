package sb.linux.client.ui.feature.community

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import sb.linux.client.domain.community.BrowsingHistoryRepository
import sb.linux.client.domain.topic.TopicCard

class BrowsingHistoryViewModel(
    private val repository: BrowsingHistoryRepository,
) : ViewModel() {
    var topics by mutableStateOf(repository.history())
        private set

    fun refresh() {
        topics = repository.history()
    }

    fun remove(topicId: Long) {
        repository.remove(topicId)
        refresh()
    }

    fun clear() {
        repository.clear()
        refresh()
    }
}

class BrowsingHistoryViewModelFactory(
    private val repository: BrowsingHistoryRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        BrowsingHistoryViewModel(repository) as T
}
