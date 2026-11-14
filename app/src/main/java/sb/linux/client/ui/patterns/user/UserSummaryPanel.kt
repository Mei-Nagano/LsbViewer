package sb.linux.client.ui.patterns.user

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import sb.linux.client.domain.account.UserSummaryRepository
import sb.linux.client.domain.account.UserSummaryStat
import sb.linux.client.ui.components.feedback.AppInlineLoading
import sb.linux.client.ui.theme.LocalAppSpacing
import java.math.RoundingMode

// 身份、简介、两列统计和底部三项操作依次分组；沿用主题表面、圆角和命名间距。
@Composable
internal fun UserSummaryContent(userId: Long, repository: UserSummaryRepository,
    close: () -> Unit, open: (String) -> Unit) {
    val scope = rememberCoroutineScope()
    val accountVersion = LocalAvatarAccountVersion.current
    val onFollowChanged = LocalUserFollowChanged.current
    val spacing = LocalAppSpacing.current
    var user by remember(userId, accountVersion) { mutableStateOf<sb.linux.client.domain.account.UserSummary?>(null) }
    var error by remember(userId, accountVersion) { mutableStateOf<String?>(null) }
    var following by remember(userId, accountVersion) { mutableStateOf(false) }
    var retry by remember(userId, accountVersion) { mutableIntStateOf(0) }
    LaunchedEffect(userId, repository, accountVersion, retry) {
        user = null
        error = null
        try { user = repository.load(userId) }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (failure: Exception) { error = failure.message ?: "加载失败" }
    }
    Column(Modifier.width(300.dp).padding(horizontal = spacing.pageHorizontal, vertical = spacing.item),
        verticalArrangement = Arrangement.spacedBy(spacing.contentGap)) {
        val current = user
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(spacing.item)) {
            if (current != null) Avatar(current.avatar, 40, shape = MaterialTheme.shapes.small)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(spacing.inline)) {
                Text(current?.username ?: "个人信息", style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (!current?.subtitle.isNullOrBlank()) Text(current!!.subtitle,
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = close, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Close, "关闭用户面板")
            }
        }
        if (current == null) {
            if (error == null) AppInlineLoading(size = 24.dp)
            else {
                Text(error.orEmpty(), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error)
                Row {
                    TextButton(onClick = { retry++ }) { Text("重试") }
                    TextButton(onClick = { open("/user/$userId") }) { Text("主页") }
                }
            }
        } else {
            if (current.bio.isNotBlank()) Surface(color = MaterialTheme.colorScheme.surfaceContainerLowest,
                shape = MaterialTheme.shapes.small, modifier = Modifier.fillMaxWidth()) {
                Box(Modifier.padding(spacing.contentGap)) { SummaryBio(current.bio, open) }
            }
            if (current.stats.isNotEmpty()) Surface(color = MaterialTheme.colorScheme.surfaceContainerLowest,
                shape = MaterialTheme.shapes.small, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(spacing.contentGap),
                    verticalArrangement = Arrangement.spacedBy(spacing.item)) {
                    current.stats.chunked(2).forEach { pair ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(spacing.item)) {
                            pair.forEach { stat -> SummaryStat(stat, Modifier.weight(1f), open) }
                            if (pair.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
            error?.let { Text(it, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error) }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(spacing.item)) {
                FilledTonalButton(
                    onClick = {
                        following = true
                        error = null
                        scope.launch {
                            try { user = repository.toggleFollow(userId); onFollowChanged() }
                            catch (cancelled: CancellationException) { throw cancelled }
                            catch (failure: Exception) { error = failure.message ?: "关注操作失败" }
                            finally { following = false }
                        }
                    }, enabled = current.canFollow && !current.self && !following,
                    shape = MaterialTheme.shapes.small, modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = spacing.inline, vertical = spacing.contentGap),
                ) { Text(if (following) "处理中" else if (current.following) "已关注" else "关注") }
                FilledTonalButton(onClick = { open(current.notify) },
                    enabled = current.canNotify && current.notify.isNotBlank(),
                    shape = MaterialTheme.shapes.small, modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = spacing.inline, vertical = spacing.contentGap),
                ) { Text("私信") }
                FilledTonalButton(onClick = { open(current.home.ifBlank { "/user/$userId" }) },
                    shape = MaterialTheme.shapes.small, modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = spacing.inline, vertical = spacing.contentGap),
                ) { Text("主页") }
            }
        }
    }
}

@Composable
private fun SummaryStat(stat: UserSummaryStat, modifier: Modifier, open: (String) -> Unit) {
    val strongColor = MaterialTheme.colorScheme.onSurface
    Row(modifier.clickable(enabled = stat.url.isNotBlank()) { open(stat.url) },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(LocalAppSpacing.current.inline)) {
        val icon = when (stat.label) {
            "积分", "烧饼" -> Icons.Default.Toll
            "粉丝" -> Icons.Default.PeopleOutline
            "关注" -> Icons.Default.PersonAdd
            "主题" -> Icons.Default.Article
            "等级" -> Icons.Default.Shield
            else -> Icons.AutoMirrored.Filled.Chat
        }
        Icon(icon, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(buildAnnotatedString {
            append("${stat.label} ")
            pushStyle(SpanStyle(fontWeight = FontWeight.Bold, color = strongColor))
            append(compactSummaryCount(stat.value)); pop()
        }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

private fun compactSummaryCount(raw: String): String {
    val number = raw.replace(",", "").toBigDecimalOrNull() ?: return raw.ifBlank { "—" }
    val scale = listOf(1_000_000_000L to "B", 1_000_000L to "M", 1_000L to "K")
        .firstOrNull { number.abs() >= it.first.toBigDecimal() } ?: return raw
    return number.divide(scale.first.toBigDecimal(), 1, RoundingMode.DOWN).stripTrailingZeros().toPlainString() + scale.second
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
