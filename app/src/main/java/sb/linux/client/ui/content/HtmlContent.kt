package sb.linux.client.ui.content

import androidx.compose.foundation.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.relocation.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import kotlinx.coroutines.*
import org.jsoup.Jsoup
import sb.linux.client.common.network.Endpoints
import sb.linux.client.ui.LocalLinkHandler
import sb.linux.client.ui.theme.LocalAppSpacing
import sb.linux.client.ui.theme.LocalReadingType
/**
 * 将帖子 HTML（服务端渲染的 Markdown 产物）转为 Compose 展示。
 * 支持：p/br、strong/em/del、a、code/pre、blockquote、ul/ol、img、视频、h1-h4、hr、table。
 * 行间距统一：所有正文文本块使用相同 lineHeight，块间不再额外加间距（由行框半行距提供），
 * 避免段落间距与行间距不一致的问题（含 @用户 拆分出的多个段落）。
 */
@Composable
fun HtmlContent(
    html: String,
    modifier: Modifier = Modifier,
    onFloor: (Int) -> Unit = {},
    mergeImages: Boolean = true,
    cacheable: Boolean = true,
    onHeadingPosition: (Int, Float) -> Unit = { _, _ -> },
    highlightQuery: String = "",
    onSearchPosition: (Float) -> Unit = {},
    /** 评论正文使用 16/26 的评论字阶；主帖使用 18/30 的正文阅读字阶。 */
    comment: Boolean = false,
) {
    val linkColor = MaterialTheme.colorScheme.primary
    val codeBg = MaterialTheme.colorScheme.surfaceContainerHighest
    // 解析结果全局缓存：楼层滚出视野后组合销毁，滚回时若只靠 remember 会
    // 在主线程重新跑 Jsoup 解析 + 富文本构建（长帖快滑的掉帧主因）。
    // 键带上主题链接/代码块颜色（解析产物中已烘焙颜色，换主题需重解析）。
    // cacheable=false 用于发帖预览：正文每敲一个键就是一份全新 HTML，若也写进这个
    // 48 条的共享缓存，会把正在阅读的楼层解析结果整批挤掉。
    val blocks = remember(html, linkColor, codeBg, mergeImages, cacheable) {
        if (!cacheable) parseHtmlToBlocks(html, linkColor, codeBg, mergeImages)
        else {
            val key = "$linkColor|$codeBg|${html.hashCode()}.${html.length}|$mergeImages"
            htmlBlockCache.get(key) ?: parseHtmlToBlocks(html, linkColor, codeBg, mergeImages)
                .also { htmlBlockCache.put(key, it) }
        }
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
    // 阅读字阶：正文 18/30、评论 16/26、引用 16/26；应用字号因子已在根部应用一次。
    val reading = LocalReadingType.current
    val spacing = LocalAppSpacing.current
    val bodyStyle = if (comment) reading.comment else reading.body
    val quoteStyle = reading.quote
    val highlightColor = MaterialTheme.colorScheme.tertiaryContainer
    fun searched(text: AnnotatedString): AnnotatedString = addSearchHighlights(text, highlightQuery, highlightColor)
    val firstMatch = remember(blocks, highlightQuery) {
        if (highlightQuery.isBlank()) -1 else blocks.indexOfFirst { block ->
            block.annotated?.text?.contains(highlightQuery.trim(), ignoreCase = true) == true ||
                block.tableRows.flatten().any { it.text.contains(highlightQuery.trim(), ignoreCase = true) }
        }
    }
    val detailsOpen = remember(blocks) {
        mutableStateMapOf<Int, Boolean>().apply {
            blocks.filter { it.isDetailsSummary }.forEach { block ->
                block.detailsId?.let { put(it, block.detailsOpenByDefault) }
            }
        }
    }
    val blockAnchorRequesters = remember(blocks) {
        blocks.indices.filter { blocks[it].anchorIds.isNotEmpty() }.associateWith { BringIntoViewRequester() }
    }
    val anchorRequesters = remember(blocks, blockAnchorRequesters) {
        buildMap {
            blocks.forEachIndexed { index, block ->
                blockAnchorRequesters[index]?.let { requester ->
                    block.anchorIds.forEach { anchor -> put(anchor, requester) }
                }
            }
        }
    }
    val contentScope = rememberCoroutineScope()
    val anchorTopInset = with(LocalDensity.current) { 72.dp.toPx() }
    fun jumpToAnchor(anchor: String) {
        val id = anchor.removePrefix("#")
        anchorRequesters[id]?.let { requester ->
            contentScope.launch {
                blocks.firstOrNull { id in it.anchorIds }?.foldPath?.forEach { detailsOpen[it] = true }
                kotlinx.coroutines.delay(32)
                // 顶栏悬浮在正文上方；把目标上方的区域也请求为可见，避免锚点
                // 虽已滚入视口却被顶栏盖住。
                requester.bringIntoView(Rect(0f, -anchorTopInset, 1f, 1f))
            }
        }
    }
    LaunchedEffect(highlightQuery, blocks) {
        if (highlightQuery.isNotBlank()) {
            blocks.filter { it.annotated?.text?.contains(highlightQuery.trim(), ignoreCase = true) == true }
                .flatMap { it.foldPath }.forEach { detailsOpen[it] = true }
        }
    }
    Column(modifier) {
        var headingOrdinal = 0
        blocks.forEachIndexed { idx, b ->
            if (b.foldPath.any { detailsOpen[it] == false }) return@forEachIndexed
            val reportMatch: (Float) -> Unit = { y -> if (idx == firstMatch) onSearchPosition(y) }
            val currentHeading = if (b.isHeading) headingOrdinal++ else -1
            val anchorModifier = blockAnchorRequesters[idx]
                ?.let { Modifier.bringIntoViewRequester(it) } ?: Modifier
            when {
                b.isDetailsSummary -> {
                    val id = b.detailsId ?: return@forEachIndexed
                    DetailsSummaryBlock(
                        block = b,
                        open = detailsOpen[id] == true,
                        text = searched(b.annotated ?: AnnotatedString("详情")),
                        style = bodyStyle,
                        modifier = anchorModifier,
                        onToggle = { detailsOpen[id] = !(detailsOpen[id] ?: false) },
                    )
                }
                b.embeddedVideo != null -> Box(anchorModifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    EmbeddedVideoPlayer(b.embeddedVideo, Modifier.padding(vertical = 4.dp))
                }
                b.imageUrls.size > 1 -> ImagePager(b.imageUrls, anchorModifier.padding(vertical = 4.dp)) { viewer = b.imageUrls to it }
                b.imageUrl != null -> {
                    BodyImage(b.imageUrl, anchorModifier.padding(vertical = 4.dp)) { viewer = listOf(b.imageUrl) to 0 }
                }
                b.isDivider -> {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 5.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                }
                b.isCode -> CodeBlockView(b.annotated!!.text, highlightQuery, reportMatch)
                b.tableRows.isNotEmpty() -> TableView(
                    b.tableRows, ::open, onFloor, highlightQuery, reportMatch, ::jumpToAnchor,
                    onImageClick = { urls, index -> viewer = urls to index },
                )
                b.isQuote -> {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.7f),
                        modifier = anchorModifier.padding(start = (b.quoteDepth * 12).dp, top = 3.dp, bottom = 3.dp)
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
                            LinkText(
                                text = searched(b.annotated!!),
                                style = quoteStyle.copy(textAlign = b.textAlign ?: TextAlign.Start),
                                modifier = Modifier.fillMaxWidth(),
                                onOpenUrl = ::open,
                                onFloor = onFloor,
                                onAnchor = ::jumpToAnchor,
                                searchQuery = highlightQuery,
                                onSearchPosition = reportMatch,
                            )
                        }
                    }
                }
                b.annotated != null && b.annotated.text.isNotBlank() -> {
                    val (txtStyle, fontWeight) = when {
                        // 标题层级保留模型语义，字重取阅读字阶（500），不再一律加粗
                        b.isHeading -> headingStyle(b.headingLevel) to null
                        b.isList -> bodyStyle.copy(
                            textIndent = TextIndent(firstLine = 0.sp, restLine = 22.sp)
                        ) to FontWeight.Normal
                        else -> bodyStyle to FontWeight.Normal
                    }
                    val base = when {
                        b.isHeading -> Modifier.padding(top = 6.dp, bottom = 2.dp)
                        b.isList -> Modifier.padding(start = (6 * b.indent).dp, bottom = 1.dp)
                        // 相邻段落之间留出 16dp（D04「段后 16dp」），避免正文连成一片
                        idx > 0 -> Modifier.padding(top = spacing.paragraph)
                        else -> Modifier
                    }
                    val headingModifier = if (b.isHeading) {
                        Modifier.semantics { heading() }
                    } else Modifier
                    LinkText(
                        text = searched(b.annotated),
                        onOpenUrl = ::open,
                        onFloor = onFloor,
                        onAnchor = ::jumpToAnchor,
                        style = txtStyle.copy(textAlign = b.textAlign ?: TextAlign.Start),
                        fontWeight = fontWeight,
                        searchQuery = highlightQuery,
                        onSearchPosition = reportMatch,
                        modifier = anchorModifier.then(headingModifier).then(
                            if (b.textAlign != null) base.fillMaxWidth() else base
                        ).let { alignedBase ->
                            if (currentHeading >= 0) alignedBase.onGloballyPositioned {
                            onHeadingPosition(currentHeading, it.positionInParent().y)
                            } else alignedBase
                        }
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

