package sb.linux.client.ui.screens
import sb.linux.client.ui.patterns.settings.SettingsActionRow

import sb.linux.client.ui.patterns.settings.settingsSearchAnchor
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import sb.linux.client.domain.ai.AiAssistantDefaults
import sb.linux.client.ui.theme.AppMenuDefaults
import sb.linux.client.ui.theme.LocalAppSpacing
import sb.linux.client.ui.patterns.settings.SettingsChoiceMenu

@Composable
internal fun AiSettingsForm(
    aiEnabled: Boolean,
    onAiEnabledChange: (Boolean) -> Unit,
    aiAutoRun: Boolean,
    onAiAutoRunChange: (Boolean) -> Unit,
    aiIncludeComments: Boolean,
    onAiIncludeCommentsChange: (Boolean) -> Unit,
    aiUrl: String,
    onAiUrlChange: (String) -> Unit,
    aiKey: String,
    onAiKeyChange: (String) -> Unit,
    aiModel: String,
    onAiModelChange: (String) -> Unit,
    modelOptions: List<String>,
    modelMenuOpen: Boolean,
    onModelMenuOpenChange: (Boolean) -> Unit,
    modelBusy: Boolean,
    onFetchModels: () -> Unit,
    aiTemp: String,
    onAiTempChange: (String) -> Unit,
    aiPrompt: String,
    onAiPromptChange: (String) -> Unit,
    onSave: () -> Unit,
    configuration: Boolean = true,
) {
    if (!configuration) {
        GroupCard {
        SwitchRow(
            "开启 AI 总结",
            "在帖子中显示 AI 总结入口，使用当前配置的服务生成总结",
            checked = aiEnabled,
            onCheckedChange = onAiEnabledChange,
        )
        SwitchRow(
            "打开帖子自动总结",
            "进入帖子后自动请求总结，会使用所配置服务的额度",
            checked = aiAutoRun,
            enabled = aiEnabled,
            onCheckedChange = onAiAutoRunChange,
        )
        SwitchRow(
            "解析评论区",
            "总结时包含回复内容；关闭后仅处理楼主正文",
            checked = aiIncludeComments,
            enabled = aiEnabled,
            onCheckedChange = onAiIncludeCommentsChange,
        )
        }
        return
    }
    val spacing = LocalAppSpacing.current
    Column(verticalArrangement = Arrangement.spacedBy(spacing.section)) {
        GroupCard("接口配置") {
            SettingsTextField(
                value = aiUrl,
                onValueChange = onAiUrlChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("API 地址") }, searchLabel = "API 地址",
                description = "OpenAI 兼容服务的接口根地址",
                placeholder = { Text("https://api.openai.com/v1") },
                shape = MaterialTheme.shapes.large,
                singleLine = true,
            )
            SettingsTextField(
                value = aiKey,
                onValueChange = onAiKeyChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("API 密钥") }, searchLabel = "API 密钥",
                description = "服务提供的访问密钥，输入时隐藏内容",
                placeholder = { Text("sk-…") },
                shape = MaterialTheme.shapes.large,
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
            )
        }
        GroupCard("模型与生成") {
            SettingsTextField(
                value = aiModel,
                onValueChange = onAiModelChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("模型") }, searchLabel = "模型",
                description = "输入模型 ID，或从已获取的模型中选择",
                placeholder = { Text("gpt-4o-mini") },
                shape = MaterialTheme.shapes.large,
                singleLine = true,
                trailingIcon = {
                    if (modelOptions.isNotEmpty()) {
                        Box {
                            IconButton(onClick = { onModelMenuOpenChange(true) }) {
                                Icon(Icons.Filled.ArrowDropDown, "选择模型")
                            }
                            SettingsChoiceMenu(
                                expanded = modelMenuOpen, onDismiss = { onModelMenuOpenChange(false) },
                                options = modelOptions.map { it to it }, selectedValue = aiModel,
                                onSelect = { onAiModelChange(it); onModelMenuOpenChange(false) },
                            )
                        }
                    }
                },
            )
            SettingsActionRow("获取模型列表", onFetchModels, enabled = !modelBusy && aiUrl.isNotBlank(),
                subtitle = if (modelBusy) "获取中…" else "从所配置的服务读取可用模型")
            SettingsTextField(
                value = aiTemp,
                onValueChange = onAiTempChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("温度 (0 - 2)") }, searchLabel = "温度 (0 - 2)",
                description = "较低值使总结更稳定，较高值增加变化",
                shape = MaterialTheme.shapes.large,
                singleLine = true,
            )
        }
        GroupCard("总结提示词") {
            SettingsTextField(
                value = aiPrompt,
                onValueChange = onAiPromptChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("自定义总结提示词") }, searchLabel = "自定义总结提示词",
                description = "指定总结要求；留空使用内置提示词",
                placeholder = { Text(AiAssistantDefaults.SUMMARY_PROMPT) },
                supportingText = { Text("留空使用内置提示词") },
                minLines = 4,
                shape = MaterialTheme.shapes.large,
            )
        }
        SettingsActionRow("保存 AI 设置", onSave)
    }
}
