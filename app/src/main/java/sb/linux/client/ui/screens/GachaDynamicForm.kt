package sb.linux.client.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import sb.linux.client.domain.gacha.GachaFormField
import sb.linux.client.domain.gacha.GachaOperationForm
import sb.linux.client.domain.gacha.SourceFormProtocol
import sb.linux.client.domain.gacha.validateSourceForm
import sb.linux.client.ui.*

/**
 * checkbox/radio 的显示文字。解析器已尽力从源站的称号徽章里取名字；万一仍然退化成字段名
 * （label == name），补上取值，避免几十行选项长得一模一样、无法分辨。
 */
internal fun GachaFormField.optionText(): String = when {
    label.isNotBlank() && label != name -> label
    value.isNotBlank() -> "$name #$value"
    else -> name
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun GachaDynamicForm(
    form: GachaOperationForm,
    enabled: Boolean,
    onSubmit: (List<Pair<String, String>>) -> Unit,
) {
    val values = remember(form) {
        mutableStateMapOf<Int, String>().also { map ->
            form.fields.forEachIndexed { index, field ->
                map[index] = if (field.type == "select") {
                    field.options.firstOrNull { it.selected && !it.disabled }?.value ?: field.options.firstOrNull { !it.disabled }?.value.orEmpty()
                } else field.value
            }
            form.fields.forEachIndexed { index, field ->
                if (field.type == "checkbox" && !field.checked) {
                    SourceFormProtocol.quantityFieldIndex(form, index)?.let { quantityIndex ->
                        map[quantityIndex] = form.fields[quantityIndex].min.ifBlank { "1" }
                    }
                }
            }
        }
    }
    val checked = remember(form) {
        mutableStateMapOf<Int, Boolean>().also { map ->
            form.fields.forEachIndexed { index, field -> map[index] = field.checked }
        }
    }
    val radioValues = remember(form) {
        mutableStateMapOf<String, String>().also { map ->
            form.fields.filter { it.type == "radio" && it.checked }.forEach { map[it.name] = it.value }
        }
    }
    val multiValues = remember(form) { mutableStateMapOf<Int, Set<String>>().also { map ->
        form.fields.forEachIndexed { index, field -> map[index] = field.options.filter { it.selected && !it.disabled }.map { it.value }.toSet() }
    } }
    var validationError by remember(form) { mutableStateOf<String?>(null) }
    val quantityFieldIndices = form.fields.indices.mapNotNull {
        SourceFormProtocol.quantityFieldIndex(form, it)
    }.toSet()
    val selectedChoiceCount = SourceFormProtocol.selectedChoiceCount(
        form, values, checked, radioValues, multiValues,
    )
    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            val checkboxIndices = form.fields.indices.filter { form.fields[it].type == "checkbox" }
            if (checkboxIndices.isNotEmpty()) {
                val groups = checkboxIndices.groupBy { index ->
                    form.fields[index].groupLabel.ifBlank {
                        form.fields[index].optionText()
                            .replace(Regex("""\s*[·|]?(?:#|No\.?|序号)\s*\d+.*$""", RegexOption.IGNORE_CASE), "")
                            .trim().ifBlank { form.fields[index].name }
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    groups.forEach { (groupLabel, indices) ->
                        fun selectedQuantity(index: Int): Int = if (checked[index] == true) {
                            SourceFormProtocol.quantityFieldIndex(form, index)?.let { quantityIndex ->
                                SourceFormProtocol.boundedQuantity(form.fields[quantityIndex], values[quantityIndex])
                            } ?: 1
                        } else 0
                        fun availableQuantity(index: Int): Int =
                            SourceFormProtocol.quantityFieldIndex(form, index)?.let { quantityIndex ->
                                form.fields[quantityIndex].max.toIntOrNull()?.coerceAtLeast(1) ?: 1
                            } ?: 1
                        val selectedCount = indices.sumOf(::selectedQuantity)
                        val availableCount = indices.sumOf(::availableQuantity)
                        var quantityText by remember(form, groupLabel) {
                            mutableStateOf(selectedCount.toString())
                        }
                        fun setSelectedCount(target: Int) {
                            var remaining = target.coerceIn(0, availableCount)
                            indices.forEach { index ->
                                val selected = minOf(remaining, availableQuantity(index))
                                val quantityIndex = SourceFormProtocol.quantityFieldIndex(form, index)
                                checked[index] = selected > 0
                                if (quantityIndex != null) {
                                    values[quantityIndex] = selected.coerceAtLeast(1).toString()
                                }
                                remaining -= selected
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (selectedCount > 0) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceContainerHigh,
                            border = if (selectedCount > 0) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(groupLabel, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                    Text(
                                        "已选 $selectedCount / 可选 $availableCount",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                FilledTonalIconButton(
                                    onClick = {
                                        val next = (selectedCount - 1).coerceAtLeast(0)
                                        setSelectedCount(next)
                                        quantityText = next.toString()
                                    },
                                    enabled = enabled && selectedCount > 0,
                                    modifier = Modifier.size(34.dp),
                                ) { Text("−", style = MaterialTheme.typography.titleMedium) }
                                OutlinedTextField(
                                    value = quantityText,
                                    onValueChange = { input ->
                                        val filtered = input.filter(Char::isDigit).take(6)
                                        quantityText = filtered
                                        filtered.toIntOrNull()?.let { requested ->
                                            val target = requested.coerceIn(0, availableCount)
                                            setSelectedCount(target)
                                            if (target != requested) quantityText = target.toString()
                                        } ?: setSelectedCount(0)
                                    },
                                    enabled = enabled,
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp),
                                    textStyle = MaterialTheme.typography.titleSmall.copy(
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    ),
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
                                    ),
                                    modifier = Modifier.width(76.dp),
                                )
                                FilledTonalIconButton(
                                    onClick = {
                                        val next = (selectedCount + 1).coerceAtMost(availableCount)
                                        setSelectedCount(next)
                                        quantityText = next.toString()
                                    },
                                    enabled = enabled && selectedCount < availableCount,
                                    modifier = Modifier.size(34.dp),
                                ) { Text("+", style = MaterialTheme.typography.titleMedium) }
                                    }
                        }
                    }
                }
            }
            form.fields.forEachIndexed { index, field ->
                if (index in quantityFieldIndices) return@forEachIndexed
                GachaFormFieldView(index, field, enabled, values, radioValues, multiValues)
            }
            // 校验提示放在按钮上方并带底色：原来是一行小字，长表单里在屏幕外，
            // 点了按钮没反应又看不到原因，跟「按钮点不了」难以区分。
            validationError?.let {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        it,
                        Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            }
            if (!form.enabled) {
                Text(
                    "源站当前禁用此操作",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Button(
                enabled = enabled && form.enabled && selectedChoiceCount >= form.minSelections,
                shape = RoundedCornerShape(50),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                onClick = {
                    val pairs = SourceFormProtocol.buildSubmissionPairs(
                        form, values, checked, radioValues, multiValues,
                    )
                    validationError = validateSourceForm(form, pairs)
                    if (validationError == null) onSubmit(pairs)
                },
                modifier = Modifier.align(Alignment.End),
            ) { Text(form.label) }
            if (form.minSelections > 0) {
                Text(
                    "已选择 $selectedChoiceCount/${form.minSelections}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.End),
                )
            }
        }
    }
}

// ---------------- 用户榜单 ----------------

internal val LeaderTabs = listOf(
    "points" to "富豪榜",
    "replies" to "评论榜",
    "topics" to "发帖榜",
    "donation" to "打赏榜",
)
