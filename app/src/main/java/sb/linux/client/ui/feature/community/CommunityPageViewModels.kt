package sb.linux.client.ui.feature.community

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import sb.linux.client.domain.community.IdentityCenterData
import sb.linux.client.domain.community.IdentityRepository
import sb.linux.client.domain.community.InviteCenter
import sb.linux.client.domain.community.InviteRepository
import sb.linux.client.domain.community.LeaderRow
import sb.linux.client.domain.community.LeaderboardRepository

data class IdentityCenterUiState(
    val data: IdentityCenterData? = null,
    val loading: Boolean = true,
    val error: String? = null,
)

class IdentityCenterViewModel(private val repository: IdentityRepository) : ViewModel() {
    private val mutableState = MutableStateFlow(IdentityCenterUiState())
    val uiState = mutableState.asStateFlow()

    fun load() {
        viewModelScope.launch {
            mutableState.update { it.copy(loading = true, error = null) }
            try {
                mutableState.update { it.copy(data = repository.loadIdentity(), loading = false) }
            } catch (error: Exception) {
                mutableState.update { it.copy(loading = false, error = error.message ?: "认证中心加载失败") }
            }
        }
    }
}

class IdentityCenterViewModelFactory(private val repository: IdentityRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        IdentityCenterViewModel(repository) as T
}

data class InviteCenterUiState(
    val data: InviteCenter? = null,
    val loading: Boolean = true,
    val error: String? = null,
)

class InviteCenterViewModel(private val repository: InviteRepository) : ViewModel() {
    private val mutableState = MutableStateFlow(InviteCenterUiState())
    val uiState = mutableState.asStateFlow()

    fun load() {
        viewModelScope.launch {
            mutableState.update { it.copy(loading = true, error = null) }
            try {
                mutableState.update { it.copy(data = repository.loadInvitations(), loading = false) }
            } catch (error: Exception) {
                mutableState.update { it.copy(loading = false, error = error.message) }
            }
        }
    }
}

class InviteCenterViewModelFactory(private val repository: InviteRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = InviteCenterViewModel(repository) as T
}

data class LeaderboardUiState(
    val type: String,
    val rows: List<LeaderRow> = emptyList(),
    val loading: Boolean = true,
    val error: String? = null,
)

class LeaderboardViewModel(
    private val repository: LeaderboardRepository,
    initialType: String,
) : ViewModel() {
    private val mutableState = MutableStateFlow(LeaderboardUiState(initialType))
    val uiState = mutableState.asStateFlow()
    private var requestVersion = 0

    fun selectType(type: String) {
        mutableState.update { it.copy(type = type) }
        load()
    }

    fun load() {
        val version = ++requestVersion
        val type = mutableState.value.type
        mutableState.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            try {
                val rows = repository.loadLeaderboard(type)
                if (version == requestVersion) mutableState.update { it.copy(rows = rows, loading = false) }
            } catch (error: Exception) {
                if (version == requestVersion) {
                    mutableState.update { it.copy(loading = false, error = error.message) }
                }
            }
        }
    }
}

class LeaderboardViewModelFactory(
    private val repository: LeaderboardRepository,
    private val initialType: String,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        LeaderboardViewModel(repository, initialType) as T
}
