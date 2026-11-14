package sb.linux.client.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import sb.linux.client.platform.webview.WebJsDialog

@Composable
internal fun SourceJsDialog(dialog: WebJsDialog, onFinish: (Boolean, String) -> Unit) {
    var value by remember(dialog) { mutableStateOf(dialog.defaultValue) }
    val leaving = dialog.kind == WebJsDialog.Kind.BEFORE_UNLOAD
    val host = android.net.Uri.parse(dialog.url).host.orEmpty()
    AlertDialog(
        onDismissRequest = { onFinish(false, value) },
        title = { Text(if (leaving) "离开此网页？" else host.ifBlank { "网页提示" }) },
        text = {
            sb.linux.client.ui.components.navigation.ApplyDialogStatusBarPreference()
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(dialog.message, style = MaterialTheme.typography.bodyMedium)
                if (dialog.kind == WebJsDialog.Kind.PROMPT) OutlinedTextField(
                    value, { value = it }, modifier = Modifier.fillMaxWidth(), label = { Text("输入内容") })
            }
        },
        confirmButton = { TextButton(onClick = { onFinish(true, value) }) { Text(if (leaving) "离开" else "确定") } },
        dismissButton = {
            if (dialog.kind != WebJsDialog.Kind.ALERT) TextButton(onClick = { onFinish(false, value) }) {
                Text(if (leaving) "留在此页" else "取消")
            }
        },
    )
}
