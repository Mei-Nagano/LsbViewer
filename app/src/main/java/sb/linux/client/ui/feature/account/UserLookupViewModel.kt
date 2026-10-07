package sb.linux.client.ui.feature.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import sb.linux.client.domain.account.UserLookupRepository

data class UserLookupUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val userId: Long? = null,
)

class UserLookupViewModel(
    private val repository: UserLookupRepository,
    private val username: String,
) : ViewModel() {
    private val mutableState = MutableStateFlow(UserLookupUiState())
    val uiState = mutableState.asStateFlow()

    fun resolve() {
        if (username.isBlank()) {
            mutableState.value = UserLookupUiState(loading = false, error = "链接里没有用户名")
            return
        }
        viewModelScope.launch {
            mutableState.value = UserLookupUiState()
            try {
                val id = repository.resolve(username)
                if (id <= 0) {
                    mutableState.update { it.copy(loading = false, error = "未找到用户「$username」") }
                } else {
                    mutableState.update { it.copy(loading = false, userId = id) }
                }
            } catch (error: Exception) {
                mutableState.update { it.copy(loading = false, error = error.message ?: "查找用户失败") }
            }
        }
    }
}

class UserLookupViewModelFactory(
    private val repository: UserLookupRepository,
    private val username: String,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        UserLookupViewModel(repository, username) as T
}
