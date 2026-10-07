package sb.linux.client.domain.ai

data class AiAssistantSettings(
    val enabled: Boolean = false,
    val autoRun: Boolean = false,
    val apiUrl: String = "",
    val apiKey: String = "",
    val model: String = "gpt-4o-mini",
    val prompt: String = "",
    val temperature: Float = 0.3f,
    val includeComments: Boolean = true,
)

data class AiConfigPreset(
    val name: String,
    val url: String,
    val key: String,
    val model: String,
    val temperature: Float,
    val prompt: String,
    val includeComments: Boolean,
)

/** Local settings and named presets for the AI assistant feature. */
interface AiConfigurationRepository {
    fun current(): AiAssistantSettings
    fun reset()
    fun presets(): List<AiConfigPreset>
    fun save(settings: AiAssistantSettings)
    fun saveToggles(enabled: Boolean, autoRun: Boolean, includeComments: Boolean)
    fun saveCredentials(apiUrl: String, apiKey: String)
    fun savePreset(name: String, settings: AiAssistantSettings): List<AiConfigPreset>
    fun applyPreset(name: String): AiAssistantSettings?
    fun removePreset(name: String): List<AiConfigPreset>
}
