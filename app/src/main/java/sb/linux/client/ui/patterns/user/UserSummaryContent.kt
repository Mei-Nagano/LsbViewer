package sb.linux.client.ui.patterns.user

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Toll
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import sb.linux.client.domain.account.UserSummary
import sb.linux.client.domain.account.UserSummaryRepository

@Composable
internal fun UserSummaryContent(
    userId: Long,
    repository: UserSummaryRepository,
    close: () -> Unit,
    open: (String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val onBlocked = LocalUserBlockChanged.current
    var blocking by remember(userId) { mutableStateOf(false) }
    var blockError by remember(userId) { mutableStateOf<String?>(null) }
    var user by remember(userId) { mutableStateOf<UserSummary?>(null) }
    var error by remember(userId) { mutableStateOf<String?>(null) }
    var retry by remember(userId) { mutableIntStateOf(0) }
    LaunchedEffect(userId, repository, retry) {
        user = null
        error = null
        try { user = repository.load(userId) }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (failure: Exception) { error = failure.message ?: "加载失败" }
    }
    Column(Modifier.width(240.dp).padding(horizontal = 12.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val current = user
        if (current == null) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("个人信息", Modifier.weight(1f))
                TextButton(onClick = close) { Text("×") }
            }
            if (error == null) CircularProgressIndicator(Modifier.size(24.dp))
            else {
                Text(error.orEmpty(), style = MaterialTheme.typography.bodySmall)
                Row {
                    TextButton(onClick = { retry++ }) { Text("重试") }
                    TextButton(onClick = { open("/user/$userId") }) { Text("主页") }
                }
            }
        } else {
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Avatar(current.avatar, 36)
                Column(Modifier.weight(1f)) {
                    Text(current.username, fontSize = 15.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { open(current.home.ifBlank { "/user/$userId" }) })
                    if (current.subtitle.isNotBlank()) Text(current.subtitle, fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1,
                        overflow = TextOverflow.Ellipsis)
                }
                Text("×", color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.clickable(onClick = close).padding(4.dp))
            }
            if (current.bio.isNotBlank()) SummaryBio(current.bio, open)
            if (current.stats.isNotEmpty()) Surface(
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp),
            ) {
                Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    current.stats.chunked(2).forEach { pair ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            pair.forEach { stat ->
                                Row(Modifier.weight(1f).clickable(enabled = stat.url.isNotBlank()) { open(stat.url) },
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                    val icon = when (stat.label) {
                                        "等级" -> Icons.Filled.Shield
                                        "主题" -> Icons.Filled.Article
                                        "积分" -> Icons.Filled.Toll
                                        else -> Icons.AutoMirrored.Filled.Chat
                                    }
                                    Icon(icon, null, Modifier.size(13.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(buildAnnotatedString {
                                        append("${stat.label} ")
                                        pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                                        append(stat.value); pop()
                                    }, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f))
                                }
                            }
                            if (pair.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
            blockError?.let { Text(it, fontSize = 11.sp, color = MaterialTheme.colorScheme.error) }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                if (current.canBlock) SummaryAction(
                    if (blocking) "处理中" else if (current.blocked) "已屏蔽" else "屏蔽",
                    !current.self && !blocking,
                ) {
                    blocking = true
                    blockError = null
                    scope.launch {
                        try { user = repository.toggleBlock(userId); onBlocked() }
                        catch (cancelled: CancellationException) { throw cancelled }
                        catch (failure: Exception) { blockError = failure.message ?: "屏蔽失败" }
                        finally { blocking = false }
                    }
                }
                if (current.canFollow) SummaryAction("关注", false) {}
                if (current.canNotify) SummaryAction("私信", current.notify.isNotBlank()) { open(current.notify) }
                if (current.home.isNotBlank()) SummaryAction("主页", true) { open(current.home) }
            }
        }
    }
}

@Composable
private fun SummaryAction(label: String, enabled: Boolean, action: () -> Unit) {
    Text(label, fontSize = 12.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (enabled) 1f else 0.38f),
        modifier = Modifier.clickable(enabled = enabled, onClick = action).padding(vertical = 8.dp, horizontal = 3.dp))
}

@Composable
private fun SummaryBio(text: String, open: (String) -> Unit) {
    val color = MaterialTheme.colorScheme.primary
    val annotated = remember(text, color) {
        buildAnnotatedString {
            var previous = 0
            Regex("""https?://[^\s<>"']+""").findAll(text).forEach { match ->
                append(text.substring(previous, match.range.first))
                val start = length
                append(match.value)
                addStringAnnotation("url", match.value, start, length)
                addStyle(SpanStyle(color = color), start, length)
                previous = match.range.last + 1
            }
            append(text.substring(previous))
        }
    }
    ClickableText(annotated, maxLines = 3, overflow = TextOverflow.Ellipsis,
        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
        onClick = { offset -> annotated.getStringAnnotations("url", offset, offset).firstOrNull()?.let { open(it.item) } })
}
