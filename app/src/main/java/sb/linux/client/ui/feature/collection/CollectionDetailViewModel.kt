package sb.linux.client.ui.feature.collection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import sb.linux.client.common.collection.TopicCollectionActionForm
import sb.linux.client.common.collection.TopicCollectionDetail
import sb.linux.client.domain.collection.TopicCollectionPageRepository

data class CollectionDetailUiState(
    val detail: TopicCollectionDetail? = null,
    val loading: Boolean = false,
    val submitting: Boolean = false,
    val error: String? = null,
    val actionMessage: String? = null,
)

class CollectionDetailViewModel(
    private val repository: TopicCollectionPageRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow(CollectionDetailUiState())
    val uiState = mutableState.asStateFlow()
    private var requestVersion = 0

    fun load(path: String) {
        val version = ++requestVersion
        mutableState.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            try {
                val detail = repository.detail(path)
                if (version == requestVersion) mutableState.update { it.copy(detail = detail, loading = false) }
            } catch (error: Exception) {
                if (version == requestVersion) mutableState.update {
                    it.copy(loading = false, error = error.message ?: "加载失败")
                }
            }
        }
    }

    fun execute(form: TopicCollectionActionForm, reloadPath: String?) {
        if (mutableState.value.submitting) return
        viewModelScope.launch {
            mutableState.update { it.copy(submitting = true, actionMessage = null) }
            try {
                val detail = repository.execute(form, reloadPath)
                mutableState.update { it.copy(detail = detail ?: it.detail, actionMessage = "已提交并同步专辑") }
            } catch (error: Exception) {
                mutableState.update { it.copy(actionMessage = error.message ?: "提交失败") }
            } finally {
                mutableState.update { it.copy(submitting = false) }
            }
        }
    }

    fun clearActionMessage() = mutableState.update { it.copy(actionMessage = null) }
}

class CollectionDetailViewModelFactory(
    private val repository: TopicCollectionPageRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        CollectionDetailViewModel(repository) as T
}
