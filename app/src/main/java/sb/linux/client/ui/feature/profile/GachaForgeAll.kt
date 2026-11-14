package sb.linux.client.ui.feature.profile

import sb.linux.client.domain.account.*

/** 每次消费后读新库存；沿用源站 max 的佩戴保护，不把反贼作为材料。 */
internal suspend fun forgeAllTitles(repository: AccountManagementRepository, isCurrent: () -> Boolean,
    onMutation: () -> Unit, onStep: (AccountManagementPage, String) -> Unit) {
    var page = repository.load("/gacha_forge_center")
    var operations = 0
    while (isCurrent()) {
        val materials = materialNode(page.nodes) ?: break
        val choice = listOf("n", "r", "sr").firstNotNullOfOrNull { rarity ->
            val action = materials.actions.firstOrNull { it.source.equals(rarity, true) } ?: return@firstNotNullOfOrNull null
            val selected = materials.materials.filter { it.formId == action.form.id && !it.selector.disabled && !it.quantity.disabled && it.badge.name != "反贼" }
                .map { it to (it.quantity.max.toIntOrNull() ?: it.quantity.value.toIntOrNull() ?: 0).coerceIn(0, 1000) }
                .filter { it.second > 0 }
            val times = (selected.sumOf { it.second.toLong() } / action.cost.coerceAtLeast(1)).coerceAtMost(1000).toInt()
            if (times <= 0) null else Triple(action, selected, times)
        } ?: break
        check(operations++ < 64) { "已完成 64 批熔炼，请核对库存后继续" }
        val (action, selected, times) = choice
        val button = action.form.buttons.firstOrNull() ?: error("源站未提供熔炼操作")
        val pairs = action.form.hidden.filterNot { it.first == action.countName } +
            listOf(action.countName to times.toString()) + selected.flatMap { (material, count) ->
                listOf(material.selector.name to material.selector.value, material.quantity.name to count.toString())
            } + if (button.name.isBlank()) emptyList() else listOf(button.name to button.value)
        check(isCurrent()) { "账号已切换" }
        repository.submit(page.url, action.form, pairs)
        check(isCurrent()) { "账号已切换" }
        onMutation()
        page = repository.load("/gacha_forge_center")
        onStep(page, "已熔炼 ${action.source.uppercase()} → ${action.target.uppercase()} × $times")
    }
}

private fun materialNode(nodes: List<AccountNode>): AccountNode.TitleMaterials? = nodes.firstNotNullOfOrNull {
    when (it) {
        is AccountNode.TitleMaterials -> it
        is AccountNode.Group -> materialNode(it.children)
        is AccountNode.Section -> materialNode(it.children)
        else -> null
    }
}
