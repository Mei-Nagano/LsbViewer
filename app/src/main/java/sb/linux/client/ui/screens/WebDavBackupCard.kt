package sb.linux.client.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@Composable
internal fun WebDavBackupCard(
    url: String,
    onUrlChange: (String) -> Unit,
    username: String,
    onUsernameChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    passwordVisible: Boolean,
    onPasswordVisibleChange: (Boolean) -> Unit,
    directory: String,
    onDirectoryChange: (String) -> Unit,
    busy: Boolean,
    onBackup: () -> Unit,
    onRestore: () -> Unit,
) {
    GroupCard {
        Column(Modifier.padding(horizontal = 18.dp, vertical = 10.dp)) {
            Text("WebDAV 云备份", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(
                "把上方已勾选的同一批内容备份到坚果云 / Nextcloud 等 WebDAV 网盘。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp, bottom = 8.dp),
            )
            OutlinedTextField(
                value = url,
                onValueChange = onUrlChange,
                label = { Text("服务器地址") },
                placeholder = { Text("https://dav.example.com/dav") },
                singleLine = true,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = username,
                onValueChange = onUsernameChange,
                label = { Text("账号") },
                singleLine = true,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = password,
                onValueChange = onPasswordChange,
                label = { Text("密码 / 应用密码") },
                singleLine = true,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                visualTransformation = if (passwordVisible) androidx.compose.ui.text.input.VisualTransformation.None
                else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { onPasswordVisibleChange(!passwordVisible) }) {
                        Icon(Icons.Filled.Visibility, if (passwordVisible) "隐藏密码" else "显示密码", Modifier.size(18.dp))
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = directory,
                onValueChange = onDirectoryChange,
                label = { Text("备份目录") },
                placeholder = { Text("/linuxsb/") },
                singleLine = true,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onBackup,
                    enabled = !busy,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    if (busy) {
                        CircularProgressIndicator(
                            Modifier.size(14.dp), strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                        Spacer(Modifier.width(6.dp))
                    }
                    Text("备份到 WebDAV")
                }
                OutlinedButton(
                    onClick = onRestore,
                    enabled = !busy,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f),
                ) { Text("从 WebDAV 恢复") }
            }
        }
    }
}
