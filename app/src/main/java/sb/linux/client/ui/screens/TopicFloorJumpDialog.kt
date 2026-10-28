package sb.linux.client.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.*
import androidx.compose.foundation.text.selection.*
import androidx.compose.material.icons.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.*
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.*
import sb.linux.client.ui.*
import sb.linux.client.ui.theme.*
import sb.linux.client.ui.content.*
import sb.linux.client.ui.patterns.topic.*
import sb.linux.client.ui.patterns.user.*
import sb.linux.client.ui.components.feedback.*
import sb.linux.client.ui.components.layout.*
import sb.linux.client.ui.components.navigation.*

@Composable
fun FloorJumpDialog(maxFloor: Int, onDismiss: () -> Unit, onJump: (Int) -> Unit, initial: Int = 0) {
    var text by remember { mutableStateOf(if (initial > 0) initial.toString() else "") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("跳转楼层") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { if (it.length <= 5 && it.all { c -> c.isDigit() }) text = it },
                    label = { Text("楼层号（1 - ${maxFloor.coerceAtLeast(1)}）") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { text.toIntOrNull()?.let { if (it >= 1) onJump(it) } },
                enabled = (text.toIntOrNull() ?: 0) >= 1
            ) { Text("跳转") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}
