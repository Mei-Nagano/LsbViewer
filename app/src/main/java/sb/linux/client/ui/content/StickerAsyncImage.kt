package sb.linux.client.ui.content

import android.graphics.ImageDecoder
import android.graphics.drawable.AnimatedImageDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.widget.ImageView
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import sb.linux.client.LsbApp
import sb.linux.client.common.network.Endpoints
import java.nio.ByteBuffer

private val ANIMATABLE_EXT = Regex("""\.(?:gif|webp)(?:$|[?#])""", RegexOption.IGNORE_CASE)

/** 表情包图片展示：API 28+ 自动用系统 ImageDecoder 播放 GIF / 动态 WebP，其余情况走 Coil AsyncImage。 */
@Composable
internal fun StickerAsyncImage(
    url: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
) {
    val context = LocalContext.current
    val resolvedUrl = remember(url) { Endpoints.abs(url) }
    val mayAnimate = remember(resolvedUrl) {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && ANIMATABLE_EXT.containsMatchIn(resolvedUrl)
    }
    var animatedDrawable by remember(resolvedUrl) { mutableStateOf<Drawable?>(null) }

    LaunchedEffect(resolvedUrl, mayAnimate) {
        if (!mayAnimate || Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return@LaunchedEffect
        val app = context.applicationContext as? LsbApp ?: return@LaunchedEffect
        val decoded = withContext(Dispatchers.IO) {
            runCatching {
                val bytes = app.client.fetchBytes(resolvedUrl)
                val source = ImageDecoder.createSource(ByteBuffer.wrap(bytes))
                ImageDecoder.decodeDrawable(source)
            }.getOrNull()
        }
        if (decoded is AnimatedImageDrawable) {
            animatedDrawable = decoded
        }
    }

    val drawable = animatedDrawable
    if (drawable != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        DisposableEffect(drawable) {
            (drawable as? AnimatedImageDrawable)?.start()
            onDispose { (drawable as? AnimatedImageDrawable)?.stop() }
        }
        AndroidView(
            modifier = modifier,
            factory = { ctx ->
                ImageView(ctx).apply {
                    scaleType = when (contentScale) {
                        ContentScale.Crop -> ImageView.ScaleType.CENTER_CROP
                        else -> ImageView.ScaleType.FIT_CENTER
                    }
                    this.contentDescription = contentDescription
                    setImageDrawable(drawable)
                }
            },
            update = { view ->
                if (view.drawable !== drawable) view.setImageDrawable(drawable)
                view.contentDescription = contentDescription
            },
        )
    } else {
        AsyncImage(
            model = ImageRequest.Builder(context).data(resolvedUrl).crossfade(false).build(),
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = modifier,
        )
    }
}
