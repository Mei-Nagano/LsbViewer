package sb.linux.client.ui.patterns.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.abs

/** 圆点为连续滑条的视觉刻度，不改变原生连续拖动与无障碍语义。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null,
    trackBrush: Brush? = null,
) {
    val colors = SliderDefaults.colors()
    val gapColor = MaterialTheme.colorScheme.surfaceContainerLow
    val tickColor = MaterialTheme.colorScheme.onSurface
    val activeTickColor = if (enabled) colors.activeTickColor else colors.disabledActiveTickColor
    val inactiveTickColor = if (enabled) colors.inactiveTickColor else colors.disabledInactiveTickColor
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    Slider(
        value = value, onValueChange = onValueChange, modifier = modifier.fillMaxWidth(),
        enabled = enabled, valueRange = valueRange, steps = steps,
        onValueChangeFinished = onValueChangeFinished, colors = colors,
        thumb = {
            Box(Modifier.size(width = 4.dp, height = 44.dp).background(
                if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                CircleShape,
            ))
        },
        track = { state ->
            if (trackBrush != null) Canvas(Modifier.fillMaxWidth().height(16.dp)) {
                val fraction = ((value - valueRange.start) / (valueRange.endInclusive - valueRange.start)).coerceIn(0f, 1f)
                val x = size.width * if (rtl) 1f - fraction else fraction
                drawRoundRect(trackBrush, cornerRadius = CornerRadius(size.height / 2))
                for (index in 0..6) drawCircle(tickColor, radius = 2.dp.toPx(),
                    center = Offset(8.dp.toPx() + (size.width - 16.dp.toPx()) * index / 6, size.height / 2))
                drawRect(gapColor, topLeft = Offset(x - 6.dp.toPx(), 0f), size = Size(12.dp.toPx(), size.height))
            } else SliderDefaults.Track(
                sliderState = state, modifier = Modifier.height(16.dp).drawWithContent {
                    drawContent()
                    if (steps == 0) {
                        val fraction = ((state.value - valueRange.start) / (valueRange.endInclusive - valueRange.start)).coerceIn(0f, 1f)
                        val thumbX = size.width * if (rtl) 1f - fraction else fraction
                        val inset = size.height / 2
                        for (index in 0..10) {
                            val dotX = inset + (size.width - inset * 2).coerceAtLeast(0f) * index / 10
                            if (abs(dotX - thumbX) > 10.dp.toPx()) drawCircle(
                                color = if (if (rtl) dotX > thumbX else dotX < thumbX) activeTickColor else inactiveTickColor,
                                radius = 2.dp.toPx(), center = Offset(dotX, size.height / 2),
                            )
                        }
                    }
                }, enabled = enabled,
                colors = colors, thumbTrackGapSize = 6.dp,
                drawStopIndicator = {},
                drawTick = { offset, color -> drawCircle(color, 2.dp.toPx(), offset) },
            )
        },
    )
}
