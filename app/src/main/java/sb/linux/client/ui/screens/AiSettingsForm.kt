package sb.linux.client.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import sb.linux.client.domain.ai.AiAssistantDefaults

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
    GroupCard {
        if (!configuration) {
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
        if (configuration) Column(
            Modifier.padding(horizontal = 18.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            OutlinedTextField(
                value = aiUrl,
                onValueChange = onAiUrlChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("API 地址") },
                placeholder = { Text("https://api.openai.com/v1") },
                shape = RoundedCornerShape(16.dp),
                singleLine = true,
            )
            OutlinedTextField(
                value = aiKey,
                onValueChange = onAiKeyChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("API 密钥") },
                placeholder = { Text("sk-…") },
                shape = RoundedCornerShape(16.dp),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
            )
            Box(Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = aiModel,
                    onValueChange = onAiModelChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("模型") },
                    placeholder = { Text("gpt-4o-mini") },
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                    trailingIcon = {
                        if (modelOptions.isNotEmpty()) {
                            IconButton(onClick = { onModelMenuOpenChange(true) }) {
                                Icon(Icons.Filled.ArrowDropDown, "选择模型")
                            }
                        }
                    },
                )
                DropdownMenu(expanded = modelMenuOpen, onDismissRequest = { onModelMenuOpenChange(false) }) {
                    modelOptions.forEach { model ->
                        DropdownMenuItem(
                            text = { Text(model) },
                            onClick = { onAiModelChange(model); onModelMenuOpenChange(false) },
                        )
                    }
                }
            }
            OutlinedButton(
                onClick = onFetchModels,
                enabled = !modelBusy && aiUrl.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) { Text(if (modelBusy) "获取中…" else "获取模型列表") }
            OutlinedTextField(
                value = aiTemp,
                onValueChange = onAiTempChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("温度 (0 - 2)") },
                shape = RoundedCornerShape(16.dp),
                singleLine = true,
            )
            OutlinedTextField(
                value = aiPrompt,
                onValueChange = onAiPromptChange,
                modifier = Modifier.fillMaxWidth().heightIn(min = 140.dp),
                label = { Text("自定义总结提示词") },
                placeholder = { Text(AiAssistantDefaults.SUMMARY_PROMPT) },
                supportingText = { Text("留空使用内置提示词") },
                shape = RoundedCornerShape(16.dp),
            )
        }
        if (configuration) Button(
            onClick = onSave,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp),
        ) { Text("保存 AI 设置") }
    }
}
