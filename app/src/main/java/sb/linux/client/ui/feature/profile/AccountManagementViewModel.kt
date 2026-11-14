package sb.linux.client.ui.feature.profile

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import sb.linux.client.domain.account.*

internal data class AccountManagementState(
    val page: AccountManagementPage? = null, val loading: Boolean = true,
    val busy: Boolean = false, val error: String? = null, val revision: Int = 0,
)

internal class AccountManagementViewModel(
    private val repository: AccountManagementRepository,
    private val identity: () -> Long,
    private val onChanged: () -> Unit,
) : ViewModel() {
    var state by mutableStateOf(AccountManagementState())
        private set
    var mutationVersion by mutableIntStateOf(0)
        private set
    var forgeProgress by mutableStateOf<String?>(null)
        private set
    private var job: Job? = null
    private val owner = identity()

    fun load(url: String) {
        if (state.busy) return
        job?.cancel()
        state = state.copy(loading = true, error = null)
        job = viewModelScope.launch {
            try {
                val page = repository.load(url)
                if (identity() == owner) state = AccountManagementState(page, loading = false, revision = state.revision + 1)
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { if (identity() == owner) state = state.copy(loading = false, error = error.message ?: "加载失败") }
        }
    }

    fun submit(form: AccountForm, fields: List<Pair<String, String>>) {
        val page = state.page ?: return
        if (state.busy || state.loading || identity() != owner) return
        state = state.copy(busy = true, error = null)
        job = viewModelScope.launch {
            try {
                val saved = repository.submit(page.url, form, fields)
                if (identity() == owner) {
                    state = AccountManagementState(saved, loading = false, revision = state.revision + 1)
                    if (form.method == "post") { mutationVersion++; onChanged() }
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { if (identity() == owner) state = state.copy(busy = false, error = error.message ?: "操作失败") }
        }
    }

    fun forgeAll() {
        if (state.busy || state.loading || identity() != owner) return
        state = state.copy(busy = true, error = null)
        forgeProgress = "正在逐级熔炼…"
        job = viewModelScope.launch {
            try {
                forgeAllTitles(repository, { identity() == owner }, { mutationVersion++; onChanged() }) { page, progress ->
                    state = state.copy(page = page, revision = state.revision + 1)
                    forgeProgress = progress
                }
                forgeProgress = "熔炼完成，已保留反贼与佩戴所需份数"
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { if (identity() == owner) state = state.copy(error = error.message ?: "熔炼失败") }
            finally { if (identity() == owner) state = state.copy(busy = false) }
        }
    }

    fun stopForge() { job?.cancel(); forgeProgress = "已停止后续熔炼"; state = state.copy(busy = false) }
}

internal class AccountManagementViewModelFactory(
    private val repository: AccountManagementRepository, private val identity: () -> Long, private val onChanged: () -> Unit,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = AccountManagementViewModel(repository, identity, onChanged) as T
}
