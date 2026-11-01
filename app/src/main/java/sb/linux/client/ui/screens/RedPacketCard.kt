package sb.linux.client.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import sb.linux.client.domain.topic.RedPacketPanel

@Composable
internal fun RedPacketCard(panel: RedPacketPanel, onOpenSource: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val details = panel.details.filter(String::isNotBlank).distinct()
    val rules = panel.rules.filter(String::isNotBlank).distinct()
    var rulesExpanded by rememberSaveable(panel) { mutableStateOf(false) }
    Surface(
        shape = MaterialTheme.shapes.large,
        color = colors.surfaceContainerLow,
        border = BorderStroke(1.dp, colors.error.copy(alpha = 0.2f)),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(shape = MaterialTheme.shapes.medium, color = colors.errorContainer) {
                    Icon(Icons.Default.CardGiftcard, contentDescription = null,
                        tint = colors.onErrorContainer, modifier = Modifier.padding(10.dp).size(22.dp))
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (panel.title.isNotBlank()) {
                        Text(panel.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    }
                    if (panel.status.isNotBlank()) {
                        Text(panel.status, style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
                    }
                }
            }
            if (panel.remaining.isNotBlank()) {
                Text(panel.remaining, style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold, color = colors.error)
            }
            details.chunked(2).forEach { fields ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    fields.forEach { field ->
                        Surface(modifier = Modifier.weight(1f), shape = MaterialTheme.shapes.medium,
                            color = colors.surfaceContainerHigh) {
                            Text(field, style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp))
                        }
                    }
                    if (fields.size == 1 && details.size > 1) Spacer(Modifier.weight(1f))
                }
            }
            if (rules.isNotEmpty()) {
                TextButton(onClick = { rulesExpanded = !rulesExpanded }) {
                    Text(if (rulesExpanded) "收起红包规则" else "查看红包规则")
                    Icon(if (rulesExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null, modifier = Modifier.size(18.dp))
                }
                if (rulesExpanded) {
                    rules.forEach { rule ->
                        Text(rule, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                    }
                }
            }
            OutlinedButton(onClick = onOpenSource, modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium) {
                Text("查看原站红包详情")
            }
        }
    }
}
