package sb.linux.client.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import sb.linux.client.domain.account.*
import sb.linux.client.ui.theme.LocalAppSpacing

internal val LocalGachaForgeAll = staticCompositionLocalOf<(() -> Unit)?> { null }

/** 共用源站材料选择、数量上限和计算公式，不提交未选中的材料数量。 */
@Composable
internal fun GachaMaterialCards(node: AccountNode.TitleMaterials, enabled: Boolean, onDirty: () -> Unit,
    onSubmit: (AccountForm, List<Pair<String, String>>) -> Unit) {
    val selected = remember(node) { mutableStateMapOf<Int, Boolean>() }
    val amounts = remember(node) { mutableStateMapOf<Int, String>().apply {
        node.materials.forEachIndexed { index, material -> this[index] = material.quantity.value }
    } }
    var pending by remember(node) { mutableStateOf<Triple<AccountForm, List<Pair<String, String>>, String>?>(null) }
    val spacing = LocalAppSpacing.current
    val forgeAll = LocalGachaForgeAll.current
    var confirmAll by remember(node) { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(spacing.contentGap)) {
        if (forgeAll != null && node.actions.any { it.source.isNotBlank() }) OutlinedButton(enabled = enabled,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp), onClick = {
                node.materials.forEachIndexed { index, item ->
                    selected[index] = item.badge.name != "反贼" && !item.selector.disabled && !item.quantity.disabled
                    amounts[index] = item.quantity.max.ifBlank { item.quantity.value }
                }
                confirmAll = true
            }) { Text("一键熔炼") }
        GachaAdaptiveGrid(node.actions.size) { index ->
            val action = node.actions[index]
            val chosen = node.materials.indices.filter { selected[it] == true && node.materials[it].formId == action.form.id }
            val valid = chosen.all { i ->
                val field = node.materials[i].quantity
                val amount = amounts[i]?.toIntOrNull()
                amount != null && amount >= (field.min.toIntOrNull() ?: 1) && amount <= (field.max.toIntOrNull() ?: Int.MAX_VALUE)
            }
            val count = chosen.sumOf { amounts[it]?.toIntOrNull()?.coerceAtLeast(0)?.toLong() ?: 0L }
            val times = (count / action.cost.coerceAtLeast(1)).coerceAtMost(1000L)
            val forge = action.source.isNotBlank()
            val summary = if (forge) "已选 $count 个 · 可熔炼 $times 次\n消耗 ${times * action.cost} 个 ${action.source} · 产生 $times 个 ${action.target}"
                else "回收 $count 个 · 共计 ${count * action.price} 积分"
            FilledTonalButton(enabled = enabled && valid && if (forge) times > 0 else count > 0, onClick = {
                val pairs = action.form.hidden.filterNot { it.first == action.countName } +
                    (if (action.countName.isNotBlank()) listOf(action.countName to times.toString()) else emptyList()) +
                    chosen.flatMap { i -> listOf(node.materials[i].selector.name to node.materials[i].selector.value,
                        node.materials[i].quantity.name to amounts[i].orEmpty()) }
                pending = Triple(action.form, pairs, summary)
            }, shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(if (forge) "${action.source} × ${action.cost} → ${action.target}" else "回收所选 SSR")
                    Text(summary, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
        Text("选择材料", style = MaterialTheme.typography.titleMedium)
        Text("只消耗勾选的称号；数量上限由源站提供，已佩戴称号保留 1 个。",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (node.materials.isEmpty()) Text("暂无可用材料", style = MaterialTheme.typography.bodyMedium)
        GachaAdaptiveGrid(node.materials.size) { index ->
            val material = node.materials[index]
            GachaSourceCard(false) {
                GachaSourceBadge(material.badge)
                if (material.description.isNotBlank()) Text(material.description, style = MaterialTheme.typography.bodySmall)
                if (material.stock.isNotBlank()) Text(material.stock, style = MaterialTheme.typography.bodySmall)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.item)) {
                    Checkbox(selected[index] == true, onCheckedChange = { selected[index] = it; onDirty() }, enabled = enabled && !material.selector.disabled)
                    Text("选择", style = MaterialTheme.typography.labelMedium)
                    OutlinedTextField(amounts[index].orEmpty(), { value ->
                        if (value.isEmpty() || value.all(Char::isDigit)) {
                            val limit = material.quantity.max.toIntOrNull() ?: Int.MAX_VALUE
                            amounts[index] = value.toIntOrNull()?.coerceAtMost(limit)?.toString() ?: value
                            onDirty()
                        }
                    }, Modifier.weight(1f), shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp), singleLine = true, enabled = enabled && !material.quantity.disabled,
                        isError = selected[index] == true && (amounts[index]?.toIntOrNull()?.let { it >= (material.quantity.min.toIntOrNull() ?: 1) } != true),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), label = { Text("数量") })
                }
            }
        }
    }
    if (confirmAll) AlertDialog(onDismissRequest = { confirmAll = false }, title = { Text("一键逐级熔炼") },
        text = { Text("选择除反贼外的全部可用材料，依次熔炼 N → R → SR → SSR。保留源站要求的佩戴份数，失败后停止；已成功的消费无法撤销。") },
        confirmButton = { TextButton(onClick = { confirmAll = false; forgeAll?.invoke() }) { Text("开始熔炼") } },
        dismissButton = { TextButton(onClick = { confirmAll = false }) { Text("取消") } })
    pending?.let { (form, pairs, summary) ->
        AlertDialog(onDismissRequest = { pending = null }, title = { Text("确认称号操作") }, text = { Text(summary) },
            confirmButton = { TextButton(onClick = { pending = null; onSubmit(form, pairs) }) { Text("确认") } },
            dismissButton = { TextButton(onClick = { pending = null }) { Text("取消") } })
    }
}
