package sb.linux.client.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import sb.linux.client.ui.feature.media.ImageHostSettingsViewModel
import sb.linux.client.ui.feature.media.ImageHostSettingsViewModelFactory
import sb.linux.client.ui.session.Session
import sb.linux.client.ui.theme.AppMenuDefaults

@Composable
internal fun ImageHostSettingsCard(session: Session, resetTick: Int) {
    val factory = remember(session.imageHostSettingsRepository) {
        ImageHostSettingsViewModelFactory(session.imageHostSettingsRepository)
    }
    val viewModel: ImageHostSettingsViewModel = viewModel(factory = factory)
    val state = viewModel.state
    LaunchedEffect(resetTick) { viewModel.refresh() }
    LaunchedEffect(state.feedback) {
        state.feedback?.let {
            session.showToast(it)
            viewModel.consumeFeedback()
        }
    }
    val useBuiltIn = state.settings.useBuiltIn

    GroupCard {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = AppMenuDefaults.contentHorizontal, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("内置图床", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(
                if (useBuiltIn)
                    "内置 Pixhost 和 Catbox，免登录、无需 API Key。上传内容将公开托管于所选第三方服务，请勿上传隐私或敏感内容。"
                else
                    "使用 multipart/form-data 上传到自定义接口，兼容返回 url、data.url、link 或 src 的常见 API。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = useBuiltIn,
                    onClick = { viewModel.setBuiltInEnabled(true) },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    modifier = Modifier.weight(1f),
                ) { Text("内置匿名图床", maxLines = 1) }
                SegmentedButton(
                    selected = !useBuiltIn,
                    onClick = { viewModel.setBuiltInEnabled(false) },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    modifier = Modifier.weight(1f),
                ) { Text("自定义接口", maxLines = 1) }
            }
            if (useBuiltIn) {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    listOf("pixhost" to "Pixhost", "catbox" to "Catbox").forEachIndexed { index, (value, label) ->
                        SegmentedButton(
                            selected = state.settings.provider == value,
                            onClick = { viewModel.setProvider(value) },
                            shape = SegmentedButtonDefaults.itemShape(index, 2),
                        ) { Text(label) }
                    }
                }
                Text(
                    "Pixhost 单张上限 10 MB，Catbox 上限 20 MB；均支持免登录上传，成功后插入原图链接。",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            if (!useBuiltIn) {
            OutlinedTextField(
                value = state.urlDraft,
                onValueChange = viewModel::setUrl,
                label = { Text("上传接口地址") },
                placeholder = { Text("https://example.com/api/upload") },
                singleLine = true,
                enabled = !useBuiltIn,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.tokenDraft,
                onValueChange = viewModel::setToken,
                label = { Text("访问令牌（可选）") },
                singleLine = true,
                enabled = !useBuiltIn,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.fileFieldDraft,
                onValueChange = viewModel::setFileField,
                label = { Text("文件字段名") },
                placeholder = { Text("file") },
                singleLine = true,
                enabled = !useBuiltIn,
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = viewModel::saveCustom,
                enabled = !useBuiltIn,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("保存自定义图床") }
            }
        }
    }
}
