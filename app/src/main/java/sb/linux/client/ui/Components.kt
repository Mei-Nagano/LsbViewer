package sb.linux.client.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.nativeCanvas
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
import android.graphics.BitmapFactory
import org.dweb_browser.resvg_render.FitMode
import org.dweb_browser.resvg_render.RenderOptions
import org.dweb_browser.resvg_render.svgToPng
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
import sb.linux.client.data.TitleBadge
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
        if (value == null) {
            value = withContext(Dispatchers.IO) {
                if (target.isEmpty()) ByteArray(0)
                else runCatching { app.client.fetchBytes(target) }.getOrNull() ?: ByteArray(0)
            }
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
        // 图片内容层：圆形裁剪只作用于图片本身，让在线点的描边能延伸到圆形外不被裁掉
        Box(
            Modifier
                .matchParentSize()
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
                // 占位图标垫底 + 普通 AsyncImage 覆盖：加载中/失败时露出兜底图标，
                // 成功后图片盖在其上。不用 SubcomposeAsyncImage——每个头像一次子组合，
                // 列表快滑时开销显著（滚动掉帧的主因）
                Box(Modifier.size(size.dp), contentAlignment = Alignment.Center) {
                    fallback()
                    AsyncImage(
                        model = model,
                        contentDescription = null,
                        modifier = Modifier.size(size.dp),
                    )
                }
            }
        }
        // 源站在线状态：右下角绿点（带描边，与源站头像上的 online-users-dot 一致）
        // 置于圆形内容层的上一层，不受 CircleShape 裁剪，描边可完整显示在圆形容器外
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

@Composable
fun Badge(text: String, bg: Color, fg: Color, small: Boolean = false) {
    Surface(shape = RoundedCornerShape(50), color = bg) {
        Text(
            text,
            style = if (small) MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp)
            else MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = fg,
            maxLines = 1,
            modifier = Modifier.padding(
                horizontal = if (small) 5.dp else 7.dp,
                vertical = if (small) 1.dp else 2.dp
            )
        )
    }
}

/** 称号稀有度配色（对齐源站 gacha-title-* 的深浅配色风格） */
fun titleRarityColor(rarity: String): Pair<Color, Color> {
    val r = rarity.uppercase().trim()
    return when {
        r.contains("UR") -> Color(0xFFFF3D6E) to Color(0xFFFFE3EA)   // 红
        r.contains("SSR") -> Color(0xFFFF8F00) to Color(0xFFFFF0D6)  // 橙金
        r.contains("SR") -> Color(0xFF9C27B0) to Color(0xFFF3E5F5)   // 紫
        r.contains("R") -> Color(0xFF1E88E5) to Color(0xFFE3F2FD)    // 蓝
        else -> Color(0xFF6B7280) to Color(0xFFE9ECEF)               // 灰（N/默认）
    }
}

/** 源站称号系统徽章（对齐源站 2026 新样式）：
 *  名称 = 稀有度色纯文本（无胶囊背景）+ 稀有度 = 细边框小标签；图标不再展示（源站已 display:none）。
 *  small=true 时缩小字号并收紧名称宽度，避免卡片/评论里称号过宽而换行。 */
@Composable
fun TitleBadgeView(title: TitleBadge, modifier: Modifier = Modifier, small: Boolean = false) {
    val (fg, _) = titleRarityColor(title.rarity)
    val style = if (small) MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp)
    else MaterialTheme.typography.labelSmall
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(if (small) 3.dp else 4.dp),
        modifier = modifier
    ) {
        Text(
            title.name,
            style = style,
            fontWeight = FontWeight.Medium,
            color = fg,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            // 小尺寸下收紧名称宽度，过长称号截断而不再撑大/换行
            modifier = if (small) Modifier.widthIn(max = 64.dp) else Modifier
        )
        // 稀有度：细边框小标签（源站 .gacha-title-rarity：1px 边框 + 透明背景 + 稀有度色）
        if (title.rarity.isNotBlank()) {
            Text(
                title.rarity,
                style = style,
                fontWeight = FontWeight.SemiBold,
                color = fg,
                maxLines = 1,
                modifier = Modifier
                    .border(
                        width = 1.dp,
                        color = fg.copy(alpha = 0.55f),
                        shape = RoundedCornerShape(4.dp)
                    )
                    .padding(horizontal = 3.dp)
            )
        }
    }
}

/** 帖子卡片。compact = 单列精简模式：隐藏头像与回复数。
 *  onElementClick 非空时进入"预览改色"模式：点按各元素回调对应 CardColorOverrides key（标题不可改色）。 */
@OptIn(ExperimentalLayoutApi::class)
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
            .padding(horizontal = 14.dp, vertical = 3.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = {
                // 预览改色模式：点按卡片空白处改背景色；正常模式打开帖子
                if (onElementClick != null) onElementClick(CardColorOverrides.BACKGROUND) else onClick()
            }),
    ) {
        Box {
            Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                if (!compact) {
                    Avatar(card.avatarUrl, 34, online = card.online)
                    Spacer(Modifier.width(8.dp))
                }
                Column(Modifier.weight(1f)) {
                    // 帖子标签（热/精华/未读/抽奖/发卡）置于标题前面；
                    // 置顶不再是文字标签——改为卡片右上角「置顶」水印（见卡片末尾）
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
                            if (card.hot) Badge("热", tagColor, fg)
                            if (card.featured) Badge("精华", tagColor, fg)
                            if (card.unread) Badge("未读", tagColor, fg)
                            if (card.lotteryStatus.isNotBlank()) Badge(card.lotteryStatus, tagColor, fg)
                            if (card.cardStatus.isNotBlank()) Badge(card.cardStatus, tagColor, fg)
                        } else {
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
                    Spacer(Modifier.size(3.dp))
                    // 底行：用户名 / 时间 / 评论数不使用分隔点，FlowRow 放不下时自动换行
                    //（避免称号徽章挤占导致用户名被截断）；板块标识固定在卡片右下角（4），
                    // FlowRow 占满剩余宽度，多行时徽标贴最末行右缘
                    Row(verticalAlignment = Alignment.Bottom) {
                        FlowRow(
                            verticalArrangement = Arrangement.Center,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                card.authorName,
                                style = MaterialTheme.typography.labelSmall,
                                color = userOverride ?: MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .let { m ->
                                        if (onElementClick != null)
                                            m.clip(RoundedCornerShape(6.dp)).clickable { onElementClick(CardColorOverrides.USER) }
                                        else m
                                    }
                            )
                            // 作者称号（源站称号系统徽章）：用 small 缩小比例，避免过宽/换行
                            card.titleBadge?.let {
                                TitleBadgeView(it, small = true)
                            }
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
                        // 板块标识：固定在卡片右下角（与作者信息底行对齐）
                        if (showForum) {
                            Spacer(Modifier.width(5.dp))
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
            // 置顶水印：右上角「置顶」两字——浅灰、细体无衬线、半透明、轻模糊、
            // -10° 微倾的平面 2D 水印标记（无边框/圆角框/图标/立体效果，字号小不遮挡
            // 主体内容；替代原线条图钉）。12sp + 更高不透明度，水印更大更清楚；
            // BlurMaskFilter 实现模糊，兼容全部 API 级别
            if (card.pinned) {
                val paint = remember { android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG) }
                Canvas(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 6.dp, end = 10.dp)
                        .size(36.dp, 20.dp)
                ) {
                    paint.textSize = 12.sp.toPx()
                    paint.typeface = android.graphics.Typeface.create("sans-serif-light", android.graphics.Typeface.NORMAL)
                    paint.color = android.graphics.Color.argb(115, 140, 140, 140)
                    paint.maskFilter = android.graphics.BlurMaskFilter(0.5.dp.toPx(), android.graphics.BlurMaskFilter.Blur.NORMAL)
                    // 文字画在画布中心，旋转围绕画布中心进行，倾斜后不偏出画布
                    val tw = paint.measureText("置顶")
                    val x = (size.width - tw) / 2f
                    val y = size.height / 2f - (paint.ascent() + paint.descent()) / 2f
                    rotate(-10f) {
                        drawContext.canvas.nativeCanvas.drawText("置顶", x, y, paint)
                    }
                }
            }
        }
    }
}

/** 分页条：单个加宽胶囊，前后翻页箭头并入胶囊两端（36） */
@Composable
fun PaginationBar(page: Int, totalPages: Int, onPage: (Int) -> Unit) {
    if (totalPages <= 1) return
    // 胶囊整体高度，两端箭头为等宽可点区域
    val h = 40.dp
    val normalTint = MaterialTheme.colorScheme.onSurface
    val disabledTint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.height(h)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // 上一页：胶囊内左端箭头（首页时禁用变淡）
                Box(
                    Modifier
                        .size(h)
                        .clip(CircleShape)
                        .then(
                            if (page > 1) Modifier.clickable { onPage((page - 1).coerceAtLeast(1)) }
                            else Modifier
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        "上一页",
                        modifier = Modifier.size(20.dp),
                        tint = if (page > 1) normalTint else disabledTint
                    )
                }
                Text(
                    "$page",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 6.dp)
                )
                Text(
                    " / $totalPages",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = 6.dp)
                )
                // 下一页：胶囊内右端箭头（末页时禁用变淡）
                Box(
                    Modifier
                        .size(h)
                        .clip(CircleShape)
                        .then(
                            if (page < totalPages) Modifier.clickable { onPage((page + 1).coerceAtMost(totalPages)) }
                            else Modifier
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        "下一页",
                        modifier = Modifier.size(20.dp),
                        tint = if (page < totalPages) normalTint else disabledTint
                    )
                }
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

/** 表格单元格数据（正文 <table> 解析产物） */
private data class TableCellData(
    val text: String,
    val isHeader: Boolean,
)

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
    val tableRows: List<List<TableCellData>> = emptyList(),  // 表格：按行列结构渲染
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
    // 解析结果全局缓存：楼层滚出视野后组合销毁，滚回时若只靠 remember 会
    // 在主线程重新跑 Jsoup 解析 + 富文本构建（长帖快滑的掉帧主因）。
    // 键带上主题链接/代码块颜色（解析产物中已烘焙颜色，换主题需重解析）
    val blocks = remember(html, linkColor, codeBg) {
        val key = "$linkColor|$codeBg|${html.hashCode()}.${html.length}"
        htmlBlockCache.get(key) ?: parseHtmlToBlocks(html, linkColor, codeBg)
            .also { htmlBlockCache.put(key, it) }
    }
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
                b.tableRows.isNotEmpty() -> TableView(b.tableRows)
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
 * 超过阈值长度才默认折叠（限高 + 纵向滚动）并显示折叠按钮；短代码完整显示且无折叠按钮（31）。
 */
@Composable
fun CodeBlockView(code: String) {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    // 超长判定：行数超过 12 行或字符超过 600 才视为长代码
    val longCode = remember(code) {
        code.count { it == '\n' } + 1 > 12 || code.length > 600
    }
    var expanded by remember(code) { mutableStateOf(!longCode) }
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
                // 折叠按钮仅长代码显示（31）
                if (longCode) {
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
            }
            val text = @Composable {
                Text(
                    code,
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodySmall.copy(lineHeight = 20.sp)
                )
            }
            if (expanded) {
                // 展开态不再横向滚动：长行自动换行，完整显示无需滑动
                Box(Modifier.padding(12.dp)) { text() }
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
 * 正文表格：按行列结构渲染的网格。
 * 列宽均分（weight），单元格文本自动换行，超宽表格无需横向滑动；
 * 表头行（th）加粗并加背景色区分。
 */
@Composable
private fun TableView(rows: List<List<TableCellData>>) {
    val colCount = rows.maxOf { it.size }
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f),
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Column(Modifier.padding(6.dp)) {
            rows.forEachIndexed { rIdx, row ->
                // 首个含表头的行视为表头行，整行加背景
                val isHeaderRow = row.any { it.isHeader }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isHeaderRow) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                            else Color.Transparent
                        )
                ) {
                    repeat(colCount) { cIdx ->
                        val cell = row.getOrNull(cIdx)
                        Box(
                            Modifier
                                .weight(1f)
                                .padding(horizontal = 5.dp, vertical = 4.dp)
                        ) {
                            if (!cell?.text.isNullOrBlank()) {
                                Text(
                                    cell!!.text,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = if (cell.isHeader) FontWeight.Bold else null,
                                    color = if (cell.isHeader) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
                if (rIdx < rows.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 2.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                    )
                }
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
private fun ZoomableImage(
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

/** 帖子 HTML 解析结果缓存（键：主题色 + html）：
 *  楼层滚回视野时直接命中，避免主线程重复 Jsoup 解析。按条数限容（约 48 个楼层正文）。 */
private val htmlBlockCache = android.util.LruCache<String, List<ContentBlock>>(48)

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
                    // 表格按行列结构解析，渲染为真正的网格（含表头加粗）
                    val rows = c.select("tr").mapNotNull { tr ->
                        val cells = tr.select("th, td").map { td ->
                            TableCellData(td.text().trim(), td.tagName().lowercase() == "th")
                        }
                        if (cells.any { it.text.isNotBlank() }) cells else null
                    }
                    if (rows.isNotEmpty()) blocks.add(ContentBlock(tableRows = rows))
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
