package sb.linux.client.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import sb.linux.client.domain.topic.TreasurePoolPanel
import sb.linux.client.ui.theme.LocalAppSpacing

@Composable
internal fun TreasurePoolCard(panel: TreasurePoolPanel) {
    val spacing = LocalAppSpacing.current
    Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth().padding(horizontal = spacing.pageHorizontal, vertical = spacing.item)) {
        Column(Modifier.padding(spacing.pageHorizontal), verticalArrangement = Arrangement.spacedBy(spacing.contentGap)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.item)) {
                Icon(Icons.Filled.Paid, null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.tertiary)
                Text(panel.title, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                if (panel.status.isNotBlank()) PollStatusPill(panel.status, highlighted = true)
            }
            TopicPanelStatistics(panel.statistics)
            panel.rules.forEach { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}
