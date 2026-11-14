package sb.linux.client.ui.screens

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
            "帖子顶部显示「AI 总结本帖」按钮，点按后才请求；关闭则帖子内不显示 AI 入口",
            checked = aiEnabled,
            onCheckedChange = onAiEnabledChange,
        )
        SwitchRow(
            "打开帖子自动总结",
            "无需点按钮，进帖即请求总结。水贴也会照总结并消耗额度，建议保持关闭",
            checked = aiAutoRun,
            enabled = aiEnabled,
            onCheckedChange = onAiAutoRunChange,
        )
        SwitchRow(
            "解析评论区",
            "开启后连同所有回复一起总结；关闭则仅总结楼主正文",
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
          Column(Modifier.fillMaxWidth().padding(spacing.pageHorizontal), verticalArrangement = Arrangement.spacedBy(spacing.contentGap)) {
            SettingsTextField(
                value = aiUrl,
                onValueChange = onAiUrlChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("API 地址") },
                placeholder = { Text("https://api.openai.com/v1") },
                shape = MaterialTheme.shapes.large,
                singleLine = true,
            )
            SettingsTextField(
                value = aiKey,
                onValueChange = onAiKeyChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("API 密钥") },
                placeholder = { Text("sk-…") },
                shape = MaterialTheme.shapes.large,
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
            )
          }
        }
        GroupCard("模型与生成") {
          Column(Modifier.fillMaxWidth().padding(spacing.pageHorizontal), verticalArrangement = Arrangement.spacedBy(spacing.contentGap)) {
            Box(Modifier.fillMaxWidth()) {
                SettingsTextField(
                    value = aiModel,
                    onValueChange = onAiModelChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("模型") },
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
            }
            FilledTonalButton(
                onClick = onFetchModels,
                enabled = !modelBusy && aiUrl.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) { Text(if (modelBusy) "获取中…" else "获取模型列表") }
            SettingsTextField(
                value = aiTemp,
                onValueChange = onAiTempChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("温度 (0 - 2)") },
                shape = MaterialTheme.shapes.large,
                singleLine = true,
            )
          }
        }
        GroupCard("总结提示词") {
          Column(Modifier.fillMaxWidth().padding(spacing.pageHorizontal)) {
            SettingsTextField(
                value = aiPrompt,
                onValueChange = onAiPromptChange,
                modifier = Modifier.fillMaxWidth().heightIn(min = 140.dp),
                label = { Text("自定义总结提示词") },
                placeholder = { Text(AiAssistantDefaults.SUMMARY_PROMPT) },
                supportingText = { Text("留空使用内置提示词") },
                shape = MaterialTheme.shapes.large,
            )
          }
        }
        Button(
            onClick = onSave,
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.fillMaxWidth(),
        ) { Text("保存 AI 设置") }
    }
}
