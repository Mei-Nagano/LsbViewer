package sb.linux.client.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Composable
fun PinnedTopicCapsule(id: Long, title: String, onOpen: () -> Unit) {
    var x by rememberSaveable(id) { mutableFloatStateOf(Float.NaN) }
    var y by rememberSaveable(id) { mutableFloatStateOf(Float.NaN) }
    var expanded by rememberSaveable(id) { mutableStateOf(false) }
    var docked by rememberSaveable(id) { mutableStateOf(true) }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val width = with(density) { (if (expanded) 240.dp else 104.dp).toPx() }
        val height = with(density) { 48.dp.toPx() }
        val maxX = constraints.maxWidth.toFloat()
        val maxY = (constraints.maxHeight - height - with(density) { 90.dp.toPx() }).coerceAtLeast(0f)
        val px = if (x.isNaN()) maxX - width / 2 else x.coerceIn(-width / 2, maxX - width / 2)
        val py = if (y.isNaN()) maxY else y.coerceIn(0f, maxY)
        Surface(
            shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.primaryContainer,
            shadowElevation = 6.dp,
            modifier = Modifier.offset { IntOffset(px.roundToInt(), py.roundToInt()) }
                .width(if (expanded) 240.dp else 104.dp).height(48.dp)
                .pointerInput(id, width, maxX, maxY) {
                    detectDragGestures(onDragStart = {
                        x = if (x.isNaN()) maxX - width / 2 else x.coerceIn(-width / 2, maxX - width / 2)
                        y = if (y.isNaN()) maxY else y.coerceIn(0f, maxY)
                        docked = false
                    }, onDragEnd = {
                        x = if (x + width / 2 < maxX / 2) -width / 2 else maxX - width / 2
                        docked = true
                    }) { change, delta ->
                        change.consume()
                        x = (x + delta.x).coerceIn(-width / 2, maxX - width / 2)
                        y = (y + delta.y).coerceIn(0f, maxY)
                    }
                }.clickable {
                    if (docked || !expanded) {
                        docked = false; expanded = true
                        x = px.coerceIn(0f, (maxX - with(density) { 240.dp.toPx() }).coerceAtLeast(0f))
                    } else onOpen()
                },
        ) {
            Row(Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Filled.PushPin, "钉住的帖子", Modifier.size(20.dp))
                Text(if (expanded) title else "已钉住", maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}
