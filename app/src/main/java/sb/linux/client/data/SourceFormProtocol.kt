package sb.linux.client.data

/**
 * 源站动态表单协议辅助方法。
 *
 * 熔炼和回收把一种称号拆成一个 checkbox 与一个 `*_quantities[id]` 数量字段；
 * 两者必须作为一个选择项处理，不能把 checkbox 自身误当成“最多一枚”。
 */
internal object SourceFormProtocol {
    fun parseInteger(raw: String): Int? {
        var result = 0L
        var foundDigit = false
        raw.forEach { char ->
            val digit = char.digitToIntOrNull() ?: return@forEach
            foundDigit = true
            result = result * 10 + digit
            if (result > Int.MAX_VALUE) return null
        }
        return result.toInt().takeIf { foundDigit }
    }

    fun reactionTiers(raw: String): List<Int> = raw
        .split(',', '，')
        .mapNotNull { it.trim().toIntOrNull() }
        .filter { it > 0 }
        .distinct()

    fun quantityFieldIndex(form: GachaOperationForm, choiceIndex: Int): Int? {
        val choice = form.fields.getOrNull(choiceIndex) ?: return null
        if (choice.type != "checkbox" || choice.value.isBlank()) return null
        val suffix = "[${choice.value}]"
        return form.fields.indices.firstOrNull { index ->
            val field = form.fields[index]
            field.type == "number" && field.name.endsWith(suffix) &&
                field.name.contains("quantit", ignoreCase = true)
        }
    }

    fun selectedChoiceCount(
        form: GachaOperationForm,
        values: Map<Int, String>,
        checked: Map<Int, Boolean>,
        radioValues: Map<String, String>,
        multiValues: Map<Int, Set<String>>,
    ): Int = form.fields.indices.sumOf { index ->
        val field = form.fields[index]
        when {
            field.type == "checkbox" && checked[index] == true -> {
                quantityFieldIndex(form, index)?.let { quantityIndex ->
                    boundedQuantity(form.fields[quantityIndex], values[quantityIndex])
                } ?: 1
            }
            field.type == "radio" && radioValues[field.name] == field.value -> 1
            field.type == "select" && field.multiple -> multiValues[index].orEmpty().size
            else -> 0
        }
    }

    /** 服务端提交参数中的选择总数量；checkbox 关联数量字段时按数量而不是种类计数。 */
    fun selectedChoiceCount(
        form: GachaOperationForm,
        pairs: List<Pair<String, String>>,
    ): Int {
        val checkboxCount = form.fields.indices.sumOf { index ->
            val field = form.fields[index]
            val submittedValue = field.value.ifBlank { "on" }
            if (field.type != "checkbox" || pairs.none { it.first == field.name && it.second == submittedValue }) {
                return@sumOf 0
            }
            quantityFieldIndex(form, index)?.let { quantityIndex ->
                val quantityField = form.fields[quantityIndex]
                boundedQuantity(quantityField, pairs.firstOrNull { it.first == quantityField.name }?.second)
            } ?: 1
        }
        val radioCount = form.fields.asSequence().filter { it.type == "radio" }.map { it.name }.distinct()
            .count { name -> pairs.any { it.first == name && it.second.isNotBlank() } }
        val selectCount = form.fields.filter { it.type == "select" }.sumOf { field ->
            val values = pairs.filter { it.first == field.name && it.second.isNotBlank() }
            if (field.multiple) values.size else values.take(1).size
        }
        return checkboxCount + radioCount + selectCount
    }

    fun buildSubmissionPairs(
        form: GachaOperationForm,
        values: Map<Int, String>,
        checked: Map<Int, Boolean>,
        radioValues: Map<String, String>,
        multiValues: Map<Int, Set<String>>,
    ): List<Pair<String, String>> {
        val selectedCount = selectedChoiceCount(form, values, checked, radioValues, multiValues)
        val hasForgeCount = form.hiddenFields.any { it.first == "forge_count" }
        val pairs = form.hiddenFields.filterNot { hasForgeCount && it.first == "forge_count" }.toMutableList()
        if (hasForgeCount) {
            val forgeCount = if (form.minSelections > 0) selectedCount / form.minSelections else 0
            pairs += "forge_count" to forgeCount.toString()
        }
        val quantityIndices = form.fields.indices.mapNotNull { quantityFieldIndex(form, it) }.toSet()
        form.fields.forEachIndexed { index, field ->
            when (field.type) {
                "checkbox" -> if (checked[index] == true) pairs += field.name to field.value.ifBlank { "on" }
                "radio" -> if (radioValues[field.name] == field.value) pairs += field.name to field.value
                "select" -> if (field.multiple) {
                    multiValues[index].orEmpty().forEach { pairs += field.name to it }
                } else {
                    pairs += field.name to values[index].orEmpty()
                }
                "number" -> pairs += field.name to if (index in quantityIndices) {
                    boundedQuantity(field, values[index]).toString()
                } else {
                    values[index].orEmpty()
                }
                else -> pairs += field.name to values[index].orEmpty()
            }
        }
        return pairs
    }

    fun boundedQuantity(field: GachaFormField, rawValue: String?): Int {
        val min = field.min.toIntOrNull() ?: 1
        val max = field.max.toIntOrNull()?.coerceAtLeast(min) ?: Int.MAX_VALUE
        return rawValue?.toIntOrNull()?.coerceIn(min, max) ?: min
    }
}

/** 源站评论投币在字段缺失时的兼容档位。 */
internal val DEFAULT_COMMENT_REWARD_TIERS = listOf(1, 5, 10, 50)
