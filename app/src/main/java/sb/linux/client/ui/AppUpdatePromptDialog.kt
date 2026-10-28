package sb.linux.client.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * 「发现新版本」提示：版本信息、发布说明摘要与「前往下载 / 取消」两个动作。
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
) {
    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        title = { Text("发现新版本") },
        text = {
            Column {
                Text("$releaseName（当前 v$currentVersion）")
                if (notes.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        notes.take(300),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onDownload) { Text("前往下载") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}
