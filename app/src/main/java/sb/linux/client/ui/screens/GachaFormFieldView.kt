package sb.linux.client.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import sb.linux.client.domain.gacha.GachaFormField

@Composable
internal fun GachaFormFieldView(
    index: Int,
    field: GachaFormField,
    enabled: Boolean,
    values: MutableMap<Int, String>,
    radioValues: MutableMap<String, String>,
    multiValues: MutableMap<Int, Set<String>>,
) {when (field.type) {
    "checkbox" -> Unit
    "radio" -> Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .clickable(enabled = enabled) { radioValues[field.name] = field.value },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = radioValues[field.name] == field.value, onClick = { radioValues[field.name] = field.value }, enabled = enabled)
        Text(field.optionText(), style = MaterialTheme.typography.bodyMedium)
    }
    // 选项做成独立选择卡：选中项带主色描边和浅色底，禁用项整体变淡。
    "select" -> Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            field.label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        field.options.forEach { option ->
            val optionEnabled = enabled && !option.disabled
            val selected = if (field.multiple) option.value in multiValues[index].orEmpty() else values[index] == option.value
            val choose = {
                if (field.multiple) multiValues[index] = multiValues[index].orEmpty().let { if (option.value in it) it - option.value else it + option.value }
                else values[index] = option.value
            }
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                else MaterialTheme.colorScheme.surfaceContainerHigh,
                border = if (selected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    Modifier.fillMaxWidth().clickable(enabled = optionEnabled) { choose() }
                        .padding(end = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (field.multiple) Checkbox(checked = selected, onCheckedChange = { choose() }, enabled = optionEnabled)
                    else RadioButton(selected = selected, onClick = { choose() }, enabled = optionEnabled)
                    Text(
                        option.label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (optionEnabled) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                    )
                }
            }
        }
    }
    else -> OutlinedTextField(
        value = values[index].orEmpty(),
        onValueChange = { values[index] = it.take(field.maxLength) },
        modifier = Modifier.fillMaxWidth(),
        enabled = enabled,
        shape = RoundedCornerShape(16.dp),
        label = { Text(field.label + if (field.required) " *" else "") },
        placeholder = field.placeholder.takeIf { it.isNotBlank() }?.let { p -> ({ Text(p) }) },
        minLines = if (field.type == "textarea") 3 else 1,
        maxLines = if (field.type == "textarea") 8 else 1,
        visualTransformation = if (field.type == "password") androidx.compose.ui.text.input.PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        keyboardOptions = KeyboardOptions(
            keyboardType = if (field.type == "number") androidx.compose.ui.text.input.KeyboardType.Number else androidx.compose.ui.text.input.KeyboardType.Text,
        ),
        supportingText = if (field.min.isNotBlank() || field.max.isNotBlank()) ({
            Text(listOf(field.min.takeIf { it.isNotBlank() }?.let { "最小 $it" }, field.max.takeIf { it.isNotBlank() }?.let { "最大 $it" }).filterNotNull().joinToString(" · "))
        }) else null,
    )
}
}
