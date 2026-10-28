package sb.linux.client.ui.screens
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitDragOrCancellation
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import sb.linux.client.ui.theme.colorSeed
import sb.linux.client.ui.theme.colorToHsv
import sb.linux.client.ui.theme.hsvColor
import sb.linux.client.ui.util.onColorFor

@Composable
private fun HueGradientSlider(hue: Float, onValueChange: (Float) -> Unit, modifier: Modifier = Modifier) {
    Canvas(
        modifier
            .fillMaxWidth()
            .height(30.dp)
            .clip(RoundedCornerShape(16.dp))
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    down.consume()
                    onValueChange((down.position.x / size.width * 360f).coerceIn(0f, 360f))
                    while (true) {
                        val change = awaitDragOrCancellation(down.id) ?: break
                        change.consume()
                        onValueChange((change.position.x / size.width * 360f).coerceIn(0f, 360f))
                    }
                }
            }
    ) {
        // 常驻彩虹渐变背景：与色相值一一对应
        drawRect(
            Brush.horizontalGradient(
                listOf(0f, 60f, 120f, 180f, 240f, 300f, 360f).map { hsvColor(it, 1f, 1f) }
            )
        )
        // 当前位置圆形手柄
        val x = size.width * (hue.coerceIn(0f, 360f) / 360f)
        val cy = size.height / 2f
        drawCircle(Color.White, radius = size.height * 0.30f, center = Offset(x, cy))
        drawCircle(
            Color.Black, radius = size.height * 0.30f,
            center = Offset(x, cy), style = Stroke(size.height * 0.06f)
        )
    }
}

/** 统一的 HSV 取色对话框：SV 方块 + 彩虹色相滑条 + 预设色 + 实时 HEX，遵循 MD3 对话框规范 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ColorPickerDialog(
    title: String = "自定义主题色",
    initial: Color,
    presets: List<String> = emptyList(),
    onDismiss: () -> Unit,
    onPick: (Color) -> Unit,
    onSavePreset: ((Color) -> Unit)? = null,
    onRemovePreset: ((String) -> Unit)? = null,
    onReset: (() -> Unit)? = null,
) {
    val initHsv = colorToHsv(initial)
    var hue by remember { mutableFloatStateOf(initHsv[0]) }
    var sat by remember { mutableFloatStateOf(initHsv[1]) }
    var value by remember { mutableFloatStateOf(initHsv[2]) }
    val current = remember(hue, sat, value) { hsvColor(hue, sat, value) }

    fun applyColor(c: Color) {
        val hsv = colorToHsv(c)
        hue = hsv[0]; sat = hsv[1]; value = hsv[2]
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(20.dp)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(14.dp))

                val hueColor = hsvColor(hue, 1f, 1f)
                // SV 方块：横轴饱和度、纵轴亮度，可拖拽/点选（合并为单个手势循环，避免点按与拖动互相吞事件）
                Canvas(
                    Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                val down = awaitFirstDown()
                                down.consume()
                                fun apply(pos: Offset) {
                                    sat = (pos.x / size.width).coerceIn(0f, 1f)
                                    value = (1f - pos.y / size.height).coerceIn(0f, 1f)
                                }
                                apply(down.position)
                                while (true) {
                                    val change = awaitDragOrCancellation(down.id) ?: break
                                    change.consume()
                                    apply(change.position)
                                }
                            }
                        }
                ) {
                    drawRect(Brush.linearGradient(listOf(Color.White, hueColor)))
                    drawRect(
                        Brush.linearGradient(
                            listOf(Color.Transparent, Color.Black),
                            start = Offset(0f, 0f), end = Offset(0f, size.height)
                        )
                    )
                    val x = size.width * sat
                    val y = size.height * (1f - value)
                    drawCircle(Color.White, radius = size.minDimension * 0.05f, center = Offset(x, y))
                    drawCircle(
                        Color.Black, radius = size.minDimension * 0.035f,
                        center = Offset(x, y), style = Stroke(size.minDimension * 0.012f)
                    )
                }
                Spacer(Modifier.height(12.dp))
                // 色相滑条：常驻彩虹渐变背景，划到哪里显示对应颜色
                HueGradientSlider(hue, { hue = it })
                Spacer(Modifier.height(4.dp))
                // 实时预览 + 十六进制值
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(current)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            "当前颜色",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            current.hex(),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                // 预设颜色：点击应用，长按移除（保存的预设统一在此展示）
                if (presets.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "预设颜色（点击应用，长按移除）",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(6.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(presets.size) { i ->
                            val key = presets[i]
                            val c = colorSeed(key)
                            Box(
                                Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(c)
                                    .combinedClickable(
                                        onClick = { applyColor(c) },
                                        onLongClick = { onRemovePreset?.invoke(key) }
                                    )
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    key.removePrefix("custom#").take(4),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 8.sp,
                                    color = sb.linux.client.ui.util.onColorFor(c)
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
                // 操作按钮：取消 / 保存到预选 / 应用
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onDismiss) { Text("取消") }
                    Spacer(Modifier.weight(1f))
                    if (onSavePreset != null) {
                        OutlinedButton(onClick = { onSavePreset(current) }) { Text("存为预选") }
                        Spacer(Modifier.width(8.dp))
                    }
                    Button(onClick = { onPick(current) }) { Text("应用") }
                }
                if (onReset != null) {
                    Spacer(Modifier.height(4.dp))
                    Row(Modifier.fillMaxWidth()) {
                        TextButton(onClick = onReset) { Text("重置为跟随主题") }
                    }
                }
            }
        }
    }
}

/** 把 Color 格式化为 #RRGGBB（Compose sRGB 编码在 value 高 32 位，必须经 toArgb 取色） */
internal fun Color.hex(): String {
    val rgb = (toArgb().toLong() and 0xFFFFFFL)
    return "#" + rgb.toString(16).padStart(6, '0').uppercase()
}

/** 重置按钮（3.9）：右上角图标 + 简洁确认对话框 */
