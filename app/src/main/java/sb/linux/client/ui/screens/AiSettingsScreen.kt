package sb.linux.client.ui.screens
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import sb.linux.client.ui.session.Session
import sb.linux.client.ui.feature.ai.AiSettingsViewModel
import sb.linux.client.ui.feature.ai.AiSettingsViewModelFactory
import sb.linux.client.domain.ai.AiSummaryRecord
import sb.linux.client.ui.theme.AppMenuDefaults
import sb.linux.client.ui.patterns.settings.SettingsChoiceMenu

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AiSettingsDetailScreen(session: Session, nav: NavHostController, section: String) {
    val settingsViewModelFactory = remember(
        session.aiConfigurationRepository, session.aiAssistantRepository, session.aiSummaryHistoryRepository,
    ) {
        AiSettingsViewModelFactory(
            session.aiConfigurationRepository,
            session.aiAssistantRepository,
            session.aiSummaryHistoryRepository,
        )
    }
    val settingsViewModel: AiSettingsViewModel = viewModel(factory = settingsViewModelFactory)
    val state = settingsViewModel.state
    val aiSettings = state.settings
    val saved = session.aiConfigurationRepository.current()
    val exit = rememberSettingsExit(section == "configuration" &&
        (aiSettings != saved || state.temperatureText != saved.temperature.toString())) { nav.popBackStack() }
    LaunchedEffect(state.feedback) {
        state.feedback?.let {
            session.showToast(it.removePrefix("✓ ").trim())
            settingsViewModel.consumeFeedback()
        }
    }
    var selectedSummary by remember { mutableStateOf<AiSummaryRecord?>(null) }
    var favoriteOnly by remember { mutableStateOf(false) }
    var historyLimit by remember { mutableIntStateOf(30) }
    val summaryHistory = state.summaryHistory.filter { !favoriteOnly || it.favorite }
    var presetMenu by remember { mutableStateOf(false) }
    val aiPresets = state.presets

    selectedSummary?.let { record ->
        AlertDialog(
            onDismissRequest = { selectedSummary = null },
            title = { Text(record.title.ifBlank { "帖子 #${record.topicId}" }) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Text(record.summary, style = MaterialTheme.typography.bodyMedium)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    selectedSummary = null
                    nav.navigate("topic/${record.topicId}?p=1&floor=0")
                }) { Text("打开原帖") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        settingsViewModel.removeSummary(record)
                        selectedSummary = null
                    }) { Text("删除") }
                    TextButton(onClick = {
                        settingsViewModel.setSummaryFavorite(record, !record.favorite)
                        selectedSummary = record.copy(favorite = !record.favorite)
                    }) { Text(if (record.favorite) "取消收藏" else "收藏总结") }
                    TextButton(onClick = { selectedSummary = null }) { Text("关闭") }
                }
            },
        )
    }

    SettingsPage(
        when (section) { "history" -> "总结历史"; "configuration" -> "接口与配置方案"; else -> "总结行为" },
        nav,
        onBack = exit,
    ) {


            if (section == "configuration") GroupCard("配置方案") {
                Column(
                    Modifier.fillMaxWidth().padding(sb.linux.client.ui.theme.LocalAppSpacing.current.pageHorizontal),
                    verticalArrangement = Arrangement.spacedBy(sb.linux.client.ui.theme.LocalAppSpacing.current.contentGap),
                ) {
                    SettingsTextField(
                        value = state.presetName, onValueChange = settingsViewModel::setPresetName,
                        label = { Text("方案名称") }, singleLine = true,
                        shape = MaterialTheme.shapes.large,
                        trailingIcon = {
                            Box {
                                IconButton(onClick = { presetMenu = true }) { Icon(Icons.Filled.ArrowDropDown, "选择已保存方案") }
                                SettingsChoiceMenu(
                                    expanded = presetMenu, onDismiss = { presetMenu = false },
                                    options = aiPresets.map { it.name to it.name }, selectedValue = state.presetName,
                                    trailingContent = { name ->
                                        IconButton(onClick = { settingsViewModel.removePreset(name) }) {
                                            Icon(Icons.Filled.Delete, "删除方案")
                                        }
                                    },
                                    onSelect = { name ->
                                        settingsViewModel.applyPreset(name)
                                        presetMenu = false
                                        session.showToast("已切换到「$name」")
                                    },
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    FilledTonalButton(
                        onClick = settingsViewModel::savePreset,
                        enabled = state.presetName.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("保存当前配置为方案") }
                }

            }

            if (section == "behavior") AiSettingsForm(
                aiEnabled = aiSettings.enabled,
                onAiEnabledChange = {
                    settingsViewModel.updateSettings { settings -> settings.copy(enabled = it) }
                    settingsViewModel.persistToggles()
                },
                aiAutoRun = aiSettings.autoRun,
                onAiAutoRunChange = {
                    settingsViewModel.updateSettings { settings -> settings.copy(autoRun = it) }
                    settingsViewModel.persistToggles()
                },
                aiIncludeComments = aiSettings.includeComments,
                onAiIncludeCommentsChange = {
                    settingsViewModel.updateSettings { settings -> settings.copy(includeComments = it) }
                    settingsViewModel.persistToggles()
                },
                aiUrl = aiSettings.apiUrl,
                onAiUrlChange = { value -> settingsViewModel.updateSettings { it.copy(apiUrl = value) } },
                aiKey = aiSettings.apiKey,
                onAiKeyChange = { value -> settingsViewModel.updateSettings { it.copy(apiKey = value) } },
                aiModel = aiSettings.model,
                onAiModelChange = { value -> settingsViewModel.updateSettings { it.copy(model = value) } },
                modelOptions = state.modelOptions,
                modelMenuOpen = state.modelMenuOpen,
                onModelMenuOpenChange = settingsViewModel::setModelMenuOpen,
                modelBusy = state.modelBusy,
                onFetchModels = settingsViewModel::fetchModels,
                aiTemp = state.temperatureText,
                onAiTempChange = {
                    if (it.length <= 4 && (it.isEmpty() || it.matches(Regex("""\d*\.?\d*""")))) {
                        settingsViewModel.setTemperatureText(it)
                    }
                },
                aiPrompt = aiSettings.prompt,
                onAiPromptChange = { value -> settingsViewModel.updateSettings { it.copy(prompt = value) } },
                onSave = settingsViewModel::saveSettings,
                // 总结行为即时写入；接口配置在独立详情中显式保存。
                configuration = false,
            )
            if (section == "configuration") AiSettingsForm(
                aiEnabled = aiSettings.enabled,
                onAiEnabledChange = {},
                aiAutoRun = aiSettings.autoRun,
                onAiAutoRunChange = {},
                aiIncludeComments = aiSettings.includeComments,
                onAiIncludeCommentsChange = {},
                aiUrl = aiSettings.apiUrl,
                onAiUrlChange = { value -> settingsViewModel.updateSettings { it.copy(apiUrl = value) } },
                aiKey = aiSettings.apiKey,
                onAiKeyChange = { value -> settingsViewModel.updateSettings { it.copy(apiKey = value) } },
                aiModel = aiSettings.model,
                onAiModelChange = { value -> settingsViewModel.updateSettings { it.copy(model = value) } },
                modelOptions = state.modelOptions,
                modelMenuOpen = state.modelMenuOpen,
                onModelMenuOpenChange = settingsViewModel::setModelMenuOpen,
                modelBusy = state.modelBusy,
                onFetchModels = settingsViewModel::fetchModels,
                aiTemp = state.temperatureText,
                onAiTempChange = {
                    if (it.length <= 4 && (it.isEmpty() || it.matches(Regex("""\d*\.?\d*""")))) {
                        settingsViewModel.setTemperatureText(it)
                    }
                },
                aiPrompt = aiSettings.prompt,
                onAiPromptChange = { value -> settingsViewModel.updateSettings { it.copy(prompt = value) } },
                onSave = settingsViewModel::saveSettings,
                configuration = true,
            )
            if (section == "history") {
            SwitchRow("只显示已收藏的总结", "开启后，历史列表仅显示手动收藏的总结；关闭可查看全部记录", checked = favoriteOnly, onCheckedChange = { favoriteOnly = it })
            if (summaryHistory.isEmpty()) Text(
                "暂无总结记录",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 4.dp),
            )
            if (summaryHistory.isNotEmpty()) {
                GroupLabel("总结历史")
                GroupCard {
                    summaryHistory.take(historyLimit).forEach { record ->
                        ListItem(
                            headlineContent = { Text(record.title.ifBlank { "帖子 #${record.topicId}" }, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            supportingContent = { Text(record.summary.replace("\n", " "), maxLines = 2, overflow = TextOverflow.Ellipsis) },
                            trailingContent = {
                                IconButton(onClick = {
                                    settingsViewModel.removeSummary(record)
                                }) { Icon(Icons.Filled.Delete, "删除") }
                            },
                            modifier = Modifier.clickable { selectedSummary = record },
                        )
                    }
                }
                if (summaryHistory.size > historyLimit) TextButton(onClick = { historyLimit += 30 }) { Text("显示更多总结") }
            }
            }
            Spacer(Modifier.height(12.dp))
    }
}
