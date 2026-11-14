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
import sb.linux.client.ui.theme.LocalAppSpacing
import sb.linux.client.ui.patterns.settings.SettingsChoiceRow
import sb.linux.client.ui.components.input.AppSwitchRow
import sb.linux.client.domain.media.ImageUploadMethod

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
    val method = state.settings.uploadMethod
    val useBuiltIn = method == ImageUploadMethod.BUILT_IN

    GroupCard {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = AppMenuDefaults.contentHorizontal, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(LocalAppSpacing.current.contentGap),
        ) {
            Text("图片上传", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(
                if (method == ImageUploadMethod.ATTACHMENT_SPACE)
                    "图片上传至当前账号的源站附件空间，保留原图，可在附件管理中查看；大小和可用空间以源站限制为准。"
                else if (useBuiltIn)
                    "内置 Pixhost 和 Catbox，免登录、无需 API Key。上传内容将公开托管于所选第三方服务，请勿上传隐私或敏感内容。"
                else
                    "使用 multipart/form-data 上传到自定义接口，兼容返回 url、data.url、link 或 src 的常见 API。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            SettingsChoiceRow("默认上传方式", ImageUploadMethod.entries.map { it to it.label },
                method, viewModel::setUploadMethod)
            AppSwitchRow("选择图片前询问上传方式", checked = state.settings.askUploadMethod,
                onCheckedChange = viewModel::setAskUploadMethod)
            if (useBuiltIn) {
                SettingsChoiceRow("图床服务", listOf("pixhost" to "Pixhost", "catbox" to "Catbox"),
                    state.settings.provider, viewModel::setProvider)
                Text(
                    "Pixhost 单张上限 10 MB，Catbox 上限 20 MB；均支持免登录上传，成功后插入原图链接。",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            if (method == ImageUploadMethod.CUSTOM) {
            SettingsTextField(
                value = state.urlDraft,
                onValueChange = viewModel::setUrl,
                label = { Text("上传接口地址") },
                placeholder = { Text("https://example.com/api/upload") },
                singleLine = true,
                enabled = !useBuiltIn,
                modifier = Modifier.fillMaxWidth(),
            )
            SettingsTextField(
                value = state.tokenDraft,
                onValueChange = viewModel::setToken,
                label = { Text("访问令牌（可选）") },
                singleLine = true,
                enabled = !useBuiltIn,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
            )
            SettingsTextField(
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
