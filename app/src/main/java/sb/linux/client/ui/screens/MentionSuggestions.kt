package sb.linux.client.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import sb.linux.client.domain.topic.MentionUser
import sb.linux.client.ui.patterns.user.UserAvatar
import sb.linux.client.ui.session.Session

/** 与源站一致，仅上传光标前的 @ 查询片段，不发送正文。 */
@Composable
internal fun MentionSuggestions(session: Session, url: String, value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit, enabled: Boolean = true) {
    val context = remember(value, enabled, url) {
        if (!enabled || url.isBlank() || value.composition != null || !value.selection.collapsed) null
        else Regex("@([^@\\s]{0,64})$").find(value.text.take(value.selection.start))
    }
    val query = context?.groupValues?.get(1)
    var dismissed by remember { mutableStateOf<String?>(null) }
    var users by remember { mutableStateOf<List<MentionUser>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    val identity = session.accountIdentityVersion
    LaunchedEffect(query, url, identity) {
        users = emptyList(); error = null; loading = false
        if (query == null) return@LaunchedEffect
        loading = true
        delay(250)
        try {
            val result = session.sourceEditorRepository.mentions(url, query)
            if (identity == session.accountIdentityVersion) users = result
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (failure: Exception) { error = failure.message ?: "用户建议暂时不可用" }
        finally { loading = false }
    }
    val signature = context?.let { "${it.range.first}:${value.selection.start}:$query" }
    if (context == null || signature == dismissed) return
    Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shadowElevation = 3.dp, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("提及用户", Modifier.weight(1f), style = MaterialTheme.typography.labelLarge)
                TextButton(onClick = { dismissed = signature }) { Text("关闭") }
            }
            if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
            error?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            if (!loading && error == null && users.isEmpty()) Text("没有匹配的用户", style = MaterialTheme.typography.bodySmall)
            users.take(8).forEach { user ->
                Row(Modifier.fillMaxWidth().clickable {
                    val start = context.range.first
                    val prefix = if (start > 0 && !value.text[start - 1].isWhitespace()) " " else ""
                    val mention = "$prefix@${user.username} "
                    val text = value.text.replaceRange(start, value.selection.start, mention)
                    onValueChange(TextFieldValue(text, TextRange(start + mention.length)))
                }.padding(8.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    UserAvatar(0L, user.avatarUrl, 32, username = user.username)
                    Column { Text(user.username, style = MaterialTheme.typography.bodyMedium)
                        if (user.group.isNotBlank()) Text(user.group, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            }
        }
    }
}
