package sb.linux.client.ui.feature.community

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import sb.linux.client.domain.community.ReportForm
import sb.linux.client.domain.community.ReportRepository

data class ReportUiState(
    val form: ReportForm? = null,
    val reason: String? = null,
    val details: String = "",
    val loading: Boolean = true,
    val submitting: Boolean = false,
    val message: String? = null,
)

class ReportViewModel(
    private val repository: ReportRepository,
    private val reportId: Long,
    private val type: String,
    private val onSubmitted: () -> Unit,
) : ViewModel() {
    private val mutableState = MutableStateFlow(ReportUiState())
    val uiState = mutableState.asStateFlow()

    fun load() {
        viewModelScope.launch {
            mutableState.update { it.copy(loading = true, message = null) }
            runCatching { repository.loadReport(reportId, type) }
                .onSuccess { form ->
                    mutableState.update { it.copy(form = form, reason = form.reasons.firstOrNull()?.first, loading = false) }
                }
                .onFailure { error ->
                    mutableState.update { it.copy(loading = false, message = error.message ?: "举报表单加载失败") }
                }
        }
    }

    fun selectReason(reason: String) = mutableState.update { it.copy(reason = reason) }

    fun updateDetails(details: String) = mutableState.update { it.copy(details = details) }

    fun submit() {
        val state = mutableState.value
        val reason = state.reason
        if (state.submitting) return
        if (reason == null) {
            mutableState.update { it.copy(message = "请选择举报理由") }
            return
        }
        viewModelScope.launch {
            mutableState.update { it.copy(submitting = true, message = null) }
            try {
                val rejection = repository.submitReport(reportId, type, reason, state.details)
                if (rejection != null) mutableState.update { it.copy(message = rejection) }
                else onSubmitted()
            } catch (error: Exception) {
                mutableState.update { it.copy(message = error.message ?: "提交失败") }
            } finally {
                mutableState.update { it.copy(submitting = false) }
            }
        }
    }
}

class ReportViewModelFactory(
    private val repository: ReportRepository,
    private val reportId: Long,
    private val type: String,
    private val onSubmitted: () -> Unit,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        ReportViewModel(repository, reportId, type, onSubmitted) as T
}
