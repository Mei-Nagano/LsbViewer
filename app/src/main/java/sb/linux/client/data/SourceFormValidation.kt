package sb.linux.client.data

internal fun validateSourceForm(form: GachaOperationForm, pairs: List<Pair<String, String>>): String? {
    if (!form.enabled) return "源站暂不允许此操作"
    if (form.minSelections > 0) {
        val choiceNames = form.fields.filter { it.type in setOf("checkbox", "radio", "select") }.map { it.name }.toSet()
        val selected = pairs.count { it.first in choiceNames && it.second.isNotBlank() }
        if (selected < form.minSelections) return "请至少选择 ${form.minSelections} 项"
    }
    form.fields.distinctBy { it.name }.forEach { field ->
        val values = pairs.filter { it.first == field.name }.map { it.second }
        // 勾选类字段共用一个 name（熔炼的 title_ids 是几十个同名 checkbox）：
        // 源站给每个 checkbox 都带 required 时，逐个字段判空会要求「每一项都勾」，
        // 于是一个都没勾和只勾了几个都被拦下，按钮看起来点了没反应。
        // 这类字段只要整组至少有一个值就算满足，具体规则交给源站。
        val isChoice = form.fields.any { it.name == field.name && (it.type == "checkbox" || it.type == "radio") }
        if (field.required && values.none { it.isNotBlank() }) {
            return if (isChoice) "请至少选择一项${field.label.takeIf { it.length <= 12 }.orEmpty()}".trim()
            else "请填写${field.label}"
        }
        values.forEach { value ->
            if (value.length > field.maxLength) return "${field.label}最多 ${field.maxLength} 个字符"
            if (field.type == "number" && value.isNotBlank()) {
                val n = value.toDoubleOrNull()?.takeIf { it.isFinite() } ?: return "${field.label}须为数字"
                if (field.min.toDoubleOrNull()?.let { n < it } == true) return "${field.label}不得小于 ${field.min}"
                if (field.max.toDoubleOrNull()?.let { n > it } == true) return "${field.label}不得大于 ${field.max}"
            }
        }
    }
    return null
}
