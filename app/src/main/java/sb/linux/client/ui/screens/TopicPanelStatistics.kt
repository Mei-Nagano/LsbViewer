package sb.linux.client.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import sb.linux.client.domain.topic.PanelStatistic
import sb.linux.client.ui.theme.LocalAppSpacing

@Composable
internal fun TopicPanelStatistics(items: List<PanelStatistic>, onItemClick: ((PanelStatistic) -> Unit)? = null) {
    val spacing = LocalAppSpacing.current
    Column(verticalArrangement = Arrangement.spacedBy(spacing.item)) {
        items.chunked(3).forEach { row ->
            // 高度只取同一行文字的固有高度，不能填满 LazyColumn 给出的无界滚动空间。
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(spacing.item)) {
                row.forEach { item ->
                    Surface(modifier = Modifier.weight(1f).fillMaxHeight().then(if (onItemClick != null && item.label == "认可条件") Modifier.clickable { onItemClick(item) } else Modifier), shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                        Column(Modifier.fillMaxWidth().padding(spacing.item),
                            verticalArrangement = Arrangement.spacedBy(spacing.inline)) {
                            Text(item.label, style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(item.value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            if (item.note.isNotBlank()) Text(item.note, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}
