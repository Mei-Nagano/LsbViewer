package sb.linux.client.ui.feature.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import sb.linux.client.common.filter.KeywordFilterRules
import sb.linux.client.common.filter.KeywordFilterSettings
import sb.linux.client.common.filter.KeywordFilterSnapshot
import sb.linux.client.domain.community.KeywordFilterRepository

data class KeywordFilterUiState(
    val snapshot: KeywordFilterSnapshot = KeywordFilterSnapshot(),
    val input: String = "",
    val userInput: String = "",
    val presets: List<String> = emptyList(),
    val custom: List<String> = emptyList(),
    val users: List<String> = emptyList(),
    val selectedForums: Set<Long> = emptySet(),
    val loading: Boolean = true,
    val busy: Boolean = false,
    val error: String? = null,
    val toastMessage: String? = null,
)

class KeywordFilterViewModel(private val repository: KeywordFilterRepository) : ViewModel() {
    var state by mutableStateOf(KeywordFilterUiState())
        private set

    fun load() {
        state = state.copy(loading = true, error = null)
        viewModelScope.launch {
            try {
                bind(repository.refresh(force = true))
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                bind(repository.snapshot())
                state = state.copy(error = error.message ?: "加载失败")
            } finally {
                state = state.copy(loading = false)
            }
        }
    }

    fun setInput(value: String) { state = state.copy(input = value) }
    fun setUserInput(value: String) { state = state.copy(userInput = value) }

    fun togglePreset(value: String) {
        val preset = KeywordFilterRules.normalize(value)
        val values = if (preset in state.presets) state.presets - preset else state.presets + preset
        state = state.copy(presets = values)
    }

    fun addCustomWords() {
        val additions = KeywordFilterRules.splitInput(state.input)
        state = state.copy(
            custom = if (additions.isEmpty()) state.custom
            else KeywordFilterRules.clean(state.custom + additions, KeywordFilterRules.MAX_CUSTOM_WORDS),
            input = "",
        )
    }

    fun removeCustomWord(value: String) { state = state.copy(custom = state.custom - value) }

    fun addUsers() {
        val additions = KeywordFilterRules.splitInput(state.userInput)
        state = state.copy(
            users = if (additions.isEmpty()) state.users
            else KeywordFilterRules.clean(state.users + additions, KeywordFilterRules.MAX_USERS),
            userInput = "",
        )
    }

    fun removeUser(value: String) { state = state.copy(users = state.users - value) }
    fun selectForums(value: Set<Long>) { state = state.copy(selectedForums = value) }

    fun save() = persist(currentSettings())

    fun clearPersonalRules() {
        val defaults = state.snapshot.policy.defaultForumIds
        state = state.copy(presets = emptyList(), custom = emptyList(), users = emptyList(), selectedForums = defaults)
        persist(KeywordFilterSettings(forumExcludedIds = emptyList(), forumExtraIds = emptyList()))
    }

    fun consumeToast() { state = state.copy(toastMessage = null) }

    private fun currentSettings(): KeywordFilterSettings {
        val defaults = state.snapshot.policy.defaultForumIds
        return KeywordFilterSettings(
            presets = state.presets,
            custom = state.custom,
            users = state.users,
            forumExcludedIds = defaults.filter { it !in state.selectedForums },
            forumExtraIds = state.selectedForums.filter { it !in defaults },
        )
    }

    private fun persist(settings: KeywordFilterSettings) {
        if (state.busy) return
        state = state.copy(busy = true, error = null)
        viewModelScope.launch {
            try {
                val saved = repository.save(settings)
                bind(saved)
                state = state.copy(
                    toastMessage = if (saved.pending) "已保存到本地，网络恢复后自动同步" else "已同步到源站",
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                state = state.copy(error = error.message ?: "保存失败")
            } finally {
                state = state.copy(busy = false)
            }
        }
    }

    private fun bind(snapshot: KeywordFilterSnapshot) {
        state = state.copy(
            snapshot = snapshot,
            presets = snapshot.settings.presets,
            custom = snapshot.settings.custom,
            users = snapshot.settings.users,
            selectedForums = KeywordFilterRules.configuredForumIds(snapshot.settings, snapshot.policy),
        )
    }
}

class KeywordFilterViewModelFactory(
    private val repository: KeywordFilterRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        KeywordFilterViewModel(repository) as T
}
