package sb.linux.client.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import sb.linux.client.ui.theme.LocalAppSpacing

@Composable
internal fun TopicForumMoveAction() {
    val context = LocalTopicSourceActions.current ?: return
    val page = context.page
    val session = context.session
    val move = page.forumMove ?: return
    if (!session.account.loginState.loggedIn) return
    val owner = session.accountIdentityVersion
    var expanded by remember(page, owner) { mutableStateOf(false) }
    var busy by remember(page, owner) { mutableStateOf(false) }
    val spacing = LocalAppSpacing.current
    PostAction(icon = Icons.AutoMirrored.Filled.DriveFileMove, label = "移动板块", onClick = {
        if (move.form == null) context.onOpenSource(sb.linux.client.common.network.Endpoints.abs(
            "/topic/${page.topicId}#${android.net.Uri.encode(move.anchorId)}"))
        else expanded = true
    })
    if (expanded) AlertDialog(
        onDismissRequest = { expanded = false },
        title = { Text("移动板块") },
        text = {
            CompositionLocalProvider(LocalAccountVerificationGateway provides session.accountVerificationGateway) {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(spacing.item)) {
                if (move.note.isNotBlank()) Text(move.note, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                move.form?.let { form -> NativeAccountForm(form, session.accountManagementRepository,
                    enabled = !busy, onDirty = {}, embedded = true, onSubmit = { target, pairs ->
                        if (busy || session.accountIdentityVersion != owner) return@NativeAccountForm
                        busy = true
                        context.interactions.moveForum(page, target, pairs) { result ->
                            if (session.accountIdentityVersion != owner) return@moveForum
                            busy = false
                            result.onSuccess { session.showToast(it.message); if (it.succeeded) { expanded = false; context.onRefresh() } }
                                .onFailure { session.showToast(it.message ?: "移动失败") }
                        }
                    }) }
            }
            }
        },
        confirmButton = { TextButton(onClick = { expanded = false }) { Text("关闭") } },
    )
}
