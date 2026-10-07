package sb.linux.client.domain.ai

data class AiAssistantConfig(
    val apiUrl: String,
    val apiKey: String,
    val model: String,
    val prompt: String,
    val temperature: Double,
) {
    val isConfigured: Boolean get() = apiUrl.isNotBlank() && model.isNotBlank()
}

interface AiAssistantRepository {
    suspend fun summarize(
        config: AiAssistantConfig,
        title: String,
        content: String,
        onStatus: (String) -> Unit,
    ): String

    suspend fun fetchModels(apiUrl: String, apiKey: String): List<String>
}

object AiAssistantDefaults {
    const val SUMMARY_PROMPT = "你是一个论坛帖子总结助手。请用简体中文对帖子做简洁总结：先一句话概括主题，再分点列出关键信息（如有重要讨论、结论或争议请说明）。控制在 200 字以内。输出使用 Markdown：分点用 `- `，需要强调用 `**加粗**`，可用 `### ` 小标题。不要用表格，不要输出思考过程，不要把整段回答包在代码块里。"
}
