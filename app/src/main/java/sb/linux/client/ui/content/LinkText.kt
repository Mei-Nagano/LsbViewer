package sb.linux.client.ui.content

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontWeight



@Composable
fun LinkText(
    text: AnnotatedString,
    onOpenUrl: (String) -> Unit,
    modifier: Modifier = Modifier,
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.bodyMedium,
    fontWeight: FontWeight? = null,
    onFloor: (Int) -> Unit = {},
    onAnchor: (String) -> Unit = {},
    searchQuery: String = "",
    onSearchPosition: (Float) -> Unit = {},
) {
    val layoutResult = remember { mutableStateOf<TextLayoutResult?>(null) }
    Text(
        text = text,
        style = style,
        fontWeight = fontWeight,
        modifier = modifier.onGloballyPositioned { coordinates ->
            val index = text.text.indexOf(searchQuery.trim(), ignoreCase = true)
            if (searchQuery.isNotBlank() && index >= 0) layoutResult.value?.let {
                onSearchPosition(coordinates.positionInRoot().y + it.getBoundingBox(index).top)
            }
        }.pointerInput(text) {
            detectTapGestures { pos ->
                val res = layoutResult.value ?: return@detectTapGestures
                val offset = res.getOffsetForPosition(pos)
                // 优先响应 #楼层号 跳转，其次打开链接
                text.getStringAnnotations("FLOOR", offset, offset).firstOrNull()?.let {
                    it.item.toIntOrNull()?.let { f -> onFloor(f) }
                    return@detectTapGestures
                }
                text.getStringAnnotations("ANCHOR", offset, offset).firstOrNull()?.let {
                    onAnchor(it.item)
                    return@detectTapGestures
                }
                text.getStringAnnotations("URL", offset, offset).firstOrNull()?.let {
                    onOpenUrl(it.item)
                }
            }
        },
        onTextLayout = { layoutResult.value = it }
    )
}

/** 阅读量展示：1.2w / 3500 */
internal fun formatViews(v: Int): String = when {
    v < 0 -> ""
    v >= 10000 -> {
        val w = v / 10000.0
        if (w >= 10) "${w.toInt()}w" else String.format(java.util.Locale.US, "%.1fw", w)
    }
    else -> v.toString()
}
