package sb.linux.client.ui.screens

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation

/** 设置表单统一使用完整圆角的 tonal 输入面，与菜单卡片共用主题形状。 */
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
) {
    TextField(value, onValueChange, modifier, enabled, readOnly, textStyle,
        label, placeholder, leadingIcon, trailingIcon, prefix, suffix, supportingText,
        isError, visualTransformation, keyboardOptions, keyboardActions, singleLine,
        maxLines, minLines, interactionSource, shape,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
        ),
    )
}
