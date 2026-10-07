package sb.linux.client.ui.screens

import sb.linux.client.common.content.HtmlTextConversion
import sb.linux.client.domain.ai.AiAssistantConfig
import sb.linux.client.ui.feature.ai.AiAssistantViewModel

/** Prepares topic text and connects summary results to screen state and its cache. */
internal class TopicAiSummaryActions(
    private val topicId: Long,
    private val state: TopicPagingState,
    private val ui: TopicScreenUiState,
    private val viewModel: AiAssistantViewModel,
) {
    fun cachedSummary(): String? = viewModel.cachedTopicSummary(topicId)

    fun shouldAutoRun(): Boolean = viewModel.shouldAutoRunTopicSummary()

    fun run(force: Boolean = false) {
        val page = state.page.value ?: return
        if (ui.aiBusy) return
        if (!force) {
            viewModel.cachedTopicSummary(topicId)?.let {
                ui.aiSummary = it
                return
            }
        }
        val settings = viewModel.topicSettings()
        val config = AiAssistantConfig(
            settings.apiUrl,
            settings.apiKey,
            settings.model,
            settings.prompt,
            settings.temperature.toDouble(),
        )
        if (!config.isConfigured) {
            ui.aiError = "请先在「我的 → 应用设置」中配置 AI API"
            return
        }
        val posts = if (settings.includeComments) page.posts else page.posts.take(1)
        val content = posts.joinToString("\n\n") {
            "[${it.authorName}]" + HtmlTextConversion.htmlToText(it.contentHtml)
        }
        ui.aiBusy = true
        ui.aiError = null
        viewModel.summarize(config, page.title, content) { result ->
            result.onSuccess { summary ->
                ui.aiSummary = summary
                viewModel.saveTopicSummary(topicId, summary, page.title)
            }.onFailure { ui.aiError = it.message }
            ui.aiBusy = false
        }
    }
}
