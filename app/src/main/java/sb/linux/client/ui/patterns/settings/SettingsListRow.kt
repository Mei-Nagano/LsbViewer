package sb.linux.client.ui.patterns.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import sb.linux.client.ui.components.layout.AppMenuRow

/** 所有层级共用设置卡片、行高、图标和文字层级。 */
@Composable
internal fun SettingsListRow(
    title: String, subtitle: String = "", modifier: Modifier = Modifier,
    icon: ImageVector? = settingsRowIcon(title), enabled: Boolean = true,
    titleWeight: FontWeight? = null, trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    SettingsCard(modifier) {
        AppMenuRow(title, subtitle, icon, enabled = enabled, prominent = true,
            titleWeight = titleWeight, trailing = trailing, onClick = onClick)
    }
}

@Composable
internal fun SettingsActionRow(title: String, onClick: () -> Unit, enabled: Boolean = true,
    modifier: Modifier = Modifier, subtitle: String = "") {
    SettingsListRow(title, subtitle, modifier, enabled = enabled, onClick = onClick)
}

/** 连续设置直接在卡片内编辑，整张卡片参与搜索定位。 */
@Composable
internal fun SettingsSliderRow(
    title: String, value: Float, onValueChange: (Float) -> Unit,
    valueText: String, subtitle: String = "", valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0, onValueChangeFinished: (() -> Unit)? = null,
) {
    SettingsCard(Modifier.settingsSearchAnchor(title)) {
        AppMenuRow(title, subtitle, settingsRowIcon(title), prominent = true, searchable = false, trailing = {
            Text(valueText, Modifier.widthIn(max = 128.dp), style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary, maxLines = 2, overflow = TextOverflow.Ellipsis)
        })
        SettingsSlider(value, onValueChange, valueRange = valueRange, steps = steps,
            onValueChangeFinished = onValueChangeFinished,
            modifier = Modifier.padding(horizontal = sb.linux.client.ui.theme.LocalAppSpacing.current.pageHorizontal)
                .padding(bottom = 12.dp))
    }
}
