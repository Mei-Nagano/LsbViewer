package sb.linux.client.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.UnfoldLess
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.RectF
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode
import sb.linux.client.data.Endpoints
import sb.linux.client.data.TopicCard
import sb.linux.client.LsbApp

/**
 * 帖子内容链接打开入口（常规设置「打开链接方式」，3.20）：
 * MainActivity 注入；站内链接路由到原生页面，其余按设置选择应用内 WebView 或外部浏览器。
 * 未注入时（预览/独立预览组件）回退系统浏览器。
 */
val LocalLinkHandler = staticCompositionLocalOf<((String) -> Unit)?> { null }

/**
 * 帖子卡片元素级自定义颜色（外观设置 → 实时预览点按改色）。
 * Session 加载/保存时同步；TopicCardView 渲染时读取，未覆盖元素跟随主题。
 */
object CardColorOverrides {
    const val TAG = "tag"         // 帖子标签（置顶/热/精华…）
    const val BACKGROUND = "background" // 帖子卡片背景容器
    const val TIME = "time"       // xx时间前
    const val USER = "user"       // 用户名
    const val VIEWS = "views"     // 浏览量
    const val COMMENTS = "comments" // 评论数
    const val FORUM = "forum"     // 所属板块

    var map by mutableStateOf<Map<String, Color>>(emptyMap())

    fun get(key: String): Color? = map[key]
}

/** 按亮度自动选择黑/白前景，保证任意背景色下可读 */
fun onColorFor(bg: Color): Color =
    if (bg.luminance() > 0.5f) Color.Black else Color.White

/**
 * 头像：一律显示服务端给出的 URL（HTML 解析结果），不做任何公式推导；URL 为空时显示占位图标。
 * 单通道加载：先走 fetchBytes（内存/磁盘缓存 + 并发去重 + 魔数校验），失败才回退 URL 直载；
 * 兜底直载禁用 Coil 磁盘缓存——验证盾可能返回 200 HTML，写入后会永久毒化该缓存键。
 */
/**
 * SVG 字节嗅探：<svg 开头，或 <?xml 声明且前 1KB 内含 <svg（与 Coil SvgDecoder 规则一致）。
 * 用于判定头像字节是否需要走 androidsvg 直接渲染。
 */
private fun isSvgContents(b: ByteArray): Boolean {
    val head = String(b, 0, minOf(1024, b.size), Charsets.UTF_8).trimStart()
    return head.startsWith("<svg") || (head.startsWith("<?xml") && head.contains("<svg"))
}

/**
 * 兼容性归一化：androidsvg(1.4) 的 `<use>`/`<image>` 只识别 xlink 命名空间的 href，
 * 不解析 SVG2 的裸 href。DiceBear bottts 头像正是用裸 `<use href="#id">` 引用 defs，
 * 若不改写，整张头像会因引用失败而渲染成空白。
 * 改写为 xlink:href 并补 xmlns:xlink 声明（已含 xlink:href 则原样返回）。
 */
private fun normalizeSvgHref(raw: String): String {
    if (!raw.contains("href=") || raw.contains("xlink:href")) return raw
    val svg = if (raw.contains("xmlns:xlink")) raw
    else raw.replaceFirst(
        Regex("""<svg\b"""),
        """<svg xmlns:xlink="http://www.w3.org/1999/xlink""""
    )
    return svg.replace("href=", "xlink:href=")
}

/**
 * 用 androidsvg 把 SVG 头像字节直接渲染成 px×px 的 Bitmap。
 * 绕开 coil-svg 的解码瓶颈——部分默认/bottts 头像（如 uid=1）在 coil-svg 下解码失败。
 * 渲染失败返回 null（调用方回退 Coil 原路径）。
 */
private fun renderSvgToBitmap(svg: ByteArray, px: Int): Bitmap? = runCatching {
    val txt = String(svg, Charsets.UTF_8)
    val doc = com.caverock.androidsvg.SVG.getFromInputStream(normalizeSvgHref(txt).byteInputStream())
    val bmp = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
    doc.renderToCanvas(Canvas(bmp), RectF(0f, 0f, px.toFloat(), px.toFloat()))
    bmp
}.getOrNull()

@Composable
fun Avatar(url: String, size: Int = 40, modifier: Modifier = Modifier, online: Boolean = false) {
    val context = LocalContext.current
    val app = context.applicationContext as LsbApp
    val target = url.trim()
    val px = with(LocalDensity.current) { size.dp.roundToPx() }
    // null=加载中；空数组=fetchBytes 失败（转 URL 兜底）；非空=字节就绪
    val fetch by produceState<ByteArray?>(null, target) {
        value = withContext(Dispatchers.IO) {
            if (target.isEmpty()) ByteArray(0)
            else runCatching { app.client.fetchBytes(target) }.getOrNull() ?: ByteArray(0)
        }
    }
    val b = fetch
    // SVG 头像：直接用 androidsvg 渲染成 Bitmap，不走 Coil；非 SVG / 未就绪为 null
    val svgBitmap by produceState<Bitmap?>(null, target, b, px) {
        value = if (b != null && b.isNotEmpty() && isSvgContents(b)) {
            withContext(Dispatchers.IO) { renderSvgToBitmap(b, px) }
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
            .size(size.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center
    ) {
        val svg = svgBitmap
        if (svg != null) {
            Image(
                bitmap = svg.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.size(size.dp)
            )
        } else if (model == null) {
            // 加载中 / 服务端未提供头像 URL：显示占位图标
            fallback()
        } else {
            // loading / error 均显示兜底图标，避免解码失败时"显示一下就没影"
            SubcomposeAsyncImage(
                model = model,
                contentDescription = null,
                modifier = Modifier.size(size.dp),
                loading = { fallback() },
                error = { fallback() }
            )
        }
        // 源站在线状态：右下角绿点（带描边，与源站头像上的 online-users-dot 一致）
        if (online) {
            val dot = (size * 0.30f).dp
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .size(dot + 2.dp)
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

@Composable
fun Badge(text: String, bg: Color, fg: Color) {
    Surface(shape = RoundedCornerShape(50), color = bg) {
        Text(
            text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = fg,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
        )
    }
}

/** 帖子卡片。compact = 单列精简模式：隐藏头像与回复数。
 *  onElementClick 非空时进入"预览改色"模式：点按各元素回调对应 CardColorOverrides key（标题不可改色）。 */
@Composable
fun TopicCardView(
    card: TopicCard,
    onClick: () -> Unit,
    onForumClick: (Long) -> Unit = {},
    compact: Boolean = false,
    views: Int = -1,
    showComments: Boolean = true,   // false 时不显示评论数（浏览历史只展示浏览时间）
    showForum: Boolean = true,      // false 时不显示板块徽标（浏览历史）
    onElementClick: ((key: String) -> Unit)? = null,
) {
    val titleColor = remember(card.titleColor) {
        runCatching { Color(android.graphics.Color.parseColor(card.titleColor)) }
            .getOrNull()?.takeIf { card.titleColor.isNotBlank() }
    }
    // 外观设置-实时预览的自定义元素颜色（未覆盖时跟随主题）
    val tagColor = CardColorOverrides.get(CardColorOverrides.TAG)
    val bgOverride = CardColorOverrides.get(CardColorOverrides.BACKGROUND)
    val timeOverride = CardColorOverrides.get(CardColorOverrides.TIME)
    val userOverride = CardColorOverrides.get(CardColorOverrides.USER)
    val commentsOverride = CardColorOverrides.get(CardColorOverrides.COMMENTS)
    val forumOverride = CardColorOverrides.get(CardColorOverrides.FORUM)
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = bgOverride
            ?: if (card.unread) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.22f)
            else MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 5.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = {
                // 预览改色模式：点按卡片空白处改背景色；正常模式打开帖子
                if (onElementClick != null) onElementClick(CardColorOverrides.BACKGROUND) else onClick()
            }),
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 9.dp)) {
            if (!compact) {
                Avatar(card.avatarUrl, 34, online = card.online)
                Spacer(Modifier.width(10.dp))
            }
            Column(Modifier.weight(1f)) {
                // 顶行：标题（含标签）占左侧，板块标识固定在卡片右上角
                Row(verticalAlignment = Alignment.Bottom) {
                    Column(Modifier.weight(1f)) {
                        // 帖子标签（置顶/热/精华/未读/抽奖/发卡）置于标题前面
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            modifier = Modifier.let { m ->
                                if (onElementClick != null)
                                    m.clip(RoundedCornerShape(6.dp)).clickable { onElementClick(CardColorOverrides.TAG) }
                                else m
                            }
                        ) {
                    if (tagColor != null) {
                        // 自定义标签色：统一应用（前景自动黑/白）
                        val fg = onColorFor(tagColor)
                        if (card.pinned) Badge("置顶", tagColor, fg)
                        if (card.hot) Badge("热", tagColor, fg)
                        if (card.featured) Badge("精华", tagColor, fg)
                        if (card.unread) Badge("未读", tagColor, fg)
                        if (card.lotteryStatus.isNotBlank()) Badge(card.lotteryStatus, tagColor, fg)
                        if (card.cardStatus.isNotBlank()) Badge(card.cardStatus, tagColor, fg)
                    } else {
                        if (card.pinned) Badge("置顶", MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer)
                        if (card.hot) Badge("热", MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer)
                        if (card.featured) Badge("精华", MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer)
                        if (card.unread) Badge("未读", MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.onPrimary)
                        if (card.lotteryStatus.isNotBlank()) {
                            val open = card.lotteryStatus.contains("开奖").not()
                            Badge(
                                card.lotteryStatus,
                                if (open) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                if (open) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                            )
                        }
                        if (card.cardStatus.isNotBlank()) {
                            val active = card.cardStatus.contains("结束").not()
                            Badge(
                                card.cardStatus,
                                if (active) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.outline,
                                if (active) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
                    Text(
                        card.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = if (card.pinned || card.featured) FontWeight.Bold else FontWeight.SemiBold,
                        // 标题颜色不可自定义，仅保留源站解析出的彩色标题
                        color = titleColor ?: Color.Unspecified,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = MaterialTheme.typography.titleSmall.lineHeight * 1.1f,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                    Spacer(Modifier.size(4.dp))
                    // 用户名 / 时间 / 评论数：不使用分隔点
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            card.authorName,
                            style = MaterialTheme.typography.labelSmall,
                            color = userOverride ?: MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .let { m ->
                                    if (onElementClick != null)
                                        m.clip(RoundedCornerShape(6.dp)).clickable { onElementClick(CardColorOverrides.USER) }
                                    else m
                                }
                        )
                        Text(
                            card.timeText,
                            style = MaterialTheme.typography.labelSmall,
                            color = timeOverride ?: MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.let { m ->
                                if (onElementClick != null)
                                    m.clip(RoundedCornerShape(6.dp)).clickable { onElementClick(CardColorOverrides.TIME) }
                                else m
                            }
                        )
                        if (!compact && showComments) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                modifier = Modifier.let { m ->
                                    if (onElementClick != null)
                                        m.clip(RoundedCornerShape(6.dp)).clickable { onElementClick(CardColorOverrides.COMMENTS) }
                                    else m
                                }
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.Chat, null,
                                    modifier = Modifier.size(12.dp),
                                    tint = commentsOverride ?: MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    "${card.replies}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = commentsOverride ?: MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                // 板块标识：固定在卡片右上角（与标题顶部对齐）
                if (showForum) {
                    Spacer(Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = forumOverride ?: MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f),
                        onClick = {
                            if (onElementClick != null) onElementClick(CardColorOverrides.FORUM)
                            else onForumClick(card.forumId)
                        }
                    ) {
                        Text(
                            card.forumName,
                            style = MaterialTheme.typography.labelSmall,
                            color = forumOverride?.let { onColorFor(it) }
                                ?: MaterialTheme.colorScheme.onSecondaryContainer,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }
    }
}
}

/** 分页条：圆形前后翻页按钮 + 宽页码胶囊，三者同尺寸 */
@Composable
fun PaginationBar(page: Int, totalPages: Int, onPage: (Int) -> Unit) {
    if (totalPages <= 1) return
    // 按钮与页码统一高度
    val btnSize = 36.dp
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 上一页：圆形按钮，与页码同高
        Surface(
            onClick = { onPage((page - 1).coerceAtLeast(1)) },
            enabled = page > 1,
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.size(btnSize)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    "上一页",
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        // 页码胶囊：加宽背景容器
        Surface(
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier
                .padding(horizontal = 10.dp)
                .height(btnSize)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(horizontal = 22.dp)
            ) {
                Text(
                    "$page",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    " / $totalPages",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        // 下一页：圆形按钮，与页码同高
        Surface(
            onClick = { onPage((page + 1).coerceAtMost(totalPages)) },
            enabled = page < totalPages,
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.size(btnSize)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    "下一页",
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun LoadingBox() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
fun ErrorBox(msg: String, onRetry: () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .padding(24.dp), contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.CloudOff, null,
                    Modifier.size(26.dp),
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            Text(
                msg,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )
            FilledTonalButton(onClick = onRetry) { Text("重试") }
        }
    }
}

@Composable
fun EmptyBox(text: String = "暂无内容") {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Inbox, null,
                    Modifier.size(26.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

/** 可点击链接的文本；支持 #楼层号 注解点击跳转 */
@Composable
fun LinkText(
    text: AnnotatedString,
    onOpenUrl: (String) -> Unit,
    modifier: Modifier = Modifier,
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.bodyMedium,
    fontWeight: FontWeight? = null,
    onFloor: (Int) -> Unit = {},
) {
    val layoutResult = remember { mutableStateOf<TextLayoutResult?>(null) }
    Text(
        text = text,
        style = style,
        fontWeight = fontWeight,
        modifier = modifier.pointerInput(text) {
            detectTapGestures { pos ->
                val res = layoutResult.value ?: return@detectTapGestures
                val offset = res.getOffsetForPosition(pos)
                // 优先响应 #楼层号 跳转，其次打开链接
                text.getStringAnnotations("FLOOR", offset, offset).firstOrNull()?.let {
                    it.item.toIntOrNull()?.let { f -> onFloor(f) }
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

// ---------------- HTML 内容渲染 ----------------

private data class ContentBlock(
    val annotated: AnnotatedString? = null,
    val imageUrl: String? = null,
    val imageUrls: List<String> = emptyList(), // 连续多图：合并为一个翻页块
    val isQuote: Boolean = false,
    val isCode: Boolean = false,
    val isDivider: Boolean = false,
    val isHeading: Boolean = false,
    val headingLevel: Int = 0,     // 标题级别 1~4，用于排版字号缩放
    val isList: Boolean = false,   // 列表项：悬挂缩进展示（对应源站 <ul>/<ol>）
    val indent: Int = 0,           // 嵌套列表层级
)

/**
 * 将帖子 HTML（服务端渲染的 Markdown 产物）转为 Compose 展示。
 * 支持：p/br、strong/em/del、a、code/pre、blockquote、ul/ol、img、h1-h4、hr、table。
 * 行间距统一：所有正文文本块使用相同 lineHeight，块间不再额外加间距（由行框半行距提供），
 * 避免段落间距与行间距不一致的问题（含 @用户 拆分出的多个段落）。
 */
@Composable
fun HtmlContent(html: String, modifier: Modifier = Modifier, onFloor: (Int) -> Unit = {}) {
    val linkColor = MaterialTheme.colorScheme.primary
    val codeBg = MaterialTheme.colorScheme.surfaceContainerHighest
    val blocks = remember(html) { parseHtmlToBlocks(html, linkColor, codeBg) }
    val uriHandler = LocalUriHandler.current
    // 常规设置「打开链接方式」处理入口（3.20）：未提供时回退外部浏览器
    val linkHandler = LocalLinkHandler.current
    // 图片点击放大：viewer = 该图片块的全部图片 URL + 初始下标
    var viewer by remember { mutableStateOf<Pair<List<String>, Int>?>(null) }
    fun open(url: String) {
        val abs = if (url.startsWith("http")) url else Endpoints.abs(url)
        if (linkHandler != null) linkHandler(abs)
        else runCatching { uriHandler.openUri(abs) }
    }
    // 统一行高：正文 1.7 倍行距（14sp × 1.7 ≈ 24sp），引用/小字 20sp
    val bodyStyle = MaterialTheme.typography.bodyMedium.copy(lineHeight = 24.sp)
    val quoteStyle = MaterialTheme.typography.bodySmall.copy(lineHeight = 20.sp)
    Column(modifier) {
        blocks.forEachIndexed { idx, b ->
            when {
                b.imageUrls.size > 1 -> ImagePager(b.imageUrls, Modifier.padding(vertical = 4.dp)) { viewer = b.imageUrls to it }
                b.imageUrl != null -> {
                    AsyncImage(
                        model = coil.request.ImageRequest.Builder(LocalContext.current)
                            .data(b.imageUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = "图片",
                        contentScale = androidx.compose.ui.layout.ContentScale.FillWidth,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .clickable { viewer = listOf(b.imageUrl) to 0 }
                    )
                }
                b.isDivider -> {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 5.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                }
                b.isCode -> CodeBlockView(b.annotated!!.text)
                b.isQuote -> {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.7f),
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Row(Modifier.padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 8.dp)) {
                            Box(
                                Modifier
                                    .width(3.dp)
                                    .height(24.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                b.annotated!!.text,
                                style = quoteStyle,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                b.annotated != null && b.annotated.text.isNotBlank() -> {
                    val (txtStyle, fontWeight) = when {
                        b.isHeading -> headingStyle(b.headingLevel) to FontWeight.Bold
                        b.isList -> bodyStyle.copy(
                            lineHeight = 24.sp,
                            textIndent = TextIndent(firstLine = 0.sp, restLine = 22.sp)
                        ) to FontWeight.Normal
                        else -> bodyStyle to FontWeight.Normal
                    }
                    val base = when {
                        b.isHeading -> Modifier.padding(top = 6.dp, bottom = 2.dp)
                        b.isList -> Modifier.padding(start = (6 * b.indent).dp, bottom = 1.dp)
                        // 相邻段落之间留出源站间距，避免正文连成一片
                        idx > 0 -> Modifier.padding(top = 4.dp)
                        else -> Modifier
                    }
                    LinkText(
                        text = b.annotated,
                        onOpenUrl = ::open,
                        onFloor = onFloor,
                        style = txtStyle,
                        fontWeight = fontWeight,
                        modifier = base
                    )
                }
            }
        }
    }
    // 图片放大查看（全屏缩放，可翻页，长按保存）
    viewer?.let { (urls, idx) ->
        ZoomImageViewer(urls = urls, initialIndex = idx) { viewer = null }
    }
}

/** 标题按级别缩放字号，还原源站 Markdown 标题层级视觉效果 */
@Composable
private fun headingStyle(level: Int): androidx.compose.ui.text.TextStyle {
    val t = MaterialTheme.typography
    return when (level) {
        1 -> t.headlineMedium
        2 -> t.headlineSmall
        3 -> t.titleLarge
        else -> t.titleMedium
    }.copy(lineHeight = 28.sp)
}

/**
 * 代码块容器：左上角「代码块」标题，右上角复制 / 展开折叠按钮。
 * 默认折叠（限高 + 纵向滚动），展开后完整显示（横向滚动）。
 */
@Composable
fun CodeBlockView(code: String) {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    var expanded by remember(code) { mutableStateOf(false) }
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Column {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 2.dp, top = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "代码块",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = {
                        clipboard.setText(AnnotatedString(code))
                        android.widget.Toast.makeText(context, "已复制代码", android.widget.Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        Icons.Filled.ContentCopy, "复制代码",
                        Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        if (expanded) Icons.Filled.UnfoldLess else Icons.Filled.UnfoldMore,
                        if (expanded) "折叠" else "展开",
                        Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            val text = @Composable {
                Text(
                    code,
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodySmall.copy(lineHeight = 20.sp)
                )
            }
            if (expanded) {
                Box(
                    Modifier
                        .padding(12.dp)
                        .horizontalScroll(rememberScrollState())
                ) { text() }
            } else {
                // 折叠态：限高 + 纵向滚动，长代码不占满屏幕
                Box(
                    Modifier
                        .heightIn(max = 200.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(12.dp)
                ) { text() }
            }
        }
    }
}

/**
 * 连续多图：占用一个图片位置，左右滑动翻页，右下角显示页码指示。
 * 点击任意图片进入全屏放大查看。
 * 容器高度：记住每张图已加载出的宽高比，切换时保持上一张的比例直到新图加载成功，
 * 避免容器先跳回默认比例再突变（边框突然变大又恢复）。
 */
@Composable
private fun ImagePager(urls: List<String>, modifier: Modifier = Modifier, onClick: (Int) -> Unit) {
    val pagerState = androidx.compose.foundation.pager.rememberPagerState { urls.size }
    val currentUrl = urls[pagerState.currentPage]
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
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable { onClick(pagerState.currentPage) }
    ) {
        androidx.compose.foundation.pager.HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            if (page == pagerState.currentPage) {
                SubcomposeAsyncImage(
                    model = currentUrl,
                    contentDescription = "图片 ${page + 1}/${urls.size}",
                    contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                    onSuccess = { state ->
                        val d = state.result.drawable
                        val w = d.intrinsicWidth
                        val h = d.intrinsicHeight
                        if (w > 0 && h > 0) {
                            val ratio = (w.toFloat() / h).coerceIn(0.4f, 3f)
                            aspect = ratio
                            aspects = aspects + (currentUrl to ratio)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                AsyncImage(
                    model = urls[page],
                    contentDescription = "图片 ${page + 1}/${urls.size}",
                    contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }
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
private fun ZoomImageViewer(urls: List<String>, initialIndex: Int, onDismiss: () -> Unit) {
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
                androidx.compose.material3.AlertDialog(
                    onDismissRequest = { if (!saving) saveTarget = null },
                    title = { Text("保存图片") },
                    text = { Text(if (saving) "正在保存…" else "将图片保存到系统相册？") },
                    confirmButton = {
                        androidx.compose.material3.TextButton(
                            enabled = !saving,
                            onClick = {
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
                            }
                        ) { Text("保存") }
                    },
                    dismissButton = {
                        androidx.compose.material3.TextButton(
                            enabled = !saving,
                            onClick = { saveTarget = null }
                        ) { Text("取消") }
                    },
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
@Composable
private fun ZoomableImage(url: String, onSingleTap: () -> Unit, onLongPress: () -> Unit = {}) {
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

private fun parseHtmlToBlocks(html: String, linkColor: Color, codeBg: Color): List<ContentBlock> {
    val root = runCatching { Jsoup.parseBodyFragment(html).body() }.getOrNull() ?: return emptyList()
    val blocks = mutableListOf<ContentBlock>()

    fun renderInlineTo(el: Element): AnnotatedString = renderInline(el, linkColor, codeBg)

    /** 去掉图片占位符"[图片]"（图片会单独成块展示）。
     *  删除时保留原有样式/链接注解与 <br> 产生的换行，避免正文被压成一行或链接失效。 */
    fun cleanImgPlaceholder(sb: AnnotatedString): AnnotatedString {
        val text = sb.text
        if (!text.contains("[图片]")) return sb
        val ph = "[图片]"
        val kept = StringBuilder(text.length)
        // oldIndex -> newIndex（被删除的占位符字符记为 -1）
        val newPos = IntArray(text.length) { -1 }
        var i = 0
        while (i < text.length) {
            if (text.startsWith(ph, i)) {
                i += ph.length
            } else {
                newPos[i] = kept.length
                kept.append(text[i])
                i++
            }
        }
        val out = kept.toString()
        // 清理后只剩空白（整段都是图片占位）：返回空串，
        // 让调用方 isNotBlank() 判空跳过；不能返回原串，否则占位符又漏回正文
        if (out.isBlank()) return AnnotatedString("")
        return buildAnnotatedString {
            append(out)
            sb.spanStyles.forEach { s ->
                val st = newPos.getOrNull(s.start) ?: return@forEach
                val en = newPos.getOrNull(s.end - 1)?.plus(1) ?: return@forEach
                if (st < en) addStyle(s.item, st, en)
            }
            sb.getStringAnnotations(0, sb.length).forEach { a ->
                val st = newPos.getOrNull(a.start) ?: return@forEach
                val en = newPos.getOrNull(a.end - 1)?.plus(1) ?: return@forEach
                if (st < en) addStringAnnotation(a.tag, a.item, st, en)
            }
        }
    }

    /** 去掉首尾空白（含 <br> 换行、图片内联占位空格）：
     *  段落中的图片抽离成独立块后，原本分隔文字与图片的空白失去意义，
     *  保留会在文字下方、图片上方留出大段异常空白（如 topic/14468） */
    fun trimEdges(sb: AnnotatedString): AnnotatedString {
        val text = sb.text
        val s = text.indexOfFirst { !it.isWhitespace() }
        if (s < 0) return AnnotatedString("")
        val e = text.indexOfLast { !it.isWhitespace() } + 1
        return if (s == 0 && e == text.length) sb else sb.subSequence(s, e)
    }

    fun renderBlock(el: Element): ContentBlock? {
        val tag = el.tagName().lowercase()
        return when {
            tag == "img" -> ContentBlock(imageUrl = abs(el))
            tag == "hr" -> ContentBlock(isDivider = true)
            tag == "pre" -> ContentBlock(annotated = buildAnnotatedString { append(el.wholeText()) }, isCode = true)
            tag == "blockquote" -> {
                val sb = renderInlineTo(el)
                if (sb.text.isBlank()) null else ContentBlock(annotated = sb, isQuote = true)
            }
            else -> {
                val imgs = el.select("img")
                val raw = renderInlineTo(el)
                // 图片抽离成独立块：文本首尾的 <br>/占位空格一并去掉，避免文字与图片间异常留白
                val sb = if (imgs.isNotEmpty()) trimEdges(cleanImgPlaceholder(raw)) else raw
                if (sb.text.isNotBlank() && imgs.isEmpty()) ContentBlock(annotated = sb)
                else null.also {
                    // 有图片的段落：先文本后图片
                    if (sb.text.isNotBlank()) blocks.add(ContentBlock(annotated = sb))
                    imgs.forEach { blocks.add(ContentBlock(imageUrl = abs(it))) }
                }
            }
        }
    }

    /** 递归收集列表项，支持无序/有序及嵌套，保留层级缩进 */
    fun collectList(listEl: Element, level: Int) {
        val ordered = listEl.tagName().lowercase() == "ol"
        listEl.children().forEachIndexed { i, child ->
            if (child.tagName().lowercase() == "li") {
                // 渲染列表项文本时去掉内嵌子列表，仅保留该项自身内容
                val liCopy = child.clone()
                liCopy.select("ul, ol").remove()
                val imgs = liCopy.select("img")
                val raw = renderInlineTo(liCopy)
                // 列表项同理：图片抽离后去掉文本首尾的 <br>/占位空格
                val sb = if (imgs.isNotEmpty()) trimEdges(cleanImgPlaceholder(raw)) else raw
                if (sb.text.isNotBlank()) {
                    val bullet = if (ordered) "${i + 1}." else "•"
                    blocks.add(
                        ContentBlock(
                            annotated = buildAnnotatedString { append(bullet); append(" "); append(sb) },
                            isList = true, indent = level
                        )
                    )
                }
                imgs.forEach { blocks.add(ContentBlock(imageUrl = abs(it))) }
                // 嵌套子列表（源站 markdown 缩进层级）
                child.children().forEach { sub ->
                    if (sub.tagName().lowercase() in setOf("ul", "ol")) collectList(sub, level + 1)
                }
            }
        }
    }

    fun collect(el: Element) {
        el.children().forEach { c ->
            when (c.tagName().lowercase()) {
                "p", "div", "section" -> renderBlock(c)?.let { blocks.add(it) }
                "blockquote", "pre", "hr" -> renderBlock(c)?.let { blocks.add(it) }
                "ul", "ol" -> collectList(c, 0)
                "h1", "h2", "h3", "h4", "h5" -> {
                    val sb = renderInlineTo(c)
                    if (sb.text.isNotBlank()) blocks.add(
                        ContentBlock(
                            annotated = sb,
                            isHeading = true,
                            headingLevel = c.tagName().substring(1).toIntOrNull() ?: 1
                        )
                    )
                }
                "img" -> blocks.add(ContentBlock(imageUrl = abs(c)))
                "table" -> {
                    // 表格按行拼接为 "列1 | 列2" 文本
                    c.select("tr").forEach { tr ->
                        val cells = tr.select("th, td").map { it.text().trim() }
                        if (cells.any { it.isNotBlank() }) {
                            blocks.add(
                                ContentBlock(
                                    annotated = buildAnnotatedString {
                                        withStyle(SpanStyle(fontWeight = if (tr.selectFirst("th") != null) FontWeight.Bold else null)) {
                                            append(cells.joinToString("  |  "))
                                        }
                                    },
                                    isCode = false
                                )
                            )
                        }
                    }
                }
                "br" -> {}
                else -> renderBlock(c)?.let { blocks.add(it) }
            }
        }
    }

    val hasBlock = root.children().any {
        it.tagName().lowercase() in setOf(
            "p", "div", "br", "blockquote", "pre", "ul", "ol",
            "h1", "h2", "h3", "h4", "h5", "img", "table", "section", "hr"
        )
    }
    if (!hasBlock) {
        val sb = renderInlineTo(root)
        if (sb.text.isNotBlank()) blocks.add(ContentBlock(annotated = sb))
    } else {
        collect(root)
    }
    // 连续多张图片合并为一个翻页块（中间仅隔空白/占位文本，无实际文字）
    val merged = mutableListOf<ContentBlock>()
    for (b in blocks) {
        if (b.imageUrl != null) {
            val last = merged.lastOrNull()
            if (last != null && last.imageUrl != null) {
                merged[merged.size - 1] = last.copy(imageUrls = last.imageUrls + b.imageUrl)
            } else {
                merged.add(b.copy(imageUrls = listOf(b.imageUrl)))
            }
        } else {
            merged.add(b)
        }
    }
    // @用户名#楼层号：给紧跟 @用户 链接之后的 #N 加楼层注解（点击应用内跳转对应楼层）
    return merged.map { b ->
        if (b.annotated != null) b.copy(annotated = annotateFloors(b.annotated!!)) else b
    }
}

/**
 * 给 AnnotatedString 中的 #楼层号 添加 FLOOR 注解。
 * 仅处理紧跟 @用户 链接（/user/N 且文本以 @ 开头）之后的 #N，避免误伤普通话题标签。
 */
private fun annotateFloors(s: AnnotatedString): AnnotatedString {
    val mentionRanges = s.getStringAnnotations("URL", 0, s.length).filter { a ->
        Regex("""/user/\d+""").containsMatchIn(a.item) &&
            s.text.substring(a.start, a.end).startsWith("@")
    }
    if (mentionRanges.isEmpty()) return s
    val floorRanges = mutableListOf<Triple<Int, Int, String>>() // start, end, item
    for (m in mentionRanges) {
        val rest = s.text.substring(m.end)
        // 仅匹配开头（允许前导空白），避免误伤正文中段的 #N
        val mm = Regex("""^[\s\u00A0]*#\d+""").find(rest) ?: continue
        floorRanges.add(
            Triple(
                m.end + mm.range.first,
                m.end + mm.range.last + 1,
                mm.value.trim().removePrefix("#")
            )
        )
    }
    if (floorRanges.isEmpty()) return s
    return buildAnnotatedString {
        append(s.text)
        s.spanStyles.forEach { addStyle(it.item, it.start, it.end) }
        s.getStringAnnotations(0, s.length).forEach { addStringAnnotation(it.tag, it.item, it.start, it.end) }
        floorRanges.forEach { (st, en, item) -> addStringAnnotation("FLOOR", item, st, en) }
    }
}

private fun abs(el: Element): String =
    el.attr("abs:src").ifBlank { el.attr("src") }.let { if (it.startsWith("http")) it else Endpoints.abs(it) }

private fun renderInline(el: Element, linkColor: Color, codeBg: Color): AnnotatedString = buildAnnotatedString {
    fun appendNode(node: Node, bold: Boolean, italic: Boolean, strike: Boolean, code: Boolean, link: String?) {
        when (node) {
            is TextNode -> {
                val text = node.text()
                if (text.isNotBlank()) {
                    val start = length
                    withStyle(
                        SpanStyle(
                            fontWeight = if (bold) FontWeight.Bold else null,
                            fontStyle = if (italic) FontStyle.Italic else null,
                            textDecoration = if (strike) androidx.compose.ui.text.style.TextDecoration.LineThrough else null,
                            fontFamily = if (code) FontFamily.Monospace else null,
                            background = if (code) codeBg else Color.Unspecified,
                            color = if (link != null) linkColor else Color.Unspecified,
                        )
                    ) { append(text) }
                    if (link != null) {
                        addStringAnnotation("URL", link, start, length)
                        // 楼层链接（/topic/xxx?floor=N）：额外打 FLOOR 注解，点击在当前帖内跳转
                        Regex("""[?&]floor=(\d+)""").find(link)?.let { m ->
                            addStringAnnotation("FLOOR", m.groupValues[1], start, length)
                        }
                    }
                }
            }
            is Element -> {
                val tag = node.tagName().lowercase()
                if (tag == "br") {
                    append("\n")
                    return
                }
                if (tag == "img") {
                    // 内联图片只占一个空格位，图片本体由 select("img") 另行成块展示；
                    // 不再写入 "[图片]" 占位文本，避免未被清理的路径把占位符露出来。
                    append(" ")
                    return
                }
                val newLink = if (tag == "a") node.attr("abs:href").ifBlank { node.attr("href") }.ifBlank { null } else link
                node.childNodes().forEach {
                    appendNode(
                        it,
                        bold || tag == "strong" || tag == "b",
                        italic || tag == "em" || tag == "i",
                        strike || tag == "del" || tag == "s",
                        code || tag == "code",
                        newLink
                    )
                }
            }
            else -> {}
        }
    }
    el.childNodes().forEach { appendNode(it, false, false, false, false, null) }
}
