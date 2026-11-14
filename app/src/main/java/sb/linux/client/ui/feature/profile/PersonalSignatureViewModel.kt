package sb.linux.client.ui.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import sb.linux.client.domain.profile.PersonalSignaturePage
import sb.linux.client.domain.profile.PersonalSignatureRepository

internal data class PersonalSignatureUiState(
    val page: PersonalSignaturePage? = null,
    val signature: String = "",
    val hidden: Boolean = false,
    val loading: Boolean = true,
    val saving: Boolean = false,
    val error: String? = null,
    val message: String? = null,
) {
    val changed: Boolean get() = page?.let { signature != it.signature || hidden != it.hidden } ?: false
}

internal class PersonalSignatureViewModel(private val repository: PersonalSignatureRepository) : ViewModel() {
    private val state = MutableStateFlow(PersonalSignatureUiState())
    val uiState = state.asStateFlow()

    fun load() {
        if (state.value.saving) return
        viewModelScope.launch {
            state.update { it.copy(loading = true, error = null, message = null) }
            try {
                applyPage(repository.load())
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                state.update { it.copy(loading = false, error = error.message ?: "签名加载失败") }
            }
        }
    }

    fun editSignature(value: String) {
        val current = state.value
        if (current.saving || current.loading) return
        if (current.page?.maxLength?.let { value.length > it } == true) return
        state.update { it.copy(signature = value, error = null, message = null) }
    }

    fun editHidden(value: Boolean) {
        if (!state.value.saving && !state.value.loading)
            state.update { it.copy(hidden = value, error = null, message = null) }
    }

    fun save() {
        val current = state.value
        if (current.page == null || current.loading || current.saving || !current.changed) return
        viewModelScope.launch {
            state.update { it.copy(saving = true, error = null, message = null) }
            try {
                applyPage(repository.save(current.signature, current.hidden))
                state.update { it.copy(message = "签名设置已保存") }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                state.update { it.copy(error = error.message ?: "签名保存失败") }
            } finally {
                state.update { it.copy(saving = false) }
            }
        }
    }

    private fun applyPage(page: PersonalSignaturePage) {
        state.update { it.copy(page = page, signature = page.signature, hidden = page.hidden,
            loading = false, error = null) }
    }
}

internal class PersonalSignatureViewModelFactory(private val repository: PersonalSignatureRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = PersonalSignatureViewModel(repository) as T
}
