package sb.linux.client.ui.screens
import sb.linux.client.ui.patterns.settings.SettingsActionRow

import sb.linux.client.ui.patterns.settings.settingsSearchAnchor
import sb.linux.client.ui.patterns.settings.searchingFor
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import sb.linux.client.ui.feature.media.ImageHostSettingsViewModel
import sb.linux.client.ui.feature.media.ImageHostSettingsViewModelFactory
import sb.linux.client.ui.session.Session
import sb.linux.client.ui.theme.AppMenuDefaults
import sb.linux.client.ui.theme.LocalAppSpacing
import sb.linux.client.ui.patterns.settings.SettingsChoiceRow
import sb.linux.client.ui.patterns.settings.settingsRowIcon
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
        SettingsChoiceRow("默认上传方式", ImageUploadMethod.entries.map { it to it.label },
            method, viewModel::setUploadMethod)
        SwitchRow("选择图片前询问上传方式", icon = settingsRowIcon("选择图片前询问上传方式"),
            subtitle = "每次选择图片时，可临时切换附件空间或图床", checked = state.settings.askUploadMethod,
            onCheckedChange = viewModel::setAskUploadMethod)
        if (useBuiltIn || searchingFor("图床服务")) {
            SettingsChoiceRow("图床服务", listOf("pixhost" to "Pixhost", "catbox" to "Catbox"),
                state.settings.provider, viewModel::setProvider)
            Text(
                "Pixhost 单张上限 10 MB，Catbox 上限 20 MB；均支持免登录上传，成功后插入原图链接。",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        if (method == ImageUploadMethod.CUSTOM || searchingFor("上传接口地址", "访问令牌（可选）", "文件字段名", "保存自定义图床")) {
        SettingsTextField(
            value = state.urlDraft,
            onValueChange = viewModel::setUrl,
            label = { Text("上传接口地址") }, searchLabel = "上传接口地址",
            description = "自定义图床接收图片的接口 URL",
            placeholder = { Text("https://example.com/api/upload") },
            singleLine = true,
            enabled = !useBuiltIn,
            modifier = Modifier.fillMaxWidth(),
        )
        SettingsTextField(
            value = state.tokenDraft,
            onValueChange = viewModel::setToken,
            label = { Text("访问令牌（可选）") }, searchLabel = "访问令牌（可选）",
            description = "图床的身份验证令牌，免验证时留空",
            singleLine = true,
            enabled = !useBuiltIn,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
        )
        SettingsTextField(
            value = state.fileFieldDraft,
            onValueChange = viewModel::setFileField,
            label = { Text("文件字段名") }, searchLabel = "文件字段名",
            description = "上传表单中的图片字段，通常为 file",
            placeholder = { Text("file") },
            singleLine = true,
            enabled = !useBuiltIn,
            modifier = Modifier.fillMaxWidth(),
        )
        SettingsActionRow("保存自定义图床", viewModel::saveCustom, enabled = !useBuiltIn)
        }
    }
}
