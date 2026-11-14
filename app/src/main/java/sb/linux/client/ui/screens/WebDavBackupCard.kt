package sb.linux.client.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import sb.linux.client.ui.patterns.settings.SettingsActionRow

@Composable
internal fun WebDavBackupCard(
    url: String, onUrlChange: (String) -> Unit,
    username: String, onUsernameChange: (String) -> Unit,
    password: String, onPasswordChange: (String) -> Unit,
    passwordVisible: Boolean, onPasswordVisibleChange: (Boolean) -> Unit,
    directory: String, onDirectoryChange: (String) -> Unit,
    busy: Boolean, onBackup: () -> Unit, onRestore: () -> Unit,
) {
    GroupCard("WebDAV 云备份") {
        Text("将勾选内容备份到坚果云、Nextcloud 等 WebDAV 网盘。",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        SettingsTextField(url, onUrlChange, label = { Text("服务器地址") }, searchLabel = "服务器地址",
            description = "WebDAV 服务的完整地址", placeholder = { Text("https://dav.example.com/dav") }, singleLine = true)
        SettingsTextField(username, onUsernameChange, label = { Text("账号") }, searchLabel = "账号", description = "网盘的登录用户名", singleLine = true)
        SettingsTextField(password, onPasswordChange, label = { Text("密码 / 应用密码") }, searchLabel = "密码 / 应用密码", singleLine = true,
            description = "网盘密码或专用应用密码，输入时隐藏内容",
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { onPasswordVisibleChange(!passwordVisible) }) {
                    Icon(Icons.Filled.Visibility, if (passwordVisible) "隐藏密码" else "显示密码", Modifier.size(18.dp))
                }
            })
        SettingsTextField(directory, onDirectoryChange, label = { Text("备份目录") }, searchLabel = "备份目录",
            description = "备份文件在网盘中的保存路径", placeholder = { Text("/linuxsb/") }, singleLine = true)
        SettingsActionRow("备份到 WebDAV", onBackup, enabled = !busy, subtitle = if (busy) "处理中…" else "上传已勾选的内容")
        SettingsActionRow("从 WebDAV 恢复", onRestore, enabled = !busy, subtitle = "恢复备份文件中存在的项目")
    }
}
