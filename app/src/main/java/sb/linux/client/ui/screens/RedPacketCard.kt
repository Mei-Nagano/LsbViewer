package sb.linux.client.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import sb.linux.client.domain.topic.RedPacketPanel
import sb.linux.client.domain.topic.PanelStatistic
import sb.linux.client.ui.theme.LocalAppSpacing

@Composable
internal fun RedPacketCard(panel: RedPacketPanel) {
    val spacing = LocalAppSpacing.current
    var showRules by remember(panel) { mutableStateOf(false) }
    val statistics = panel.details.filter(String::isNotBlank).distinct().map { text ->
        val parts = text.split(" · ")
        PanelStatistic(parts.first(), parts.getOrElse(1) { "" }, parts.drop(2).joinToString(" · "))
    }
    Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth().padding(horizontal = spacing.pageHorizontal, vertical = spacing.item)) {
        Column(Modifier.padding(spacing.pageHorizontal), verticalArrangement = Arrangement.spacedBy(spacing.contentGap)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.item)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(spacing.inline)) {
                    Text(panel.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    if (panel.status.isNotBlank()) Text(panel.status, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (panel.remaining.isNotBlank()) Text(panel.remaining, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            }
            TopicPanelStatistics(statistics, if (panel.title.contains("讨论")) ({ showRules = !showRules }) else null)
            if (!panel.title.contains("讨论") || showRules) panel.rules.filter(String::isNotBlank).distinct().forEach {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
