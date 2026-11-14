package sb.linux.client.ui.feature.profile

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import sb.linux.client.domain.account.*

internal class GachaOverviewViewModel(private val users: UserPageRepository,
    private val management: AccountManagementRepository, private val uid: Long, private val identity: () -> Long) : ViewModel() {
    var points by mutableStateOf(users.cached(uid, "points_rewards")?.points.orEmpty()); private set
    var recipes by mutableStateOf<List<AccountNode.TitleCard>>(emptyList()); private set
    var error by mutableStateOf<String?>(null); private set
    var loading by mutableStateOf(false); private set
    private val owner = identity()
    private var version = 0
    private var job: kotlinx.coroutines.Job? = null
    fun refresh(force: Boolean = false) {
        val request = ++version
        job?.cancel()
        loading = true
        job = viewModelScope.launch {
            try {
                val results = kotlinx.coroutines.coroutineScope {
                    val records = async { users.load(uid, "points_rewards", force).points }
                    val page = async { management.load("/gacha_recipes") }
                    records.await() to page.await()
                }
                if (identity() == owner && request == version) {
                    points = results.first; recipes = cards(results.second.nodes); error = null
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (failure: Exception) { if (identity() == owner && request == version) error = failure.message }
            finally { if (request == version) loading = false }
        }
    }
    private fun cards(nodes: List<AccountNode>): List<AccountNode.TitleCard> = nodes.flatMap {
        when (it) {
            is AccountNode.TitleCard -> if (it.children.any { child -> child is AccountNode.Pills }) listOf(it) else emptyList()
            is AccountNode.Grid -> cards(it.children)
            is AccountNode.Group -> cards(it.children)
            is AccountNode.Section -> cards(it.children)
            else -> emptyList()
        }
    }
}

internal class GachaOverviewFactory(private val users: UserPageRepository, private val management: AccountManagementRepository,
    private val uid: Long, private val identity: () -> Long) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST") override fun <T : ViewModel> create(modelClass: Class<T>): T = GachaOverviewViewModel(users, management, uid, identity) as T
}
