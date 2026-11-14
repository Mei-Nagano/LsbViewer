package sb.linux.client.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import sb.linux.client.domain.account.*
import sb.linux.client.platform.verification.CapVerificationGateway

internal val LocalAccountVerificationGateway = staticCompositionLocalOf<CapVerificationGateway?> { null }
private val titleControlShape = RoundedCornerShape(12.dp)
private val titleInlineMeta = Regex("×\\s*\\d+|#[0-9]+|(?:(?:数量[：:]?|共|拥有)\\s*)?\\d+\\s*(?:个|份)")

internal fun isGachaWearForm(form: AccountForm): Boolean =
    android.net.Uri.parse(form.action).path in setOf("/gacha_equip", "/gacha_unequip")

@Composable
internal fun GachaGiftPanel(form: AccountForm, repository: AccountManagementRepository, enabled: Boolean,
    onDirty: () -> Unit, onSubmit: (AccountForm, List<Pair<String, String>>) -> Unit) {
    var expanded by remember(form) { mutableStateOf(false) }
    OutlinedButton(onClick = { expanded = !expanded }, enabled = enabled, shape = titleControlShape) { Text("赠送") }
    if (expanded) NativeAccountForm(form, repository, enabled, onDirty, onSubmit, embedded = true)
}

@Composable
internal fun GachaOwnedTitleCard(node: AccountNode.OwnedTitle, repository: AccountManagementRepository,
    enabled: Boolean, onDirty: () -> Unit, onSubmit: (AccountForm, List<Pair<String, String>>) -> Unit) {
    var giftOpen by remember(node) { mutableStateOf(false) }
    GachaSourceCard(node.equipped) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp),
            itemVerticalAlignment = Alignment.CenterVertically) {
            GachaSourceBadge(node.badge)
            node.meta.flatMap { text -> titleInlineMeta.findAll(text).map { it.value }.toList() }
                .distinct().forEach { Text(it, style = MaterialTheme.typography.labelLarge) }
        }
        if (node.description.isNotBlank()) Text(node.description, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        node.meta.map { it.replace(titleInlineMeta, "").trim(' ', '·') }
            .filter(String::isNotBlank).distinct().forEach { Text(it, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant) }
        GachaWearControls(node.actions, enabled, onDirty, onSubmit)
        node.gift?.let { form ->
            OutlinedButton(onClick = { giftOpen = !giftOpen }, enabled = enabled, shape = titleControlShape) { Text(if (giftOpen) "收起赠送" else "赠送") }
            if (giftOpen) NativeAccountForm(form, repository, enabled, onDirty, onSubmit, embedded = true)
        }
    }

}

/** 选择佩戴、切换、卸下同排；表单仍保留源站隐藏字段和实例选择。 */
@Composable
internal fun GachaWearControls(forms: List<AccountForm>, enabled: Boolean, onDirty: () -> Unit,
    onSubmit: (AccountForm, List<Pair<String, String>>) -> Unit) {
    var pending by remember(forms) { mutableStateOf<Pair<AccountForm, List<Pair<String, String>>>?>(null) }
    val selected = remember(forms) { mutableStateMapOf<String, String>() }
    var menu by remember(forms) { mutableStateOf<String?>(null) }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            forms.forEach { form ->
                form.fields.filter { it.type == "select" && !it.disabled }.forEach { field ->
                    val value = selected[form.id + field.name] ?: field.options.firstOrNull { it.selected }?.value
                        ?: field.options.firstOrNull()?.value.orEmpty()
                    Box(Modifier.weight(1f)) {
                        OutlinedButton(onClick = { menu = form.id + field.name }, enabled = enabled,
                            shape = titleControlShape, contentPadding = PaddingValues(horizontal = 8.dp), modifier = Modifier.fillMaxWidth()) {
                            Text(field.options.firstOrNull { it.value == value }?.label ?: "选择编号", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        DropdownMenu(menu == form.id + field.name, { menu = null }) {
                            field.options.filterNot { it.disabled }.forEach { option -> DropdownMenuItem(text = { Text(option.label) }, onClick = {
                                selected[form.id + field.name] = option.value; menu = null; onDirty()
                            }) }
                        }
                    }
                }
                form.buttons.forEach { button ->
                    FilledTonalButton(enabled = enabled && button.enabled, shape = titleControlShape,
                        contentPadding = PaddingValues(horizontal = 10.dp), onClick = {
                            val pairs = form.hidden + form.fields.filterNot { it.disabled }.flatMap { f ->
                                when (f.type) {
                                    "select" -> listOf(f.name to (selected[form.id + f.name] ?: f.options.firstOrNull { it.selected }?.value ?: f.options.firstOrNull()?.value.orEmpty()))
                                    "checkbox", "radio" -> if (f.checked) listOf(f.name to f.value) else emptyList()
                                    else -> listOf(f.name to f.value)
                                }
                            } + if (button.name.isNotBlank()) listOf(button.name to button.value) else emptyList()
                            pending = form.copy(action = button.action.ifBlank { form.action }) to pairs
                        }) { Text(button.label, maxLines = 1) }
                }
            }
            if (forms.isEmpty()) Text("当前不可佩戴", style = MaterialTheme.typography.bodySmall)
        }
    pending?.let { (form, pairs) -> AlertDialog(onDismissRequest = { pending = null }, title = { Text("变更佩戴称号") },
        text = { Text(form.confirmation.ifBlank { "确认此操作？绑定原编号的称号权益以源站规则为准。" }) },
        confirmButton = { TextButton(onClick = { pending = null; onSubmit(form, pairs) }) { Text("确认") } },
        dismissButton = { TextButton(onClick = { pending = null }) { Text("取消") } }) }
}

@Composable
internal fun GachaDrawActions(forms: List<AccountForm>, enabled: Boolean,
    onSubmit: (AccountForm, List<Pair<String, String>>) -> Unit) {
    var pending by remember(forms) { mutableStateOf<Pair<AccountForm, AccountSubmit>?>(null) }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        forms.forEach { form ->
            val button = form.buttons.firstOrNull() ?: return@forEach
            val label = button.label.replace(" 积分", "").replace("积分", "").replace("今日", "")
            FilledTonalButton(onClick = { pending = form to button }, enabled = enabled && button.enabled,
                modifier = Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 4.dp), shape = titleControlShape) {
                Text(label, style = MaterialTheme.typography.labelMedium, maxLines = 2)
            }
        }
    }
    pending?.let { (form, button) -> AlertDialog(onDismissRequest = { pending = null }, title = { Text(button.label) },
        text = { Text((listOf(form.confirmation) + form.notes).filter(String::isNotBlank).joinToString("\n").ifBlank { "确认抽取称号？" }) },
        confirmButton = { TextButton(onClick = { pending = null; onSubmit(form,
            form.hidden + if (button.name.isBlank()) emptyList() else listOf(button.name to button.value)) }) { Text("确认") } },
        dismissButton = { TextButton(onClick = { pending = null }) { Text("取消") } }) }
}
