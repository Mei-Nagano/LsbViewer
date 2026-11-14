package sb.linux.client.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import sb.linux.client.domain.media.ImageUploadMethod
import sb.linux.client.ui.feature.media.ImageUploadViewModel
import sb.linux.client.ui.feature.media.ImageUploadViewModelFactory
import sb.linux.client.ui.session.Session
import sb.linux.client.ui.theme.LocalAppSpacing

internal data class EditorImageUploadAction(val uploading: Boolean, val choose: () -> Unit)

/** 两个编辑器共用同一选择规则，选图即上传，只有显式勾选才记住选择。 */
@Composable
internal fun rememberEditorImageUpload(session: Session, onUploaded: (String) -> Unit): EditorImageUploadAction {
    val repository = session.imageHostSettingsRepository
    val identity = session.accountIdentityVersion
    val insert by rememberUpdatedState(onUploaded)
    val factory = remember(session.imageUploadRepository, repository) {
        ImageUploadViewModelFactory(session.imageUploadRepository, repository)
    }
    val vm: ImageUploadViewModel = viewModel(key = "editor-image-upload:$identity", factory = factory)
    var uploading by remember(identity) { mutableStateOf(false) }
    var ask by remember(identity) { mutableStateOf(false) }
    var method by remember(identity) { mutableStateOf(repository.current().uploadMethod) }
    var rememberChoice by remember(identity) { mutableStateOf(false) }
    var pickerIdentity by remember { mutableLongStateOf(-1L) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            if (pickerIdentity != session.accountIdentityVersion) {
                session.showToast("账号已变化，请重新选择图片")
                return@rememberLauncherForActivityResult
            }
            val ownerVersion = session.accountIdentityVersion
            uploading = true
            vm.upload(uri.toString(), method) { result ->
                if (ownerVersion == session.accountIdentityVersion) {
                    result.onSuccess { insert(it); session.showToast("图片上传成功") }
                        .onFailure { session.showToast(it.message ?: "图片上传失败") }
                }
                uploading = false
            }
        }
    }
    fun chooseFile(selected: ImageUploadMethod) {
        if (selected == ImageUploadMethod.ATTACHMENT_SPACE && !session.account.loginState.loggedIn) {
            session.showToast("登录后才能上传到附件空间")
        } else if (!vm.hasUsableHost(selected)) {
            session.showToast("请先在设置中配置自定义上传接口")
        } else {
            method = selected
            pickerIdentity = session.accountIdentityVersion
            picker.launch("image/*")
        }
    }
    if (ask) {
        val spacing = LocalAppSpacing.current
        AlertDialog(
            onDismissRequest = { ask = false }, title = { Text("图片上传方式") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.item)) {
                    ImageUploadMethod.entries.forEach { option ->
                        Row(Modifier.fillMaxWidth().clickable { method = option },
                            verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = method == option, onClick = null)
                            Text(option.label, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                    Row(Modifier.fillMaxWidth().clickable { rememberChoice = !rememberChoice },
                        verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = rememberChoice, onCheckedChange = null)
                        Text("设为默认，不再询问", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            },
            confirmButton = { TextButton(onClick = {
                ask = false
                if (rememberChoice) repository.saveUploadPreference(method, false)
                chooseFile(method)
            }) { Text("选择图片") } },
            dismissButton = { TextButton(onClick = { ask = false }) { Text("取消") } },
        )
    }
    return EditorImageUploadAction(uploading) {
        if (!uploading) {
            val settings = repository.current()
            method = settings.uploadMethod
            rememberChoice = false
            if (settings.askUploadMethod) ask = true else chooseFile(method)
        }
    }
}
