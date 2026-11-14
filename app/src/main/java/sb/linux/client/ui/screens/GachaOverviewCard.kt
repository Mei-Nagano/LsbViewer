package sb.linux.client.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import sb.linux.client.domain.account.AccountNode
import sb.linux.client.ui.feature.profile.GachaOverviewViewModel
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.abs

@Composable
internal fun GachaOverviewCard(model: GachaOverviewViewModel, owner: Long) {
    var target by rememberSaveable(owner) { mutableStateOf("") }
    var menu by remember { mutableStateOf(false) }
    val start = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    val today = model.points.filter { it.timestamp >= start }
    val groups = listOf("抽取" to today.filter { Regex("称号.*(抽|连)|(?:抽|连).*称号|(?:十|百)连抽").containsMatchIn(it.eventReason) },
        "交易" to today.filter { it.eventReason.contains("称号") && Regex("购买|出售|交易|市场|买入|卖出|成交").containsMatchIn(it.eventReason) },
        "回收" to today.filter { Regex("称号|SSR", RegexOption.IGNORE_CASE).containsMatchIn(it.eventReason) && it.eventReason.contains("回收") })
    val peak = groups.maxOfOrNull { (_, rows) -> rows.sumOf { abs(it.signedAmount) } }?.coerceAtLeast(1) ?: 1L
    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("今日称号积分", Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                if (model.loading) Text("更新中…", style = MaterialTheme.typography.labelSmall)
            }
            groups.forEach { (name, rows) ->
                val income = rows.filter { it.signedAmount > 0 }.sumOf { it.signedAmount }
                val spent = rows.filter { it.signedAmount < 0 }.sumOf { abs(it.signedAmount) }
                Row(Modifier.fillMaxWidth()) {
                    Text(name, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                    Text("收入 $income · 支出 $spent", style = MaterialTheme.typography.labelMedium)
                }
                LinearProgressIndicator(progress = { (income + spent).toFloat() / peak }, drawStopIndicator = {}, modifier = Modifier.fillMaxWidth())
            }
            model.error?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
            val options = model.recipes.mapNotNull { recipe ->
                recipe.children.filterIsInstance<AccountNode.Badge>().firstOrNull()?.let { it.value.name to recipe }
            }
            Box {
                OutlinedButton(onClick = { menu = true }, shape = RoundedCornerShape(12.dp), enabled = options.isNotEmpty()) {
                    Text(if (target.isBlank()) "选择 UR 合成目标" else "UR 目标：$target")
                }
                DropdownMenu(menu, { menu = false }) {
                    DropdownMenuItem(text = { Text("暂不设置") }, onClick = { target = ""; menu = false })
                    options.forEach { (name, _) -> DropdownMenuItem(text = { Text(name) }, onClick = { target = name; menu = false }) }
                }
            }
            options.firstOrNull { it.first == target }?.second?.let { recipe ->
                val materials = recipe.children.filterIsInstance<AccountNode.Pills>().flatMap { it.values }
                val missing = materials.mapNotNull { (label, _) ->
                    Regex("^(.*?)\\s+(\\d+)/(\\d+)$").find(label)?.let { match ->
                        val have = match.groupValues[2].toLongOrNull() ?: 0
                        val need = match.groupValues[3].toLongOrNull() ?: 0
                        if (have < need) "${match.groupValues[1]} 还差 ${need - have}" to false else "${match.groupValues[1]} 已齐" to true
                    }
                }
                GachaSourcePills(missing.ifEmpty { materials })
            }
        }
    }
}
