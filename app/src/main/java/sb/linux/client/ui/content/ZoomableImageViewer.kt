package sb.linux.client.ui.content

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import android.graphics.Bitmap
import coil.compose.AsyncImage



@Composable
internal fun ZoomableImage(
    url: String,
    onSingleTap: () -> Unit,
    onLongPress: () -> Unit = {},
    bitmap: android.graphics.Bitmap? = null,   // 非空时直接渲染（SVG 头像经 resvg 归一化后的结果）
) {
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }

    Box(
        Modifier
            .fillMaxSize()
            .clipToBounds()
            .pointerInput(url) {
                detectTapGestures(
                    onTap = { onSingleTap() },
                    onLongPress = { onLongPress() },
                    onDoubleTap = {
                        if (scale > 1.05f) {
                            scale = 1f
                            offset = androidx.compose.ui.geometry.Offset.Zero
                        } else {
                            scale = 2.5f
                        }
                    }
                )
            }
            .pointerInput(url) {
                // detectTransformGestures 仅在双指时触发，自动消费事件阻止 Pager 翻页
                detectTransformGestures(panZoomLock = true) { _, pan, zoom, _ ->
                    val newScale = (scale * zoom).coerceIn(0.5f, 6f)
                    scale = newScale
                    if (newScale <= 1f) {
                        offset = androidx.compose.ui.geometry.Offset.Zero
                    } else {
                        offset = androidx.compose.ui.geometry.Offset(
                            offset.x + pan.x,
                            offset.y + pan.y
                        )
                    }
                }
            }
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationX = offset.x
                translationY = offset.y
            }
    ) {
        val bmp = bitmap
        if (bmp != null) {
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = "放大图片",
                contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // 无位图时回退网络 URL 直载
            AsyncImage(
                model = coil.request.ImageRequest.Builder(LocalContext.current)
                    .data(url)
                    .crossfade(true)
                    .build(),
                contentDescription = "放大图片",
                contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

