package sb.linux.client.ui.patterns.user

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import org.dweb_browser.resvg_render.FitMode
import org.dweb_browser.resvg_render.RenderOptions
import org.dweb_browser.resvg_render.svgToPng
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import sb.linux.client.LsbApp



/**
 * 头像：一律显示服务端给出的 URL（HTML 解析结果），不做任何公式推导；URL 为空时显示占位图标。
 * 单通道加载：先走 fetchBytes（内存/磁盘缓存 + 并发去重 + 魔数校验），失败才回退 URL 直载；
 * 兜底直载禁用 Coil 磁盘缓存——验证盾可能返回 200 HTML，写入后会永久毒化该缓存键。
 */
/**
 * SVG 字节嗅探：<svg 开头，或 <?xml 声明且前 1KB 内含 <svg。
 * 用于判定头像字节是否需要走 resvg 直接渲染。
 */
private fun isSvgContents(b: ByteArray): Boolean {
    val head = String(b, 0, minOf(1024, b.size), Charsets.UTF_8).trimStart()
    return head.startsWith("<svg") || (head.startsWith("<?xml") && head.contains("<svg"))
}

/**
 * 用 resvg（Rust 渲染引擎，JNA 加载 native .so）把 SVG 头像字节渲染成 px×px 的 PNG
 * 再解码为 Bitmap。resvg 完整支持 SVG 1.1/2.0（mask-type、裸 href、缺 width/height、
 * CSS 等），旧方案为绕开 AndroidSVG 软肋做的 sanitize 清洗全部不再需要。
 * FitMode.CONTAIN：保持纵横比缩放进 px×px 画布（与旧 AndroidSVG 渲染行为一致）。
 * 渲染失败返回 null（调用方回退占位图标）。
 */
private fun renderSvgToBitmap(svg: ByteArray, px: Int): Bitmap? = runCatching {
    val png = svgToPng(svg, RenderOptions(px.toFloat(), px.toFloat(), null, FitMode.CONTAIN))
    BitmapFactory.decodeByteArray(png, 0, png.size)
}.onFailure { android.util.Log.e("ResvgRender", "SVG render failed (px=$px, bytes=${svg.size})", it) }
    .getOrNull()

/**
 * SVG 渲染结果缓存（url + 目标像素 → Bitmap）：
 * 头像列表项滚出视野后状态丢弃，滚回时若不缓存，同一 SVG 会经 JNA 反复重渲染，
 * 快速滑动时 CPU 开销显著。容量按个数计（头像 Bitmap 约 px*px*4 字节，64 个足够）。
 */
private val svgBitmapCache = android.util.LruCache<String, Bitmap>(64)

@Composable
fun Avatar(url: String, size: Int = 40, modifier: Modifier = Modifier, online: Boolean = false) {
    val context = LocalContext.current
    val app = context.applicationContext as LsbApp
    val target = url.trim()
    val px = with(LocalDensity.current) { size.dp.roundToPx() }
    // 首帧同步缓存窥探：字节已进内存缓存（此前展示过）时跳过 IO 协程调度，
    // 直接以就绪态组合——列表滚回视野的缓存命中头像不闪占位符、不掉帧
    val initialBytes = remember(target) {
        if (target.isEmpty()) ByteArray(0) else app.client.peekAvatarBytes(target)
    }
    // null=加载中；空数组=fetchBytes 失败（转 URL 兜底）；非空=字节就绪
    val fetch by produceState<ByteArray?>(initialBytes, target) {
        // produceState 的 value 不会因 key 变化而重置回 initialValue，所以不能用
        // 「value == null 才拉取」当守卫：同一个组合位切换到另一个 URL 时守卫会短路，
        // 把上一个头像的字节一直显示下去。这里按当前 target 重新求值——先同步窥探
        // 内存缓存（命中即无占位符闪烁），未命中才置空并走 IO。
        val peeked = if (target.isEmpty()) ByteArray(0) else app.client.peekAvatarBytes(target)
        if (peeked != null) {
            value = peeked
            return@produceState
        }
        value = null
        value = withContext(Dispatchers.IO) {
            runCatching { app.client.fetchBytes(target) }.getOrNull() ?: ByteArray(0)
        }
    }
    val b = fetch
    // SVG 头像：直接用 resvg 渲染成 Bitmap，不走 Coil；非 SVG / 未就绪为 null
    // 初始值同步查渲染缓存：滚回视野的 SVG 头像首帧直接出图
    val svgBitmap by produceState<Bitmap?>(
        initialValue = run {
            val key = "$target#$px"
            if (b != null && b.isNotEmpty() && isSvgContents(b)) svgBitmapCache.get(key) else null
        },
        target, b, px,
    ) {
        value = if (b != null && b.isNotEmpty() && isSvgContents(b)) {
            val key = "$target#$px"
            svgBitmapCache.get(key) ?: withContext(Dispatchers.IO) {
                renderSvgToBitmap(b, px)
            }?.also { svgBitmapCache.put(key, it) }
        } else null
    }

    // 加载中（b==null）不发起图片请求；字节就绪用字节渲染，fetchBytes 失败才用 URL 直载
    val model = remember(target, px, b) {
        if (target.isEmpty() || b == null) null
        else coil.request.ImageRequest.Builder(context)
            .data(
                if (b.isNotEmpty()) b
                else if (target.contains('?')) target.substringBefore('?')
                else target
            )
            .size(px)
            .memoryCacheKey(target)
            .diskCachePolicy(coil.request.CachePolicy.DISABLED)
            .crossfade(180)
            .build()
    }
    val fallback: @Composable () -> Unit = {
        Icon(
            Icons.Filled.Person,
            null,
            modifier = Modifier.size((size * 0.5f).dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    Box(
        modifier = modifier
            .size(size.dp),
        contentAlignment = Alignment.Center
    ) {
        // 图片内容层：圆角裁剪只作用于图片本身，让在线点的描边能延伸到头像外不被裁掉
        Box(
            Modifier
                .matchParentSize()
                .clip(MaterialTheme.shapes.extraSmall)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center
        ) {
            val svg = svgBitmap
            if (svg != null) {
                Image(
                    bitmap = svg.asImageBitmap(),
                    contentScale = ContentScale.Crop,
                    contentDescription = null,
                    modifier = Modifier.size(size.dp)
                )
            } else if (model == null) {
                // 加载中 / 服务端未提供头像 URL：显示占位图标
                fallback()
            } else {
                // 占位图标垫底 + 普通 AsyncImage 覆盖：加载中/失败时露出兜底图标，
                // 成功后图片盖在其上。不用 SubcomposeAsyncImage——每个头像一次子组合，
                // 列表快滑时开销显著（滚动掉帧的主因）
                Box(Modifier.size(size.dp), contentAlignment = Alignment.Center) {
                    fallback()
                    AsyncImage(
                        model = model,
                        contentScale = ContentScale.Crop,
                        contentDescription = null,
                        modifier = Modifier.size(size.dp),
                    )
                }
            }
        }
        // 源站在线状态：右下角绿点（带描边，与源站头像上的 online-users-dot 一致）
        // 置于头像内容层的上一层，不受 CircleShape 裁剪，描边可完整显示在头像容器外
        if (online) {
            val dot = (size * 0.30f).dp
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .size(dot + 3.dp)
                    .background(MaterialTheme.colorScheme.surface, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    Modifier
                        .size(dot)
                        .background(OnlineDotColor, CircleShape)
                )
            }
        }
    }
}

/** 在线状态绿点颜色（源站在线徽标同色系） */
val OnlineDotColor = Color(0xFF34C759)
