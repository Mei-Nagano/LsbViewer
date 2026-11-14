package sb.linux.client.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import sb.linux.client.domain.community.PointRow
import sb.linux.client.ui.LocalLinkHandler
import sb.linux.client.ui.theme.LocalAppSpacing
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import kotlin.math.abs

internal fun LazyListScope.pointsHistoryRows(rows: List<PointRow>, balance: String) {
    item { PointsHistoryChart(rows, balance) }
    itemsIndexed(rows, key = { index, row -> "${row.timeText}-${row.reason}-$index" }) { index, row ->
        val open = LocalLinkHandler.current
        Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = Modifier.fillMaxWidth().padding(horizontal = LocalAppSpacing.current.pageHorizontal,
                vertical = LocalAppSpacing.current.inline)) {
        ListItem(headlineContent = { Text(row.reason) }, supportingContent = { Text(row.timeText) },
            trailingContent = { Text("${if (row.positive) "+" else "−"}${pointsNumber(abs(row.signedAmount))}",
                color = if (row.positive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.labelLarge) },
            colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            modifier = if (row.topicId > 0 && open != null) Modifier.clickable { open("/topic/${row.topicId}") } else Modifier)
        }
    }
}

@Composable
private fun PointsHistoryChart(rows: List<PointRow>, balance: String) {
    val spacing = LocalAppSpacing.current
    var period by remember { mutableIntStateOf(0) }
    var selectedDay by remember(rows, period) { mutableStateOf<String?>(null) }
    var allReasons by remember(period) { mutableStateOf(false) }
    val filtered = remember(rows, period) {
        if (period == 0) rows else {
            val start = java.time.LocalDate.now().minusDays(period.toLong() - 1)
                .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            rows.filter { it.timestamp >= start }
        }
    }
    val income = filtered.filter { it.signedAmount > 0 }.sumOf { it.signedAmount }
    val expense = filtered.filter { it.signedAmount < 0 }.sumOf { abs(it.signedAmount) }
    val days = remember(filtered) { filtered.groupBy(::pointDay).toSortedMap().entries.toList() }
    val peak = days.maxOfOrNull { entry -> maxOf(entry.value.filter { it.positive }.sumOf { it.signedAmount },
        entry.value.filterNot { it.positive }.sumOf { abs(it.signedAmount) }) }?.coerceAtLeast(1L) ?: 1L
    Column(Modifier.fillMaxWidth().padding(spacing.pageHorizontal), verticalArrangement = Arrangement.spacedBy(spacing.contentGap)) {
        Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surfaceContainerLow) {
            Column(Modifier.fillMaxWidth().padding(spacing.pageHorizontal), verticalArrangement = Arrangement.spacedBy(spacing.contentGap)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("积分收支", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                    if (balance.isNotBlank()) Text("余额 $balance", style = MaterialTheme.typography.labelLarge)
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(spacing.item)) {
                    listOf(0 to "全部", 1 to "今天", 7 to "近7天", 30 to "近30天").forEach { (value, label) ->
                        FilterChip(period == value, { period = value }, label = { Text(label) }, shape = CircleShape)
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(spacing.item)) {
                    PointMetric("收入", income, Modifier.weight(1f))
                    PointMetric("支出", expense, Modifier.weight(1f))
                    PointMetric("净变化", income - expense, Modifier.weight(1f))
                }
                Text("源站可读取的全部明细 ${rows.size} 条 · 当前范围 ${filtered.size} 条",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (filtered.isEmpty()) Text("此范围暂无积分记录", style = MaterialTheme.typography.bodyMedium)
                else {
                    Text("每日收支", style = MaterialTheme.typography.titleSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(spacing.contentGap)) {
                        PointLegend("收入", MaterialTheme.colorScheme.primary)
                        PointLegend("支出", MaterialTheme.colorScheme.error)
                    }
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(spacing.inline)) {
                        items(days, key = { it.key }) { day ->
                            val plus = day.value.filter { it.positive }.sumOf { it.signedAmount }
                            val minus = day.value.filterNot { it.positive }.sumOf { abs(it.signedAmount) }
                            Surface(shape = MaterialTheme.shapes.medium,
                                color = if (selectedDay == day.key) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
                                onClick = { selectedDay = if (selectedDay == day.key) null else day.key },
                                modifier = Modifier.semantics { contentDescription = "${day.key}，收入$plus，支出$minus" }) {
                                Column(Modifier.width(60.dp).padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Row(Modifier.height(108.dp), verticalAlignment = Alignment.Bottom,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        PointBar(plus, peak, MaterialTheme.colorScheme.primary)
                                        PointBar(minus, peak, MaterialTheme.colorScheme.error)
                                    }
                                    Spacer(Modifier.height(6.dp))
                                    Text(day.key.takeLast(5), style = MaterialTheme.typography.labelSmall, maxLines = 1)
                                }
                            }
                        }
                    }
                    selectedDay?.let { selected ->
                        val transactions = days.firstOrNull { it.key == selected }?.value.orEmpty()
                        Text("$selected · ${transactions.size} 笔 · 收入 ${pointsNumber(transactions.filter { it.positive }.sumOf { it.signedAmount })}" +
                            " · 支出 ${pointsNumber(transactions.filterNot { it.positive }.sumOf { abs(it.signedAmount) })}",
                            style = MaterialTheme.typography.bodySmall)
                    }
                    Text("收支来源", style = MaterialTheme.typography.titleSmall)
                    val reasons = filtered.groupBy(::pointCategory).entries.sortedByDescending { it.value.sumOf { row -> abs(row.signedAmount) } }
                    val maxReason = reasons.maxOf { it.value.sumOf { row -> abs(row.signedAmount) } }.coerceAtLeast(1L)
                    (if (allReasons) reasons else reasons.take(5)).forEach { reason ->
                        val sum = reason.value.sumOf { it.signedAmount }
                        val total = reason.value.sumOf { abs(it.signedAmount) }
                        Column(verticalArrangement = Arrangement.spacedBy(spacing.inline)) {
                            Row { Text(reason.key, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                                Text("${if (sum > 0) "+" else ""}${pointsNumber(sum)}", style = MaterialTheme.typography.labelMedium) }
                            LinearProgressIndicator(progress = { total.toFloat() / maxReason }, drawStopIndicator = {},
                                color = if (sum >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                modifier = Modifier.fillMaxWidth().height(6.dp))
                        }
                    }
                    if (reasons.size > 5) TextButton(onClick = { allReasons = !allReasons }) { Text(if (allReasons) "收起来源" else "全部来源") }
                }
            }
        }
        Text("积分明细", style = MaterialTheme.typography.titleSmall)
    }
}

@Composable
private fun PointMetric(label: String, value: Long, modifier: Modifier) {
    Surface(modifier, shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Column(Modifier.padding(10.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(pointsNumber(value), style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun PointBar(amount: Long, peak: Long, color: androidx.compose.ui.graphics.Color) {
    if (amount > 0) Surface(Modifier.width(18.dp).height((amount.toFloat() / peak * 108).coerceAtLeast(2f).dp),
        shape = MaterialTheme.shapes.small, color = color) {}
    else Spacer(Modifier.width(18.dp))
}

@Composable
private fun PointLegend(label: String, color: androidx.compose.ui.graphics.Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Surface(Modifier.size(8.dp), shape = CircleShape, color = color) {}
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

private fun pointDay(row: PointRow): String = if (row.timestamp > 0)
    Instant.ofEpochMilli(row.timestamp).atZone(ZoneId.systemDefault()).toLocalDate().toString()
else row.timeText.take(10).ifBlank { "日期未知" }

private fun pointsNumber(value: Long): String = NumberFormat.getIntegerInstance().format(value)

private fun pointCategory(row: PointRow): String {
    val reason = row.eventReason.ifBlank { row.reason }
    return when {
        row.positive && "主题" in reason && Regex("打赏|投币").containsMatchIn(reason) -> "主题打赏收入"
        !row.positive && Regex("打赏|投币").containsMatchIn(reason) && Regex("主题|回帖|评论").containsMatchIn(reason) -> "主题与评论打赏支出"
        else -> reason
    }
}
