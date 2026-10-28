package sb.linux.client.ui.components.feedback

import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import sb.linux.client.ui.theme.LocalAppSizes

/** Callers own visibility and side effects; this component only renders the confirmation. */
@Composable
fun AppConfirmDialog(
    title: String, message: String, onDismiss: () -> Unit, onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    confirmText: String = "确认", dismissText: String = "取消", enabled: Boolean = true, destructive: Boolean = false,
) {
    AlertDialog(
        // 对话框最大宽度 560dp，长内容由 AlertDialog 自身滚动；不把整页表单挤进确认框。
        modifier = modifier.widthIn(max = LocalAppSizes.current.dialogMaxWidth),
        onDismissRequest = { if (enabled) onDismiss() },
        title = { Text(title) }, text = { Text(message) },
        confirmButton = { TextButton(onClick = onConfirm, enabled = enabled) { Text(confirmText, color = if (destructive) MaterialTheme.colorScheme.error else Color.Unspecified) } },
        dismissButton = { TextButton(onClick = onDismiss, enabled = enabled) { Text(dismissText) } },
    )
}
