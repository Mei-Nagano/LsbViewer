package sb.linux.client.ui.feature.gacha

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import sb.linux.client.domain.gacha.CardRedemptionsRepository

class CardRedemptionsViewModel(
    private val repository: CardRedemptionsRepository,
) : ViewModel() {
    var records by mutableStateOf(repository.records())
        private set

    fun refresh() { records = repository.records() }

    fun clear() {
        repository.clear()
        refresh()
    }
}

class CardRedemptionsViewModelFactory(
    private val repository: CardRedemptionsRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        CardRedemptionsViewModel(repository) as T
}
