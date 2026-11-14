package sb.linux.client.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * 自动与手动检查共用版本、完整发布说明及跳过／停用／下载动作。
 *
 * 关于页与常规设置页需要同一份提示，原来各写了一遍完全相同的 `AlertDialog`；
 * 这里收敛为唯一实现。只接收显示所需的纯文本，不依赖 UpdateInfo 或任何 repository。
 * 可见性与下载动作由调用方拥有（见 UI_DEVELOPMENT_GUIDE 5.2 第 9 条）。
 */
@Composable
fun AppUpdatePromptDialog(
    releaseName: String,
    currentVersion: String,
    notes: String,
    onDownload: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    releaseVersion: String = releaseName,
    onSkipVersion: () -> Unit = onDismiss,
    onNeverPrompt: () -> Unit = onDismiss,
) {
    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        title = { Text("发现新版本") },
        text = {
            sb.linux.client.ui.components.navigation.ApplyDialogStatusBarPreference()
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text("新版本 $releaseVersion", style = MaterialTheme.typography.titleMedium)
                Text("当前版本 v$currentVersion", style = MaterialTheme.typography.bodySmall)
                if (releaseName != releaseVersion) Text(releaseName)
                if (notes.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = onSkipVersion, modifier = Modifier.weight(5f),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp)) {
                    Text("跳过此版本", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                TextButton(onClick = onNeverPrompt, modifier = Modifier.weight(4f),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp)) {
                    Text("不再提示", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                TextButton(onClick = onDownload, modifier = Modifier.weight(3f),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp)) {
                    Text("下载", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        },
    )
}
