package sb.linux.client.ui.components.feedback

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import sb.linux.client.ui.theme.AppNoticeDefaults

@Composable
fun AppNotice(
    message: String, modifier: Modifier = Modifier, error: Boolean = true,
    actionLabel: String = "重试", onAction: (() -> Unit)? = null, onDismiss: (() -> Unit)? = null,
) {
    val foreground = if (error) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant
    Surface(modifier.fillMaxWidth(), shape = AppNoticeDefaults.shape,
        color = if (error) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceContainer) {
        Row(Modifier.padding(AppNoticeDefaults.contentPadding),
            verticalAlignment = Alignment.CenterVertically) {
            Text(message, Modifier.weight(1f), color = foreground, style = MaterialTheme.typography.bodySmall)
            if (onAction != null) TextButton(onClick = onAction) { Text(actionLabel) }
            if (onDismiss != null) TextButton(onClick = onDismiss) { Text("关闭") }
        }
    }
}
