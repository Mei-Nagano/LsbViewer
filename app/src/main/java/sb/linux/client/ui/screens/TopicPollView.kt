package sb.linux.client.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import sb.linux.client.domain.topic.EssenceApplication
import sb.linux.client.domain.topic.EssenceReview
import sb.linux.client.domain.topic.TopicPoll
import sb.linux.client.domain.topic.TopicPollOption
@Composable
internal fun TopicPollView(
    poll: TopicPoll?,
    essence: EssenceReview?,
    application: EssenceApplication?,
    loggedIn: Boolean,
    busy: Boolean,
    topUpBusy: Boolean,
    onLogin: () -> Unit,
    onApply: (EssenceApplication) -> Unit,
    onTopUp: (Int) -> Unit,
    onVote: (List<TopicPollOption>, String) -> Unit,
) {
    val vote = poll
    var selectedValues by remember(vote?.action, vote?.options) {
        mutableStateOf(vote?.options.orEmpty().filter { it.selected }.map { it.value }.toSet())
    }
    var reason by remember(vote?.action) { mutableStateOf("") }
    val topUp = essence?.topUp
    var topUpText by remember(topUp?.action) { mutableStateOf(topUp?.defaultAmount.orEmpty()) }
    var confirmTopUp by remember { mutableStateOf<Int?>(null) }
    @Composable
    fun optionCard(option: TopicPollOption, modifier: Modifier = Modifier) {
        val currentPoll = vote ?: return
        val selected = option.value in selectedValues
        Surface(
            shape = RoundedCornerShape(13.dp),
            color = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                else MaterialTheme.colorScheme.surfaceContainerHigh,
            border = if (selected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
            modifier = modifier,
        ) {
            Row(
                Modifier.fillMaxWidth().clickable(
                    enabled = !currentPoll.closed && currentPoll.unavailableReason.isBlank() && !option.disabled
                ) {
                    selectedValues = if (currentPoll.multiple) {
                        if (option.value in selectedValues) selectedValues - option.value else selectedValues + option.value
                    } else setOf(option.value)
                }.padding(horizontal = 8.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (currentPoll.multiple) Checkbox(selected, null, enabled = !currentPoll.closed && currentPoll.unavailableReason.isBlank() && !option.disabled)
                else RadioButton(selected, null, enabled = !currentPoll.closed && currentPoll.unavailableReason.isBlank() && !option.disabled)
                Text(option.label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            }
        }
    }
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    Modifier.size(38.dp).clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.AutoAwesome, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
                Column(Modifier.weight(1f)) {
                    Text(essence?.title ?: vote?.title.orEmpty(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        when {
                            essence?.subtitle?.isNotBlank() == true -> essence.subtitle
                            vote?.unavailableReason?.isNotBlank() == true -> vote.unavailableReason
                            vote?.closed == true -> "投票已结束"
                            vote?.multiple == true -> "可选择多项"
                            vote != null -> "请选择一项"
                            else -> "申精评议"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (essence?.status in setOf("voting", "featured", "pending_approval") || (vote != null && !vote.closed && vote.unavailableReason.isBlank()))
                        MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
                ) {
                    Text(
                        essence?.statusLabel?.ifBlank { null }
                            ?: if (vote?.closed == true) "已结束" else if (vote?.unavailableReason?.isNotBlank() == true) "不可参与" else "进行中",
                        Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (essence?.status in setOf("voting", "featured", "pending_approval") || (vote != null && !vote.closed && vote.unavailableReason.isBlank()))
                            MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            essence?.takeIf { it.target > 0 || it.progressText.isNotBlank() }?.let { review ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        Modifier.weight(1f).height(10.dp).clip(RoundedCornerShape(50))
                            .background(if (review.progress < 0) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceContainerHighest)
                    ) {
                        Box(
                            Modifier.fillMaxHeight()
                                .fillMaxWidth((review.progress.coerceAtLeast(0).toFloat() / review.target.coerceAtLeast(1)).coerceIn(0f, 1f))
                                .background(if (review.progress < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                        )
                    }
                    Text(review.progressText.ifBlank { "${review.progress} / ${review.target}" }, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
                if (review.progressNote.isNotBlank()) Text(
                    review.progressNote,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            essence?.meta?.takeIf { it.isNotEmpty() }?.let { items ->
                FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items.forEach { StatChip(null, it) }
                }
            }
            essence?.takeIf { it.poolTitle.isNotBlank() || it.poolItems.isNotEmpty() }?.let { review ->
                Surface(shape = RoundedCornerShape(13.dp), color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.55f)) {
                    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Paid, null, Modifier.size(17.dp), tint = MaterialTheme.colorScheme.onTertiaryContainer)
                            Spacer(Modifier.width(6.dp))
                            Text(review.poolTitle.ifBlank { "积分奖池" }, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        }
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            review.poolItems.forEach { Text(it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onTertiaryContainer) }
                        }
                    }
                }
            }
            val panelNote = essence?.actionNote.orEmpty().ifBlank { vote?.note.orEmpty() }
            if (panelNote.isNotBlank()) Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
                Text(
                    panelNote,
                    Modifier.fillMaxWidth().padding(horizontal = 11.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (vote != null && (essence != null || vote.title.contains("精华")) && vote.options.size == 2) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    vote.options.forEach { option -> optionCard(option, Modifier.weight(1f)) }
                }
            } else vote?.options.orEmpty().forEach { option -> optionCard(option, Modifier.fillMaxWidth()) }
            if (vote?.reasonName?.isNotBlank() == true) OutlinedTextField(
                value = reason, onValueChange = { reason = it.take(vote.reasonMaxLength) },
                label = { Text(if (vote.reasonRequired) "投票理由（必填）" else "投票理由") },
                supportingText = { Text(vote.reasonHint.ifBlank { "${reason.length}/${vote.reasonMaxLength}" }) },
                modifier = Modifier.fillMaxWidth(), enabled = !busy && !vote.closed,
                shape = RoundedCornerShape(14.dp),
            )
            if (vote != null) when {
                vote.unavailableReason.isNotBlank() -> Text(
                    vote.unavailableReason,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                vote.closed -> Text("当前不可投票", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                !loggedIn -> Button(onClick = onLogin, Modifier.fillMaxWidth()) { Text("登录后投票") }
                else -> Button(
                    onClick = { onVote(vote.options.filter { it.value in selectedValues }, reason.trim()) },
                    enabled = !busy && selectedValues.isNotEmpty() && vote.action.isNotBlank() && (!vote.reasonRequired || reason.isNotBlank()),
                    modifier = Modifier.align(Alignment.End),
                    shape = RoundedCornerShape(50),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 10.dp),
                ) { Text(if (busy) "提交中…" else vote.submitLabel) }
            }
            if (application != null) {
                Button(
                    onClick = { if (loggedIn) onApply(application) else onLogin() },
                    enabled = !busy && (!loggedIn || application.enabled) && application.action.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(50),
                ) { Text(if (busy) "提交中…" else if (loggedIn) application.label else "登录后申请加精") }
            }
            if (topUp != null) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = topUpText,
                        onValueChange = { value -> topUpText = value.filter(Char::isDigit).take(9) },
                        label = { Text(topUp.label) },
                        supportingText = {
                            Text(topUp.hint.ifBlank {
                                if (topUp.max == Int.MAX_VALUE) "至少 ${topUp.min} 积分" else "${topUp.min}～${topUp.max} 积分"
                            })
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        enabled = !topUpBusy,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f),
                    )
                    val amount = topUpText.toIntOrNull()
                    FilledTonalButton(
                        onClick = { confirmTopUp = amount },
                        enabled = !topUpBusy && topUp.enabled && amount != null && amount in topUp.min..topUp.max,
                        modifier = Modifier.padding(top = 8.dp),
                    ) { Text(if (topUpBusy) "追加中…" else topUp.submitLabel) }
                }
            }
        }
    }
    confirmTopUp?.let { amount ->
        AlertDialog(
            onDismissRequest = { confirmTopUp = null },
            title = { Text("确认追加积分") },
            text = { Text("将向本帖的申精奖池追加 $amount 积分。提交后积分会按源站规则扣除。") },
            confirmButton = {
                TextButton(onClick = { confirmTopUp = null; onTopUp(amount) }) { Text("确认追加") }
            },
            dismissButton = { TextButton(onClick = { confirmTopUp = null }) { Text("取消") } },
        )
    }
}


