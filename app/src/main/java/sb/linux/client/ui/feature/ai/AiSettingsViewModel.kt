package sb.linux.client.ui.feature.ai

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import sb.linux.client.domain.ai.AiAssistantRepository
import sb.linux.client.domain.ai.AiAssistantSettings
import sb.linux.client.domain.ai.AiConfigPreset
import sb.linux.client.domain.ai.AiConfigurationRepository
import sb.linux.client.domain.ai.AiSummaryHistoryRepository
import sb.linux.client.domain.ai.AiSummaryRecord

data class AiSettingsUiState(
    val settings: AiAssistantSettings,
    val temperatureText: String = settings.temperature.toString(),
    val presets: List<AiConfigPreset> = emptyList(),
    val summaryHistory: List<AiSummaryRecord> = emptyList(),
    val presetName: String = "",
    val modelOptions: List<String> = emptyList(),
    val modelMenuOpen: Boolean = false,
    val modelBusy: Boolean = false,
    val feedback: String? = null,
)

class AiSettingsViewModel(
    private val settingsRepository: AiConfigurationRepository,
    private val assistantRepository: AiAssistantRepository,
    private val historyRepository: AiSummaryHistoryRepository,
) : ViewModel() {
    var state by mutableStateOf(
        AiSettingsUiState(
            settingsRepository.current(),
            presets = settingsRepository.presets(),
            summaryHistory = historyRepository.history(),
        ),
    )
        private set

    fun updateSettings(update: (AiAssistantSettings) -> AiAssistantSettings) {
        state = state.copy(settings = update(state.settings))
    }

    fun setTemperatureText(value: String) { state = state.copy(temperatureText = value) }
    fun setPresetName(value: String) { state = state.copy(presetName = value) }
    fun setModelMenuOpen(value: Boolean) { state = state.copy(modelMenuOpen = value) }
    fun consumeFeedback() { state = state.copy(feedback = null) }

    fun saveSettings() {
        val settings = normalizedSettings()
        settingsRepository.save(settings)
        state = state.copy(settings = settings, feedback = "✓ AI 设置已保存")
    }

    fun persistToggles() = settingsRepository.saveToggles(
        state.settings.enabled,
        state.settings.autoRun,
        state.settings.includeComments,
    )

    fun fetchModels() {
        val settings = state.settings.copy(apiUrl = state.settings.apiUrl.trim(), apiKey = state.settings.apiKey.trim())
        settingsRepository.saveCredentials(settings.apiUrl, settings.apiKey)
        state = state.copy(settings = settings, modelBusy = true, feedback = null)
        viewModelScope.launch {
            try {
                val models = assistantRepository.fetchModels(settings.apiUrl, settings.apiKey)
                state = state.copy(
                    modelOptions = models,
                    modelMenuOpen = models.isNotEmpty(),
                    feedback = if (models.isEmpty()) "✗ 接口没有返回可用模型" else "✓ 已获取 ${models.size} 个模型",
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                state = state.copy(feedback = "✗ ${error.message}")
            } finally {
                state = state.copy(modelBusy = false)
            }
        }
    }

    fun savePreset() {
        if (state.presetName.isBlank()) return
        val settings = normalizedSettings()
        val presets = settingsRepository.savePreset(state.presetName, settings)
        state = state.copy(
            settings = settings,
            presets = presets,
            feedback = "✓ AI 配置方案已保存",
        )
    }

    fun applyPreset(name: String): AiConfigPreset? {
        val settings = settingsRepository.applyPreset(name) ?: return null
        val preset = state.presets.firstOrNull { it.name == name } ?: return null
        state = state.copy(settings = settings, temperatureText = settings.temperature.toString(), presetName = name)
        return preset
    }

    fun removePreset(name: String) {
        state = state.copy(presets = settingsRepository.removePreset(name))
    }

    fun removeSummary(record: AiSummaryRecord) {
        historyRepository.remove(record)
        state = state.copy(summaryHistory = historyRepository.history())
    }

    fun setSummaryFavorite(record: AiSummaryRecord, favorite: Boolean) {
        historyRepository.setFavorite(record, favorite)
        state = state.copy(summaryHistory = historyRepository.history())
    }

    private fun normalizedSettings(): AiAssistantSettings = state.settings.copy(
        apiUrl = state.settings.apiUrl.trim(),
        apiKey = state.settings.apiKey.trim(),
        model = state.settings.model.trim(),
        temperature = (state.temperatureText.toFloatOrNull() ?: 0.3f).coerceIn(0f, 2f),
    )
}

class AiSettingsViewModelFactory(
    private val settingsRepository: AiConfigurationRepository,
    private val assistantRepository: AiAssistantRepository,
    private val historyRepository: AiSummaryHistoryRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        AiSettingsViewModel(settingsRepository, assistantRepository, historyRepository) as T
}
