package sb.linux.client.ui.feature.gacha

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import sb.linux.client.domain.gacha.GachaOperationPage
import sb.linux.client.domain.gacha.GachaOperationRepository

data class GachaOperationUiState(
    val page: GachaOperationPage? = null,
    val path: String,
    val loading: Boolean = true,
    val error: String? = null,
    val submitting: Boolean = false,
)

class GachaOperationViewModel(
    private val repository: GachaOperationRepository,
    private val kind: String,
    private val preferredTitle: String,
    initialPath: String,
    private val showToast: (String) -> Unit,
) : ViewModel() {
    private val mutableState = MutableStateFlow(GachaOperationUiState(path = initialPath))
    val uiState = mutableState.asStateFlow()
    private var loadVersion = 0

    fun load() {
        val version = ++loadVersion
        val path = mutableState.value.path
        mutableState.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            try {
                val parsed = repository.load(path)
                val page = if (kind == "gift") parsed.copy(forms = filterGiftForms(parsed.forms)) else parsed
                if (version == loadVersion) mutableState.update { it.copy(page = page, loading = false) }
            } catch (error: Exception) {
                if (version == loadVersion) {
                    mutableState.update { it.copy(loading = false, error = error.message ?: "加载失败") }
                }
            }
        }
    }

    fun openPath(path: String) {
        if (mutableState.value.path == path) return
        mutableState.update { it.copy(path = path) }
        load()
    }

    fun submit(label: String, path: String, fields: List<Pair<String, String>>) {
        if (mutableState.value.submitting) return
        viewModelScope.launch {
            mutableState.update { it.copy(submitting = true) }
            try {
                val rejection = repository.submit(path, fields)
                if (rejection != null) showToast(rejection) else {
                    showToast("${label}成功，已同步源站")
                    load()
                }
            } catch (error: Exception) {
                showToast(error.message ?: "提交失败")
            } finally {
                mutableState.update { it.copy(submitting = false) }
            }
        }
    }

    private fun filterGiftForms(forms: List<sb.linux.client.domain.gacha.GachaOperationForm>) =
        forms.filter { form -> signature(form).let { containsGiftTarget(it) &&
            (preferredTitle.isBlank() || it.contains(preferredTitle, true)) } }.ifEmpty {
            forms.filter { containsGiftTarget(signature(it)) }
        }

    private fun signature(form: sb.linux.client.domain.gacha.GachaOperationForm) = buildString {
        append(form.label); append(' '); append(form.action); append(' ')
        form.fields.forEach { append(it.name); append(' '); append(it.label); append(' ') }
    }

    private fun containsGiftTarget(text: String) = text.contains("赠送", true) ||
        text.contains("gift", true) || text.contains("recipient", true) || text.contains("receiver", true)
}

class GachaOperationViewModelFactory(
    private val repository: GachaOperationRepository,
    private val kind: String,
    private val preferredTitle: String,
    private val initialPath: String,
    private val showToast: (String) -> Unit,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        GachaOperationViewModel(repository, kind, preferredTitle, initialPath, showToast) as T
}
