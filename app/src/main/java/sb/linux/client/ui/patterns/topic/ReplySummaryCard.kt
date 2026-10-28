package sb.linux.client.ui.patterns.topic

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import sb.linux.client.ui.theme.AppSummaryDefaults
import sb.linux.client.ui.theme.LocalAppSpacing

/** Shared summary layout; full post bodies and private-message bubbles remain separate. */
@Composable
fun ReplySummaryCard(
    title: String, excerpt: String, time: String, modifier: Modifier = Modifier,
    maxLines: Int = 3, onClick: (() -> Unit)? = null,
    leading: (@Composable () -> Unit)? = null, badge: (@Composable () -> Unit)? = null,
    footer: (@Composable ColumnScope.() -> Unit)? = null,
) {
    val spacing = LocalAppSpacing.current
    Surface(
        modifier.fillMaxWidth().padding(AppSummaryDefaults.itemPadding)
            .clickable(enabled = onClick != null) { onClick?.invoke() },
        shape = AppSummaryDefaults.cardShape, color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(Modifier.padding(AppSummaryDefaults.contentPadding),
            verticalArrangement = Arrangement.spacedBy(AppSummaryDefaults.contentSpacing)) {
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.item)) {
                leading?.invoke()
                Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                badge?.invoke()
            }
            if (excerpt.isNotBlank()) Text(excerpt, style = MaterialTheme.typography.bodyMedium,
                maxLines = maxLines, overflow = TextOverflow.Ellipsis)
            if (time.isNotBlank()) Text(time, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            footer?.invoke(this)
        }
    }
}
