package sb.linux.client.ui.feature.network

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import sb.linux.client.domain.network.DohBenchmark
import sb.linux.client.domain.network.DohConfiguration
import sb.linux.client.domain.network.DohServer
import sb.linux.client.domain.network.DohSettingsRepository
import java.util.concurrent.ConcurrentHashMap

data class DohBenchmarkUiState(
    val configuration: DohConfiguration,
    val testingIds: Set<String> = emptySet(),
    val runningIds: Set<String> = emptySet(),
    val results: Map<String, DohBenchmark> = emptyMap(),
)

class DohSettingsViewModel(private val repository: DohSettingsRepository) : ViewModel() {
    val defaultServers: List<DohServer> = repository.defaultServers
    private val semaphore = Semaphore(16)
    private val jobs = ConcurrentHashMap<String, Job>()
    private val mutableUiState = MutableStateFlow(DohBenchmarkUiState(repository.configuration()))
    val uiState: StateFlow<DohBenchmarkUiState> = mutableUiState

    fun isValidEndpoint(url: String): Boolean = repository.isValidEndpoint(url)

    fun setEnabled(enabled: Boolean) {
        repository.setEnabled(enabled)
        refreshConfiguration()
    }

    fun selectServer(url: String) {
        repository.selectServer(url)
        refreshConfiguration()
    }

    fun setBypassVpn(enabled: Boolean) {
        repository.setBypassVpn(enabled)
        refreshConfiguration()
    }

    fun setAutoWithoutVpn(enabled: Boolean) {
        repository.setAutoWithoutVpn(enabled)
        refreshConfiguration()
    }

    fun saveServers(servers: List<DohServer>) {
        repository.saveServers(servers)
        refreshConfiguration()
    }

    private fun refreshConfiguration() {
        mutableUiState.update { it.copy(configuration = repository.configuration()) }
    }

    fun test(servers: List<DohServer>) {
        servers.forEach { server ->
            val job = viewModelScope.launch(start = CoroutineStart.LAZY) { runBenchmark(server) }
            val previous = jobs.putIfAbsent(server.id, job)
            if (previous != null) {
                job.cancel()
                return@forEach
            }
            mutableUiState.update {
                it.copy(testingIds = it.testingIds + server.id, results = it.results - server.id)
            }
            job.invokeOnCompletion {
                jobs.remove(server.id, job)
                mutableUiState.update { state -> state.copy(testingIds = state.testingIds - server.id) }
            }
            job.start()
        }
    }

    fun cancel(serverId: String) { jobs[serverId]?.cancel() }

    fun cancelAll() { jobs.values.toList().forEach(Job::cancel) }

    fun clearResult(serverId: String) {
        mutableUiState.update { it.copy(results = it.results - serverId) }
    }

    private suspend fun runBenchmark(server: DohServer) {
        var acquired = false
        try {
            semaphore.acquire()
            acquired = true
            mutableUiState.update { it.copy(runningIds = it.runningIds + server.id) }
            val result = try {
                repository.benchmark(server.url)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                DohBenchmark(emptyList(), emptyList(), 1)
            }
            mutableUiState.update { it.copy(results = it.results + (server.id to result)) }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } finally {
            if (acquired) semaphore.release()
            mutableUiState.update { it.copy(runningIds = it.runningIds - server.id) }
        }
    }
}

class DohSettingsViewModelFactory(
    private val repository: DohSettingsRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = DohSettingsViewModel(repository) as T
}
