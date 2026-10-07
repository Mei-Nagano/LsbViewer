package sb.linux.client.ui.feature.gacha

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import sb.linux.client.domain.gacha.GachaMarketPage
import sb.linux.client.domain.gacha.GachaMarketListing
import sb.linux.client.domain.gacha.GachaMarketRepository

data class GachaMarketUiState(
    val market: GachaMarketPage? = null,
    val query: String = "",
    val rarity: String = "",
    val sort: String = "latest",
    val page: Int = 1,
    val loading: Boolean = true,
    val error: String? = null,
    val submitting: Boolean = false,
)

class GachaMarketViewModel(
    private val repository: GachaMarketRepository,
    private val showToast: (String) -> Unit,
) : ViewModel() {
    private val mutableState = MutableStateFlow(GachaMarketUiState())
    val uiState = mutableState.asStateFlow()
    private var requestVersion = 0

    fun setQuery(value: String) = mutableState.update { it.copy(query = value) }

    fun load(targetPage: Int = mutableState.value.page) {
        val version = ++requestVersion
        val request = mutableState.value
        mutableState.update { it.copy(loading = true, error = null, page = targetPage) }
        viewModelScope.launch {
            try {
                val market = repository.load(request.query, request.rarity, request.sort, targetPage)
                if (version == requestVersion) mutableState.update { it.copy(market = market) }
            } catch (error: Exception) {
                if (version == requestVersion) {
                    mutableState.update { it.copy(error = error.message ?: "加载失败") }
                }
            } finally {
                if (version == requestVersion) mutableState.update { it.copy(loading = false) }
            }
        }
    }

    fun selectRarity(value: String) {
        if (mutableState.value.rarity == value) return
        mutableState.update { it.copy(rarity = value, page = 1) }
        load(1)
    }

    fun selectSort(value: String) {
        if (mutableState.value.sort == value) return
        mutableState.update { it.copy(sort = value, page = 1) }
        load(1)
    }

    fun purchase(listing: GachaMarketListing, quantity: Int) {
        if (mutableState.value.submitting) return
        viewModelScope.launch {
            mutableState.update { it.copy(submitting = true) }
            try {
                val rejection = repository.purchase(listing, quantity)
                if (rejection != null) showToast(rejection) else {
                    showToast("购买成功")
                    load()
                }
            } catch (error: Exception) {
                showToast(error.message ?: "购买失败")
            } finally {
                mutableState.update { it.copy(submitting = false) }
            }
        }
    }
}

class GachaMarketViewModelFactory(
    private val repository: GachaMarketRepository,
    private val showToast: (String) -> Unit,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        GachaMarketViewModel(repository, showToast) as T
}
