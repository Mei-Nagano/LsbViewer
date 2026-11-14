package sb.linux.client.ui.patterns.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.ParentDataModifier
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import sb.linux.client.ui.theme.LocalAppSpacing

internal val LocalSettingsGrouped = staticCompositionLocalOf { false }

internal object SettingsItemData : ParentDataModifier {
    override fun Density.modifyParentData(parentData: Any?): Any = SettingsItemData
}

@Composable
internal fun settingsCardShape(): Shape =
    if (LocalSettingsGrouped.current) RoundedCornerShape(8.dp) else MaterialTheme.shapes.extraLarge

/** 按实际相邻设置项连接；说明、子分组和条件项不需要维护首尾计数。 */
@Composable
internal fun SettingsGroup(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val outer = MaterialTheme.shapes.extraLarge
    val inner = RoundedCornerShape(8.dp)
    val spacing = LocalAppSpacing.current
    CompositionLocalProvider(LocalSettingsGrouped provides true) {
        Layout(content = content, modifier = modifier.fillMaxWidth()) { measurables, constraints ->
            val items = measurables.map { it.parentData === SettingsItemData }
            val placeables = measurables.map { it.measure(constraints.copy(minHeight = 0)) }
            val gaps = placeables.indices.map { index ->
                when {
                    index == 0 -> 0
                    items[index] && items[index - 1] -> spacing.inline.roundToPx()
                    else -> spacing.contentGap.roundToPx()
                }
            }
            val height = placeables.sumOf { it.height } + gaps.sum()
            val width = constraints.constrainWidth(placeables.maxOfOrNull { it.width } ?: 0)
            layout(width, constraints.constrainHeight(height)) {
                var y = 0
                placeables.forEachIndexed { index, placeable ->
                    y += gaps[index]
                    if (items[index]) {
                        val first = index == 0 || !items[index - 1]
                        val last = index == items.lastIndex || !items[index + 1]
                        placeable.placeRelativeWithLayer(0, y) {
                            shape = outer.copy(
                                topStart = if (first) outer.topStart else inner.topStart,
                                topEnd = if (first) outer.topEnd else inner.topEnd,
                                bottomStart = if (last) outer.bottomStart else inner.bottomStart,
                                bottomEnd = if (last) outer.bottomEnd else inner.bottomEnd,
                            )
                            clip = true
                        }
                    } else placeable.placeRelative(0, y)
                    y += placeable.height
                }
            }
        }
    }
}
