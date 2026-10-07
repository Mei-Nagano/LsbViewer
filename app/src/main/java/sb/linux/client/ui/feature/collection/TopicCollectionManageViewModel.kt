package sb.linux.client.ui.feature.collection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import sb.linux.client.common.collection.TopicCollectionActionForm
import sb.linux.client.common.collection.key
import sb.linux.client.common.collection.withEditedValues
import sb.linux.client.domain.collection.TopicCollectionPageRepository

data class TopicCollectionManageUiState(
    val title: String = "淘帖管理",
    val actions: List<TopicCollectionActionForm> = emptyList(),
    val loading: Boolean = true,
    val submitting: Boolean = false,
    val error: String? = null,
    val editedValues: Map<String, Map<String, String>> = emptyMap(),
    val actionMessage: String? = null,
)

class TopicCollectionManageViewModel(
    private val repository: TopicCollectionPageRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow(TopicCollectionManageUiState())
    val uiState = mutableState.asStateFlow()
    private var loadVersion = 0

    fun load(path: String) {
        val version = ++loadVersion
        mutableState.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            reload(path, version)
        }
    }

    fun updateValue(form: TopicCollectionActionForm, name: String, value: String) {
        val key = form.key()
        mutableState.update { state ->
            state.copy(editedValues = state.editedValues + (key to (state.editedValues[key].orEmpty() + (name to value))))
        }
    }

    fun submit(form: TopicCollectionActionForm, path: String, values: Map<String, String>, success: String) {
        if (mutableState.value.submitting) return
        viewModelScope.launch {
            mutableState.update { it.copy(submitting = true, actionMessage = null) }
            try {
                repository.execute(form.withEditedValues(values))
                val loaded = reload(path, ++loadVersion)
                mutableState.update {
                    it.copy(actionMessage = if (loaded) success else "操作已提交，但刷新失败")
                }
            } catch (error: Exception) {
                mutableState.update { it.copy(actionMessage = error.message ?: "提交失败") }
            } finally {
                mutableState.update { it.copy(submitting = false) }
            }
        }
    }

    fun clearActionMessage() = mutableState.update { it.copy(actionMessage = null) }

    private suspend fun reload(path: String, version: Int): Boolean {
        return try {
            val page = repository.manage(path)
            if (version == loadVersion) mutableState.update {
                it.copy(
                    title = page.title,
                    actions = page.actions,
                    editedValues = page.actions.associate { form ->
                        form.key() to form.controls.associate { control -> control.name to control.value }
                    },
                    error = null,
                )
            }
            true
        } catch (error: Exception) {
            if (version == loadVersion) mutableState.update { it.copy(error = error.message ?: "加载失败") }
            false
        } finally {
            if (version == loadVersion) mutableState.update { it.copy(loading = false) }
        }
    }
}

class TopicCollectionManageViewModelFactory(
    private val repository: TopicCollectionPageRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        TopicCollectionManageViewModel(repository) as T
}
