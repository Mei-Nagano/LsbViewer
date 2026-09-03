package sb.linux.client.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Scale

/** 半屏预取窗口。返回 false 时不创建图片请求；大图跨越窗口仍算可见。 */
internal fun imageNearViewport(left: Float, top: Float, width: Int, height: Int, viewportWidth: Int, viewportHeight: Int): Boolean {
    if (width <= 0 || height <= 0 || viewportWidth <= 0 || viewportHeight <= 0) return false
    return left < viewportWidth * 1.5f && left + width > -viewportWidth * 0.5f &&
        top < viewportHeight * 1.5f && top + height > -viewportHeight * 0.5f
}

internal fun bodyImageDecodeWidth(width: Int): Int = width.coerceIn(1, 2048)

internal fun bodyImageStatus(near: Boolean, loading: Boolean, failed: Boolean): String? = when {
    failed -> "图片加载失败"
    !near -> "图片 · 即将加载"
    loading -> "图片加载中…"
    else -> null
}

private val bodyImageRatios = android.util.LruCache<String, Float>(256)
internal fun clearBodyImageLayoutCache() = bodyImageRatios.evictAll()

/** 正文与轮播共用：可见区附近才请求，按展示尺寸解码，失败可重试。 */
@Composable
internal fun BodyImage(
    url: String,
    modifier: Modifier = Modifier,
    fillContainer: Boolean = false,
    onAspectRatio: (Float) -> Unit = {},
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    val view = LocalView.current
    var near by remember(url) { mutableStateOf(false) }
    var widthPx by remember(url) { mutableIntStateOf(0) }
    var ratio by remember(url) { mutableFloatStateOf(bodyImageRatios.get(url) ?: 1.4f) }
    var retry by remember(url) { mutableIntStateOf(0) }
    var failed by remember(url) { mutableStateOf(false) }
    var loading by remember(url) { mutableStateOf(true) }
    val request = remember(url, widthPx, retry) {
        ImageRequest.Builder(context).data(url)
            .size(bodyImageDecodeWidth(widthPx), 4096).scale(Scale.FIT)
            .crossfade(false).build()
    }
    Box(
        modifier.fillMaxWidth()
            .then(if (fillContainer) Modifier.fillMaxSize() else Modifier.aspectRatio(ratio.coerceIn(0.02f, 20f)))
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
            .onGloballyPositioned { coordinates ->
                val position = coordinates.positionInWindow()
                widthPx = coordinates.size.width
                near = imageNearViewport(position.x, position.y, coordinates.size.width, coordinates.size.height, view.width, view.height)
            }
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (near && widthPx > 0) {
            // 移出预取范围时退出组合，Coil 取消不再需要的请求；已加载的磁盘/内存缓存保留。
            key(retry) {
                AsyncImage(model = request, contentDescription = "正文图片",
                    modifier = Modifier.matchParentSize(), contentScale = ContentScale.Fit,
                    onLoading = { loading = true; failed = false },
                    onSuccess = { state ->
                        loading = false; failed = false
                        val drawable = state.result.drawable
                        if (drawable.intrinsicWidth > 0 && drawable.intrinsicHeight > 0) {
                            val actual = drawable.intrinsicWidth.toFloat() / drawable.intrinsicHeight
                            ratio = actual; bodyImageRatios.put(url, actual); onAspectRatio(actual)
                        }
                    }, onError = { loading = false; failed = true })
            }
        }
        // 首帧也显示明确占位，不等网络请求创建后才显示进度；失败不留下空白区域。
        bodyImageStatus(near && widthPx > 0, loading, failed)?.let { label ->
            Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(if (failed) Icons.Filled.BrokenImage else Icons.Filled.Image,
                    contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(32.dp))
                Column(Modifier.weight(1f, fill = false)) {
                    Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (failed) TextButton(onClick = { failed = false; loading = true; retry++ }) { Text("重新加载") }
                    else Text("点击可打开图片查看器", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (near && loading && !failed) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            }
        }
    }
}
