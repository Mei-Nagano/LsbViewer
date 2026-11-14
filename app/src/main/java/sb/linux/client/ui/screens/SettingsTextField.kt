package sb.linux.client.ui.screens

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.VisualTransformation
import sb.linux.client.ui.patterns.settings.settingsSearchAnchor
import sb.linux.client.ui.patterns.settings.settingsRowIcon

/** 输入在设置卡片内直接编辑，标签、说明、验证及密码转换沿用调用方。 */
@Composable
internal fun SettingsTextField(
    value: String, onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier, enabled: Boolean = true, readOnly: Boolean = false,
    textStyle: TextStyle = LocalTextStyle.current,
    label: (@Composable () -> Unit)? = null, placeholder: (@Composable () -> Unit)? = null,
    leadingIcon: (@Composable () -> Unit)? = null, trailingIcon: (@Composable () -> Unit)? = null,
    prefix: (@Composable () -> Unit)? = null, suffix: (@Composable () -> Unit)? = null,
    supportingText: (@Composable () -> Unit)? = null, isError: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = false, maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1, interactionSource: MutableInteractionSource? = null,
    shape: Shape = MaterialTheme.shapes.large,
    searchLabel: String = "",
    description: String = "",
) {
    val input: @Composable () -> Unit = {
        TextField(value, onValueChange,
            modifier = if (searchLabel.isBlank()) modifier else Modifier.fillMaxWidth()
                .padding(horizontal = sb.linux.client.ui.theme.LocalAppSpacing.current.pageHorizontal).padding(bottom = 12.dp),
            enabled = enabled, readOnly = readOnly, textStyle = textStyle,
            label = if (searchLabel.isBlank()) label else null, placeholder = placeholder,
            leadingIcon = leadingIcon, trailingIcon = trailingIcon,
            prefix = prefix, suffix = suffix, supportingText = supportingText, isError = isError,
            visualTransformation = visualTransformation, keyboardOptions = keyboardOptions, keyboardActions = keyboardActions,
            singleLine = singleLine, maxLines = maxLines, minLines = minLines, interactionSource = interactionSource, shape = shape,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = if (searchLabel.isBlank()) MaterialTheme.colorScheme.surfaceContainerHigh else Color.Transparent,
                unfocusedContainerColor = if (searchLabel.isBlank()) MaterialTheme.colorScheme.surfaceContainerHigh else Color.Transparent,
                disabledContainerColor = if (searchLabel.isBlank()) MaterialTheme.colorScheme.surfaceContainerHigh else Color.Transparent,
                focusedIndicatorColor = if (searchLabel.isBlank()) Color.Transparent else MaterialTheme.colorScheme.primary,
                unfocusedIndicatorColor = if (searchLabel.isBlank()) Color.Transparent else MaterialTheme.colorScheme.outlineVariant,
                disabledIndicatorColor = Color.Transparent,
            ))
    }
    if (searchLabel.isBlank()) input()
    else sb.linux.client.ui.patterns.settings.SettingsCard(modifier.settingsSearchAnchor(searchLabel)) {
        sb.linux.client.ui.components.layout.AppMenuRow(searchLabel, description, settingsRowIcon(searchLabel),
            enabled = enabled, prominent = true, searchable = false, trailing = {})
        input()
    }
}
