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
import sb.linux.client.domain.topic.DEFAULT_COMMENT_REWARD_TIERS
import sb.linux.client.ui.*
import sb.linux.client.ui.theme.*
import sb.linux.client.ui.content.*
import sb.linux.client.ui.patterns.topic.*
import sb.linux.client.ui.patterns.user.*
import sb.linux.client.ui.components.feedback.*
import sb.linux.client.ui.components.layout.*
import sb.linux.client.ui.components.navigation.*
/** 评论投币底部弹窗：金额档位来自源站，理由选填（最多 120 字）。 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CoinDialog(tiers: List<Int>, onDismiss: () -> Unit, onConfirm: (Int, String) -> Unit) {
    val availableTiers = remember(tiers) { tiers.filter { it > 0 }.distinct().ifEmpty { DEFAULT_COMMENT_REWARD_TIERS } }
    var amount by remember(availableTiers) { mutableIntStateOf(availableTiers.first()) }
    var reason by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("投币", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                "投币积分将赠送给作者，感谢你对优质内容的支持",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            // 源站只接受 data-tiers 声明的金额，放不下时自动换行。
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                availableTiers.forEach { value ->
                    FilterChip(
                        selected = amount == value,
                        onClick = { amount = value },
                        label = { Text(value.toString(), style = MaterialTheme.typography.labelMedium) }
                    )
                }
            }
            OutlinedTextField(
                value = reason,
                onValueChange = { if (it.length <= 120) reason = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("理由（选填）") },
                placeholder = { Text("例如：内容很有帮助") },
                shape = MaterialTheme.shapes.large,
                singleLine = true,
                supportingText = { Text("${reason.length}/120") }
            )
            Button(
                onClick = { onConfirm(amount, reason.trim()) },
                enabled = amount in availableTiers,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = MaterialTheme.shapes.large
            ) {
                Text(
                    "投币 $amount 积分",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
