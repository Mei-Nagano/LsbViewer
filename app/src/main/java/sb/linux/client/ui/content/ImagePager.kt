package sb.linux.client.ui.content

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.Image
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import sb.linux.client.common.network.Endpoints
import sb.linux.client.LsbApp



@Composable
internal fun ImagePager(urls: List<String>, modifier: Modifier = Modifier, onClick: (Int) -> Unit) {
    val pagerState = androidx.compose.foundation.pager.rememberPagerState { urls.size }
    val currentUrl = urls.getOrNull(pagerState.currentPage) ?: urls.first()
    var aspects by remember { mutableStateOf(mapOf<String, Float>()) }
    // 当前比例：已知用已知值；未知先沿用上一张的比例（不重置），加载成功后更新
    var aspect by remember { mutableStateOf(1.15f) }
    LaunchedEffect(currentUrl) {
        aspects[currentUrl]?.let { aspect = it }
    }
    Box(
        Modifier
            .then(modifier)
            .fillMaxWidth()
            .aspectRatio(aspect)
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable { onClick(pagerState.currentPage) }
    ) {
        androidx.compose.foundation.pager.HorizontalPager(
            state = pagerState,
            beyondViewportPageCount = 1,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            BodyImage(urls[page], Modifier.fillMaxSize(), fillContainer = true, onAspectRatio = { actual ->
                val loadedRatio = actual.coerceIn(0.4f, 3f)
                aspects = aspects + (urls[page] to loadedRatio)
                if (page == pagerState.currentPage) aspect = loadedRatio
            }, onClick = { onClick(page) })
        }
        // 页码指示：当前页/总页数
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = Color(0x99000000),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(8.dp)
        ) {
            Text(
                "${pagerState.currentPage + 1} / ${urls.size}",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }
    }
}

/**
 * 全屏图片查看器：黑底，可左右翻页、双指缩放、单击关闭、双击放大、长按保存到本地。
 */
@Composable
internal fun ZoomImageViewer(urls: List<String>, initialIndex: Int, onDismiss: () -> Unit) {
    val pagerState = androidx.compose.foundation.pager.rememberPagerState(initialPage = initialIndex) { urls.size }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // 长按图片 → 保存确认弹窗
    var saveTarget by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .clipToBounds()
                .background(Color.Black)
        ) {
            androidx.compose.foundation.pager.HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                ZoomableImage(urls[page], onSingleTap = onDismiss, onLongPress = { saveTarget = urls[page] })
            }
            // 顶部关闭按钮
            Surface(
                shape = CircleShape,
                color = Color(0x66000000),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, "关闭", tint = Color.White)
                }
            }
            // 底部页码
            if (urls.size > 1) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0x99000000),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                ) {
                    Text(
                        "${pagerState.currentPage + 1} / ${urls.size}",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }
            // 长按图片保存确认弹窗
            saveTarget?.let { target ->
                sb.linux.client.ui.components.feedback.AppConfirmDialog(
                    title = "保存图片",
                    message = if (saving) "正在保存…" else "将图片保存到系统相册？",
                    onDismiss = { saveTarget = null },
                    onConfirm = {
                        saving = true
                        scope.launch {
                            val ok = saveImageToGallery(context, target)
                            saving = false
                            saveTarget = null
                            android.widget.Toast.makeText(
                                context,
                                if (ok) "已保存到相册" else "保存失败",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    confirmText = "保存",
                    enabled = !saving,
                )
            }
        }
    }
}

/** 下载网络图片并保存到系统相册（MediaStore Pictures/LinuxSB） */
private suspend fun saveImageToGallery(context: android.content.Context, url: String): Boolean =
    kotlinx.coroutines.withContext(Dispatchers.IO) {
        runCatching {
            val app = context.applicationContext as LsbApp
            val u = if (url.startsWith("http")) url else Endpoints.abs(url)
            val bytes = app.client.fetchBytes(u)
            if (bytes == null || bytes.isEmpty()) return@runCatching false
            val ext = Regex("""\.(png|jpe?g|gif|webp|svg)(\?|$)""").find(url.lowercase())
                ?.groupValues?.get(1)?.takeIf { it != "svg" } ?: "png"
            val mime = when (ext) {
                "jpg", "jpeg" -> "image/jpeg"
                "gif" -> "image/gif"
                "webp" -> "image/webp"
                else -> "image/png"
            }
            val name = "LinuxSB_${System.currentTimeMillis()}.$ext"
            val values = android.content.ContentValues().apply {
                put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, name)
                put(android.provider.MediaStore.Images.Media.MIME_TYPE, mime)
                put(
                    android.provider.MediaStore.Images.Media.RELATIVE_PATH,
                    android.os.Environment.DIRECTORY_PICTURES + "/LinuxSB"
                )
            }
            val uri = context.contentResolver.insert(
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values
            ) ?: return@runCatching false
            context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) } ?: return@runCatching false
            true
        }.getOrDefault(false)
    }

/** 支持双指缩放 / 双击放大 / 单击关闭 / 长按保存的单张图片。
 *  使用 detectTransformGestures 处理缩放+平移，自动消费双指手势避免父级 HorizontalPager 误翻页；
 *  未缩放（scale≈1）时单指左右滑动交给 Pager 翻页，双指捏合时接管缩放（可缩小到 0.5x、放大到 6x）。 */

