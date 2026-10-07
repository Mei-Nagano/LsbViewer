package sb.linux.client.ui.feature.topic

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import sb.linux.client.domain.community.UsageStatisticsRepository
import sb.linux.client.domain.topic.NewTopicFormData
import sb.linux.client.domain.topic.NewTopicDraft
import sb.linux.client.domain.topic.NewTopicDraftRepository
import sb.linux.client.domain.topic.NewTopicRepository
import sb.linux.client.domain.topic.NewTopicSubmission

data class NewTopicUiState(
    val formData: NewTopicFormData? = null,
    val loading: Boolean = true,
    val submitting: Boolean = false,
    val error: String? = null,
    val submitError: String? = null,
    val submissionRevision: Int = 0,
    val hasRestorableDraft: Boolean = false,
    val hasSavedDraft: Boolean = false,
)

class NewTopicViewModel(
    private val repository: NewTopicRepository,
    private val usageStatistics: UsageStatisticsRepository,
    private val drafts: NewTopicDraftRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow(NewTopicUiState())
    val uiState = mutableState.asStateFlow()

    fun load(editId: Long) {
        viewModelScope.launch {
            val allowDraft = editId == 0L
            val hasRestorableDraft = allowDraft && drafts.hasRestorableDraft()
            val hasSavedDraft = allowDraft && drafts.hasSavedDraft()
            mutableState.update {
                it.copy(
                    loading = true,
                    error = null,
                    hasRestorableDraft = hasRestorableDraft,
                    hasSavedDraft = hasSavedDraft,
                )
            }
            try {
                val form = repository.load(editId)
                mutableState.update { it.copy(formData = form, loading = false) }
            } catch (error: Exception) {
                mutableState.update { it.copy(loading = false, error = error.message ?: "加载失败") }
            }
        }
    }

    fun saveDraft(draft: NewTopicDraft) {
        drafts.save(draft)
        refreshDraftAvailability()
    }

    fun loadDraft(): NewTopicDraft = drafts.load()

    fun clearDraft() {
        drafts.clear()
        refreshDraftAvailability()
    }

    private fun refreshDraftAvailability() {
        mutableState.update {
            it.copy(
                hasRestorableDraft = drafts.hasRestorableDraft(),
                hasSavedDraft = drafts.hasSavedDraft(),
            )
        }
    }

    fun submit(input: NewTopicSubmission) {
        if (mutableState.value.submitting) return
        viewModelScope.launch {
            mutableState.update { it.copy(submitting = true, submitError = null) }
            try {
                val failure = repository.submit(input)
                if (failure != null) mutableState.update { it.copy(submitError = failure) }
                else {
                    if (input.editId == 0L) usageStatistics.recordEvent("topic", title = input.title)
                    mutableState.update { it.copy(submissionRevision = it.submissionRevision + 1) }
                }
            } catch (error: Exception) {
                mutableState.update { it.copy(submitError = error.message ?: "发布失败") }
            } finally {
                mutableState.update { it.copy(submitting = false) }
            }
        }
    }
}

class NewTopicViewModelFactory(
    private val repository: NewTopicRepository,
    private val usageStatistics: UsageStatisticsRepository,
    private val drafts: NewTopicDraftRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        NewTopicViewModel(repository, usageStatistics, drafts) as T
}
