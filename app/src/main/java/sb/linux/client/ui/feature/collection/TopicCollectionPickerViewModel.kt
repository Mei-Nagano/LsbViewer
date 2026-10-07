package sb.linux.client.ui.feature.collection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import sb.linux.client.common.collection.TopicCollectionActionForm
import sb.linux.client.common.collection.TopicCollectionPicker
import sb.linux.client.domain.collection.TopicCollectionPageRepository

data class TopicCollectionPickerUiState(
    val picker: TopicCollectionPicker? = null,
    val loading: Boolean = true,
    val error: String? = null,
    val submittingOwner: Long? = null,
    val createValues: Map<String, String> = emptyMap(),
    val actionMessage: String? = null,
)

class TopicCollectionPickerViewModel(
    private val repository: TopicCollectionPageRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow(TopicCollectionPickerUiState())
    val uiState = mutableState.asStateFlow()
    private var requestVersion = 0

    fun load(topicPath: String) {
        val version = ++requestVersion
        mutableState.update { it.copy(loading = true, error = null) }
        viewModelScope.launch { reload(topicPath, version) }
    }

    fun updateCreateValue(name: String, value: String) {
        mutableState.update { it.copy(createValues = it.createValues + (name to value)) }
    }

    fun submit(form: TopicCollectionActionForm, owner: Long, success: String, topicPath: String) {
        if (mutableState.value.submittingOwner != null) return
        viewModelScope.launch {
            mutableState.update { it.copy(submittingOwner = owner, actionMessage = null) }
            try {
                repository.execute(form)
                val refreshed = reload(topicPath, ++requestVersion)
                mutableState.update {
                    it.copy(actionMessage = if (refreshed) success else "$success，刷新状态失败")
                }
            } catch (error: Exception) {
                mutableState.update { it.copy(actionMessage = error.message ?: "提交失败") }
            } finally {
                mutableState.update { it.copy(submittingOwner = null) }
            }
        }
    }

    fun clearActionMessage() = mutableState.update { it.copy(actionMessage = null) }

    private suspend fun reload(topicPath: String, version: Int): Boolean {
        return try {
            val picker = repository.picker(topicPath)
            if (version == requestVersion) mutableState.update {
                it.copy(
                    picker = picker,
                    createValues = picker?.createForm?.controls.orEmpty().associate { control -> control.name to control.value },
                    error = null,
                )
            }
            true
        } catch (error: Exception) {
            if (version == requestVersion) mutableState.update { it.copy(error = error.message ?: "加载失败") }
            false
        } finally {
            if (version == requestVersion) mutableState.update { it.copy(loading = false) }
        }
    }
}

class TopicCollectionPickerViewModelFactory(
    private val repository: TopicCollectionPageRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        TopicCollectionPickerViewModel(repository) as T
}
