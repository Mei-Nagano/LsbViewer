package sb.linux.client.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import sb.linux.client.domain.topic.*
import sb.linux.client.ui.theme.LocalAppSpacing

/** 福利投票仅允许一次，以源站提供的表单和可选状态为准。 */
@Composable
internal fun ResourceVoteCard(
    panel: ResourceVotePanel, loggedIn: Boolean, busy: Boolean,
    onLogin: () -> Unit, onVote: (List<TopicPollOption>) -> Unit,
) {
    val poll = panel.poll
    val spacing = LocalAppSpacing.current
    var selected by remember(poll) { mutableStateOf(poll.options.firstOrNull { it.selected }?.value) }
    val canVote = loggedIn && !busy && !poll.voted && !poll.closed && poll.action.isNotBlank() && poll.unavailableReason.isBlank()
    Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth().padding(horizontal = spacing.pageHorizontal, vertical = spacing.item)) {
        Column(Modifier.padding(spacing.pageHorizontal), verticalArrangement = Arrangement.spacedBy(spacing.contentGap)) {
            Text(poll.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(spacing.inline), verticalArrangement = Arrangement.spacedBy(spacing.inline)) {
                if (poll.participants.isNotBlank()) PollStatusPill(poll.participants)
                if (poll.voted) PollStatusPill("已投票", highlighted = true)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${panel.available} 可用", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Text("${panel.unavailable} 不可用", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            LinearProgressIndicator(progress = { panel.fraction }, modifier = Modifier.fillMaxWidth().height(12.dp), drawStopIndicator = {})
            if (panel.summary.isNotBlank()) Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                sb.linux.client.ui.patterns.topic.TopicTagCapsule(panel.summary)
            }
            if (poll.options.isNotEmpty()) {
                Text("你的实际体验", style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.item)) {
                    poll.options.forEach { option ->
                        Surface(modifier = Modifier.weight(1f), shape = MaterialTheme.shapes.large,
                            color = if (selected == option.value) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh) {
                            Row(Modifier.fillMaxWidth().heightIn(min = 56.dp)
                                .selectable(selected == option.value, enabled = canVote && !option.disabled, role = Role.RadioButton) { selected = option.value },
                                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                                RadioButton(selected == option.value, null, enabled = canVote && !option.disabled)
                                Text(option.label, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
            if (poll.note.isNotBlank()) Text(poll.note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (panel.recent.isNotEmpty()) {
                Text("最近 ${panel.recent.size} 票", style = MaterialTheme.typography.labelLarge)
                Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                    Column(Modifier.padding(spacing.contentGap), verticalArrangement = Arrangement.spacedBy(spacing.item)) {
                        panel.recent.forEach { record ->
                            Column(verticalArrangement = Arrangement.spacedBy(spacing.inline)) {
                                Row {
                                    Text(record.user, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                                    Text(record.result, style = MaterialTheme.typography.labelMedium)
                                }
                                Text(record.time, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
            if (!poll.voted && poll.action.isNotBlank()) Button(
                onClick = { if (!loggedIn) onLogin() else onVote(poll.options.filter { it.value == selected }) },
                enabled = !busy && (!loggedIn || (canVote && selected != null)), modifier = Modifier.align(Alignment.End), shape = MaterialTheme.shapes.large,
            ) { Text(if (busy) "提交中…" else if (!loggedIn) "登录后投票" else poll.submitLabel) }
            else if (!loggedIn) OutlinedButton(onClick = onLogin, modifier = Modifier.align(Alignment.End)) { Text("登录后投票") }
        }
    }
}
