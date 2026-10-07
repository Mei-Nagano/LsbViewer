package sb.linux.client.data

import sb.linux.client.domain.ai.AiAssistantSettings
import sb.linux.client.domain.ai.AiConfigPreset
import sb.linux.client.domain.ai.AiConfigurationRepository

/** Adapts the existing SharedPreferences settings store to the AI configuration contract. */
class AppSettingsAiConfigurationRepository(private val appSettings: AppSettings) : AiConfigurationRepository {
    override fun current() = AiAssistantSettings(
        enabled = appSettings.aiEnabled,
        autoRun = appSettings.aiAutoRun,
        apiUrl = appSettings.aiUrl,
        apiKey = appSettings.aiKey,
        model = appSettings.aiModel,
        prompt = appSettings.aiPrompt,
        temperature = appSettings.aiTemp,
        includeComments = appSettings.aiIncludeComments,
    )

    override fun reset() = appSettings.resetAi()

    override fun presets(): List<AiConfigPreset> = appSettings.aiConfigPresets()

    override fun save(settings: AiAssistantSettings) {
        appSettings.aiEnabled = settings.enabled
        appSettings.aiAutoRun = settings.autoRun
        appSettings.aiUrl = settings.apiUrl
        appSettings.aiKey = settings.apiKey
        appSettings.aiModel = settings.model
        appSettings.aiPrompt = settings.prompt
        appSettings.aiTemp = settings.temperature
        appSettings.aiIncludeComments = settings.includeComments
    }

    override fun saveToggles(enabled: Boolean, autoRun: Boolean, includeComments: Boolean) {
        appSettings.aiEnabled = enabled
        appSettings.aiAutoRun = autoRun
        appSettings.aiIncludeComments = includeComments
    }

    override fun saveCredentials(apiUrl: String, apiKey: String) {
        appSettings.aiUrl = apiUrl
        appSettings.aiKey = apiKey
    }

    override fun savePreset(name: String, settings: AiAssistantSettings): List<AiConfigPreset> {
        appSettings.aiUrl = settings.apiUrl
        appSettings.aiKey = settings.apiKey
        appSettings.aiModel = settings.model
        appSettings.aiPrompt = settings.prompt
        appSettings.aiTemp = settings.temperature
        appSettings.aiIncludeComments = settings.includeComments
        appSettings.saveCurrentAiPreset(name)
        return presets()
    }

    override fun applyPreset(name: String): AiAssistantSettings? {
        val preset = presets().firstOrNull { it.name == name } ?: return null
        appSettings.applyAiPreset(preset)
        return current()
    }

    override fun removePreset(name: String): List<AiConfigPreset> {
        appSettings.removeAiPreset(name)
        return presets()
    }
}
