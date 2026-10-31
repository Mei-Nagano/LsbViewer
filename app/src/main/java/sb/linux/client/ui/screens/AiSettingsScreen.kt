package sb.linux.client.ui.screens
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiSettingsScreen(session: Session, nav: NavHostController, section: String = "behavior") {
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
        when (section) { "history" -> "AI 总结历史"; "configuration" -> "AI 接口与方案"; else -> "AI 助手" },
        nav,
        onBack = exit,
    ) {


            if (section == "configuration") GroupCard {
                Text(
                    "AI 配置方案",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(start = AppMenuDefaults.contentHorizontal, top = 12.dp, bottom = 4.dp),
                )
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = AppMenuDefaults.contentHorizontal, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = state.presetName, onValueChange = settingsViewModel::setPresetName,
                        label = { Text("方案名称 / 选择方案") }, singleLine = true,
                        trailingIcon = {
                            Box {
                                IconButton(onClick = { presetMenu = true }) { Icon(Icons.Filled.ArrowDropDown, "选择已保存方案") }
                                DropdownMenu(expanded = presetMenu, onDismissRequest = { presetMenu = false }) {
                                    if (aiPresets.isEmpty()) DropdownMenuItem(text = { Text("暂无已保存方案") }, onClick = {}, enabled = false)
                                    aiPresets.forEach { preset ->
                                        DropdownMenuItem(
                                            text = { Text(preset.name) },
                                            trailingIcon = { IconButton(onClick = {
                                                settingsViewModel.removePreset(preset.name)
                                            }) { Icon(Icons.Filled.Delete, "删除方案") } },
                                            onClick = {
                                                settingsViewModel.applyPreset(preset.name)
                                                presetMenu = false
                                                session.showToast("已切换到「${preset.name}」")
                                            },
                                        )
                                    }
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                    )
                    Button(
                        onClick = settingsViewModel::savePreset,
                        enabled = state.presetName.isNotBlank(),
                    ) { Text("保存") }
                }

            }

            if (section == "behavior") SettingMenuRow("接口与配置方案", "API 地址 · 模型 · 提示词 · 预设") { nav.navigate("aiConfiguration") }
            if (section != "history") AiSettingsForm(
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
                configuration = section == "configuration",
            )
            if (section == "history") {
            SwitchRow("仅看收藏的总结", "历史保留每次总结结果，不再覆盖同帖旧记录", checked = favoriteOnly, onCheckedChange = { favoriteOnly = it })
            if (summaryHistory.isEmpty()) Text("暂无总结记录")
            if (summaryHistory.isNotEmpty()) {
                GroupLabel("总结历史")
                GroupCard {
                    summaryHistory.take(historyLimit).forEachIndexed { index, record ->
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
                        if (index < summaryHistory.take(historyLimit).lastIndex) HorizontalDivider(Modifier.padding(horizontal = AppMenuDefaults.contentHorizontal))
                    }
                }
                if (summaryHistory.size > historyLimit) TextButton(onClick = { historyLimit += 30 }) { Text("显示更多总结") }
            }
            }
            Spacer(Modifier.height(12.dp))
    }
}

/** 备份设置子页面：本地 JSON 导入 / 导出 + WebDAV 云备份 */
