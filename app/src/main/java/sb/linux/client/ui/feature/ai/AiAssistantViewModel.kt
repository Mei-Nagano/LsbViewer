package sb.linux.client.ui.feature.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import sb.linux.client.domain.ai.AiAssistantConfig
import sb.linux.client.domain.ai.AiAssistantRepository
import sb.linux.client.domain.ai.AiAssistantSettings
import sb.linux.client.domain.ai.AiConfigurationRepository
import sb.linux.client.domain.ai.AiSummaryHistoryRepository

class AiAssistantViewModel(
    private val repository: AiAssistantRepository,
    private val configuration: AiConfigurationRepository,
    private val summaries: AiSummaryHistoryRepository,
) : ViewModel() {
    fun topicSettings(): AiAssistantSettings = configuration.current()

    fun cachedTopicSummary(topicId: Long): String? = summaries.cached(topicId)

    fun shouldAutoRunTopicSummary(): Boolean = configuration.current().let { it.enabled && it.autoRun }

    fun saveTopicSummary(topicId: Long, summary: String, title: String) =
        summaries.save(topicId, summary, title)

    fun summarize(
        config: AiAssistantConfig,
        title: String,
        content: String,
        onComplete: (Result<String>) -> Unit,
    ) = runRequest({ repository.summarize(config, title, content) {} }, onComplete)

    fun fetchModels(apiUrl: String, apiKey: String, onComplete: (Result<List<String>>) -> Unit) =
        runRequest({ repository.fetchModels(apiUrl, apiKey) }, onComplete)

    private fun <T> runRequest(request: suspend () -> T, onComplete: (Result<T>) -> Unit) {
        viewModelScope.launch {
            try {
                onComplete(Result.success(request()))
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                onComplete(Result.failure(error))
            }
        }
    }
}

class AiAssistantViewModelFactory(
    private val repository: AiAssistantRepository,
    private val configuration: AiConfigurationRepository,
    private val summaries: AiSummaryHistoryRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        AiAssistantViewModel(repository, configuration, summaries) as T
}
