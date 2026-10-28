package sb.linux.client.ui.screens
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import sb.linux.client.ui.session.Session

import androidx.activity.compose.BackHandler
@Composable
internal fun rememberSettingsExit(dirty: Boolean, onExit: () -> Unit): () -> Unit {
    var confirm by remember { mutableStateOf(false) }
    val exit = { if (dirty) confirm = true else onExit() }
    BackHandler(enabled = dirty) { confirm = true }
    if (confirm) sb.linux.client.ui.components.feedback.AppConfirmDialog(
        title = "有未保存的修改",
        message = "离开后将丢弃未保存的输入。",
        onDismiss = { confirm = false },
        onConfirm = { confirm = false; onExit() },
        confirmText = "放弃并离开", dismissText = "继续编辑",
    )
    return exit
}
