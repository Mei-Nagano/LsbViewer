package sb.linux.client.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import sb.linux.client.domain.account.*
import sb.linux.client.ui.patterns.settings.SettingsCheckRow
import sb.linux.client.ui.patterns.settings.SettingsChoiceRow
import sb.linux.client.ui.theme.LocalAppSpacing
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

// 主操作为源站 submit，说明为次级；尺寸、容器和字阶继承应用 M3E token。
@Composable
internal fun NativeAccountForm(
    form: AccountForm, repository: AccountManagementRepository, enabled: Boolean,
    onDirty: () -> Unit, onSubmit: (AccountForm, List<Pair<String, String>>) -> Unit,
    hideDisabled: Boolean = false,
    bodyEditor: (@Composable (String, (String) -> Unit, Boolean, (Boolean) -> Unit, () -> Unit) -> Unit)? = null,
    onValuesChange: ((List<Pair<String, String>>) -> Unit)? = null,
    embedded: Boolean = false,
    compact: Boolean = false,
) {
    var captchaAnswer by remember(form) { mutableStateOf("") }
    var captchaRevision by remember(form) { mutableIntStateOf(0) }
    val verification = form.verification
    val verificationGateway = LocalAccountVerificationGateway.current
    val extraPrizes = remember(form.id, form.action) { mutableStateListOf<AccountField>() }
    val removedPrizes = remember(form.id, form.action) { mutableStateListOf<Int>() }
    val prizeTemplate = form.fields.filter { it.name.startsWith("lottery_prize_") }.distinctBy { it.name }
    val allFields = form.fields + extraPrizes.map { it.copy(disabled = prizeTemplate.firstOrNull()?.disabled == true) }
    val prizeRows = allFields.indices.filter { allFields[it].name == "lottery_prize_name[]" }.map { start ->
        (start until allFields.size).takeWhile { it == start || allFields[it].name != "lottery_prize_name[]" }
            .filter { allFields[it].name.startsWith("lottery_prize_") }
    }
    val removedIndices = prizeRows.filter { it.first() in removedPrizes }.flatten().toSet()
    val values = remember(form.id, form.action) { mutableStateMapOf<Int, String>().apply {
        form.fields.forEachIndexed { index, field ->
            this[index] = if (field.type == "select") field.options.firstOrNull { it.selected }?.value
                ?: field.options.firstOrNull { !it.disabled }?.value.orEmpty() else field.value
        }
    } }
    val checked = remember(form.id, form.action) { mutableStateMapOf<Int, Boolean>().apply { form.fields.forEachIndexed { i, f -> this[i] = f.checked } } }
    val radio = remember(form.id, form.action) { mutableStateMapOf<String, String>().apply { form.fields.filter { it.type == "radio" && it.checked }.forEach { this[it.name] = it.value } } }
    val multi = remember(form.id, form.action) { mutableStateMapOf<Int, Set<String>>().apply { form.fields.forEachIndexed { i, f -> this[i] = f.options.filter { it.selected }.map { it.value }.toSet() } } }
    val notifyValues by rememberUpdatedState(onValuesChange)
    LaunchedEffect(form.id, form.action, form.fields, extraPrizes.size, removedPrizes.toList()) {
        if (notifyValues == null) return@LaunchedEffect
        snapshotFlow {
            allFields.flatMapIndexed { index, field ->
                if (index in removedIndices) return@flatMapIndexed emptyList()
                if (field.name.isBlank() || field.type in setOf("password", "file", "hidden")) emptyList()
                else when (field.type) {
                    "checkbox" -> listOf(field.name to if (checked[index] == true) field.value else "")
                    "radio" -> if (radio[field.name] == field.value) listOf(field.name to field.value) else emptyList()
                    "select" -> if (field.multiple) multi[index].orEmpty().map { field.name to it }
                        .ifEmpty { listOf(field.name to "") }
                        else listOf(field.name to values[index].orEmpty())
                    else -> listOf(field.name to values[index].orEmpty())
                }
            }
        }.collect { notifyValues?.invoke(it) }
    }
    fun included(field: AccountField): Boolean {
        if (field.showWhenName.isBlank()) return true
        val controlIndex = form.fields.indexOfFirst { it.name == field.showWhenName }
        val control = form.fields.getOrNull(controlIndex) ?: return false
        val value = when (control.type) {
            "radio" -> radio[control.name].orEmpty()
            "checkbox" -> if (checked[controlIndex] == true) control.value else ""
            else -> values[controlIndex].orEmpty()
        }
        return (value == field.showWhenValue) != field.showWhenDifferent
    }
    var failure by remember(form) { mutableStateOf<String?>(null) }
    var confirm by remember(form) { mutableStateOf<Pair<AccountSubmit, List<Pair<String, String>>>?>(null) }
    var recipient by remember(form) { mutableStateOf<AccountRecipient?>(null) }
    var uploadedImage by remember(form) { mutableStateOf<String?>(null) }
    var localBusy by remember(form) { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val active = enabled && !localBusy
    val spacing = LocalAppSpacing.current
    fun submitButton(button: AccountSubmit) {
        if (!active || (!button.enabled && form.recipientLookup.isBlank()) || (form.recipientLookup.isNotBlank() && recipient == null)) return
        val pairs = buildList {
            form.hidden.forEach { (name, value) -> add(name to when {
                name == "recipient_id" && recipient != null -> recipient!!.id.toString()
                name == "image_url" && uploadedImage != null -> uploadedImage!!
                else -> value
            }) }
            allFields.forEachIndexed { i, f ->
                if (i in removedIndices || f.disabled || !included(f) || f.name.isBlank() || f.type == "file") return@forEachIndexed
                when (f.type) {
                    "checkbox" -> if (checked[i] == true) add(f.name to f.value.ifBlank { "on" })
                    "radio" -> if (radio[f.name] == f.value) add(f.name to f.value)
                    "select" -> if (f.multiple) multi[i].orEmpty().forEach { add(f.name to it) } else add(f.name to values[i].orEmpty())
                    else -> add(f.name to values[i].orEmpty())
                }
            }
            if (button.name.isNotBlank()) add(button.name to button.value)
        }
        if (verification != null && captchaAnswer.isBlank()) { failure = "请先完成人机验证"; return }
        val verifiedPairs = pairs.filterNot { it.first == (verification as? LoginVerification.Cap)?.fieldName } +
            when (verification) {
                is LoginVerification.Cap -> listOf(verification.fieldName to captchaAnswer)
                is LoginVerification.Native -> listOf("native_captcha_token" to verification.token, "native_captcha_answer" to captchaAnswer)
                else -> emptyList()
            }
        failure = validateAccountForm(form.copy(fields = allFields.filterIndexed { index, field -> index !in removedIndices && included(field) }), pairs)
        if (failure == null) {
            if (form.method == "post" && (form.confirmation.isNotBlank() || Regex("支付|购买|扩容|赠送|删除|回收|熔炼|合成|屏蔽|抽|托管").containsMatchIn(button.label))) confirm = button to verifiedPairs
            else onSubmit(form.copy(action = button.action.ifBlank { form.action }, method = button.method.ifBlank { form.method }), verifiedPairs)
        }
    }
    Surface(shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp), color = if (embedded) androidx.compose.ui.graphics.Color.Transparent else MaterialTheme.colorScheme.surfaceContainer) {
        Column((if (compact) Modifier else Modifier.fillMaxWidth())
            .then(if (embedded) Modifier else Modifier.padding(spacing.pageHorizontal)), verticalArrangement = Arrangement.spacedBy(spacing.contentGap)) {
            allFields.forEachIndexed { index, originalField ->
                if (index in removedIndices) return@forEachIndexed
                val prize = prizeRows.firstOrNull { index in it }
                val typeIndex = prize?.firstOrNull { allFields[it].name == "lottery_prize_type[]" }
                val field = if (originalField.name == "lottery_prize_value[]") originalField.copy(
                    label = when (values[typeIndex]) { "wallet" -> "每份烧饼"; "code" -> "兑换码（每行一个）"; "manual" -> "发奖说明"; else -> "每份积分" }
                ) else originalField
                if (!included(field)) return@forEachIndexed
                if (hideDisabled && field.disabled) return@forEachIndexed
                if (field.name == "lottery_prize_name[]") Row(Modifier.fillMaxWidth()) {
                    Text("奖品 ${prizeRows.indexOf(prize) + 1}", Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                    TextButton(shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp), enabled = active && prizeRows.count { it.first() !in removedPrizes } > 1, onClick = {
                        removedPrizes.add(index); onDirty()
                    }) { Text("删除奖品") }
                }
                if (field.name == "body" && bodyEditor != null) {
                    bodyEditor(values[index].orEmpty(), { values[index] = it; onDirty() }, active, { localBusy = it }, {
                        form.buttons.firstOrNull()?.let(::submitButton)
                    })
                    return@forEachIndexed
                }
                val editable = active && !field.disabled
                when (field.type) {
                    "datetime-local" -> SourceDateTimeField(field, values[index].orEmpty(), editable) { values[index] = it; onDirty() }
                    "checkbox" -> SettingsCheckRow(field.label, checked[index] == true, { checked[index] = it; onDirty() }, enabled = editable,
                        subtitle = if (field.visibilityControl) { if (checked[index] == true) "所有人可见" else "仅自己可见" } else "")
                    "radio" -> SettingsCheckRow(field.label, radio[field.name] == field.value, { radio[field.name] = field.value; onDirty() }, enabled = editable)
                    "select" -> if (field.multiple) {
                        field.options.forEach { option ->
                            SettingsCheckRow(option.label, option.value in multi[index].orEmpty(), { selected ->
                                multi[index] = if (selected) multi[index].orEmpty() + option.value else multi[index].orEmpty() - option.value
                                onDirty()
                            }, enabled = editable && !option.disabled)
                        }
                    } else SettingsChoiceRow(field.label, field.options.filterNot { it.disabled }.map { it.value to it.label }, values[index].orEmpty(),
                        { if (editable) { values[index] = it; onDirty() } }, enabled = editable)
                    "file" -> if (form.imageUpload.isNotBlank()) PromotionImageField(
                        repository, form, enabled = active, imageUrl = uploadedImage ?: form.hidden.firstOrNull { it.first == "image_url" }?.second.orEmpty(),
                        onBusy = { localBusy = it }, onUploaded = { uploadedImage = it; onDirty() }, onError = { failure = it },
                    ) else Text("${field.label}暂时无法上传，请刷新后重试", color = MaterialTheme.colorScheme.error)
                    else -> OutlinedTextField(
                        value = values[index].orEmpty(), onValueChange = { value ->
                            if (value.length <= field.maxLength) { values[index] = value; failure = null; if (field.name == "recipient") recipient = null; onDirty() }
                        }, enabled = editable, label = { Text(field.label) }, placeholder = { Text(field.placeholder) },
                        singleLine = field.type != "textarea", minLines = if (field.type == "textarea") 3 else 1,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth(),
                        visualTransformation = if (field.type == "password") PasswordVisualTransformation() else VisualTransformation.None,
                        keyboardOptions = KeyboardOptions(keyboardType = when (field.type) { "number" -> KeyboardType.Number; "url" -> KeyboardType.Uri; "email" -> KeyboardType.Email; else -> KeyboardType.Text }),
                    )
                }
            }
            if (prizeTemplate.isNotEmpty() && !prizeTemplate.first().disabled) OutlinedButton(enabled = active, onClick = {
                val start = allFields.size
                prizeTemplate.forEachIndexed { offset, field ->
                    val value = if (field.type == "number") "1" else if (field.type == "select") "points" else ""
                    extraPrizes.add(field.copy(value = value))
                    values[start + offset] = value
                }
                onDirty()
            }) { Text("添加奖品") }
            if (form.recipientLookup.isNotBlank()) {
                OutlinedButton(enabled = active, onClick = {
                    val query = form.fields.indexOfFirst { it.name == "recipient" }.let { values[it].orEmpty() }.trim()
                    if (query.isBlank()) { failure = "请输入接收用户"; return@OutlinedButton }
                    localBusy = true
                    scope.launch {
                        try { recipient = repository.resolveRecipient(form.recipientLookup, query); failure = null }
                        catch (cancelled: CancellationException) { throw cancelled }
                        catch (error: Exception) { failure = error.message ?: "用户查询失败" }
                        finally { localBusy = false }
                    }
                }) { Text("查询接收用户") }
                recipient?.let { Text("接收用户：${it.name} · UID ${it.id}", color = MaterialTheme.colorScheme.primary) }
            }
            if (verification is LoginVerification.Cap && verificationGateway != null) {
                CapVerificationWidget(verification, captchaRevision, verificationGateway,
                    onToken = { captchaAnswer = it }, onStatus = {}, onError = { failure = it; captchaAnswer = "" })
                TextButton(onClick = { captchaAnswer = ""; captchaRevision++ }) { Text("重新加载验证") }
            } else if (verification is LoginVerification.Native) OutlinedTextField(captchaAnswer, { captchaAnswer = it }, label = { Text(verification.question) })
            else if (verification != null) Text("人机验证暂不可用，请重新打开页面", color = MaterialTheme.colorScheme.error)
            form.notes.forEach { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            failure?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(spacing.item), verticalArrangement = Arrangement.spacedBy(spacing.inline)) {
                form.buttons.forEach { button ->
                    Button(shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp), enabled = active &&
                        (form.recipientLookup.isBlank() && button.enabled || form.recipientLookup.isNotBlank() && recipient != null) &&
                        (verification == null || captchaAnswer.isNotBlank()),
                        onClick = { submitButton(button) }) { Text(button.label) }
                }
            }
        }
    }
    confirm?.let { (button, pairs) ->
        AlertDialog(onDismissRequest = { confirm = null }, title = { Text(button.label) },
            text = { Text((listOf(form.confirmation).filter(String::isNotBlank) + form.notes + form.fields.filter { it.type == "radio" && (it.name to it.value) in pairs }.map { it.label } +
                listOfNotNull(recipient?.let { "接收用户：${it.name} · UID ${it.id}" })).joinToString("\n").ifBlank { "确认执行此操作？" }) },
            confirmButton = { TextButton(onClick = { confirm = null; onSubmit(form.copy(action = button.action.ifBlank { form.action }, method = button.method.ifBlank { form.method }), pairs) }) { Text("确认") } },
            dismissButton = { TextButton(onClick = { confirm = null }) { Text("取消") } })
    }
}

private fun validateAccountForm(form: AccountForm, pairs: List<Pair<String, String>>): String? {
    val positions = mutableMapOf<String, Int>()
    form.fields.filter { !it.disabled && it.type != "file" }.forEach { field ->
        val submitted = pairs.filter { it.first == field.name }.map { it.second }
        val actual = if (field.type in setOf("checkbox", "radio") || field.multiple) submitted else {
            val position = positions.getOrDefault(field.name, 0)
            positions[field.name] = position + 1
            listOf(submitted.getOrNull(position).orEmpty())
        }
        if (field.required && actual.none(String::isNotBlank)) return "请填写${field.label}"
        actual.forEach { value ->
            if (value.length > field.maxLength) return "${field.label}超过源站长度限制"
            if (field.type == "number" && value.isNotBlank()) {
                val number = value.toDoubleOrNull() ?: return "${field.label}需要数字"
                if (field.min.toDoubleOrNull()?.let { number < it } == true || field.max.toDoubleOrNull()?.let { number > it } == true) return "${field.label}超出源站允许范围"
            }
            if (field.type == "datetime-local" && value.isNotBlank()) {
                val time = runCatching { java.time.LocalDateTime.parse(value) }.getOrNull() ?: return "${field.label}需要有效日期与时间"
                val min = runCatching { java.time.LocalDateTime.parse(field.min) }.getOrNull()
                val max = runCatching { java.time.LocalDateTime.parse(field.max) }.getOrNull()
                if ((min != null && time < min) || (max != null && time > max)) return "${field.label}超出源站允许范围"
            }
        }
    }
    return null
}
