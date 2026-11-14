package sb.linux.client.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import sb.linux.client.domain.account.AccountActivityMetric
import sb.linux.client.domain.account.AccountNode
import sb.linux.client.ui.theme.LocalAppSpacing

// §2.1 / §2.5.3：数量为主，名称、得分为次，查看规则为辅助操作。
// §3.6 / §8.5：两列等宽卡片，仅用容器色区分；间距用 AppSpacing，字阶、颜色和圆角用 MaterialTheme。
@Composable
internal fun CommunityActivityCards(activity: AccountNode.Activity, enabled: Boolean) {
    val spacing = LocalAppSpacing.current
    var selected by remember(activity) { mutableStateOf<AccountActivityMetric?>(null) }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(spacing.section)) {
        Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surfaceContainerLow) {
            Column(Modifier.fillMaxWidth().padding(spacing.pageHorizontal), verticalArrangement = Arrangement.spacedBy(spacing.contentGap)) {
                Text(activity.label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.contentGap)) {
                    Text(activity.score, Modifier.weight(1f), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
                    if (activity.tier.isNotBlank()) Text(activity.tier, style = MaterialTheme.typography.titleSmall)
                }
                if (activity.status.isNotBlank()) Text(activity.status, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                activity.reasons.forEach { reason ->
                    Text(reason, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(spacing.contentGap)) {
            activity.metrics.chunked(2).forEach { row ->
                Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(spacing.contentGap)) {
                    row.forEach { metric ->
                        ActivityMetricCard(metric, enabled, Modifier.weight(1f).fillMaxHeight()) { selected = metric }
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
    selected?.let { metric ->
        AccountDetailsDialog("${metric.label}规则", onDismiss = { selected = null }) {
            val spacing = LocalAppSpacing.current
            Column(verticalArrangement = Arrangement.spacedBy(spacing.section)) {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.inline)) {
                    Text(metric.value, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                    Text(metric.score, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(metric.rule, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                metric.notes.forEach { note ->
                    Text(note, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun ActivityMetricCard(metric: AccountActivityMetric, enabled: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val spacing = LocalAppSpacing.current
    Card(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(Modifier.fillMaxWidth().padding(spacing.contentGap), verticalArrangement = Arrangement.spacedBy(spacing.item)) {
            Text(metric.label, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(metric.value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f))
            Text(metric.score, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            metric.fraction?.let { fraction ->
                LinearProgressIndicator(progress = { fraction }, drawStopIndicator = {}, modifier = Modifier.fillMaxWidth())
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.inline)) {
                Text("查看规则", Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
internal fun AccountDetailsCard(title: String, enabled: Boolean, content: @Composable () -> Unit) {
    val spacing = LocalAppSpacing.current
    var show by remember(title) { mutableStateOf(false) }
    Card(onClick = { show = true }, enabled = enabled, modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Row(Modifier.fillMaxWidth().padding(spacing.pageHorizontal), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.contentGap)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(spacing.inline)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text("查看完整说明", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    if (show) AccountDetailsDialog(title, onDismiss = { show = false }, content = content)
}

@Composable
private fun AccountDetailsDialog(title: String, onDismiss: () -> Unit, content: @Composable () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) { content() } },
        confirmButton = { TextButton(onClick = onDismiss) { Text("关闭") } },
    )
}
