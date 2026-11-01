package sb.linux.client.ui.screens
import sb.linux.client.domain.topic.DonationForm


import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import sb.linux.client.ui.*
import sb.linux.client.ui.theme.*
import sb.linux.client.ui.content.*
import sb.linux.client.ui.patterns.topic.*
import sb.linux.client.ui.patterns.user.*
import sb.linux.client.ui.components.feedback.*
import sb.linux.client.ui.components.layout.*
import sb.linux.client.ui.components.navigation.*

import sb.linux.client.ui.feature.topic.TopicInteractionViewModel
/** 打赏底部弹窗（28）：从底部滑出，不再跳转独立页面。
 *  金额支持自定义（38）：预设档位 + 「自定义」切换数字输入；快捷留言取自源站打赏表单 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DonateSheet(
    viewModel: TopicInteractionViewModel,
    tid: Long,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit,
) {
    var info by remember { mutableStateOf<DonationForm?>(null) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var preset by remember { mutableStateOf("6") }
    var custom by remember { mutableStateOf(false) }
    var customText by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var msg by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(tid) {
        viewModel.loadDonateForm(tid) { result ->
            result.onSuccess { info = it }.onFailure { loadError = it.message }
        }
    }

    val amount = if (custom) customText else preset
    val amountNum = amount.toIntOrNull()
    val validAmount = amountNum != null && amountNum in 1..99

    ModalBottomSheet(onDismissRequest = { if (!busy) onDismiss() }) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            when {
                loadError != null -> Text(
                    "加载失败：${loadError}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(vertical = 24.dp)
                )
                info == null -> Box(
                    Modifier.fillMaxWidth().height(200.dp),
                    contentAlignment = Alignment.Center
                ) { AppInlineLoading(size = 32.dp) }
                info?.unavailableReason?.isNotBlank() == true -> {
                    Text("暂时无法打赏", style = MaterialTheme.typography.titleLarge)
                    Text(info!!.unavailableReason)
                    TextButton(onClick = onDismiss) { Text("知道了") }
                }
                else -> {
                    val i = info!!
                    Text(
                        "打赏作者",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    msg?.let {
                        Surface(
                            shape = MaterialTheme.shapes.large,
                            color = MaterialTheme.colorScheme.errorContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                it,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(horizontal = LocalAppSpacing.current.pageHorizontal, vertical = 10.dp)
                            )
                        }
                    }
                    Text(
                        "打赏积分将直接赠送给楼主，感谢你对优质内容的支持",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    // 金额档位 + 自定义切换（38），与投币一致：预设 6/33/66/88，自定义范围 1～99
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("6", "33", "66", "88").forEach { v ->
                            FilterChip(
                                selected = !custom && preset == v,
                                onClick = { custom = false; preset = v },
                                label = { Text(v, style = MaterialTheme.typography.labelMedium) }
                            )
                        }
                        FilterChip(
                            selected = custom,
                            onClick = { custom = !custom },
                            label = { Text("自定义", style = MaterialTheme.typography.labelMedium) }
                        )
                    }
                    if (custom) {
                        OutlinedTextField(
                            value = customText,
                            onValueChange = { t -> customText = t.filter { it.isDigit() }.take(2) },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("自定义金额（积分）") },
                            placeholder = { Text("范围 1～99") },
                            shape = MaterialTheme.shapes.large,
                            singleLine = true,
                            isError = customText.isNotBlank() && !validAmount,
                            supportingText = { Text("仅支持 1～99 积分") }
                        )
                    }
                    // 快捷留言（源站打赏表单自带）
                    if (i.quickMessages.isNotEmpty()) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(i.quickMessages) { q ->
                                AssistChip(
                                    onClick = { message = q },
                                    label = {
                                        Text(
                                            q,
                                            style = MaterialTheme.typography.labelMedium,
                                            maxLines = 1
                                        )
                                    }
                                )
                            }
                        }
                    }
                    OutlinedTextField(
                        value = message,
                        onValueChange = { message = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("留言") },
                        shape = MaterialTheme.shapes.large,
                        singleLine = true
                    )
                    Button(
                        onClick = {
                            busy = true; msg = null
                            viewModel.donate(tid, amount, message) { result ->
                                result.onSuccess { rejection ->
                                    if (rejection != null) msg = rejection else onSuccess()
                                }.onFailure { msg = it.message }
                                busy = false
                            }
                        },
                        enabled = !busy && validAmount,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Text(
                            if (busy) "提交中…" else "打赏 $amount 积分",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}
