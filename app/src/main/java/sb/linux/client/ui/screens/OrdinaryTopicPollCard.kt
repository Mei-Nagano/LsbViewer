package sb.linux.client.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import sb.linux.client.domain.topic.*
import sb.linux.client.ui.theme.LocalAppSpacing

/** 标题和状态为主信息，选项为连续列表；页尾仅一个主要提交动作。 */
@Composable
internal fun OrdinaryTopicPollCard(
    poll: TopicPoll, loggedIn: Boolean, busy: Boolean,
    onLogin: () -> Unit, onVote: (List<TopicPollOption>) -> Unit, onView: () -> Unit,
    viewBusy: Boolean = false,
) {
    val spacing = LocalAppSpacing.current
    var selected by remember(poll) { mutableStateOf(poll.options.filter { it.selected }.map { it.value }.toSet()) }
    val initial = poll.options.filter { it.selected }.map { it.value }.toSet()
    val canSelect = loggedIn && !busy && !viewBusy && !poll.closed && poll.unavailableReason.isBlank() && poll.action.isNotBlank()
    val status = when {
        poll.closed -> "已结束"
        poll.voted -> "已投票"
        poll.unavailableReason.isNotBlank() -> "不可参与"
        else -> poll.statusLabel.ifBlank { "进行中" }
    }
    Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth().padding(horizontal = spacing.pageHorizontal, vertical = spacing.item)) {
        Column {
            Column(Modifier.padding(spacing.pageHorizontal), verticalArrangement = Arrangement.spacedBy(spacing.item)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(spacing.item)) {
                    Text(poll.title, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    PollStatusPill(status, highlighted = true)
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(spacing.inline), verticalArrangement = Arrangement.spacedBy(spacing.inline)) {
                    PollStatusPill(poll.choiceLabel.ifBlank { if (poll.multiple) "多选投票" else "单选投票" })
                    if (poll.remainingActions.isNotBlank()) PollStatusPill(poll.remainingActions)
                    if (poll.participants.isNotBlank()) PollStatusPill(poll.participants)
                }
                Text(poll.deadline.ifBlank { "截止时间：源站未提供" }, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (!poll.resultView) poll.options.forEachIndexed { index, option ->
                val checked = option.value in selected
                ListItem(
                    headlineContent = { Text(option.label, style = MaterialTheme.typography.bodyLarge) },
                    leadingContent = {
                        if (poll.multiple) Checkbox(checked, null, enabled = canSelect && !option.disabled)
                        else RadioButton(checked, null, enabled = canSelect && !option.disabled)
                    },
                    modifier = Modifier.selectable(checked, enabled = canSelect && !option.disabled,
                        role = if (poll.multiple) Role.Checkbox else Role.RadioButton) {
                        selected = if (!poll.multiple) setOf(option.value)
                        else if (checked) selected - option.value
                        else if (selected.size < poll.maxChoices) selected + option.value else selected
                    },
                    colors = ListItemDefaults.colors(containerColor = if (checked) MaterialTheme.colorScheme.secondaryContainer
                        else MaterialTheme.colorScheme.surfaceContainerLow),
                )
                if (index < poll.options.lastIndex) HorizontalDivider(Modifier.padding(horizontal = spacing.pageHorizontal),
                    color = MaterialTheme.colorScheme.outlineVariant)
            }
            poll.results.forEach { result ->
                ListItem(
                    headlineContent = { Text(result.label) },
                    trailingContent = { Text(result.count, style = MaterialTheme.typography.labelLarge) },
                    supportingContent = {
                        LinearProgressIndicator(progress = { result.fraction.coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth().padding(top = spacing.item), drawStopIndicator = {})
                    },
                    colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                )
            }
            if (poll.unavailableReason.isNotBlank()) Text(poll.unavailableReason,
                Modifier.padding(horizontal = spacing.pageHorizontal), style = MaterialTheme.typography.bodySmall)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            FlowRow(Modifier.fillMaxWidth().padding(spacing.pageHorizontal),
                horizontalArrangement = Arrangement.spacedBy(spacing.item, Alignment.End),
                verticalArrangement = Arrangement.spacedBy(spacing.item)) {
                if (poll.viewUrl.isNotBlank()) OutlinedButton(onClick = onView, enabled = !busy && !viewBusy, shape = MaterialTheme.shapes.large) {
                    Text(if (viewBusy) "加载中…" else if (poll.resultView && poll.voted && !poll.closed) "修改投票" else poll.viewLabel.ifBlank { if (poll.resultView) "返回投票" else "查看结果" })
                }
                if (!poll.resultView && !poll.closed && poll.unavailableReason.isBlank()) Button(
                    onClick = { if (!loggedIn) onLogin() else onVote(poll.options.filter { it.value in selected }) },
                    enabled = !busy && !viewBusy && (!loggedIn || (canSelect && selected.isNotEmpty() && selected.size <= poll.maxChoices &&
                        (!poll.selectionMustChange || selected != initial))), shape = MaterialTheme.shapes.large,
                ) { Text(if (!loggedIn) "登录后投票" else if (busy) "提交中…" else if (poll.voted) "修改投票" else "立即投票") }
            }
        }
    }
}

@Composable
internal fun PollStatusPill(text: String, highlighted: Boolean = false) {
    Surface(shape = CircleShape, color = if (highlighted) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh) {
        Text(text, Modifier.padding(horizontal = 10.dp, vertical = 5.dp), style = MaterialTheme.typography.labelMedium,
            color = if (highlighted) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
