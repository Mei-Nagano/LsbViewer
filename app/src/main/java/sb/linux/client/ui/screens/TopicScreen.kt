package sb.linux.client.ui.screens

import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.rememberLazyListState as rememberRowState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.CopyAll
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.FormatStrikethrough
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SentimentSatisfied
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Title
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.navigation.NavHostController
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import sb.linux.client.data.AiClient
import sb.linux.client.data.DanmakuItem
import sb.linux.client.data.DEFAULT_COMMENT_REWARD_TIERS
import sb.linux.client.data.EssenceApplication
import sb.linux.client.data.EssenceReview
import sb.linux.client.data.HtmlParser
import sb.linux.client.data.ImageHostClient
import sb.linux.client.data.LotteryPanel
import sb.linux.client.data.PostEntry
import sb.linux.client.data.Session
import sb.linux.client.data.TopicExport
import sb.linux.client.data.TopicPageData
import sb.linux.client.data.TopicPoll
import sb.linux.client.data.TopicPollOption
import sb.linux.client.data.VirtualCard
import sb.linux.client.ui.*
import java.io.File

/**
 * 评论区实时楼层胶囊（37）：暂时不启用——不加设置开关，保留实现，
 * 需要恢复时改回 true 即可。跳楼弹窗入口（帖子菜单）不受影响。
 */
private const val FLOOR_PILL_ENABLED = false

private data class TopicSearchHit(val post: PostEntry, val snippet: String)

@Composable
private fun SearchHighlightedText(text: String, query: String, maxLines: Int) {
    val highlight = MaterialTheme.colorScheme.tertiaryContainer
    val marked = remember(text, query, highlight) {
        buildAnnotatedString {
            append(text)
            if (query.isNotBlank()) {
                Regex(Regex.escape(query), RegexOption.IGNORE_CASE).findAll(text).forEach { match ->
                    addStyle(
                        SpanStyle(background = highlight, fontWeight = FontWeight.SemiBold),
                        match.range.first,
                        match.range.last + 1,
                    )
                }
            }
        }
    }
    Text(marked, maxLines = maxLines, overflow = TextOverflow.Ellipsis)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TopicScreen(session: Session, nav: NavHostController) {
    val entry = nav.currentBackStackEntry ?: return
    val tid = entry.arguments?.getLong("tid") ?: return
    val initialPage = 1
    // 定位楼层（收藏评论跳转用，34）：>0 时首屏加载完成后自动跳到该楼
    val initialFloor = entry.arguments?.getInt("floor") ?: 0
    val context = LocalContext.current
    val searchKeyboard = LocalSoftwareKeyboardController.current
    val clipboard = LocalClipboardManager.current

    val pinnedSnapshot = remember(tid) {
        session.floatingTopicSnapshot?.takeIf { session.floatingTopicId == tid && it.topic.topicId == tid }
    }
    // 通过首页浮窗或其他方式重新进入已钉住帖子后，浮窗自动完成使命。
    LaunchedEffect(tid) { session.clearFloatingTopic(tid) }

    var data by remember { mutableStateOf<TopicPageData?>(null) }
    var loading by remember { mutableStateOf(true) }
    var loadingMore by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var showReply by remember { mutableStateOf(false) }
    var quoteText by remember { mutableStateOf("") }
    // 回复弹窗「换一题」后的人机验证（覆盖页面初始解析的那个；重新打开弹窗时重置）
    var captchaOverride by remember { mutableStateOf<sb.linux.client.data.NativeCaptcha?>(null) }
    var coinTarget by remember { mutableStateOf<PostEntry?>(null) }
    var copyPost by remember { mutableStateOf<PostEntry?>(null) }
    // 长按菜单 → 查看该用户在本帖的所有回复
    var userReplies by remember { mutableStateOf<List<PostEntry>?>(null) }
    var userRepliesTitle by remember { mutableStateOf("") }
    var showFloorDialog by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    var topicSearchOpen by remember { mutableStateOf(false) }
    var topicSearchListOpen by remember { mutableStateOf(false) }
    var topicSearchIndex by remember { mutableIntStateOf(0) }
    var allSearchPagesLoaded by remember(tid) { mutableStateOf(false) }
    var pendingSearchPost by remember { mutableStateOf<PostEntry?>(null) }
    val searchPositions = remember(tid) { mutableMapOf<Long, Pair<String, Float>>() }
    var tocOpen by remember { mutableStateOf(false) }
    var topicSearchQuery by remember { mutableStateOf("") }
    var showExportDialog by remember { mutableStateOf(false) }
    var exportBusy by remember { mutableStateOf<String?>(null) }
    // 打赏：底部弹层，不再跳转独立页面（28）
    var showDonate by remember { mutableStateOf(false) }
    var pollBusy by remember { mutableStateOf(false) }
    var essenceBusy by remember { mutableStateOf(false) }
    var essenceTopUpBusy by remember { mutableStateOf(false) }

    // 评论区排序：0 = 热度（楼主正文固定首位 + 其余按点赞数降序，同赞按楼层号升序保证稳定），
    //           1 = 正序（楼层号升序），2 = 倒序（楼层号降序）；记住上次选择
    var sortOrder by remember { mutableIntStateOf(session.commentSortOrder) }

    // 打赏弹幕：数据 + 每帖开关（默认跟随全局设置）
    var danmaku by remember { mutableStateOf<List<DanmakuItem>>(emptyList()) }
    var danmakuLocalOn by remember(tid) { mutableStateOf(session.danmakuOn) }

    // #N 对话串联弹窗状态
    var threadPosts by remember { mutableStateOf<List<PostEntry>?>(null) }
    var threadLoading by remember { mutableStateOf(false) }
    var threadError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    // Navigation 返回时由 SaveableStateHolder 保留精确索引与像素偏移。
    val listState = rememberLazyListState()

    // 楼层定位：高亮目标楼层 + 记住原位置以便返回（2.13）
    var highlightPostId by remember { mutableStateOf<Long?>(null) }
    var returnPos by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var pendingRestoreOffset by remember(tid) { mutableStateOf<Int?>(null) }
    var readingPositionRestored by remember(tid) { mutableStateOf(false) }
    var usageViewRecorded by rememberSaveable(tid) { mutableStateOf(false) }

    // 顶栏常驻显示（11）：正文标题滑出顶栏下沿后，顶栏显示帖子标题，此外隐藏。
    // 标题行底边与内容区顶边实时测量（root 坐标），标题完全没入顶栏即切换。
    var titleBottomInRoot by remember { mutableFloatStateOf(Float.MAX_VALUE) }
    var contentTopInRoot by remember { mutableFloatStateOf(0f) }
    val topBarPx = with(LocalDensity.current) { 64.dp.toPx() }
    val showTitleInTopBar by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 ||
                titleBottomInRoot - contentTopInRoot <= topBarPx
        }
    }

    // 收藏状态（12）：打开帖子时取源站解析结果（收藏表单按钮为「取消收藏」即已收藏），
    // 操作成功后本地即时翻转并同步进当前 data，无需整页刷新
    var isFav by remember { mutableStateOf(false) }
    var showUnfavDialog by remember { mutableStateOf(false) }
    LaunchedEffect(data?.topicId, data?.favorited) {
        data?.let { isFav = it.favorited }
    }

    // 本地收藏评论（34）：评论操作行的收藏按钮；纯本地快照，与源站无关
    var favCommentIds by remember { mutableStateOf(session.settings.commentFavoriteList().map { it.replyId }.toSet()) }
    fun toggleCommentFav(post: PostEntry) {
        if (post.id in favCommentIds) {
            session.settings.removeCommentFavorite(post.id)
            session.showToast("已取消收藏")
        } else {
            val excerpt = HtmlParser.htmlToText(post.contentHtml).trim().let {
                if (it.length > 120) it.take(120) + "…" else it
            }
            session.settings.addCommentFavorite(
                replyId = post.id, topicId = tid, topicTitle = data?.title ?: "",
                floor = post.floor, authorId = post.authorId, authorName = post.authorName,
                avatarUrl = post.avatarUrl, content = excerpt,
            )
            session.showToast("已收藏该评论")
        }
        favCommentIds = session.settings.commentFavoriteList().map { it.replyId }.toSet()
    }

    // AI 总结状态
    var aiSummary by remember { mutableStateOf<String?>(null) }
    var aiBusy by remember { mutableStateOf(false) }
    var aiError by remember { mutableStateOf<String?>(null) }

    // 评论区当前楼层（37）：首个可见回复的楼层号；0 = 还没滚进评论区。
    // 楼层胶囊实时展示，点击弹出跳楼弹窗（预填当前楼层）
    val idToFloor = remember(data) {
        (data?.posts ?: emptyList()).associate { it.id to it.floor }
    }
    val latestIdToFloor by rememberUpdatedState(idToFloor)
    DisposableEffect(tid) {
        onDispose {
            val first = listState.layoutInfo.visibleItemsInfo.firstOrNull()
            val replyId = (first?.key as? String)?.removePrefix("r-")?.toLongOrNull()
            val floor = replyId?.let { latestIdToFloor[it] } ?: 0
            session.saveReadingPosition(
                topicId = tid,
                floor = floor,
                listIndex = listState.firstVisibleItemIndex,
                scrollOffset = listState.firstVisibleItemScrollOffset,
            )
        }
    }
    val currentFloor by remember(idToFloor) {
        derivedStateOf {
            listState.layoutInfo.visibleItemsInfo
                .firstOrNull { (it.key as? String)?.startsWith("r-") == true }
                ?.let { info ->
                    (info.key as String).removePrefix("r-").toLongOrNull()?.let { idToFloor[it] }
                } ?: 0
        }
    }

    // 排序后的楼层列表（楼主正文固定首位）：热度档按点赞数降序（同赞按楼层号，顺序稳定）；
    // 正序档按楼层号升序（源站页面顺序）。未登录时源站不输出点赞计数（全 0），热度档自然退化为正序
    val sortedPosts = remember(data, sortOrder) {
        val d = data ?: return@remember emptyList()
        val main = d.posts.firstOrNull { it.floor == 0 }
        val comments = d.posts.filter { it.id != main?.id }
        val rest = when (sortOrder) {
            1 -> comments.sortedBy { it.floor }
            // 倒序：楼层号降序
            2 -> comments.sortedByDescending { it.floor }
            else -> comments.sortedWith(
                compareByDescending<PostEntry> { it.likeCount }.thenBy { it.floor }
            )
        }
        if (main != null) listOf(main) + rest else rest
    }
    val mainPost = sortedPosts.firstOrNull { it.floor == 0 }
    val topicSearchHits = remember(data?.posts, topicSearchQuery) {
        val query = topicSearchQuery.trim()
        if (query.isBlank()) emptyList() else data?.posts.orEmpty().sortedBy { it.floor }.mapNotNull { post ->
            val text = HtmlParser.htmlToSearchText(post.contentHtml)
            val inAuthor = post.authorName.contains(query, ignoreCase = true)
            val at = text.indexOf(query, ignoreCase = true)
            if (!inAuthor && at < 0) null else {
                val center = at.coerceAtLeast(0)
                val start = (center - 45).coerceAtLeast(0)
                val end = (center + query.length + 90).coerceAtMost(text.length)
                TopicSearchHit(post, buildString {
                    if (start > 0) append('…')
                    append(text.substring(start, end).replace(Regex("""\s+"""), " "))
                    if (end < text.length) append('…')
                }.ifBlank { "用户名匹配" })
            }
        }.orEmpty()
    }
    LaunchedEffect(topicSearchQuery, topicSearchHits.size) {
        topicSearchIndex = topicSearchIndex.coerceIn(0, (topicSearchHits.size - 1).coerceAtLeast(0))
    }
    val readingStats = remember(mainPost?.contentHtml) {
        mainPost?.contentHtml?.let { estimateReading(HtmlParser.htmlToText(it)) }
    }
    data class TocHeading(val text: String, val level: Int)
    val tocHeadings = remember(mainPost?.contentHtml) {
        mainPost?.contentHtml?.let { html ->
            org.jsoup.Jsoup.parseBodyFragment(html).select("h1,h2,h3,h4,h5,h6").mapNotNull { node ->
                node.text().trim().takeIf { it.isNotBlank() }?.let {
                    TocHeading(it, node.tagName().removePrefix("h").toIntOrNull() ?: 1)
                }
            }
        }.orEmpty()
    }
    val headingPositions = remember(tid) { mutableStateMapOf<Int, Float>() }
    var bodyTop by remember(tid) { mutableFloatStateOf(0f) }
    var bodyHeight by remember(tid) { mutableFloatStateOf(0f) }
    val bodyVisible by remember {
        derivedStateOf {
            listState.layoutInfo.visibleItemsInfo.any { (it.key as? String)?.startsWith("main-") == true } &&
                bodyHeight > 0 && bodyTop + bodyHeight > contentTopInRoot + topBarPx &&
                bodyTop < contentTopInRoot + listState.layoutInfo.viewportEndOffset
        }
    }
    val readingProgress by remember {
        derivedStateOf {
            val viewportHeight = (listState.layoutInfo.viewportEndOffset - topBarPx).coerceAtLeast(1f)
            ((contentTopInRoot + topBarPx - bodyTop) / (bodyHeight - viewportHeight).coerceAtLeast(1f)).coerceIn(0f, 1f)
        }
    }

    // 帖子分类生效的滚动模式（浏览设置可按分类覆盖，3.13）
    val topicInfinite = session.effectiveInfiniteScroll("topic")
    // 翻页模式：按设置的每页评论数本地切片分页（3.11）。
    // 用 rememberSaveable 使其在跳转到用户主页后返回本帖时得以保留，
    // 结合 loadedMaxPage 从缓存重建 data 即可恢复原来的评论滚动位置。
    var localReplyPage by rememberSaveable(tid) { mutableIntStateOf(1) }
    // 已载入 data 的源站分页范围（加载是顺序的，恒为 1..loadedMaxPage）。
    // 保存后返回本帖时据此从缓存重建 data，避免只加载第一页导致滚动位置丢失。
    var loadedMaxPage by rememberSaveable(tid) { mutableIntStateOf(1) }
    // 每条有子回复的评论独立折叠，不再用评论区统一开关。
    var collapsedReplyIds by remember(tid) { mutableStateOf(emptySet<Long>()) }
    // 滚动模式切换的触发标记：scrollModeOverrides 存于 SharedPreferences（非 Compose 状态），
    // 切换后靠此标记强制重组，让 topicInfinite 从源设置重新读取，菜单选项与分页即时同步。
    var replyModeTick by remember { mutableIntStateOf(0) }
    // 倒序全量加载可能因源站页码不推进而中途停下（见 loadAllRemaining 的进度守卫）。
    // 记录停下时的页码：下面那个 LaunchedEffect 以 loadingMore 为 key，若不记录会在
    // 守卫跳出后立刻重新触发，形成 effect 级别的无限重试。
    var reverseStalledAtPage by remember { mutableIntStateOf(0) }
    val replies = sortedPosts.filter { it.floor > 0 }
    // 楼层号 → 回复：树形「回复 #N」胶囊展示父楼作者头像用（父楼未加载时无头像，胶囊降级为图标）
    val repliesByFloor = remember(replies) { replies.associateBy { it.floor } }
    // 树形评论（源站 data-quote-threads-parent-floor）：存在回复关系时按树形嵌套展示，
    // 多级子回复按层级缩进 + 引导线；旧帖无此数据时保持平铺
    val hasTree = remember(replies) { replies.any { it.parentFloor > 0 } }
    val replyTree = remember(replies, sortOrder) {
        if (hasTree) buildReplyTree(replies, sortOrder) else emptyList()
    }
    val treeChildCounts = remember(replyTree) {
        buildMap<Long, Int> {
            fun collect(nodes: List<ReplyNode>) {
                nodes.forEach { node ->
                    if (node.children.isNotEmpty()) put(node.post.id, flattenTree(node.children).size)
                    collect(node.children)
                }
            }
            collect(replyTree)
        }
    }
    // 「对话串联」开关（默认关闭）：源站已支持树形结构评论（data-quote-threads-parent-floor），
    // 楼层卡直接展示「回复 #N」标识并支持点击跳转，引用链弹窗仅作兼容保留
    val threadEnabled = session.threadDialogEnabled
    // 楼号 -> 回复它的楼层数量：决定评论区是否显示该楼的「对话串联」入口
    val threadReplyCount = remember(sortedPosts) {
        val m = mutableMapOf<Int, Int>()
        sortedPosts.forEach { p -> p.referencedFloors.forEach { r -> if (r > 0) m[r] = (m[r] ?: 0) + 1 } }
        m
    }
    val repliesPer = session.settings.commentsPerPage.coerceAtLeast(1)
    val pagedReplies = if (topicInfinite) replies
    else replies.drop((localReplyPage - 1) * repliesPer).take(repliesPer)
    // 树形模式的可见列表：翻页按「顶层节点」分页，子树整棵跟随顶层所在页（不拆散对话）
    val flatTree = remember(replyTree, localReplyPage, topicInfinite, repliesPer, collapsedReplyIds) {
        if (!hasTree) emptyList()
        else if (topicInfinite) flattenTree(replyTree, collapsedReplyIds)
        else flattenTree(replyTree.drop((localReplyPage - 1) * repliesPer).take(repliesPer), collapsedReplyIds)
    }
    // 本地总页数：已加载评论的整页数；源站还有更多页时 +1（允许继续翻页触发加载）。
    // 树形模式按顶层节点数计页
    val localReplyTotal = if (topicInfinite) (data?.totalPages ?: 1) else run {
        val base = if (hasTree) replyTree.size else replies.size
        val full = ((base + repliesPer - 1) / repliesPer).coerceAtLeast(1)
        (if ((data?.page ?: 1) < (data?.totalPages ?: 1)) full + 1 else full).coerceAtLeast(localReplyPage)
    }

    fun loadDanmaku() {
        session.getDanmaku(tid)?.let {
            danmaku = it
            return
        }
        scope.launch {
            val items = try { session.client.getDonateFeed(tid) } catch (_: Exception) { emptyList() }
            danmaku = items
            session.saveDanmaku(tid, items)
        }
    }

    fun load(p: Int, append: Boolean = false, force: Boolean = false) {
        scope.launch {
            if (append) loadingMore = true else { loading = true; error = null }
            try {
                // 强制刷新：先清掉本帖分页缓存，确保下面的 getTopicPage 命中为 null 而从源站重抓
                if (force) session.clearTopicPageCache(tid)
                val d = session.getTopicPage(tid, p) ?: run {
                    val resp = session.client.get("/topic/$tid?p=$p")
                    HtmlParser.parseTopicPage(resp.html, tid).also { session.saveTopicPage(tid, p, it) }
                }
                val missingPosts = mutableListOf<PostEntry>()
                if (append && data != null) {
                    for (missingPage in (data!!.page + 1) until p) {
                        val earlier = session.getTopicPage(tid, missingPage) ?: HtmlParser.parseTopicPage(
                            session.client.get("/topic/$tid?p=$missingPage").html, tid,
                        ).also { session.saveTopicPage(tid, missingPage, it) }
                        missingPosts += earlier.posts
                    }
                }
                data = if (append && data != null) data!!.copy(
                    posts = (data!!.posts + missingPosts + d.posts).associateBy { it.id }.values.toList(),
                    page = d.page,
                    totalPages = d.totalPages,
                ) else d
                if (!append) session.settings.recordCardRedemptions(tid, d.title, d.virtualCard)
                // 记录已载入的源站分页范围（1..loadedMaxPage）
                loadedMaxPage = if (append) maxOf(loadedMaxPage, d.page.coerceAtLeast(1))
                else d.page.coerceAtLeast(1)
                // 重新加载（非追加）时重置本地评论分页页码（3.11）与倒序停滞标记
                if (!append) { localReplyPage = 1; reverseStalledAtPage = 0 }
                // 阅读量回填首页卡片缓存
                if (!append) d.viewsText.toIntOrNull()?.let { session.saveTopicViews(tid, it) }
                // 记录到本地浏览历史（3.2）
                if (!append && p == 1) {
                    val op = d.posts.firstOrNull { it.floor == 0 } ?: d.posts.firstOrNull()
                    session.settings.addHistory(
                        topicId = tid, title = d.title,
                        authorId = op?.authorId ?: 0, authorName = op?.authorName ?: "",
                        forumId = 0, forumName = "",
                        avatarUrl = op?.avatarUrl ?: "", titleColor = "",
                    )
                    if (!usageViewRecorded) {
                        session.settings.recordUsageEvent("view", tid, d.title)
                        usageViewRecorded = true
                    }
                }
            } catch (e: Exception) {
                if (!append) error = e.message
                else session.showToast(e.message ?: "加载失败")
            } finally { loading = false; loadingMore = false }
        }
    }

    /** 翻页模式：按每页评论数本地切片分页，目标页评论数不足时自动连续加载源站后续页（3.11） */
    fun goLocalReplyPage(target: Int) {
        scope.launch {
            val per = session.settings.commentsPerPage.coerceAtLeast(1)
            val need = target * per + 1 // 楼主正文占首位
            error = null
            while ((data?.posts?.size ?: 0) < need && (data?.page ?: 1) < (data?.totalPages ?: 1)) {
                loadingMore = true
                val prevPage = data?.page ?: 1
                val prevCount = data?.posts?.size ?: 0
                try {
                    val np = prevPage + 1
                    val d = session.getTopicPage(tid, np) ?: run {
                        val resp = session.client.get("/topic/$tid?p=$np")
                        HtmlParser.parseTopicPage(resp.html, tid).also { session.saveTopicPage(tid, np, it) }
                    }
                    data = data?.copy(
                        posts = (data!!.posts + d.posts).distinctBy { it.id },
                        page = d.page,
                        totalPages = d.totalPages,
                    ) ?: d
                    loadedMaxPage = maxOf(loadedMaxPage, d.page.coerceAtLeast(1))
                } catch (e: Exception) {
                    session.showToast(e.message ?: "加载失败")
                    break
                }
                // 进度守卫：源站对越界 ?p= 常返回同一页内容，该页若无分页控件解析出的
                // page 会回落为 1 —— 此时 page 不推进、posts 也不增长，循环条件恒真。
                // 命中缓存后还会退化成纯 CPU 空转（每轮全量 distinctBy），必须主动跳出。
                if ((data?.page ?: 1) <= prevPage && (data?.posts?.size ?: 0) <= prevCount) break
            }
            loadingMore = false
            localReplyPage = target.coerceAtLeast(1)
            // 本地翻页跳回评论区顶部
            listState.scrollToItem(if (mainPost != null) 2 else 1)
        }
    }

    /** 倒序模式：一次性加载剩余全部页（上限 100 页防止极端大帖卡死）。
     *  增量加载下新页插入会让已看内容跳动、本地页码随加载不断变化，无法形成真正倒序；
     *  全量加载后本地倒序切片稳定，翻页页码固定。 */
    fun loadAllRemaining() {
        scope.launch {
            loadingMore = true
            try {
                while ((data?.page ?: 1) < (data?.totalPages ?: 1) &&
                    (data?.page ?: 1) < 100
                ) {
                    val prevPage = data?.page ?: 1
                    val prevCount = data?.posts?.size ?: 0
                    val np = prevPage + 1
                    val d = session.getTopicPage(tid, np) ?: run {
                        val resp = session.client.get("/topic/$tid?p=$np")
                        HtmlParser.parseTopicPage(resp.html, tid).also { session.saveTopicPage(tid, np, it) }
                    }
                    data = data?.copy(
                        posts = (data!!.posts + d.posts).distinctBy { it.id },
                        page = d.page,
                        totalPages = d.totalPages,
                    ) ?: d
                    loadedMaxPage = maxOf(loadedMaxPage, d.page.coerceAtLeast(1))
                    // 进度守卫：上面的 page < 100 限的是页码而非迭代次数——源站页码若不
                    // 推进（越界 ?p= 返回同一页 / 该页解析不到分页控件回落为 1），page 恒为
                    // prevPage，那个上限永远挡不住，必须按「本轮是否真有进展」跳出。
                    if ((data?.page ?: 1) <= prevPage && (data?.posts?.size ?: 0) <= prevCount) {
                        reverseStalledAtPage = prevPage
                        break
                    }
                }
            } catch (e: Exception) {
                session.showToast(e.message ?: "加载失败")
            } finally { loadingMore = false }
        }
    }

    /** 搜索全帖从源站第一页补齐，兼容从深层楼层直接进入时缺少前几页。 */
    fun loadAllForSearch() {
        if (loading || loadingMore) return
        scope.launch {
            loadingMore = true
            try {
                var page = 1
                var lastPage = data?.totalPages ?: 1
                while (page <= lastPage) {
                    val loaded = session.getTopicPage(tid, page) ?: run {
                        val response = session.client.get("/topic/$tid?p=$page")
                        HtmlParser.parseTopicPage(response.html, tid).also { session.saveTopicPage(tid, page, it) }
                    }
                    check(loaded.page == page) { "源站未返回第 $page 页，已保留当前搜索结果" }
                    data = data?.copy(
                        posts = (data!!.posts + loaded.posts).associateBy { it.id }.values.toList(),
                        page = maxOf(data!!.page, loaded.page),
                        totalPages = loaded.totalPages,
                    ) ?: loaded
                    lastPage = loaded.totalPages
                    loadedMaxPage = maxOf(loadedMaxPage, page)
                    page++
                }
                allSearchPagesLoaded = true
            } catch (e: Exception) {
                session.showToast(e.message ?: "加载失败，可重试搜索全帖")
            } finally { loadingMore = false }
        }
    }

    // 倒序被选中（含进入帖子时记住的上次选择）：评论未加载完就全量补齐，保证完整真实倒序。
    // loading/loadingMore 也作为 key：首次 load(1) 结束时 data 与 loading 几乎同时落定，
    // 仅依赖 data key 重启会读到 loading=true 而漏触发。
    LaunchedEffect(sortOrder, data?.page, data?.totalPages, loading, loadingMore) {
        if (sortOrder == 2 && data != null && !loading && !loadingMore &&
            (data?.page ?: 1) < (data?.totalPages ?: 1) &&
            // 上一轮已在这一页停住（源站页码不推进）：不再重试，否则本 effect 会随
            // loadingMore 的翻转被无限重新触发
            reverseStalledAtPage != (data?.page ?: 1)
        ) loadAllRemaining()
    }

    /** 跳转到指定楼层：借助源站 ?floor=N 重定向定位页码（跨页时使用） */
    fun jumpFloor(floor: Int) {
        scope.launch {
            loading = true; error = null
            try {
                val resp = session.client.get("/topic/$tid?floor=$floor")
                val p = Regex("""[?&]p=(\d+)""").find(resp.url)?.groupValues?.get(1)?.toIntOrNull() ?: 1
                val targetPage = HtmlParser.parseTopicPage(resp.html, tid)
                val collected = mutableListOf<PostEntry>()
                for (page in 1 until p) {
                    val earlier = session.getTopicPage(tid, page) ?: HtmlParser.parseTopicPage(
                        session.client.get("/topic/$tid?p=$page").html, tid
                    ).also { session.saveTopicPage(tid, page, it) }
                    collected += earlier.posts
                }
                data = targetPage.copy(posts = (collected + targetPage.posts).distinctBy { it.id })
                sortOrder = 1 // 跨页跳楼层需按楼层序渲染，data.posts 索引才能对上滚动位置
                localReplyPage = 1
                loadedMaxPage = p.coerceAtLeast(1)
                loading = false
                collapsedReplyIds = emptySet()
                pendingSearchPost = data?.posts?.firstOrNull { it.floor == floor }
                if (pendingSearchPost == null) session.showToast("未找到该楼层")
            } catch (e: Exception) {
                error = e.message; loading = false
            }
        }
    }

    /** 所有楼层入口共用定位：先切页、展开祖先，再等待新布局滚动。 */
    fun jumpToFloor(floor: Int) {
        returnPos = listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset
        val target = sortedPosts.firstOrNull { it.floor == floor }
        if (target == null) jumpFloor(floor) else pendingSearchPost = target
    }

    LaunchedEffect(pendingSearchPost, flatTree, localReplyPage, collapsedReplyIds, sortedPosts) {
        val target = pendingSearchPost ?: return@LaunchedEffect
        val isMain = target.id == mainPost?.id
        if (!isMain && hasTree) {
            var root = target
            val ancestors = mutableSetOf<Long>()
            val visited = mutableSetOf(target.floor)
            while (true) {
                val parent = repliesByFloor[root.parentFloor] ?: break
                if (!visited.add(parent.floor)) break
                ancestors.add(parent.id)
                root = parent
            }
            val topIndex = replyTree.indexOfFirst { it.post.id == root.id }
            val page = if (topicInfinite || topIndex < 0) localReplyPage else topIndex / repliesPer + 1
            val expanded = collapsedReplyIds - ancestors
            if (expanded != collapsedReplyIds || page != localReplyPage) {
                collapsedReplyIds = expanded
                localReplyPage = page
                return@LaunchedEffect
            }
        } else if (!isMain && !topicInfinite) {
            val index = replies.indexOfFirst { it.id == target.id }
            if (index < 0) { pendingSearchPost = null; return@LaunchedEffect }
            val page = index / repliesPer + 1
            if (page != localReplyPage) {
                localReplyPage = page
                return@LaunchedEffect
            }
        }
        val visible = if (hasTree) flatTree.map { it.first } else pagedReplies
        val replyIndex = visible.indexOfFirst { it.id == target.id }
        if (!isMain && replyIndex < 0) { pendingSearchPost = null; return@LaunchedEffect }
        // header、主楼、可选打赏弹幕、评论标题：必须与 LazyColumn 的真实条目一致。
        val replyStart = 1 + (if (mainPost != null) 1 else 0) +
            (if (danmakuLocalOn && danmaku.isNotEmpty()) 1 else 0) +
            (if (replies.isNotEmpty() || (data?.loginVisibleReplies ?: -1) >= 0) 1 else 0)
        withFrameNanos { }
        highlightPostId = target.id
        listState.animateScrollToItem(if (isMain) 1 else replyStart + replyIndex, pendingRestoreOffset ?: 0)
        pendingRestoreOffset = null
        if (topicSearchOpen && topicSearchQuery.isNotBlank()) {
            withFrameNanos { }
            withFrameNanos { }
            searchPositions[target.id]?.takeIf { it.first == topicSearchQuery }?.let { (_, y) ->
                listState.scrollBy(y - contentTopInRoot - 12f)
            }
        }
        pendingSearchPost = null
    }

    LaunchedEffect(highlightPostId) {
        if (highlightPostId != null) { delay(1800); highlightPostId = null }
    }

    LaunchedEffect(topicSearchQuery, topicSearchOpen) {
        if (topicSearchOpen && topicSearchQuery.isNotBlank()) {
            delay(300)
            topicSearchHits.firstOrNull()?.let { pendingSearchPost = it.post }
        }
    }
    // 收藏评论定位（34）：首屏数据加载完成后自动跳到目标楼层（仅进入时执行一次）
    var floorJumped by remember { mutableStateOf(false) }
    LaunchedEffect(loading, data) {
        if (!floorJumped && !loading && data != null && initialFloor > 0) {
            floorJumped = true
            if (data!!.posts.any { it.floor == initialFloor }) jumpToFloor(initialFloor)
            else jumpFloor(initialFloor)
        }
    }

    LaunchedEffect(loading, data, initialFloor) {
        if (readingPositionRestored || loading || data == null) return@LaunchedEffect
        readingPositionRestored = true
        if (pinnedSnapshot != null) {
            withFrameNanos { }
            listState.scrollToItem(pinnedSnapshot.listIndex, pinnedSnapshot.offset)
            returnPos = null
        }
    }

    /** 直达评论区：滚动到「全部回复」标题处（2.10） */
    fun scrollToComments() {
        val idx = if (mainPost != null) 2 else 1
        scope.launch { listState.animateScrollToItem(idx) }
    }

    /**
     * #N 对话串联：拉取帖子全部页（上限 10 页），提取「当前楼层相关的完整对话树」：
     * 向上回溯本楼引用的楼层（含间接引用），向下从链内全部楼层出发收集引用了它们的后续回复，
     * 兄弟分支（同一楼层的不同回复，如 #6、#7 都回复 #3）也一并串起来。
     */
    fun openThread(startPost: PostEntry) {
        scope.launch {
            threadLoading = true; threadError = null; threadPosts = null
            try {
                val all = mutableListOf<PostEntry>()
                var page = 1
                // 拉取帖子全部页（上限 30 页）：大帖对话链可能跨到后面的页，
                // 之前 10 页上限会导致串联对话「评论显示不完全」
                val totalPages = (data?.totalPages ?: 1).coerceAtMost(30)
                while (page <= totalPages) {
                    val d = session.getTopicPage(tid, page) ?: run {
                        val resp = if (page <= 1) session.client.get("/topic/$tid")
                        else session.client.get("/topic/$tid?p=$page")
                        HtmlParser.parseTopicPage(resp.html, tid).also { session.saveTopicPage(tid, page, it) }
                    }
                    all += d.posts
                    page++
                }

                val start = startPost.floor
                val byFloor = all.filter { it.floor > 0 }.associateBy { it.floor }
                // 向上：本楼 + 其引用链（含间接）
                val chain = linkedSetOf<Int>()
                val up = ArrayDeque<Int>()
                if (start > 0) { chain.add(start); up.add(start) }
                while (up.isNotEmpty()) {
                    val p = byFloor[up.removeFirst()] ?: continue
                    p.referencedFloors.forEach { r -> if (r > 0 && chain.add(r)) up.add(r) }
                }
                // 向下：从链内全部楼层（含祖先）出发收集后续回复，
                // 这样兄弟分支（同样回复了链内楼层的其他楼层）也能串进来
                val down = ArrayDeque(chain)
                while (down.isNotEmpty()) {
                    val f = down.removeFirst()
                    all.forEach { p ->
                        if (p.floor > 0 && p.floor !in chain && f in p.referencedFloors) {
                            chain.add(p.floor); down.add(p.floor)
                        }
                    }
                }
                val posts = all.filter { it.floor > 0 && it.floor in chain }
                    .sortedBy { it.floor }
                if (posts.isEmpty()) threadError = "未找到相关楼层，可能已被删除"
                threadPosts = posts
            } catch (e: Exception) {
                threadError = e.message ?: "加载失败"
            } finally { threadLoading = false }
        }
    }

    /** 点赞/投币：源站 v8.6+ 统一走 /donate_reply_reaction（points=0 仅点赞，>0 为投币积分，理由选填） */
    fun doLike(post: PostEntry, points: Int, reason: String = "") {
        scope.launch {
            try {
                val d = data ?: return@launch
                val json = session.client.postAjax(
                    "/donate_reply_reaction", mapOf(
                        "_csrf" to d.csrf,
                        "donate_reaction_reply_id" to post.id.toString(),
                        "donate_reaction_points" to points.toString(),
                        "donate_reaction_reason" to reason,
                    )
                )
                val ok = json.optInt("ok") == 1
                session.showToast(json.optString("message", if (ok) "已点赞" else "操作失败"))
                if (ok && points > 0) session.settings.recordUsageEvent("coin", tid, d.title, points)
                load(d.page)
                if (points > 0) loadDanmaku()
            } catch (e: Exception) {
                session.showToast(e.message ?: "操作失败")
            }
        }
    }

    fun doFavorite() {
        val d = data ?: return
        val wasFav = isFav
        scope.launch {
            try {
                val resp = session.client.postForm(
                    d.favAction, mapOf(
                        "_csrf" to d.csrf,
                        "topic_id" to d.topicId.toString(),
                    )
                )
                if (resp.url.contains("form_error")) {
                    session.showToast("操作失败")
                } else {
                    session.showToast(if (wasFav) "已取消收藏" else "已收藏")
                    // 本地即时翻转并同步进当前 data（不整页刷新，避免评论分页/滚动位置重置）；
                    // 同时清掉本帖分页缓存，下次进入重新解析最新收藏状态
                    isFav = !wasFav
                    data = d.copy(favorited = !wasFav)
                    session.clearTopicPageCache(tid)
                    if (wasFav) {
                        // 取消收藏：本地「收藏内容」快照同步移除
                        session.settings.removeFavorite(d.topicId)
                    } else {
                        // 本地收藏快照：收藏即记入本地「收藏内容」，供离线检索，不额外访问源站（3.15）
                        val op = d.posts.firstOrNull { it.floor == 0 } ?: d.posts.firstOrNull()
                        session.settings.addFavorite(
                            topicId = d.topicId, title = d.title,
                            authorId = op?.authorId ?: 0, authorName = op?.authorName ?: "",
                            forumId = 0, forumName = "",
                            avatarUrl = op?.avatarUrl ?: "", titleColor = "",
                        )
                        session.settings.recordUsageEvent("favorite", d.topicId, d.title)
                    }
                }
            } catch (e: Exception) { session.showToast(e.message ?: "失败") }
        }
    }

    /** 收藏入口（底栏图标 / 顶栏菜单共用）：未收藏直接收藏；已收藏按设置决定是否弹确认框（12） */
    fun onFavoriteClick() {
        if (isFav && session.unfavoriteConfirm) showUnfavDialog = true
        else doFavorite()
    }

    // 长图所见即所得：捕获当前主题色（供导出使用，深色模式下导出深色长图）
    val colorScheme = MaterialTheme.colorScheme

    fun export(kind: String, scopeMode: String, fromFloor: Int, toFloor: Int, multiPage: Boolean, selectedIds: Set<Long>) {
        val d = data ?: return
        exportBusy = kind
        val exportTheme = TopicExport.ExportTheme.fromColorScheme(colorScheme)
        val selectedPosts = when (scopeMode) {
            "main" -> d.posts.filter { it.floor == 0 }.take(1)
            "range" -> d.posts.filter { it.floor in fromFloor.coerceAtMost(toFloor)..fromFloor.coerceAtLeast(toFloor) }
            "selected" -> d.posts.filter { it.id in selectedIds }
            else -> d.posts
        }
        if (selectedPosts.isEmpty()) {
            exportBusy = null
            session.showToast("所选范围没有可导出的楼层，请先加载完整帖子")
            return
        }
        scope.launch {
            try {
                val url = sb.linux.client.data.Endpoints.abs("/topic/$tid")
                val files: List<File> = withContext(Dispatchers.IO) {
                    when (kind) {
                        "html" -> listOf(TopicExport.exportHtml(context, d.title, selectedPosts, url))
                        "md" -> listOf(TopicExport.exportMarkdown(context, d.title, selectedPosts, url))
                        else -> {
                            TopicExport.renderLongImages(context, d.title, selectedPosts, url, exportTheme, multiPage)
                        }
                    }
                }
                TopicExport.shareFiles(
                    context, files,
                    when (kind) {
                        "html" -> "text/html"; "md" -> "text/markdown"; else -> "image/png"
                    }
                )
            } catch (e: Exception) {
                session.showToast("导出失败：${e.message}")
            } finally { exportBusy = null }
        }
    }

    /**
     * 请求 AI 总结。默认走「同帖只请求一次」的缓存；force=true 用于卡片上的重新总结
     * （缓存命中时用户没别的办法再触发一次）。
     */
    fun runAi(force: Boolean = false) {
        val d = data ?: return
        if (aiBusy) return
        if (!force) {
            session.getAiSummary(tid)?.let {
                aiSummary = it
                return
            }
        }
        if (!AiClient.isConfigured(session.settings)) {
            aiError = "请先在「我的 → 应用设置」中配置 AI API"
            return
        }
        scope.launch {
            aiBusy = true; aiError = null
            try {
                // 是否解析评论区：false 时仅用楼主正文
                val posts = if (session.settings.aiIncludeComments) d.posts else d.posts.take(1)
                val content = posts.joinToString("\n\n") {
                    "[${it.authorName}]" + HtmlParser.htmlToText(it.contentHtml)
                }
                val summary = AiClient.summarize(session.settings, d.title, content) { }
                aiSummary = summary
                session.saveAiSummary(tid, summary, d.title)
            } catch (e: Exception) {
                aiError = e.message
            } finally { aiBusy = false }
        }
    }

    LaunchedEffect(tid) {
        if (pinnedSnapshot != null) {
            sortOrder = pinnedSnapshot.sort
            localReplyPage = pinnedSnapshot.localPage
            collapsedReplyIds = pinnedSnapshot.collapsed
            loadedMaxPage = pinnedSnapshot.topic.page
            data = pinnedSnapshot.topic
            danmaku = pinnedSnapshot.danmaku
            danmakuLocalOn = pinnedSnapshot.danmakuEnabled
            loading = false
            return@LaunchedEffect
        }
        // 返回本帖（比如从用户主页 pop 回来）时，若之前在评论区翻到过较深位置，
        // 用 topicPageCache 重建 1..loadedMaxPage 的全部 data，让 rememberLazyListState
        // 恢复的滚动位置能落到对应的评论上；缓存缺失则退化为常规加载。
        if (loadedMaxPage > 1) {
            var built: TopicPageData? = null
            var ok = true
            for (p in 1..loadedMaxPage) {
                val cached = session.getTopicPage(tid, p)
                if (cached == null) { ok = false; break }
                built = if (built == null) cached else built.copy(
                    posts = (built.posts + cached.posts).distinctBy { it.id },
                    page = cached.page,
                    totalPages = cached.totalPages,
                )
            }
            if (ok && built != null) {
                loading = false
                data = built
            } else load(initialPage)
        } else {
            load(initialPage)
        }
        loadDanmaku()
    }

    // 登录态变化（登录/退出后返回本帖）：重新加载当前页（缓存已随登录态清空，能看到评论区）
    var lastLoggedIn by remember { mutableStateOf(session.loginState.loggedIn) }
    LaunchedEffect(session.loginState.loggedIn) {
        if (session.loginState.loggedIn != lastLoggedIn) {
            lastLoggedIn = session.loginState.loggedIn
            val cur = data?.page ?: initialPage
            load(1)
            loadDanmaku()
        }
    }

    // 帖子数据就绪：已有缓存总结先回填（换页/返回本帖时不丢），
    // 未缓存则仅在开启「打开帖子自动总结」时才发请求 —— 否则等用户点卡片上的按钮
    LaunchedEffect(data) {
        if (data == null) return@LaunchedEffect
        val cached = session.getAiSummary(tid)
        if (cached != null) aiSummary = cached
        else if (session.settings.aiEnabled && session.settings.aiAutoRun) runAi()
    }

    // 评论区无限滚动：接近末尾自动加载下一页（仅无限滚动模式）
    val listLast by remember {
        derivedStateOf { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1 }
    }
    LaunchedEffect(
        listLast, data?.posts?.size, data?.page, data?.totalPages,
        loadingMore, loading
    ) {
        val d = data
        if (topicInfinite && d != null) {
            val total = listState.layoutInfo.totalItemsCount
            if (listLast >= total - 3 && d.posts.isNotEmpty() &&
                (d.page ?: 1) < (d.totalPages ?: 1) && !loadingMore && !loading
            ) {
                load(d.page + 1, append = true)
            }
        }
    }

    // 评论底栏样式（0=经典 1=液态玻璃，设置内可选）：经典为 Scaffold bottomBar 普通底栏；
    // 液态玻璃为悬浮胶囊——帖子列表画进 backdrop 图层，玻璃条实时折射身后滚动内容
    val replyGlass = session.replyBarStyle != 0
    BackHandler(enabled = topicSearchOpen) { topicSearchOpen = false; topicSearchListOpen = false }
    val glassBg = MaterialTheme.colorScheme.background
    val replyBackdrop = rememberLayerBackdrop {
        drawRect(glassBg)
        drawContent()
    }
    Scaffold(
        topBar = {
    if (topicSearchOpen) {
        fun openHit(index: Int) {
            if (topicSearchHits.isEmpty()) return
            topicSearchIndex = (index + topicSearchHits.size) % topicSearchHits.size
            searchKeyboard?.hide()
            pendingSearchPost = topicSearchHits[topicSearchIndex].post
        }
                Surface(
                    shape = RoundedCornerShape(bottomStart = 22.dp, bottomEnd = 22.dp),
                    tonalElevation = 8.dp,
                    shadowElevation = 12.dp,
                    modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 8.dp),
                ) {
                    Column(Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            IconButton(onClick = { topicSearchOpen = false; topicSearchListOpen = false }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, "关闭搜索")
                            }
                            OutlinedTextField(
                                value = topicSearchQuery,
                                onValueChange = { topicSearchQuery = it; topicSearchIndex = 0 },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(onSearch = { openHit(topicSearchIndex) }),
                                placeholder = { Text("搜索正文、代码块、评论或用户名") },
                                leadingIcon = { Icon(Icons.Filled.Search, null) },
                                trailingIcon = {
                                    if (topicSearchQuery.isNotEmpty()) IconButton(onClick = { topicSearchQuery = "" }) {
                                        Icon(Icons.Filled.Close, "清空")
                                    }
                                },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                if (topicSearchQuery.isBlank()) "正文 · 代码 · 评论 · 用户"
                                else if (topicSearchHits.isEmpty()) "没有找到结果"
                                else "${topicSearchIndex + 1} / ${topicSearchHits.size}",
                                modifier = Modifier.weight(1f).padding(start = 8.dp),
                                style = MaterialTheme.typography.labelMedium,
                            )
                            IconButton(enabled = topicSearchHits.isNotEmpty(), onClick = { openHit(topicSearchIndex - 1) }) {
                                Icon(Icons.Filled.KeyboardArrowUp, "上一个")
                            }
                            IconButton(enabled = topicSearchHits.isNotEmpty(), onClick = { openHit(topicSearchIndex + 1) }) {
                                Icon(Icons.Filled.ExpandMore, "下一个")
                            }
                            IconButton(enabled = topicSearchHits.isNotEmpty(), onClick = { topicSearchListOpen = !topicSearchListOpen }) {
                                Icon(Icons.AutoMirrored.Filled.FormatListBulleted, "全部结果")
                            }
                        }
                        if (!allSearchPagesLoaded && (data?.totalPages ?: 1) > 1) {
                            TextButton(onClick = { loadAllForSearch() }, enabled = !loadingMore) {
                                Text(if (loadingMore) "正在加载剩余评论…" else "当前为已加载内容，点击搜索全帖")
                            }
                        }
                        if (topicSearchListOpen && topicSearchHits.isNotEmpty()) {
                            LazyColumn(Modifier.fillMaxWidth().heightIn(max = (LocalConfiguration.current.screenHeightDp * 0.36f).dp)) {
                                itemsIndexed(topicSearchHits, key = { _, hit -> hit.post.id }) { index, hit ->
                                    val post = hit.post
                                    ListItem(
                                        headlineContent = { SearchHighlightedText(if (post.floor > 0) "#${post.floor} · ${post.authorName}" else "楼主 · ${post.authorName}", topicSearchQuery.trim(), maxLines = 1) },
                                        supportingContent = { SearchHighlightedText(hit.snippet, topicSearchQuery.trim(), maxLines = 2) },
                                        leadingContent = {
                                            if (index == topicSearchIndex) Icon(Icons.Filled.Search, null, tint = MaterialTheme.colorScheme.primary)
                                        },
                                        modifier = Modifier.clickable { topicSearchListOpen = false; openHit(index) },
                                    )
                                }
                            }
                        }
                    }
                }
    }
        },
        bottomBar = {
            // 底部快捷栏：回复入口 + 点赞主楼 + 收藏（未登录点击跳转登录）；常驻显示
            //（液态玻璃样式改走内容区悬浮覆盖层，不占 bottomBar 槽位）
            if (!replyGlass && data != null && error == null) {
                ReplyBar(
                    loggedIn = session.loginState.loggedIn,
                    liked = mainPost?.liked == true,
                    favorited = isFav,
                    likeCount = mainPost?.likeCount ?: 0,
                    showLike = mainPost?.canLike == true,
                    showFavorite = data?.canFavorite == true,
                    pinned = session.floatingTopicId == tid,
                    onReply = {
                        if (session.loginState.loggedIn) {
                            quoteText = ""; showReply = true
                        }
                        else nav.navigate("login")
                    },
                    onLike = {
                        if (session.loginState.loggedIn) mainPost?.let { doLike(it, 0) }
                        else nav.navigate("login")
                    },
                    onFavorite = {
                        if (session.loginState.loggedIn) onFavoriteClick()
                        else nav.navigate("login")
                    },
                    onPin = {
                        data?.let { page -> session.floatingTopicSnapshot = sb.linux.client.data.PinnedTopicSnapshot(
                            page, listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset,
                            localReplyPage, sortOrder, collapsedReplyIds, danmaku, danmakuLocalOn,
                        ) }
                        session.pinFloatingTopic(tid, data?.title.orEmpty())
                        nav.navigate("home") { popUpTo("home") { inclusive = false }; launchSingleTop = true }
                    },
                )
            }
        }
    ) { pad ->
        val d = data
        Box(
            Modifier
                .padding(pad)
                .onGloballyPositioned { contentTopInRoot = it.boundsInRoot().top }
        ) {
        when {
            loading -> LoadingBox()
            error != null -> ErrorBox(error!!) { load(initialPage) }
            d == null -> EmptyBox()
            else -> {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        // 液态玻璃样式：列表绘制进 backdrop 图层，供悬浮玻璃评论栏取样折射
                        .then(if (replyGlass) Modifier.layerBackdrop(replyBackdrop) else Modifier),
                    contentPadding = PaddingValues(top = if (topicSearchOpen) 8.dp else 72.dp, bottom = 8.dp)
                ) {
                    // ---------- 头部信息块（完整标题/统计/弹幕/AI/抽奖） ----------
                    item(key = "header") {
                        Column {
                            // 完整标题（不截断）+ 阅读量/评论数：单一富文本段落，
                            // 统计小字内联跟随标题文字末尾，随文字自然折行 ——
                            // 不用 Row/FlowRow 布局，不存在子项测量导致的预留空位
                            // 标题以全角【开头时做负 padding 视觉补偿：
                            // 【字形左下自带空腔，大字号下看起来像标题前有空位
                            // onGloballyPositioned：标题行滑出顶栏后，顶栏切换为显示标题（11）
                            Text(
                                buildAnnotatedString {
                                    withStyle(
                                        SpanStyle(fontWeight = FontWeight.Bold)
                                    ) { append(d.title) }
                                    withStyle(
                                        SpanStyle(
                                            fontSize = MaterialTheme.typography.labelMedium.fontSize,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    ) {
                                        append("  ${d.viewsText.ifBlank { "-" }}阅读 · ${d.repliesText.ifBlank { "${d.posts.size}" }}评论")
                                        readingStats?.let { (words, minutes) ->
                                            append(" · ${words}字 · 约${minutes}分钟")
                                        }
                                    }
                                },
                                style = MaterialTheme.typography.titleLarge.copy(
                                    lineHeight = MaterialTheme.typography.titleLarge.lineHeight * 1.15f
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onGloballyPositioned { titleBottomInRoot = it.boundsInRoot().bottom }
                                    .padding(
                                        start = if (d.title.startsWith("【")) 11.dp else 20.dp,
                                        end = 20.dp,
                                        top = 10.dp,
                                        bottom = 10.dp
                                    ),
                            )
                            // AI 总结：开启总开关后显示入口，默认等用户点按钮才请求
                            if (session.settings.aiEnabled) {
                                AiSummaryCard(
                                    summary = aiSummary,
                                    busy = aiBusy,
                                    error = aiError,
                                    enabled = AiClient.isConfigured(session.settings),
                                    onRun = { runAi() },
                                    onRegenerate = { runAi(force = true) },
                                    onDismiss = { aiSummary = null; aiError = null },
                                )
                            }
                            // 抽奖面板
                            d.lottery?.let { lp ->
                                LotteryPanelView(
                                    lp = lp,
                                    loggedIn = session.loginState.loggedIn,
                                    selfName = session.loginState.username,
                                    onJoin = { quoteText = ""; showReply = true },
                                    onJoinForm = { option ->
                                        scope.launch {
                                            try {
                                                // 令牌优先用帖子页自带的那份；页面可能来自缓存或登录前抓取，
                                                // 令牌为空/已随会话失效时会被源站以 CSRF 错误拒绝——为空先退回客户端取
                                                fun buildForm(token: String) = buildMap {
                                                    put("_csrf", token)
                                                    putAll(lp.joinFields)
                                                    if (option.isNotBlank()) put("option", option)
                                                }
                                                var resp = session.client.postForm(
                                                    lp.joinAction,
                                                    buildForm(d.csrf.ifBlank { session.client.csrf() })
                                                )
                                                // 令牌被拒（无法访问 CSRF 令牌）：强制刷新令牌重试一次
                                                if (resp.url.contains("form_error") && resp.html.contains("csrf", ignoreCase = true)) {
                                                    resp = session.client.postForm(
                                                        lp.joinAction,
                                                        buildForm(session.client.csrf(forceRefresh = true))
                                                    )
                                                }
                                                session.showToast(if (resp.url.contains("form_error")) HtmlParser.extractError(resp.html).ifBlank { "参与失败" } else "已参与抽奖")
                                                load(d.page, force = true)
                                            } catch (e: Exception) { session.showToast(e.message ?: "失败") }
                                        }
                                    },
                                )
                            }
                            if (d.poll != null || d.essenceReview != null || d.essenceApplication != null) {
                                TopicPollView(
                                    poll = d.poll,
                                    essence = d.essenceReview,
                                    application = d.essenceApplication,
                                    loggedIn = session.loginState.loggedIn,
                                    busy = pollBusy || essenceBusy,
                                    topUpBusy = essenceTopUpBusy,
                                    onLogin = { nav.navigate("login") },
                                    onApply = { application ->
                                        scope.launch {
                                            essenceBusy = true
                                            try {
                                                val form = buildList {
                                                    add("_csrf" to d.csrf.ifBlank { session.client.csrf() })
                                                    addAll(application.fields.filterKeys { it != "_csrf" }.toList())
                                                }
                                                val resp = session.client.postFormPairs(application.action, form)
                                                val ok = !resp.url.contains("form_error")
                                                session.showToast(if (ok) "申精已提交" else HtmlParser.extractError(resp.html).ifBlank { "申请失败" })
                                                if (ok) load(d.page, force = true)
                                            } catch (e: Exception) { session.showToast(e.message ?: "申请失败") }
                                            finally { essenceBusy = false }
                                        }
                                    },
                                    onTopUp = { amount ->
                                        val topUp = d.essenceReview?.topUp ?: return@TopicPollView
                                        scope.launch {
                                            essenceTopUpBusy = true
                                            try {
                                                val form = buildList {
                                                    add("_csrf" to d.csrf.ifBlank { session.client.csrf() })
                                                    addAll(topUp.fields.filterKeys { it != "_csrf" }.toList())
                                                    add(topUp.amountName to amount.toString())
                                                }
                                                val resp = session.client.postFormPairs(topUp.action, form)
                                                val ok = !resp.url.contains("form_error")
                                                session.showToast(if (ok) "已追加 $amount 积分" else HtmlParser.extractError(resp.html).ifBlank { "追加失败" })
                                                if (ok) load(d.page, force = true)
                                            } catch (e: Exception) { session.showToast(e.message ?: "追加失败") }
                                            finally { essenceTopUpBusy = false }
                                        }
                                    },
                                    onVote = { selected, reason ->
                                        val poll = d.poll ?: return@TopicPollView
                                        scope.launch {
                                            pollBusy = true
                                            try {
                                                val form = buildList {
                                                    add("_csrf" to d.csrf.ifBlank { session.client.csrf() })
                                                    addAll(poll.fields.toList())
                                                    selected.forEach { add(it.name to it.value) }
                                                    if (poll.reasonName.isNotBlank()) add(poll.reasonName to reason)
                                                }
                                                val resp = session.client.postFormPairs(poll.action, form)
                                                val ok = !resp.url.contains("form_error")
                                                session.showToast(if (ok) "投票已提交" else HtmlParser.extractError(resp.html).ifBlank { "投票失败" })
                                                if (ok) load(d.page, force = true)
                                            } catch (e: Exception) { session.showToast(e.message ?: "投票失败") }
                                            finally { pollBusy = false }
                                        }
                                    },
                                )
                            }
                            // 发卡帖兑换卡片
                            d.virtualCard?.let { vc ->
                                VirtualCardView(
                                    vc = vc,
                                    loggedIn = session.loginState.loggedIn,
                                    onBuy = {
                                        scope.launch {
                                            try {
                                                val form = buildMap {
                                                    put("_csrf", d.csrf)
                                                    putAll(vc.buyFields)
                                                }
                                                val resp = session.client.postForm(vc.buyAction, form)
                                                val ok = !resp.url.contains("form_error")
                                                session.showToast(if (ok) "兑换成功，卡片见本帖购买记录" else HtmlParser.extractError(resp.html).ifBlank { "兑换失败" })
                                                if (ok) load(d.page, force = true)
                                            } catch (e: Exception) { session.showToast(e.message ?: "失败") }
                                        }
                                    }
                                )
                            }
                        }
                    }
                    // ---------- 楼主正文 ----------
                    mainPost?.let { main ->
                        item(key = "main-${main.id}") {
                            PostCard(
                                post = main,
                                loggedIn = session.loginState.loggedIn,
                                isOpAuthor = main.authorName == d.posts.firstOrNull()?.authorName,
                                isMainPost = true,
                                onUser = { nav.navigate("user/${main.authorId}") },
                                onQuote = {
                                    quoteText = "@${main.authorName} "
                                    showReply = true
                                },
                                onLike = { doLike(main, 0) },
                                onCoin = { coinTarget = main },
                                onReport = { nav.navigate("report/${main.likeCoinType.ifBlank { "topic" }}/${main.id}") },
                                onFloor = { f -> jumpToFloor(f) },
                                onThread = { openThread(main) },
                                threadEnabled = threadEnabled,
                                threadReplyCount = threadReplyCount[main.floor] ?: 0,
                                onCopy = { copyPost = main },
                                onHeadingPosition = { index, y -> headingPositions[index] = y },
                                onBodyLayout = { top, height -> bodyTop = top; bodyHeight = height },
                                onSearchPosition = { y -> searchPositions[main.id] = topicSearchQuery to y },
                                searchHighlight = topicSearchQuery.takeIf { topicSearchOpen }.orEmpty(),
                                // 打赏按钮仅登录时显示（源站打赏页需登录），入口见操作行
                                onDonate = if (d.donateUrl != null && session.loginState.loggedIn) ({ showDonate = true }) else null,
                                onEditUser = { uid -> nav.navigate("user/$uid") },
                            )
                        }
                    }
                    // 打赏弹幕：展示在帖子正文与评论区之间（28）
                    if (danmakuLocalOn && danmaku.isNotEmpty()) {
                        item(key = "danmaku") { DanmakuBar(danmaku) }
                    }
                    // ---------- 评论区 ----------
                    if (replies.isNotEmpty() || d.loginVisibleReplies >= 0) {
                        item(key = "comments-header") {
                            Column {
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(start = 20.dp, end = 16.dp, top = 14.dp, bottom = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "全部回复",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        d.repliesText.ifBlank { "${replies.size}" },
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(Modifier.weight(1f))
                                    // 排序切换：紧凑药丸式（选中态 secondaryContainer，非选中 surfaceContainerHigh）
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(50))
                                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                            .padding(2.dp)
                                    ) {
                                        listOf("热度" to 0, "正序" to 1, "倒序" to 2).forEach { (label, idx) ->
                                            val sel = sortOrder == idx
                                            Text(
                                                label,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (sel) MaterialTheme.colorScheme.onSecondaryContainer
                                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(50))
                                                    .background(
                                                        if (sel) MaterialTheme.colorScheme.secondaryContainer
                                                        else androidx.compose.ui.graphics.Color.Transparent
                                                    )
                                                    .clickable {
                                                        if (sortOrder != idx) {
                                                            sortOrder = idx
                                                            session.saveCommentSortOrder(idx)
                                                        }
                                                    }
                                                    .padding(horizontal = 10.dp, vertical = 3.dp)
                                            )
                                        }
                                    }
                                }
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                    // 可见回复：树形模式 = 展平的树（含层级深度），平铺模式 = 本页切片（深度 0）
                    val visibleReplies: List<Pair<PostEntry, Int>> =
                        if (hasTree) flatTree else pagedReplies.map { it to 0 }
                    itemsIndexed(visibleReplies, key = { _, v -> "r-${v.first.id}" }) { _, (post, depth) ->
                        // 树形层级：每级缩进 14dp（最多 6 级封顶，避免深层挤压），
                        // 子回复左侧画竖向引导线连接父层级
                        val lineColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .padding(start = (depth.coerceAtMost(6) * 14).dp)
                                .drawBehind {
                                    if (depth > 0) drawLine(
                                        color = lineColor,
                                        start = Offset(6.dp.toPx(), 0f),
                                        end = Offset(6.dp.toPx(), size.height),
                                        strokeWidth = 2.dp.toPx(),
                                    )
                                }
                        ) {
                            PostCard(
                                post = post,
                                loggedIn = session.loginState.loggedIn,
                                isOpAuthor = post.authorName == d.posts.firstOrNull()?.authorName,
                                isMainPost = false,
                                highlight = highlightPostId == post.id,
                                // 「回复 #N」胶囊：所有带 parentFloor 的楼层都显示。
                                // 嵌套子回复虽然父卡片就在上方，但长对话树中父楼层可能已滚出
                                // 屏幕，胶囊提供快速跳转定位入口（点击定位到目标楼层）；
                                // 父楼作者头像置于楼层号前，正文首段的「@用户 #N」锚点已在解析层剥离
                                replyParentAvatar = post.parentFloor.takeIf { it > 0 }
                                    ?.let { repliesByFloor[it]?.avatarUrl } ?: "",
                                showReplyCapsule = !hasTree,
                                childReplyCount = treeChildCounts[post.id] ?: 0,
                                childrenCollapsed = post.id in collapsedReplyIds,
                                onToggleChildren = {
                                    collapsedReplyIds = if (post.id in collapsedReplyIds) collapsedReplyIds - post.id
                                    else collapsedReplyIds + post.id
                                },
                                onUser = { nav.navigate("user/${post.authorId}") },
                                onQuote = {
                                    quoteText = buildString {
                                        if (post.floor > 0) append("@${post.authorName} #${post.floor} ")
                                        else append("@${post.authorName} ")
                                    }
                                    showReply = true
                                },
                                onLike = { doLike(post, 0) },
                                onCoin = { coinTarget = post },
                                onReport = { nav.navigate("report/${post.likeCoinType.ifBlank { "reply" }}/${post.id}") },
                                onFloor = { f -> jumpToFloor(f) },
                                onThread = { openThread(post) },
                                threadEnabled = threadEnabled,
                                threadReplyCount = threadReplyCount[post.floor] ?: 0,
                                onCopy = { copyPost = post },
                                onEditUser = { uid -> nav.navigate("user/$uid") },
                                // 本地收藏评论（34）：操作行收藏按钮，收藏后图标填充
                                commentFavorited = post.id in favCommentIds,
                                onSearchPosition = { y -> searchPositions[post.id] = topicSearchQuery to y },
                                onFavComment = { toggleCommentFav(post) },
                                searchHighlight = topicSearchQuery.takeIf { topicSearchOpen }.orEmpty(),
                            )
                        }
                    }
                    // 未登录评论区提示
                    if (d.loginVisibleReplies >= 0) {
                        item(key = "login-prompt") {
                            LoginPromptCard(
                                count = d.loginVisibleReplies,
                                onLogin = { nav.navigate("login") }
                            )
                        }
                    }
                    item(key = "pagination-$replyModeTick") {
                        if (topicInfinite) {
                            // 无限滚动模式：加载更多按钮在列表底部
                            if ((d.page ?: 1) < (d.totalPages ?: 1)) {
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (loadingMore) CircularProgressIndicator(Modifier.size(22.dp))
                                    else TextButton(onClick = { load(d.page + 1, append = true) }) { Text("加载更多") }
                                }
                            }
                        } else {
                            // 翻页模式：翻页条在列表底部
                            PaginationBar(localReplyPage, localReplyTotal) { goLocalReplyPage(it) }
                        }
                    }
                    // 底部留白：为悬浮按钮和输入栏留出空间
                    //（液态玻璃评论栏为悬浮胶囊：52dp 高 + 10dp 底距）
                    item(key = "footer") { Spacer(Modifier.height(if (replyGlass) 62.dp else 64.dp)) }
                }
            }
            }

            // Markdown 目录与右侧阅读进度（36）。点进度条展开目录，标题按正文层级缩进。
            if (tocHeadings.isNotEmpty() && bodyVisible) {
                Surface(
                    onClick = { tocOpen = true },
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 3.dp)
                        .width(14.dp)
                        .height(150.dp),
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.9f),
                ) {
                    Box(Modifier.fillMaxSize().padding(4.dp)) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(readingProgress.coerceAtLeast(0.025f))
                                .align(Alignment.TopCenter)
                                .clip(RoundedCornerShape(50))
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    }
                }
            }

            if (tocOpen) {
                AlertDialog(
                    onDismissRequest = { tocOpen = false },
                    title = { Text("文章目录") },
                    text = {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth().heightIn(max = 440.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            itemsIndexed(tocHeadings) { index, heading ->
                                Text(
                                    heading.text,
                                    style = when (heading.level) {
                                        1 -> MaterialTheme.typography.titleMedium
                                        2 -> MaterialTheme.typography.titleSmall
                                        else -> MaterialTheme.typography.bodyMedium
                                    },
                                    fontWeight = if (heading.level <= 2) FontWeight.SemiBold else FontWeight.Normal,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            tocOpen = false
                                            scope.launch {
                                                listState.scrollToItem(1)
                                                withFrameNanos { }
                                                withFrameNanos { }
                                                headingPositions[index]?.let { y ->
                                                    listState.scrollBy(bodyTop + y - contentTopInRoot - topBarPx - 8f)
                                                }
                                            }
                                        }
                                        .padding(start = ((heading.level - 1).coerceAtLeast(0) * 14).dp, top = 9.dp, bottom = 9.dp),
                                )
                            }
                        }
                    },
                    confirmButton = { TextButton(onClick = { tocOpen = false }) { Text("关闭") } },
                )
            }

            // 一键回顶按钮
            val showBackTop by remember {
                derivedStateOf { listState.firstVisibleItemIndex > 1 }
            }
            AnimatedVisibility(
                visible = showBackTop,
                modifier = Modifier.align(Alignment.BottomEnd),
                enter = scaleIn(),
                exit = scaleOut(),
            ) {
                SmallFloatingActionButton(
                    onClick = {
                        returnPos = listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset
                        scope.launch { listState.animateScrollToItem(0) }
                    },
                    // 液态玻璃评论栏为悬浮覆盖层：回顶按钮需抬高避开玻璃条
                    modifier = Modifier.padding(end = 16.dp, bottom = if (replyGlass) 74.dp else 16.dp),
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.92f),
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ) {
                    Icon(Icons.Filled.KeyboardArrowUp, "回到顶部")
                }
            }

            // 评论区当前楼层胶囊（37）：实时显示首个可见回复的楼层号，
            // 点击弹出跳楼输入框；位于回顶按钮上方。
            // 暂时不启用（FLOOR_PILL_ENABLED = false，无开关；跳楼入口见帖子菜单）
            AnimatedVisibility(
                visible = FLOOR_PILL_ENABLED && currentFloor > 0,
                modifier = Modifier.align(Alignment.BottomEnd),
                enter = scaleIn(),
                exit = scaleOut(),
            ) {
                Surface(
                    onClick = { showFloorDialog = true },
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.92f),
                    modifier = Modifier.padding(end = 16.dp, bottom = 76.dp)
                ) {
                    Text(
                        "#$currentFloor",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                    )
                }
            }

            // 楼层定位后的「回到原位置」按钮（2.13）
            AnimatedVisibility(
                visible = returnPos != null,
                modifier = Modifier.align(Alignment.BottomCenter),
                enter = scaleIn(),
                exit = scaleOut(),
            ) {
                Row(
                    // 液态玻璃评论栏为悬浮覆盖层：抬高避开玻璃条
                    Modifier.padding(bottom = if (replyGlass) 74.dp else 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SmallFloatingActionButton(
                        onClick = {
                            returnPos?.let { (idx, off) ->
                                scope.launch { listState.animateScrollToItem(idx, off) }
                            }
                            returnPos = null
                        },
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.95f),
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    ) {
                        Row(
                            Modifier.padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Chat, null, Modifier.size(15.dp))
                            Text("回到原位置", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    // × 关闭：仅收起按钮，不回滚位置
                    SmallFloatingActionButton(
                        onClick = { returnPos = null },
                        containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.95f),
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ) {
                        Icon(Icons.Filled.Close, "关闭", Modifier.size(16.dp))
                    }
                }
            }

        // 常驻顶栏（覆盖层，不参与布局，杜绝内容顶到状态栏的抖动问题 2.1）：
        // 滑动到标题外后顶栏显示帖子标题，此外隐藏（11）
        if (!topicSearchOpen) TopAppBar(
            modifier = Modifier.align(Alignment.TopCenter),
            title = {
                AnimatedVisibility(visible = showTitleInTopBar, enter = fadeIn(), exit = fadeOut()) {
                    Text(
                        data?.title ?: "",
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
                actions = {
                    // 直达评论区（原跳转楼层按钮改为直接跳到评论区，2.10）
                    IconButton(onClick = { scrollToComments() }) {
                        Icon(Icons.AutoMirrored.Filled.Chat, "直达评论区")
                    }
                    // 帖子菜单常驻顶栏（原右上角悬浮按钮取消）
                    Box {
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(Icons.Filled.MoreVert, "菜单")
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            // 跳转楼层移入菜单（2.10）
                            DropdownMenuItem(
                                text = { Text("跳转楼层") },
                                leadingIcon = { Icon(Icons.Filled.FormatListNumbered, null) },
                                onClick = { menuOpen = false; showFloorDialog = true }
                            )
                            DropdownMenuItem(
                                text = { Text("帖内搜索") },
                                leadingIcon = { Icon(Icons.Filled.Search, null) },
                                onClick = { menuOpen = false; topicSearchOpen = true }
                            )
                            // 在浏览器打开（2.7）
                            DropdownMenuItem(
                                text = { Text("在浏览器打开") },
                                leadingIcon = { Icon(Icons.Filled.OpenInNew, null) },
                                onClick = {
                                    menuOpen = false
                                    runCatching {
                                        context.startActivity(
                                            android.content.Intent(
                                                android.content.Intent.ACTION_VIEW,
                                                android.net.Uri.parse(sb.linux.client.data.Endpoints.abs("/topic/$tid"))
                                            ).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                        )
                                    }.onFailure { session.showToast("打开失败：${it.message}") }
                                }
                            )
                            // 复制帖子链接
                            DropdownMenuItem(
                                text = { Text("复制链接") },
                                leadingIcon = { Icon(Icons.Filled.Link, null) },
                                onClick = {
                                    menuOpen = false
                                    clipboard.setText(AnnotatedString(sb.linux.client.data.Endpoints.abs("/topic/$tid")))
                                    session.showToast("链接已复制")
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(if (danmakuLocalOn) "关闭打赏弹幕" else "显示打赏弹幕") },
                                leadingIcon = { Icon(Icons.Filled.Subtitles, null) },
                                onClick = {
                                    menuOpen = false
                                    danmakuLocalOn = !danmakuLocalOn
                                    session.saveDanmaku(danmakuLocalOn)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(if (exportBusy != null) "导出中…" else "导出帖子") },
                                leadingIcon = { Icon(Icons.Filled.IosShare, null) },
                                onClick = { menuOpen = false; showExportDialog = true }
                            )
                            DropdownMenuItem(
                                text = { Text("收录到淘帖专辑") },
                                leadingIcon = { Icon(Icons.Filled.CollectionsBookmark, null) },
                                onClick = { menuOpen = false; nav.navigate("collectionActions?path=${android.net.Uri.encode("/topic/$tid")}") }
                            )
                            DropdownMenuItem(
                                text = { Text("刷新") },
                                leadingIcon = { Icon(Icons.Filled.Refresh, null) },
                                onClick = { menuOpen = false; load(data?.page ?: 1, force = true); loadDanmaku() }
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(if (topicInfinite) "切换为翻页模式" else "切换为无限滚动")
                                },
                                leadingIcon = { Icon(Icons.Filled.SwapVert, null) },
                                onClick = {
                                    menuOpen = false
                                    // 仅覆盖帖子分类的滚动模式（3.13），不影响其他分类
                                    session.saveScrollModeOverride("topic", !topicInfinite)
                                    localReplyPage = 1
                                    replyModeTick++    // 见 replyModeTick 注释：强制重组使新模式即时生效
                                    // 切换后回到顶部（参考首页切换逻辑）
                                    scope.launch { listState.scrollToItem(0) }
                                    session.showToast(
                                        if (!topicInfinite) "已切换为无限滚动" else "已切换为翻页模式"
                                    )
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                // Scaffold 内容区已含状态栏内边距，这里置零避免双重叠加
                // （此前顶栏被垫高，遮住标题顶部并与悬浮菜单按钮重叠）
                windowInsets = WindowInsets(0, 0, 0, 0),
            )

            // 液态玻璃评论底栏：悬浮于列表之上，实时折射身后滚动内容；
            // 经典样式则走 Scaffold bottomBar（见上），二者互斥
            if (replyGlass && d != null && error == null) {
                GlassReplyBar(
                    modifier = Modifier.align(Alignment.BottomCenter),
                    backdrop = replyBackdrop,
                    loggedIn = session.loginState.loggedIn,
                    liked = mainPost?.liked == true,
                    favorited = isFav,
                    likeCount = mainPost?.likeCount ?: 0,
                    showLike = mainPost?.canLike == true,
                    showFavorite = d.canFavorite,
                    pinned = session.floatingTopicId == tid,
                    onReply = {
                        if (session.loginState.loggedIn) {
                            quoteText = ""; showReply = true
                        }
                        else nav.navigate("login")
                    },
                    onLike = {
                        if (session.loginState.loggedIn) mainPost?.let { doLike(it, 0) }
                        else nav.navigate("login")
                    },
                    onFavorite = {
                        if (session.loginState.loggedIn) onFavoriteClick()
                        else nav.navigate("login")
                    },
                    onPin = {
                        session.pinFloatingTopic(tid, d.title)
                        nav.navigate("home") { popUpTo("home") { inclusive = false }; launchSingleTop = true }
                    },
                )
            }
        }
    }

    // 取消收藏确认框（12）：已收藏状态下再次点击收藏图标时弹出；
    // 关闭「设置 → 浏览设置 → 取消收藏确认」后不再提示、直接取消收藏
    if (showUnfavDialog) {
        AlertDialog(
            onDismissRequest = { showUnfavDialog = false },
            title = { Text("取消收藏") },
            text = { Text("确定取消收藏吗？") },
            confirmButton = {
                TextButton(onClick = { showUnfavDialog = false; doFavorite() }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showUnfavDialog = false }) { Text("取消") }
            }
        )
    }

    // 回复弹窗（带人机验证时需用户作答，可「换一题」刷新验证码）
    if (showReply) {
        LaunchedEffect(Unit) { captchaOverride = null }
        val activeCaptcha = captchaOverride ?: data?.replyCaptcha
        ReplyDialog(
            session = session,
            initial = quoteText,
            captcha = activeCaptcha,
            onRefreshCaptcha = {
                scope.launch {
                    try {
                        // 重新拉取帖子页：源站每次渲染都会签发新的题目与 token
                        //（注意源站分页参数是 p，page 会被忽略）
                        val resp = session.client.get("/topic/$tid?p=${data?.page ?: 1}")
                        val c = HtmlParser.parseNativeCaptcha(resp.html)
                        if (c != null) {
                            captchaOverride = c
                        } else session.showToast("未找到人机验证")
                    } catch (e: Exception) {
                        session.showToast(e.message ?: "刷新失败")
                    }
                }
            },
            onDismiss = { showReply = false },
            onSubmit = { body, captchaAnswer, onDone ->
                scope.launch {
                    try {
                        // 令牌优先用帖子页自己带的那份（回复框里就有）：不必再多打一次首页，
                        // 也避开首页偶发被访问盾拦成挑战页导致取不到令牌、回复直接失败
                        val csrf = data?.csrf?.takeIf { it.isNotBlank() } ?: session.client.csrf()
                        val form = buildMap {
                            put("_csrf", csrf)
                            put("topic_id", tid.toString())
                            put("body", body)
                            // 源站当前表单只提交 topic_id/body；评论树由正文开头严格的
                            // “@用户名 #楼层 ”前缀识别。不要附加不存在的父节点字段。
                            // 人机验证：用户答案 + 客户端计算 PoW（用弹窗当前展示的那份，而非页面初始解析的）
                            activeCaptcha?.let { c ->
                                put("native_captcha_token", c.token)
                                put("native_captcha_answer", captchaAnswer)
                                put("native_captcha_pow", sb.linux.client.data.LsbClient.solvePow(c.powPrefix, c.powZeros))
                            }
                        }
                        val resp = session.client.postForm("/reply_edit", form)
                        if (resp.url.contains("form_error")) {
                            session.showToast(HtmlParser.extractError(resp.html).ifBlank { "回复失败" })
                            // 验证码 token 多半已失效：自动换一题，用户重填即可
                            if (activeCaptcha != null) {
                                runCatching {
                                    val r2 = session.client.get("/topic/$tid?p=${data?.page ?: 1}")
                                    HtmlParser.parseNativeCaptcha(r2.html)?.let { captchaOverride = it }
                                }
                            }
                        } else {
                            session.showToast("回复成功")
                            session.settings.recordUsageEvent("reply", tid, data?.title.orEmpty())
                            showReply = false
                            // 强制刷新末页并与已加载页合并（append）：
                            // 整页替换会丢弃前序页——树形评论的父楼层多在前面页，
                            // 丢失后自己的新回复挂不上树（显示为顶层），且会把用户踢回第 1 页。
                            // force 清缓存确保新回复立即可见（否则命中旧缓存表现为「评论显示不完全」）
                            val responsePage = runCatching {
                                android.net.Uri.parse(resp.url).getQueryParameter("p")?.toIntOrNull()
                            }.getOrNull() ?: (data?.totalPages ?: 1)
                            load(responsePage, append = true, force = true)
                        }
                    } catch (e: Exception) {
                        session.showToast(e.message ?: "回复失败")
                    } finally { onDone() }
                }
            }
        )
    }

    // 跳楼弹窗（菜单入口与楼层胶囊共用；胶囊进入时预填当前楼层）
    if (showFloorDialog) {
        FloorJumpDialog(
            maxFloor = data?.repliesText?.toIntOrNull() ?: ((data?.totalPages ?: 1) * 20),
            initial = currentFloor,
            onDismiss = { showFloorDialog = false },
            onJump = {
                showFloorDialog = false
                jumpToFloor(it)
            }
        )
    }


    // #N 对话串联弹窗
    if (threadLoading || threadError != null || threadPosts != null) {
        ThreadDialog(
            posts = threadPosts,
            loading = threadLoading,
            error = threadError,
            currentPostId = -1L,
            onDismiss = { threadPosts = null; threadError = null },
            onUser = { uid -> nav.navigate("user/$uid") },
            onJumpFloor = { f ->
                threadPosts = null; threadError = null
                jumpToFloor(f)
            },
        )
    }

    // 评论投币档位完全跟随源站 data-tiers，不向接口提交未声明的金额。
    coinTarget?.let { target ->
        CoinDialog(
            onDismiss = { coinTarget = null },
            tiers = target.coinTiers.ifEmpty { DEFAULT_COMMENT_REWARD_TIERS },
            onConfirm = { points, reason ->
                coinTarget = null
                doLike(target, points, reason)
            }
        )
    }

    // 长按正文操作菜单（复制全文 / 自由复制 / 查看该用户在本帖的所有回复）
    copyPost?.let { target ->
        PostCopySheet(
            post = target,
            onDismiss = { copyPost = null },
            onCopied = { session.showToast("已复制到剪贴板") },
            onViewUserReplies = {
                val d = data ?: return@PostCopySheet
                userRepliesTitle = "${target.authorName} 在本帖的回复"
                // 不把楼主写的帖子正文算上，仅统计其回复（2.16）
                userReplies = d.posts.filter { it.authorId == target.authorId && it.floor > 0 }
            },
        )
    }

    // 某用户在本帖的所有回复
    if (userReplies != null) {
        ThreadDialog(
            posts = userReplies,
            loading = false,
            error = if (userReplies.isNullOrEmpty()) "TA 在本帖还没有回复" else null,
            currentPostId = -1L,
            onDismiss = { userReplies = null },
            onUser = { uid -> nav.navigate("user/$uid") },
            title = userRepliesTitle,
            countText = { "共 $it 条回复" },
            onJumpFloor = { f ->
                userReplies = null
                jumpToFloor(f)
            },
        )
    }

    // 导出弹窗：统一入口，选择导出格式
    if (showExportDialog) {
        ExportDialog(
            posts = data?.posts.orEmpty(),
            busy = exportBusy,
            loadingPosts = loadingMore,
            onLoadAll = { loadAllForSearch() },
            allLoaded = allSearchPagesLoaded,
            onDismiss = { if (exportBusy == null) showExportDialog = false },
            onExport = { kind, scopeMode, fromFloor, toFloor, multiPage, selectedIds ->
                showExportDialog = false
                export(kind, scopeMode, fromFloor, toFloor, multiPage, selectedIds)
            }
        )
    }

    // 打赏底部弹窗（28）：从底部滑出，不再跳转独立页面
    if (showDonate) {
        DonateSheet(
            session = session,
            tid = tid,
            onDismiss = { showDonate = false },
            onSuccess = {
                showDonate = false
                session.showToast("打赏成功")
                // 刷新打赏弹幕（新打赏会追加一条记录），更新缓存
                scope.launch {
                    runCatching {
                        val items = session.client.getDonateFeed(tid)
                        danmaku = items
                        session.saveDanmaku(tid, items)
                    }
                }
            }
        )
    }
}

@Composable
private fun TopicPollView(
    poll: TopicPoll?,
    essence: EssenceReview?,
    application: EssenceApplication?,
    loggedIn: Boolean,
    busy: Boolean,
    topUpBusy: Boolean,
    onLogin: () -> Unit,
    onApply: (EssenceApplication) -> Unit,
    onTopUp: (Int) -> Unit,
    onVote: (List<TopicPollOption>, String) -> Unit,
) {
    val vote = poll
    var selectedValues by remember(vote?.action, vote?.options) {
        mutableStateOf(vote?.options.orEmpty().filter { it.selected }.map { it.value }.toSet())
    }
    var reason by remember(vote?.action) { mutableStateOf("") }
    val topUp = essence?.topUp
    var topUpText by remember(topUp?.action) { mutableStateOf(topUp?.defaultAmount.orEmpty()) }
    var confirmTopUp by remember { mutableStateOf<Int?>(null) }
    @Composable
    fun optionCard(option: TopicPollOption, modifier: Modifier = Modifier) {
        val currentPoll = vote ?: return
        val selected = option.value in selectedValues
        Surface(
            shape = RoundedCornerShape(13.dp),
            color = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                else MaterialTheme.colorScheme.surfaceContainerHigh,
            border = if (selected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
            modifier = modifier,
        ) {
            Row(
                Modifier.fillMaxWidth().clickable(
                    enabled = !currentPoll.closed && currentPoll.unavailableReason.isBlank() && !option.disabled
                ) {
                    selectedValues = if (currentPoll.multiple) {
                        if (option.value in selectedValues) selectedValues - option.value else selectedValues + option.value
                    } else setOf(option.value)
                }.padding(horizontal = 8.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (currentPoll.multiple) Checkbox(selected, null, enabled = !currentPoll.closed && currentPoll.unavailableReason.isBlank() && !option.disabled)
                else RadioButton(selected, null, enabled = !currentPoll.closed && currentPoll.unavailableReason.isBlank() && !option.disabled)
                Text(option.label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            }
        }
    }
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    Modifier.size(38.dp).clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.AutoAwesome, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
                Column(Modifier.weight(1f)) {
                    Text(essence?.title ?: vote?.title.orEmpty(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        when {
                            essence?.subtitle?.isNotBlank() == true -> essence.subtitle
                            vote?.unavailableReason?.isNotBlank() == true -> vote.unavailableReason
                            vote?.closed == true -> "投票已结束"
                            vote?.multiple == true -> "可选择多项"
                            vote != null -> "请选择一项"
                            else -> "申精评议"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (essence?.status in setOf("voting", "featured", "pending_approval") || (vote != null && !vote.closed && vote.unavailableReason.isBlank()))
                        MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
                ) {
                    Text(
                        essence?.statusLabel?.ifBlank { null }
                            ?: if (vote?.closed == true) "已结束" else if (vote?.unavailableReason?.isNotBlank() == true) "不可参与" else "进行中",
                        Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (essence?.status in setOf("voting", "featured", "pending_approval") || (vote != null && !vote.closed && vote.unavailableReason.isBlank()))
                            MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            essence?.takeIf { it.target > 0 || it.progressText.isNotBlank() }?.let { review ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        Modifier.weight(1f).height(10.dp).clip(RoundedCornerShape(50))
                            .background(if (review.progress < 0) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceContainerHighest)
                    ) {
                        Box(
                            Modifier.fillMaxHeight()
                                .fillMaxWidth((review.progress.coerceAtLeast(0).toFloat() / review.target.coerceAtLeast(1)).coerceIn(0f, 1f))
                                .background(if (review.progress < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                        )
                    }
                    Text(review.progressText.ifBlank { "${review.progress} / ${review.target}" }, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
                if (review.progressNote.isNotBlank()) Text(
                    review.progressNote,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            essence?.meta?.takeIf { it.isNotEmpty() }?.let { items ->
                FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items.forEach { StatChip(null, it) }
                }
            }
            essence?.takeIf { it.poolTitle.isNotBlank() || it.poolItems.isNotEmpty() }?.let { review ->
                Surface(shape = RoundedCornerShape(13.dp), color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.55f)) {
                    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Paid, null, Modifier.size(17.dp), tint = MaterialTheme.colorScheme.onTertiaryContainer)
                            Spacer(Modifier.width(6.dp))
                            Text(review.poolTitle.ifBlank { "积分奖池" }, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        }
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            review.poolItems.forEach { Text(it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onTertiaryContainer) }
                        }
                    }
                }
            }
            val panelNote = essence?.actionNote.orEmpty().ifBlank { vote?.note.orEmpty() }
            if (panelNote.isNotBlank()) Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
                Text(
                    panelNote,
                    Modifier.fillMaxWidth().padding(horizontal = 11.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (vote != null && (essence != null || vote.title.contains("精华")) && vote.options.size == 2) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    vote.options.forEach { option -> optionCard(option, Modifier.weight(1f)) }
                }
            } else vote?.options.orEmpty().forEach { option -> optionCard(option, Modifier.fillMaxWidth()) }
            if (vote?.reasonName?.isNotBlank() == true) OutlinedTextField(
                value = reason, onValueChange = { reason = it.take(vote.reasonMaxLength) },
                label = { Text(if (vote.reasonRequired) "投票理由（必填）" else "投票理由") },
                supportingText = { Text(vote.reasonHint.ifBlank { "${reason.length}/${vote.reasonMaxLength}" }) },
                modifier = Modifier.fillMaxWidth(), enabled = !busy && !vote.closed,
                shape = RoundedCornerShape(14.dp),
            )
            if (vote != null) when {
                vote.unavailableReason.isNotBlank() -> Text(
                    vote.unavailableReason,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                vote.closed -> Text("当前不可投票", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                !loggedIn -> Button(onClick = onLogin, Modifier.fillMaxWidth()) { Text("登录后投票") }
                else -> Button(
                    onClick = { onVote(vote.options.filter { it.value in selectedValues }, reason.trim()) },
                    enabled = !busy && selectedValues.isNotEmpty() && vote.action.isNotBlank() && (!vote.reasonRequired || reason.isNotBlank()),
                    modifier = Modifier.align(Alignment.End),
                    shape = RoundedCornerShape(50),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 10.dp),
                ) { Text(if (busy) "提交中…" else vote.submitLabel) }
            }
            if (application != null) {
                Button(
                    onClick = { if (loggedIn) onApply(application) else onLogin() },
                    enabled = !busy && (!loggedIn || application.enabled) && application.action.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(50),
                ) { Text(if (busy) "提交中…" else if (loggedIn) application.label else "登录后申请加精") }
            }
            if (topUp != null) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = topUpText,
                        onValueChange = { value -> topUpText = value.filter(Char::isDigit).take(9) },
                        label = { Text(topUp.label) },
                        supportingText = {
                            Text(topUp.hint.ifBlank {
                                if (topUp.max == Int.MAX_VALUE) "至少 ${topUp.min} 积分" else "${topUp.min}～${topUp.max} 积分"
                            })
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        enabled = !topUpBusy,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f),
                    )
                    val amount = topUpText.toIntOrNull()
                    FilledTonalButton(
                        onClick = { confirmTopUp = amount },
                        enabled = !topUpBusy && topUp.enabled && amount != null && amount in topUp.min..topUp.max,
                        modifier = Modifier.padding(top = 8.dp),
                    ) { Text(if (topUpBusy) "追加中…" else topUp.submitLabel) }
                }
            }
        }
    }
    confirmTopUp?.let { amount ->
        AlertDialog(
            onDismissRequest = { confirmTopUp = null },
            title = { Text("确认追加积分") },
            text = { Text("将向本帖的申精奖池追加 $amount 积分。提交后积分会按源站规则扣除。") },
            confirmButton = {
                TextButton(onClick = { confirmTopUp = null; onTopUp(amount) }) { Text("确认追加") }
            },
            dismissButton = { TextButton(onClick = { confirmTopUp = null }) { Text("取消") } },
        )
    }
}

// ==================== 通用小组件 ====================

/** 统计胶囊：图标 + 文本 */
@Composable
private fun StatChip(icon: ImageVector?, text: String) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Row(
            Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (icon != null) {
                Icon(icon, null, Modifier.size(13.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                text,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** 帖内轻量操作：图标 + 文字，无边框 */
@Composable
private fun PostAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    iconTint: Color? = null,
) {
    Row(
        Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(icon, null, Modifier.size(15.dp), tint = iconTint ?: tint)
        if (label.isNotBlank()) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = tint)
        }
    }
}

/** 底部快捷回复栏：伪输入条 + 点赞主楼 + 收藏 */
@Composable
private fun ReplyBar(
    loggedIn: Boolean,
    liked: Boolean,
    favorited: Boolean,
    likeCount: Int,
    showLike: Boolean,
    showFavorite: Boolean,
    pinned: Boolean,
    onReply: () -> Unit,
    onLike: () -> Unit,
    onFavorite: () -> Unit,
    onPin: () -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 3.dp) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.weight(1.8f).height(52.dp),
            ) {
                Row(
                    Modifier.fillMaxSize().padding(horizontal = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        if (loggedIn) "说点什么…" else "登录后回复",
                        Modifier.weight(1f).clip(RoundedCornerShape(50)).clickable(onClick = onReply).padding(horizontal = 10.dp, vertical = 10.dp),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                    if (showLike) TopicBarAction(if (liked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder, if (likeCount > 0) "$likeCount" else "点赞", liked, onLike)
                }
            }
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.weight(1f).height(52.dp),
            ) {
                Row(
                    Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    TopicBarAction(Icons.Filled.PushPin, if (pinned) "已钉住" else "钉住", pinned, onPin)
                    if (showFavorite) TopicBarAction(if (favorited) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder, if (favorited) "已收藏" else "收藏", favorited, onFavorite)
                }
            }
        }
    }
}

@Composable
private fun TopicBarAction(icon: ImageVector, label: String, active: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(horizontal = 7.dp, vertical = 3.dp),
    ) {
        val color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        Icon(icon, label, Modifier.size(18.dp), tint = color)
        Text(label, style = MaterialTheme.typography.labelSmall, color = color, maxLines = 1)
    }
}

/** 液态玻璃评论底栏（评论底栏样式设置开启时）：悬浮胶囊，实时折射身后滚动的帖子内容
 *  （vibrancy 提饱和 + blur + lens 深度折射/边缘色散）；伪输入条 + 点赞主楼 + 收藏，
 *  交互与经典 ReplyBar 完全一致；Scaffold 空置 bottomBar 后内容区已含导航栏内边距，
 *  这里只需底部悬浮边距，无需再垫 navigationBarsPadding */
@Composable
private fun GlassReplyBar(
    modifier: Modifier = Modifier,
    backdrop: Backdrop,
    loggedIn: Boolean,
    liked: Boolean,
    favorited: Boolean,
    likeCount: Int,
    showLike: Boolean,
    showFavorite: Boolean,
    pinned: Boolean,
    onReply: () -> Unit,
    onLike: () -> Unit,
    onFavorite: () -> Unit,
    onPin: () -> Unit,
) {
    // 玻璃表面色调：浅色主题垫白、深色主题垫黑，保证内容可读性（同玻璃底栏）
    val light = MaterialTheme.colorScheme.background.luminance() > 0.5f
    val surfaceTint = if (light) Color.White.copy(alpha = 0.35f) else Color.Black.copy(alpha = 0.4f)
    Row(
        modifier.padding(start = 14.dp, end = 14.dp, bottom = 10.dp).fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            Modifier.weight(1f).height(52.dp).drawBackdrop(
                backdrop, { RoundedCornerShape(50) },
                effects = { vibrancy(); blur(6.dp.toPx()); lens(18.dp.toPx(), 18.dp.toPx(), depthEffect = true, chromaticAberration = true) },
                onDrawSurface = { drawRect(surfaceTint) },
            ).padding(horizontal = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                if (loggedIn) "说点什么…" else "登录后回复",
                Modifier.weight(1f).clip(RoundedCornerShape(50)).clickable(onClick = onReply).padding(horizontal = 9.dp, vertical = 9.dp),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            if (showLike) TopicBarAction(if (liked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder, if (likeCount > 0) "$likeCount" else "点赞", liked, onLike)
        }
        Row(
            Modifier.weight(1f).height(52.dp).drawBackdrop(
                backdrop, { RoundedCornerShape(50) },
                effects = { vibrancy(); blur(6.dp.toPx()); lens(18.dp.toPx(), 18.dp.toPx(), depthEffect = true, chromaticAberration = true) },
                onDrawSurface = { drawRect(surfaceTint) },
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            TopicBarAction(Icons.Filled.PushPin, if (pinned) "已钉住" else "钉住", pinned, onPin)
            if (showFavorite) TopicBarAction(if (favorited) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder, if (favorited) "已收藏" else "收藏", favorited, onFavorite)
        }
    }
}

/** 未登录评论区引导卡 */
@Composable
private fun LoginPromptCard(count: Int, onLogin: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Column(
            Modifier.padding(vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Lock, null, Modifier.size(24.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(12.dp))
            Text("还有 $count 条回复", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text(
                "登录后即可查看全部评论并参与讨论",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))
            Button(onClick = onLogin, shape = RoundedCornerShape(50)) { Text("立即登录") }
        }
    }
}

// ==================== 楼层卡片 ====================

/** 树形评论节点（源站 data-quote-threads-parent-floor 还原的多级回复结构） */
internal data class ReplyNode(val post: PostEntry, val depth: Int, val children: List<ReplyNode>)

/**
 * 由已加载回复构建评论树：parentFloor 指向的楼层已加载且楼层号更小时挂为其子节点，
 * 否则视为顶层（父楼层必然小于子楼层——回复只能针对已有楼层，天然无环）。
 * 顶层节点按排序模式（0 热度 / 1 正序 / 2 倒序）排列，子节点恒按楼层正序。
 */
internal fun buildReplyTree(replies: List<PostEntry>, sortOrder: Int): List<ReplyNode> {
    val byFloor = replies.filter { it.floor > 0 }.associateBy { it.floor }
    val childrenOf = HashMap<Int, MutableList<PostEntry>>()
    val tops = mutableListOf<PostEntry>()
    replies.sortedBy { it.floor }.forEach { p ->
        val pf = p.parentFloor
        if (pf > 0 && pf < p.floor && byFloor.containsKey(pf)) {
            childrenOf.getOrPut(pf) { mutableListOf() }.add(p)
        } else tops.add(p)
    }
    val ordered = when (sortOrder) {
        2 -> tops.sortedByDescending { it.floor }
        0 -> tops.sortedWith(compareByDescending<PostEntry> { it.likeCount }.thenBy { it.floor })
        else -> tops
    }
    fun node(p: PostEntry, depth: Int): ReplyNode =
        ReplyNode(p, depth, (childrenOf[p.floor] ?: emptyList()).sortedBy { it.floor }.map { node(it, depth + 1) })
    return ordered.map { node(it, 0) }
}

/** 树 → 展平列表（楼层 + 层级深度），子树整棵跟随顶层节点（翻页按顶层分页不拆散对话） */
internal fun flattenTree(nodes: List<ReplyNode>, collapsedIds: Set<Long> = emptySet()): List<Pair<PostEntry, Int>> =
    nodes.flatMap { n ->
        listOf(n.post to n.depth) + if (n.post.id in collapsedIds) emptyList() else flattenTree(n.children, collapsedIds)
    }

private data class SmartDecodedContent(val type: String, val source: String, val decoded: String)

/** 保守识别常用文本编码：只展示高可读结果，二进制、乱码和过短片段不提示。 */
private fun detectSmartEncodedContent(html: String): List<SmartDecodedContent> {
    val plain = HtmlParser.htmlToText(html)
    val found = mutableListOf<SmartDecodedContent>()
    fun add(type: String, source: String, result: String?) {
        val decoded = result?.trim().orEmpty()
        if (source.length > 8_000 || decoded.length < 4 || decoded == source || decoded.contains('\uFFFD')) return
        val readable = decoded.count { it == '\n' || it == '\r' || it == '\t' || !it.isISOControl() }
        if (readable.toFloat() / decoded.length >= 0.9f) found += SmartDecodedContent(type, source, decoded)
    }
    Regex("(?<![A-Za-z0-9_+/=-])([A-Za-z0-9_+/-]{16,}={0,2})(?![A-Za-z0-9_+/=-])")
        .findAll(plain).forEach { m ->
            val source = m.groupValues[1]
            val padded = source + "=".repeat((4 - source.length % 4) % 4)
            val flags = if (source.contains('-') || source.contains('_')) android.util.Base64.URL_SAFE else android.util.Base64.DEFAULT
            val decoded = runCatching { android.util.Base64.decode(padded, flags).toString(Charsets.UTF_8) }.getOrNull()
            add(if (flags == android.util.Base64.URL_SAFE) "Base64 URL" else "Base64", source, decoded)
        }
    Regex("(?<![0-9A-Fa-f])([0-9A-Fa-f]{16,})(?![0-9A-Fa-f])").findAll(plain).forEach { m ->
        val source = m.groupValues[1]
        if (source.length % 2 == 0) add("Hex", source, runCatching {
            source.chunked(2).map { it.toInt(16).toByte() }.toByteArray().toString(Charsets.UTF_8)
        }.getOrNull())
    }
    Regex("(?:%[0-9A-Fa-f]{2}){4,}").findAll(plain).forEach { m -> add("URL", m.value, runCatching { java.net.URLDecoder.decode(m.value, "UTF-8") }.getOrNull()) }
    Regex("(?:\\\\u[0-9A-Fa-f]{4}){2,}").findAll(plain).forEach { m ->
        add("Unicode", m.value, Regex("\\\\u([0-9A-Fa-f]{4})").replace(m.value) { it.groupValues[1].toInt(16).toChar().toString() })
    }
    Regex("(?i)rot13\\s*[:：]\\s*([A-Za-z][A-Za-z ]{3,})").findAll(plain).forEach { m ->
        val source = m.groupValues[1].trim()
        add("ROT13", source, source.map { c -> when (c) { in 'a'..'z' -> 'a' + (c - 'a' + 13) % 26; in 'A'..'Z' -> 'A' + (c - 'A' + 13) % 26; else -> c } }.joinToString(""))
    }
    return found.distinctBy { it.type to it.source }.take(6)
}

/**
 * 楼层条目。
 * 楼主正文 = 柔和主题色圆角容器；回复 = 扁平列表项（细分隔线分隔）。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PostCard(
    post: PostEntry,
    loggedIn: Boolean,
    isOpAuthor: Boolean,
    onUser: () -> Unit,
    onQuote: () -> Unit,
    onLike: () -> Unit,
    onCoin: () -> Unit,
    onReport: () -> Unit,
    onFloor: (Int) -> Unit,
    onThread: () -> Unit,
    isMainPost: Boolean = false,
    highlight: Boolean = false,
    onCopy: () -> Unit = {},
    onDonate: (() -> Unit)? = null,
    onEditUser: (Long) -> Unit = {},
    threadEnabled: Boolean = true,
    threadReplyCount: Int = 0,
    replyParentAvatar: String = "",
    showReplyCapsule: Boolean = true,
    childReplyCount: Int = 0,
    childrenCollapsed: Boolean = false,
    onToggleChildren: () -> Unit = {},
    commentFavorited: Boolean = false,
    onFavComment: (() -> Unit)? = null,
    onHeadingPosition: (Int, Float) -> Unit = { _, _ -> },
    onBodyLayout: (Float, Float) -> Unit = { _, _ -> },
    searchHighlight: String = "",
    onSearchPosition: (Float) -> Unit = {},
) {
    if (isMainPost) {
        // ---------- 楼主正文：正文长按为系统原生文本选择，不再弹功能菜单（2.6） ----------
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.13f))
                .padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 2.dp)
        ) {
            PostCardContent(
                post, loggedIn, isOpAuthor,
                avatarSize = 40,
                onUser, onQuote, onLike, onCoin, onReport, onFloor, onThread,
                showFloorBadge = false,
                onCopy = onCopy,
                onDonate = onDonate,
                onEditUser = onEditUser,
                threadEnabled = threadEnabled,
                threadReplyCount = threadReplyCount,
                showReplyCapsule = showReplyCapsule,
                replyParentAvatar = replyParentAvatar,
                childReplyCount = childReplyCount,
                childrenCollapsed = childrenCollapsed,
                onToggleChildren = onToggleChildren,
                onHeadingPosition = onHeadingPosition,
                onBodyLayout = onBodyLayout,
                searchHighlight = searchHighlight,
                onSearchPosition = onSearchPosition,
            )
        }
    } else {
        // ---------- 回复：单独卡片容器，长按弹功能菜单；高亮 = 楼层定位（2.13） ----------
        // 背景色 + 描边同时作为定位提示，让被定位楼层一眼可见
        val bg by animateColorAsState(
            targetValue = if (highlight) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.62f)
            else MaterialTheme.colorScheme.surfaceContainer,
            animationSpec = tween(400),
            label = "postBg"
        )
        val borderColor by animateColorAsState(
            targetValue = if (highlight) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0f),
            animationSpec = tween(400),
            label = "postBorder"
        )
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = bg,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp)
                // 先裁剪再挂手势，长按遮罩与圆角容器完全贴合（2.17）
                .clip(RoundedCornerShape(16.dp))
                .border(
                    width = if (highlight) 2.dp else 0.dp,
                    color = borderColor,
                    shape = RoundedCornerShape(16.dp),
                )
                .combinedClickable(onClick = {}, onLongClick = onCopy)
        ) {
            Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                PostCardContent(
                    post, loggedIn, isOpAuthor,
                    avatarSize = 34,
                    onUser, onQuote, onLike, onCoin, onReport, onFloor, onThread,
                    showFloorBadge = true,
                    onCopy = onCopy,
                    onDonate = onDonate,
                    onEditUser = onEditUser,
                    threadEnabled = threadEnabled,
                    threadReplyCount = threadReplyCount,
                    showReplyCapsule = showReplyCapsule,
                replyParentAvatar = replyParentAvatar,
                    childReplyCount = childReplyCount,
                    childrenCollapsed = childrenCollapsed,
                    onToggleChildren = onToggleChildren,
                    commentFavorited = commentFavorited,
                    onFavComment = onFavComment,
                    onHeadingPosition = onHeadingPosition,
                onBodyLayout = onBodyLayout,
                    searchHighlight = searchHighlight,
                    onSearchPosition = onSearchPosition,
                )
            }
        }
    }
}

/** 帖内统一徽章胶囊：UID / 楼主 / 头衔（组别）/ 称号共用同一规格，
 *  labelSmall 字号 · 16dp 最小高度 · 45% 透明背景 · 6dp/1dp 内边距，仅配色语义不同 */
@Composable
private fun PillBadge(text: String, fg: Color, bg: Color, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(50),
        color = bg.copy(alpha = 0.45f),
        modifier = modifier.heightIn(min = 16.dp)
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = fg,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
private fun PostCardContent(
    post: PostEntry,
    loggedIn: Boolean,
    isOpAuthor: Boolean,
    avatarSize: Int,
    onUser: () -> Unit,
    onQuote: () -> Unit,
    onLike: () -> Unit,
    onCoin: () -> Unit,
    onReport: () -> Unit,
    onFloor: (Int) -> Unit,
    onThread: () -> Unit,
    showFloorBadge: Boolean,
    modifier: Modifier = Modifier,
    onCopy: () -> Unit = {},
    onDonate: (() -> Unit)? = null,
    onEditUser: (Long) -> Unit = {},
    threadEnabled: Boolean = true,
    threadReplyCount: Int = 0,
    replyParentAvatar: String = "",
    showReplyCapsule: Boolean = true,
    childReplyCount: Int = 0,
    childrenCollapsed: Boolean = false,
    onToggleChildren: () -> Unit = {},
    commentFavorited: Boolean = false,
    onFavComment: (() -> Unit)? = null,
    onHeadingPosition: (Int, Float) -> Unit = { _, _ -> },
    onBodyLayout: (Float, Float) -> Unit = { _, _ -> },
    searchHighlight: String = "",
    onSearchPosition: (Float) -> Unit = {},
) {
    Column(modifier) {
        // 作者行：头像 + 名称/徽章 + 元信息，楼层号固定右上角
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Avatar(post.avatarUrl, avatarSize, Modifier.clickable(onClick = onUser), online = post.online)
                if (LocalShowUid.current && post.authorUid > 0) {
                    Text(
                        post.authorUid.toString(),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f).offset(y = (-2).dp)) {
                // 用户信息行（源站格式）：名字后面直接跟 UID，随后是组别/楼主徽章。
                // 用 FlowRow 在放不下时自动换行，避免徽章挤占导致用户名被截断
                FlowRow(
                    verticalArrangement = Arrangement.Center,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        addSearchHighlights(AnnotatedString(post.authorName), searchHighlight, MaterialTheme.colorScheme.tertiaryContainer),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (isOpAuthor) {
                        PillBadge("楼主", MaterialTheme.colorScheme.onPrimaryContainer, MaterialTheme.colorScheme.primaryContainer, Modifier.align(Alignment.CenterVertically))
                    }
                    if (post.userGroup.isNotBlank()) {
                        PillBadge(post.userGroup, MaterialTheme.colorScheme.onSecondaryContainer, MaterialTheme.colorScheme.secondaryContainer, Modifier.align(Alignment.CenterVertically))
                    }
                    if (post.essenceVoteLabel.isNotBlank()) {
                        val support = post.essenceVoteSupport
                        PillBadge(
                            post.essenceVoteLabel,
                            when (support) {
                                true -> MaterialTheme.colorScheme.onTertiaryContainer
                                false -> MaterialTheme.colorScheme.onErrorContainer
                                null -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            when (support) {
                                true -> MaterialTheme.colorScheme.tertiaryContainer
                                false -> MaterialTheme.colorScheme.errorContainer
                                null -> MaterialTheme.colorScheme.surfaceContainerHighest
                            },
                            Modifier.align(Alignment.CenterVertically),
                        )
                    }
                }
                // 元信息行：称号矮胶囊（字号与时间一致）· 发送时间 · IP 属地
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    post.titleBadge?.let { t ->
                        // 称号独立渲染，避免与用户组/头衔/积分拼成一个标签；UR 保留序列号特化。
                        TitleBadgeView(t, small = true)
                    }
                    Text(
                        buildString {
                            if (post.timeText.isNotBlank()) append(post.timeText)
                            if (post.ipLocation.isNotBlank()) {
                                if (isNotEmpty()) append(" · ")
                                append(post.ipLocation)
                            }
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (showFloorBadge && post.floor > 0) {
                Spacer(Modifier.width(8.dp))
                Text(
                    "#${post.floor}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onFloor(post.floor) }
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        // 正文：交给系统原生长按选择菜单（复制/翻译/搜索由系统提供）；#楼层号可点击跳转
        SelectionContainer {
            HtmlContent(post.contentHtml, modifier = Modifier.onGloballyPositioned { onBodyLayout(it.positionInRoot().y, it.size.height.toFloat()) }, onFloor = onFloor, onHeadingPosition = onHeadingPosition, highlightQuery = searchHighlight, onSearchPosition = onSearchPosition)
        }
        if (LocalSmartDecode.current) {
            val decodedItems = remember(post.contentHtml) { detectSmartEncodedContent(post.contentHtml) }
            if (decodedItems.isNotEmpty()) {
                val clipboard = LocalClipboardManager.current
                Spacer(Modifier.height(8.dp))
                decodedItems.forEach { decoded ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.55f),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                    ) {
                        Column(Modifier.padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "识别到 ${decoded.type}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.weight(1f),
                                )
                                IconButton(
                                    onClick = { clipboard.setText(AnnotatedString(decoded.decoded)) },
                                    modifier = Modifier.size(30.dp),
                                ) { Icon(Icons.Filled.ContentCopy, "复制解码结果", Modifier.size(16.dp)) }
                            }
                            SelectionContainer {
                                Text(decoded.decoded, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        }
        // 回复目标与本评论的子树折叠均位于卡片右下角；二者互不抢占点击区域。
        if ((showReplyCapsule && post.parentFloor > 0) || childReplyCount > 0) {
            Spacer(Modifier.height(6.dp))
            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.End), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (showReplyCapsule && post.parentFloor > 0) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
                        modifier = Modifier.clip(RoundedCornerShape(50)).clickable { onFloor(post.parentFloor) }
                    ) {
                        Row(
                            Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            if (replyParentAvatar.isNotBlank()) Avatar(replyParentAvatar, 16)
                            else Icon(Icons.AutoMirrored.Filled.Reply, null, Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
                            Text("回复 #${post.parentFloor}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
                        }
                    }
                }
                if (childReplyCount > 0) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.58f),
                        modifier = Modifier.clip(RoundedCornerShape(50)).clickable(onClick = onToggleChildren),
                    ) {
                        Row(Modifier.padding(horizontal = 9.dp, vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (childrenCollapsed) Icons.Filled.ExpandMore else Icons.Filled.ExpandLess,
                                null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onTertiaryContainer,
                            )
                            Spacer(Modifier.width(2.dp))
                            Text(
                                if (childrenCollapsed) "展开 $childReplyCount 条回复" else "折叠 $childReplyCount 条回复",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                            )
                        }
                    }
                }
            }
        }
        // 对话串联入口（默认关闭，设置中可开启）：本楼引用了其他楼层、或被其他楼层回复时显示
        val refFloors = post.referencedFloors.filter { it > 0 }
        if (threadEnabled && (refFloors.isNotEmpty() || threadReplyCount > 0)) {
            Spacer(Modifier.height(6.dp))
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                modifier = Modifier.clip(RoundedCornerShape(50))
                    .clickable { onThread() }
            ) {
                Row(
                    Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Chat, null,
                        Modifier.size(13.dp),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        when {
                            refFloors.size > 1 ->
                                "查看 ${refFloors.joinToString(" ") { "#$it" }} 对话"
                            refFloors.size == 1 ->
                                "查看 #${refFloors.first()} 对话"
                            post.floor > 0 ->
                                "查看 #${post.floor} 的对话"
                            else ->
                                "查看楼主对话"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        // 功能按钮与正文之间的分割线
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
        Spacer(Modifier.height(4.dp))
        // 最后编辑信息：移到分割线下方显示（2.5）；编辑人可点击跳转其主页（2.12）
        if (post.editInfo.isNotBlank()) {
            EditInfoText(post, onEditUser)
            Spacer(Modifier.height(2.dp))
        }
        // 操作行：楼主正文 = 点赞 / 投币 / 举报（2.9 去除收藏）；回复 = 点赞 / 投币 / 回复 / 举报。
        // 树形回复（showFloorBadge）层级缩进挤占宽度，操作行只留图标省空间；
        // 状态由图标表达（点赞=实心爱心、投币/收藏=主题色），点赞数保留数字
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            if (post.canLike) {
                PostAction(
                    icon = if (post.liked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    label = when {
                        showFloorBadge -> if (post.likeCount > 0) "${post.likeCount}" else ""
                        post.likeCount > 0 -> "赞 ${post.likeCount}"
                        else -> "点赞"
                    },
                    onClick = { if (loggedIn) onLike() },
                    iconTint = if (post.liked) MaterialTheme.colorScheme.primary else null
                )
            }
            // 投币（金额可自定义，理由选填）：与点赞共用 donate-reaction 表单，主楼/未登录无表单时不显示
            if (post.canLike) {
                PostAction(
                    icon = Icons.Filled.Paid,
                    label = if (showFloorBadge) "" else if (post.coined) "已投币" else "投币",
                    onClick = { if (loggedIn) onCoin() },
                    iconTint = if (post.coined) MaterialTheme.colorScheme.primary else null
                )
            }
            // 打赏：源站的打赏入口不再解析进正文，集成在分割线下方的操作行（仅楼主正文）
            onDonate?.let { donate ->
                PostAction(
                    icon = Icons.Filled.VolunteerActivism,
                    label = "打赏",
                    onClick = donate
                )
            }
            PostAction(
                icon = Icons.Filled.Flag,
                label = if (showFloorBadge) "" else "举报",
                onClick = onReport
            )
            // 本地收藏评论（34）：仅回复显示，收藏后图标填充
            if (showFloorBadge && onFavComment != null) {
                PostAction(
                    icon = if (commentFavorited) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                    label = "",
                    onClick = onFavComment,
                    iconTint = if (commentFavorited) MaterialTheme.colorScheme.primary else null
                )
            }
            if (showFloorBadge) {
                // 回复的操作行：点赞 / 打赏 / 举报 / 回复（原"引用"统一为回复）
                PostAction(icon = Icons.Filled.FormatQuote, label = "", onClick = { if (loggedIn) onQuote() })
            }
            Spacer(Modifier.weight(1f))
        }
    }
}

/** 中文按单字计，拉丁/数字连续串按一个词计；按约 300 字/分钟给出保守阅读时长。 */
internal fun estimateReading(text: String): Pair<Int, Int> {
    val cjk = text.count { c ->
        c.code in 0x3400..0x4DBF || c.code in 0x4E00..0x9FFF ||
            c.code in 0xF900..0xFAFF || c.code in 0x3040..0x30FF || c.code in 0xAC00..0xD7AF
    }
    val words = Regex("[A-Za-z0-9]+(?:['_-][A-Za-z0-9]+)*").findAll(text).count()
    val total = (cjk + words).coerceAtLeast(text.trim().takeIf { it.isNotEmpty() }?.let { 1 } ?: 0)
    return total to ((total + 299) / 300).coerceAtLeast(if (total > 0) 1 else 0)
}

/** 最后编辑信息：编辑人名字高亮可点击，跳转其个人主页 */
@Composable
private fun EditInfoText(post: PostEntry, onEditUser: (Long) -> Unit) {
    val info = post.editInfo
    val name = post.editUserName
    if (post.editUserId > 0 && name.isNotBlank() && info.contains(name)) {
        val idx = info.indexOf(name)
        val annotated = buildAnnotatedString {
            append(info.substring(0, idx))
            withStyle(
                SpanStyle(
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            ) {
                pushStringAnnotation("USER", post.editUserId.toString())
                append(name)
                pop()
            }
            append(info.substring(idx + name.length))
        }
        val layoutResult = remember { mutableStateOf<TextLayoutResult?>(null) }
        Text(
            annotated,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            onTextLayout = { layoutResult.value = it },
            modifier = Modifier.pointerInput(annotated) {
                detectTapGestures { pos ->
                    val res = layoutResult.value ?: return@detectTapGestures
                    val offset = res.getOffsetForPosition(pos)
                    annotated.getStringAnnotations("USER", offset, offset).firstOrNull()?.let {
                        it.item.toLongOrNull()?.let { uid -> onEditUser(uid) }
                    }
                }
            }
        )
    } else {
        Text(
            info,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ==================== 头部附属面板 ====================

/** 打赏弹幕条：横向自动滚动展示最近打赏；切换时把当前展示的弹幕滚动为可见区第一条 */
@Composable
fun DanmakuBar(items: List<DanmakuItem>) {
    val rowState = rememberRowState()
    LaunchedEffect(items) {
        var target = 0
        while (true) {
            if (items.isNotEmpty()) {
                // target 即当前应展示的弹幕下标：滚动到其位置（成为可见区第一条）
                // 若该弹幕较长，滚动距离随内容长度自然增加，确保其完整可见
                rowState.animateScrollToItem(target)
                target = (target + 1) % items.size
                // 若剩余弹幕不足以铺满可视区，回到开头
                val first = rowState.layoutInfo.visibleItemsInfo.firstOrNull()?.index ?: 0
                if (items.size - first <= 2) target = 0
            }
            delay(2600)
        }
    }
    LazyRow(
        state = rowState,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        itemsIndexed(items, key = { i, d -> "$i-${d.user}-${d.amount}" }) { _, d ->
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.75f)
            ) {
                Row(
                    Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Avatar(d.avatarUrl, 15)
                    Text(
                        "${d.user} ${d.action} ${d.amount}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

/** 打赏底部弹窗（28）：从底部滑出，不再跳转独立页面。
 *  金额支持自定义（38）：预设档位 + 「自定义」切换数字输入；快捷留言取自源站打赏表单 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DonateSheet(
    session: Session,
    tid: Long,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit,
) {
    var info by remember { mutableStateOf<HtmlParser.DonateInfo?>(null) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var preset by remember { mutableStateOf("6") }
    var custom by remember { mutableStateOf(false) }
    var customText by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var msg by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(tid) {
        runCatching { info = HtmlParser.parseDonate(session.client.get("/donate?topic_id=$tid").html) }
            .onFailure { loadError = it.message }
    }

    val amount = if (custom) customText else preset
    val amountNum = amount.toIntOrNull()
    val validAmount = amountNum != null && amountNum in 1..99

    ModalBottomSheet(onDismissRequest = { if (!busy) onDismiss() }) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            when {
                loadError != null -> Text(
                    "加载失败：${loadError}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(vertical = 24.dp)
                )
                info == null -> Box(
                    Modifier.fillMaxWidth().height(200.dp),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }
                info?.unavailableReason?.isNotBlank() == true -> {
                    Text("暂时无法打赏", style = MaterialTheme.typography.titleLarge)
                    Text(info!!.unavailableReason)
                    TextButton(onClick = onDismiss) { Text("知道了") }
                }
                else -> {
                    val i = info!!
                    Text(
                        "打赏作者",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    msg?.let {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.errorContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                it,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                            )
                        }
                    }
                    Text(
                        "打赏积分将直接赠送给楼主，感谢你对优质内容的支持",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    // 金额档位 + 自定义切换（38），与投币一致：预设 6/33/66/88，自定义范围 1～99
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("6", "33", "66", "88").forEach { v ->
                            FilterChip(
                                selected = !custom && preset == v,
                                onClick = { custom = false; preset = v },
                                label = { Text(v, style = MaterialTheme.typography.labelMedium) }
                            )
                        }
                        FilterChip(
                            selected = custom,
                            onClick = { custom = !custom },
                            label = { Text("自定义", style = MaterialTheme.typography.labelMedium) }
                        )
                    }
                    if (custom) {
                        OutlinedTextField(
                            value = customText,
                            onValueChange = { t -> customText = t.filter { it.isDigit() }.take(2) },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("自定义金额（积分）") },
                            placeholder = { Text("范围 1～99") },
                            shape = RoundedCornerShape(16.dp),
                            singleLine = true,
                            isError = customText.isNotBlank() && !validAmount,
                            supportingText = { Text("仅支持 1～99 积分") }
                        )
                    }
                    // 快捷留言（源站打赏表单自带）
                    if (i.quickMessages.isNotEmpty()) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(i.quickMessages) { q ->
                                AssistChip(
                                    onClick = { message = q },
                                    label = {
                                        Text(
                                            q,
                                            style = MaterialTheme.typography.labelMedium,
                                            maxLines = 1
                                        )
                                    }
                                )
                            }
                        }
                    }
                    OutlinedTextField(
                        value = message,
                        onValueChange = { message = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("留言") },
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true
                    )
                    Button(
                        onClick = {
                            busy = true; msg = null
                            scope.launch {
                                try {
                                    val resp = session.client.postForm(
                                        "/donate", mapOf(
                                            "_csrf" to i.csrf,
                                            "topic_id" to i.topicId.ifBlank { tid.toString() },
                                            "request_key" to i.requestKey,
                                            "amount" to amount,
                                            "message" to message,
                                        )
                                    )
                                    if (resp.url.contains("form_error")) {
                                        msg = HtmlParser.extractError(resp.html).ifBlank { "打赏失败" }
                                    } else {
                                        onSuccess()
                                    }
                                } catch (e: Exception) { msg = e.message } finally { busy = false }
                            }
                        },
                        enabled = !busy && validAmount,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            if (busy) "提交中…" else "打赏 $amount 积分",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

/** 抽奖帖面板：状态 / 奖品 / 条件 / 中奖名单（已结束默认折叠，自己中奖高亮）/ 参与方式 */
@Composable
fun LotteryPanelView(
    lp: LotteryPanel,
    loggedIn: Boolean,
    onJoin: () -> Unit,
    onJoinForm: (String) -> Unit,
    selfName: String = "",
) {
    val drawn = lp.status.contains("开奖") || lp.result.isNotBlank() || lp.winners.isNotEmpty()
    // 已开奖：默认折叠中奖名单；自己中奖则默认展开并高亮
    val selfWon = selfName.isNotBlank() && lp.winners.any { it.user.startsWith(selfName) }
    var expanded by remember(lp) { mutableStateOf(!drawn || selfWon) }
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = if (drawn) MaterialTheme.colorScheme.surfaceContainerHighest
        else MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("抽奖帖", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    if (lp.note.isNotBlank()) {
                        Text(lp.note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Badge(
                        lp.status.ifBlank { if (drawn) "已开奖" else "抽奖中" },
                        if (drawn) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.primary,
                        if (drawn) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onPrimary,
                    )
                    if (lp.participants.isNotBlank()) {
                        Spacer(Modifier.height(3.dp))
                        Text(
                            lp.participants, style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            if (lp.prizes.isNotEmpty()) {
                Text("奖品", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                lp.prizes.forEach { p ->
                    Text("· $p", style = MaterialTheme.typography.bodySmall)
                }
            }
            if (lp.condition.isNotBlank()) {
                Text(lp.condition, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
            if (lp.result.isNotBlank() || lp.winners.isNotEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { expanded = !expanded }
                        .padding(vertical = 2.dp)
                ) {
                    Text("开奖结果", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    if (selfWon) {
                        Spacer(Modifier.width(6.dp))
                        Badge("恭喜你中奖了", MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.onPrimary)
                    }
                    Spacer(Modifier.weight(1f))
                    Text(
                        if (expanded) "收起" else "展开",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (expanded) {
                    if (lp.result.isNotBlank()) Text(lp.result, style = MaterialTheme.typography.bodySmall)
                    lp.winners.forEach { w ->
                        val isSelf = selfName.isNotBlank() && w.user.startsWith(selfName)
                        Text(
                            "· ${w.user}${if (w.prize.isNotBlank()) " · ${w.prize}" else ""}",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isSelf) MaterialTheme.colorScheme.primary else Color.Unspecified,
                            fontWeight = if (isSelf) FontWeight.Bold else FontWeight.Normal,
                        )
                        // 兑换码（仅本人可见）：用代码块单独成行展示，方便一键复制
                        if (w.code.isNotBlank()) {
                            Spacer(Modifier.height(4.dp))
                            CodeBlockView(w.code)
                        }
                    }
                }
            }
            lp.joinedText.takeIf { it.isNotBlank() }?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
            // 参与：源站规则为回复参与；存在表单选项时按选项提交
            if (!drawn) {
                if (lp.joinAction.isNotBlank() && lp.options.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        lp.options.forEach { (v, label) ->
                            FilledTonalButton(onClick = { onJoinForm(v) }, enabled = loggedIn) { Text(label) }
                        }
                    }
                } else if (lp.joinAction.isNotBlank()) {
                    Button(onClick = { onJoinForm("") }, enabled = loggedIn, modifier = Modifier.fillMaxWidth()) {
                        Text(if (loggedIn) "参与抽奖" else "登录后参与")
                    }
                } else {
                    Button(onClick = onJoin, enabled = loggedIn, modifier = Modifier.fillMaxWidth()) {
                        Text(if (loggedIn) "回复参与抽奖" else "登录后参与")
                    }
                }
            }
        }
    }
}

/** AI 总结卡片（置顶显示）：未总结时是一颗手动触发按钮，出结果后按 Markdown 渲染 */
@Composable
fun AiSummaryCard(
    summary: String?,
    busy: Boolean,
    error: String?,
    enabled: Boolean,
    onRun: () -> Unit,
    onRegenerate: () -> Unit,
    onDismiss: () -> Unit,
) {
    if (busy || summary != null || error != null) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Column(Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(26.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.AutoAwesome, null, Modifier.size(15.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                    Spacer(Modifier.width(8.dp))
                    Text("AI 总结", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
                    Spacer(Modifier.weight(1f))
                    // 重新总结：结果按帖缓存，没这个入口的话同一帖再也换不出新总结
                    if (!busy && summary != null) {
                        IconButton(onClick = onRegenerate, modifier = Modifier.size(32.dp)) {
                            Icon(
                                Icons.Filled.Refresh, "重新总结",
                                Modifier.size(17.dp),
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                    TextButton(onClick = onDismiss) { Text("关闭") }
                }
                Spacer(Modifier.height(6.dp))
                when {
                    busy -> Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(Modifier.size(15.dp), strokeWidth = 2.dp)
                        Text("正在总结…", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                    error != null -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(error, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                        FilledTonalButton(
                            onClick = onRegenerate,
                            enabled = enabled,
                            shape = RoundedCornerShape(50)
                        ) { Text("重试") }
                    }
                    // 模型返回的是 Markdown：直接 Text 会把 ## 和 ** 记号原样显示出来
                    summary != null -> MarkdownText(
                        summary,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
            }
        }
    } else {
        FilledTonalButton(
            onClick = onRun,
            enabled = enabled,
            shape = RoundedCornerShape(50),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Icon(Icons.Filled.AutoAwesome, null, Modifier.size(15.dp))
            Spacer(Modifier.width(6.dp))
            Text(if (enabled) "AI 总结本帖" else "AI 未配置")
        }
    }
}

/** 发卡帖兑换卡片 */
@Composable
fun VirtualCardView(vc: VirtualCard, loggedIn: Boolean, onBuy: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = if (vc.inStock) MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.8f)
        else MaterialTheme.colorScheme.surfaceContainerHighest,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(vc.kicker, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onTertiaryContainer)
                    Text(vc.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Badge(vc.status, MaterialTheme.colorScheme.surfaceContainerHighest, MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("${vc.priceText}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            if (vc.footText.isNotBlank()) {
                Text(vc.footText, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onTertiaryContainer)
            }
            Text(
                vc.tip.ifBlank { if (loggedIn) "" else "登录后可使用积分兑换" },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
            // 我的购买记录：兑换码用代码块单独成行展示，方便一键复制
            if (vc.orders.isNotEmpty()) {
                Text("我的购买记录", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                vc.orders.forEach { o ->
                    Column {
                        CodeBlockView(o.code)
                        val meta = listOf(o.time, o.price).filter { it.isNotBlank() }
                        if (meta.isNotEmpty()) {
                            Text(
                                meta.joinToString(" · "),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                }
            }
            if (vc.buyAction.isNotBlank() && vc.inStock) {
                Button(
                    onClick = onBuy,
                    enabled = loggedIn,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.fillMaxWidth()
                ) { Text(if (loggedIn) "确认兑换" else "登录后兑换") }
            } else if (!vc.inStock) {
                Text(
                    "已发完 / 暂不可兑换",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    }
}

// ==================== 弹窗 ====================

/** #N 对话串联弹窗：把引用链上的楼层按时间正序完整展示；也用于查看某用户在本帖的全部回复 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThreadDialog(
    posts: List<PostEntry>?,
    loading: Boolean,
    error: String?,
    currentPostId: Long,
    onDismiss: () -> Unit,
    onUser: (Long) -> Unit,
    title: String = "对话串联",
    countText: (Int) -> String = { "共 ${it} 条相关楼层" },
    onJumpFloor: ((Int) -> Unit)? = null,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(horizontal = 20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.AutoMirrored.Filled.Chat, null,
                    Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(8.dp))
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                if (!loading) TextButton(onClick = onDismiss) { Text("关闭") }
            }
            Spacer(Modifier.height(6.dp))
            when {
                loading -> Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(vertical = 20.dp)
                ) {
                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    Text("正在串联全部相关楼层…", style = MaterialTheme.typography.bodyMedium)
                }
                posts == null || posts.isEmpty() -> Text(
                    error ?: "加载失败",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(vertical = 20.dp)
                )
                else -> {
                    Text(
                        countText(posts.size),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    LazyColumn(
                        modifier = Modifier
                            .heightIn(max = 440.dp)
                            .padding(top = 6.dp),
                        contentPadding = PaddingValues(bottom = 28.dp)
                    ) {
                        itemsIndexed(posts, key = { i, p -> "$i-${p.floor}-${p.id}" }) { i, p ->
                            ThreadPostItem(
                                post = p,
                                isCurrent = p.id == currentPostId,
                                showDivider = i < posts.size - 1,
                                onUser = { onUser(p.authorId) },
                                onJumpFloor = onJumpFloor,
                            )
                        }
                    }
                    if (error != null) {
                        Text(
                            error,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    }
                }
            }
        }
    }
}

/** 对话链中的单条楼层：扁平条目，当前楼层高亮；支持点击楼层号跳转定位（2.16） */
@Composable
private fun ThreadPostItem(
    post: PostEntry,
    isCurrent: Boolean,
    showDivider: Boolean,
    onUser: () -> Unit,
    onJumpFloor: ((Int) -> Unit)? = null,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
            .then(
                if (isCurrent) Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.22f))
                    .padding(10.dp)
                else Modifier.padding(horizontal = 2.dp)
            )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Avatar(post.avatarUrl, 28, Modifier.clickable(onClick = onUser), online = post.online)
            Text(
                post.authorName,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (post.floor > 0) {
                Text(
                    "#${post.floor}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = if (onJumpFloor != null) Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onJumpFloor(post.floor) }
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                    else Modifier
                )
            } else {
                // 与楼层卡片中的楼主徽章保持统一样式与配色
                Badge(
                    "楼主",
                    MaterialTheme.colorScheme.primaryContainer,
                    MaterialTheme.colorScheme.onPrimaryContainer,
                    small = true
                )
            }
            Spacer(Modifier.weight(1f))
            Text(
                post.timeText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(8.dp))
        HtmlContent(post.contentHtml)
    }
    if (showDivider && !isCurrent) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    }
}

/** 跳楼对话框 */
@Composable
fun FloorJumpDialog(maxFloor: Int, onDismiss: () -> Unit, onJump: (Int) -> Unit, initial: Int = 0) {
    var text by remember { mutableStateOf(if (initial > 0) initial.toString() else "") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("跳转楼层") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { if (it.length <= 5 && it.all { c -> c.isDigit() }) text = it },
                    label = { Text("楼层号（1 - ${maxFloor.coerceAtLeast(1)}）") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { text.toIntOrNull()?.let { if (it >= 1) onJump(it) } },
                enabled = (text.toIntOrNull() ?: 0) >= 1
            ) { Text("跳转") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

/** 回复编辑：底部抽屉，预填引用内容，Markdown 工具栏 + 人机验证（照搬源站编辑器）。
 *  title/submitLabel/showToolbar 可定制：私信等场景复用此编辑器但隐藏 Markdown 工具栏。
 *  抽屉内为紧凑输入框（内嵌「展开」按钮）；点击展开进入全屏编辑（3.21） */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReplyDialog(
    session: Session,
    initial: String,
    captcha: sb.linux.client.data.NativeCaptcha?,
    onDismiss: () -> Unit,
    onSubmit: (String, String, () -> Unit) -> Unit,
    title: String = "发表回复",
    submitLabel: String = "发布",
    showToolbar: Boolean = true,
    onRefreshCaptcha: (() -> Unit)? = null,
) {
    // 引用回复必须保持 @用户名 #楼层 位于正文最开头；光标默认放到预填内容末尾，
    // 否则用户输入会插到引用前面，源站无法把回复加入评论树。
    var body by remember(initial) {
        mutableStateOf(TextFieldValue(initial, selection = TextRange(initial.length)))
    }
    var answer by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    var previewReply by remember { mutableStateOf(false) }
    var imageUploadBusy by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val canSubmit = body.text.isNotBlank() && !busy && (captcha == null || answer.isNotBlank())
    // 换题后清空旧答案，避免带着上一题的答案提交
    LaunchedEffect(captcha) { if (answer.isNotBlank()) answer = "" }

    // Markdown 工具栏：在光标处插入/包裹，与源站编辑器一致
    fun insert(before: String, after: String = before, placeholder: String = "") {
        val text = body.text
        val sel = body.selection
        val start = sel.min.coerceIn(0, text.length)
        val end = sel.max.coerceIn(0, text.length)
        val selected = text.substring(start, end)
        val mid = if (selected.isNotEmpty()) selected
        else if (placeholder.isNotBlank()) placeholder
        else ""
        val newText = text.substring(0, start) + before + mid + after + text.substring(end)
        val cursor = start + before.length + mid.length + after.length
        body = TextFieldValue(newText, selection = TextRange(cursor))
    }

    fun insertUploadedImage(url: String) {
        val text = body.text
        val start = body.selection.min.coerceIn(0, text.length)
        val end = body.selection.max.coerceIn(0, text.length)
        val markdown = "![图片]($url)"
        body = TextFieldValue(
            text = text.substring(0, start) + markdown + text.substring(end),
            selection = TextRange(start + markdown.length),
        )
    }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            imageUploadBusy = true
            scope.launch {
                try {
                    insertUploadedImage(ImageHostClient.upload(context, session.settings, uri))
                    session.showToast("图片上传成功")
                } catch (e: Exception) {
                    session.showToast(e.message ?: "图片上传失败")
                } finally {
                    imageUploadBusy = false
                }
            }
        }
    }

    fun chooseImage() {
        if (!session.settings.useBuiltInImageHost && session.settings.imageHostUrl.isBlank()) {
            session.showToast("请先在设置中配置图床")
        } else {
            imagePicker.launch("image/*")
        }
    }

    fun submit() {
        busy = true
        onSubmit(body.text, answer) { busy = false }
    }

    if (expanded) {
        // ---------- 全屏编辑（3.21）：抽屉内点「展开」进入 ----------
        Dialog(
            onDismissRequest = { if (!busy) expanded = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            // 修复软键盘遮挡（含底部人机验证输入框）：Dialog window 默认 softInputMode 为
            // UNSPECIFIED，部分设备上表现为 pan（整体平移窗口）而非 resize，输入框/验证码被
            // 键盘盖住。必须在 Dialog 内容里拿到所属窗口强制 ADJUST_RESIZE（在外层取到的是
            // 主窗口，无效），配合 imePadding 让内容稳定避开 IME。
            val dialogView = LocalView.current
            LaunchedEffect(dialogView) {
                @Suppress("DEPRECATION") // Dialog window 上 ADJUST_RESIZE 仍是让 IME insets 生效的可靠手段
                (dialogView.parent as? DialogWindowProvider)?.window?.setSoftInputMode(
                    WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
                )
            }
            Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .imePadding()
                        .navigationBarsPadding()
                        .statusBarsPadding()
                        .padding(horizontal = 20.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.weight(1f))
                if (showToolbar) TextButton(onClick = { previewReply = !previewReply }) { Text(if (previewReply) "编辑" else "预览") }
                        TextButton(onClick = { if (!busy) expanded = false }) { Text("收起") }
                        TextButton(onClick = { submit() }, enabled = canSubmit) {
                            Text(if (busy) "发送中…" else submitLabel)
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    if (showToolbar) ReplyToolbar(
                        onInsert = { b, a, p -> insert(b, a, p) },
                        onUploadImage = ::chooseImage,
                        imageUploadBusy = imageUploadBusy,
                    )
                    Spacer(Modifier.height(8.dp))
                    if (previewReply) {
                        Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState())) {
                            HtmlContent(remember(body.text) { HtmlParser.markdownToHtml(body.text) }, cacheable = false)
                        }
                    } else OutlinedTextField(
                        value = body,
                        onValueChange = { body = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        shape = RoundedCornerShape(14.dp)
                    )
                    ReplyCaptchaField(
                        captcha = captcha,
                        answer = answer,
                        onAnswerChange = { if (it.length <= 8) answer = it },
                        onRefresh = onRefreshCaptcha,
                        topPadding = 10.dp
                    )
                    Spacer(Modifier.height(12.dp))
                }
            }
        }
        return
    }

    // ---------- 紧凑抽屉 ----------
    ModalBottomSheet(
        onDismissRequest = { if (!busy) onDismiss() },
        // 让 BottomSheet 的内容区感知 IME 空间，键盘弹出时自动上推输入框
        contentWindowInsets = {
            androidx.compose.foundation.layout.WindowInsets.ime
                .union(androidx.compose.foundation.layout.WindowInsets.navigationBars)
        }
    ) {
        Column(
            Modifier
                .padding(horizontal = 20.dp)
                .imePadding()
                .navigationBarsPadding()
                // 键盘弹出后可视空间不足时允许滚动，保证输入框/验证码始终可达
                .verticalScroll(rememberScrollState())
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                if (showToolbar) TextButton(onClick = { previewReply = !previewReply }) { Text(if (previewReply) "编辑" else "预览") }
                TextButton(onClick = { submit() }, enabled = canSubmit) {
                    Text(if (busy) "发送中…" else submitLabel)
                }
            }
            Spacer(Modifier.height(4.dp))
            if (showToolbar) ReplyToolbar(
                onInsert = { b, a, p -> insert(b, a, p) },
                onUploadImage = ::chooseImage,
                imageUploadBusy = imageUploadBusy,
            )
            Spacer(Modifier.height(8.dp))
            // 紧凑输入框：「展开」按钮嵌入输入框尾随槽位（trailingIcon），点按进入全屏编辑
            if (previewReply) {
                Column(Modifier.fillMaxWidth().heightIn(min = 96.dp, max = 320.dp).verticalScroll(rememberScrollState())) {
                    HtmlContent(remember(body.text) { HtmlParser.markdownToHtml(body.text) }, cacheable = false)
                }
            } else OutlinedTextField(
                value = body,
                onValueChange = { body = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 96.dp),
                shape = RoundedCornerShape(14.dp),
                trailingIcon = {
                    IconButton(onClick = { expanded = true }) {
                        Icon(
                            Icons.Filled.OpenInFull,
                            "全屏编辑",
                            Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
            ReplyCaptchaField(
                captcha = captcha,
                answer = answer,
                onAnswerChange = { if (it.length <= 8) answer = it },
                onRefresh = onRefreshCaptcha,
                topPadding = 10.dp
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

/** Markdown 工具栏（横向滚动；私信等场景隐藏），onInsert(before, after, placeholder) */
@Composable
private fun ReplyToolbar(
    onInsert: (String, String, String) -> Unit,
    onUploadImage: (() -> Unit)? = null,
    imageUploadBusy: Boolean = false,
) {
    sb.linux.client.ui.MarkdownEditorTools(
        onInsert = onInsert, onUpload = { onUploadImage?.invoke() ?: onInsert("![", "](https://)", "图片描述") },
        uploading = imageUploadBusy,
    )
}

/** 人机验证输入框（抽奖帖等）：题目展示，答案由用户填写；提供「换一题」刷新按钮。captcha 为 null 时不显示 */
@Composable
private fun ReplyCaptchaField(
    captcha: sb.linux.client.data.NativeCaptcha?,
    answer: String,
    onAnswerChange: (String) -> Unit,
    onRefresh: (() -> Unit)?,
    topPadding: androidx.compose.ui.unit.Dp,
) {
    if (captcha == null) return
    Spacer(Modifier.height(topPadding))
    OutlinedTextField(
        value = answer,
        onValueChange = onAnswerChange,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        label = { Text("人机验证：${captcha.question}") },
        singleLine = true,
        trailingIcon = if (onRefresh != null) {
            {
                IconButton(onClick = onRefresh) {
                    Icon(Icons.Filled.Refresh, "换一题")
                }
            }
        } else null,
    )
}

/**
 * 长按正文功能菜单（MD3 风格，2.14）：
 * - 复制全文：把该楼层正文纯文本复制到剪贴板
 * - 自由复制：弹出全屏大窗口展示内容，长按自由选择复制（2.15）
 * - TA 的回复：查看发出该条内容的用户在本帖的所有回复
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostCopySheet(
    post: PostEntry,
    onDismiss: () -> Unit,
    onCopied: () -> Unit,
    onViewUserReplies: (() -> Unit)? = null,
) {
    val plain = remember(post.contentHtml) { HtmlParser.htmlToText(post.contentHtml) }
    var freeMode by remember(post) { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current
    fun copyAll() {
        if (plain.isNotBlank()) clipboard.setText(AnnotatedString(plain))
        onCopied()
        onDismiss()
    }
    if (freeMode) {
        // 全屏大窗口自由复制（2.15）
        FreeCopyDialog(
            title = "${post.authorName}${if (post.floor > 0) " #${post.floor}" else ""}",
            plain = plain,
            onClose = { freeMode = false },
        )
        return
    }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(horizontal = 16.dp)) {
            Row(
                Modifier.padding(start = 4.dp, top = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("正文操作", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onDismiss) { Text("关闭") }
            }
            Text(
                "${post.authorName}${if (post.floor > 0) " #${post.floor}" else " · 楼主"}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
            )
            CopySheetItem(
                icon = Icons.Filled.CopyAll,
                title = "复制全文",
                subtitle = "复制整段正文到剪贴板"
            ) { copyAll() }
            CopySheetItem(
                icon = Icons.Filled.ContentPaste,
                title = "自由复制",
                subtitle = "全屏展示正文，长按选择任意部分复制"
            ) { freeMode = true }
            if (onViewUserReplies != null) {
                CopySheetItem(
                    icon = Icons.AutoMirrored.Filled.Chat,
                    title = "TA 的回复",
                    subtitle = "查看该用户在本帖的所有回复"
                ) {
                    onDismiss()
                    onViewUserReplies()
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

/** MD3 列表项：圆角色调图标容器 + 标题/副标题 */
@Composable
private fun CopySheetItem(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon, null, Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** 自由复制：全屏大窗口，仅展示正文内容，方便长按选择（2.15） */
@Composable
private fun FreeCopyDialog(
    title: String,
    plain: String,
    onClose: () -> Unit,
) {
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .systemBarsPadding(),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "自由复制",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = onClose) { Text("关闭") }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                if (plain.isBlank()) {
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Text(
                            "（本楼层无文本内容）",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Column(
                        Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                    ) {
                        Text(
                            title,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        SelectionContainer {
                            Text(
                                plain,
                                style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 26.sp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}

/** 导出格式选择弹窗：MD3 底部弹层，圆角色调图标容器 + 标题/副标题 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportDialog(
    posts: List<PostEntry>,
    busy: String?,
    loadingPosts: Boolean = false,
    allLoaded: Boolean = false,
    onLoadAll: () -> Unit = {},
    onDismiss: () -> Unit,
    onExport: (kind: String, scopeMode: String, fromFloor: Int, toFloor: Int, multiPage: Boolean, selectedIds: Set<Long>) -> Unit,
) {
    var scopeMode by remember { mutableStateOf("all") }
    var fromFloor by remember { mutableStateOf("0") }
    var toFloor by remember { mutableStateOf("20") }
    var multiPage by remember { mutableStateOf(true) }
    var selectedIds by remember { mutableStateOf(posts.map { it.id }.toSet()) }
    ModalBottomSheet(onDismissRequest = { if (busy == null) onDismiss() },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), sheetGesturesEnabled = false) {
        Column(Modifier.fillMaxHeight(0.9f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            Row(
                Modifier.padding(start = 4.dp, top = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("导出帖子", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onDismiss, enabled = busy == null) { Text("关闭") }
            }
            Text(
                "选择内容范围和格式，文件保存后可在「设置 → 已导出的帖子」查看",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
            )
            TextButton(enabled = busy == null && !loadingPosts && !allLoaded, onClick = onLoadAll) {
                Text(if (loadingPosts) "正在加载完整帖子…" else if (allLoaded) "已加载完整帖子（${posts.size} 楼）" else "加载完整帖子后导出（当前 ${posts.size} 楼）")
            }
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                listOf("all" to "已加载", "main" to "仅正文", "range" to "范围", "selected" to "勾选").forEachIndexed { index, (value, label) ->
                    SegmentedButton(
                        selected = scopeMode == value,
                        onClick = { scopeMode = value },
                        shape = SegmentedButtonDefaults.itemShape(index, 4),
                    ) { Text(label) }
                }
            }
            if (scopeMode == "range") {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = fromFloor, onValueChange = { fromFloor = it.filter(Char::isDigit) },
                        label = { Text("起始楼层") }, singleLine = true, modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = toFloor, onValueChange = { toFloor = it.filter(Char::isDigit) },
                        label = { Text("结束楼层") }, singleLine = true, modifier = Modifier.weight(1f),
                    )
                }
            }
            if (scopeMode == "selected") {
                Row {
                    TextButton(onClick = { selectedIds = posts.map { it.id }.toSet() }) { Text("全选") }
                    TextButton(onClick = { selectedIds = emptySet() }) { Text("清空") }
                    Text("已选 ${selectedIds.size} 层", modifier = Modifier.padding(12.dp))
                }
                LazyColumn(Modifier.fillMaxWidth().heightIn(max = 220.dp)) {
                    items(posts, key = { it.id }) { post ->
                        Row(Modifier.fillMaxWidth().clickable {
                            selectedIds = if (post.id in selectedIds) selectedIds - post.id else selectedIds + post.id
                        }, verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = post.id in selectedIds, onCheckedChange = null)
                            Text(if (post.floor == 0) "正文 · ${post.authorName}" else "#${post.floor} · ${post.authorName}", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
            Row(
                Modifier.fillMaxWidth().padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("长图自动分页", style = MaterialTheme.typography.bodyMedium)
                    Text("按实际高度分页，单篇超长正文也完整导出", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = multiPage, onCheckedChange = { multiPage = it })
            }
            listOf(
                Triple("html", "导出 HTML", "保留原帖结构与样式，适合存档"),
                Triple("md", "导出 Markdown", "纯文本轻量格式，便于编辑"),
                Triple("img", "导出长图", "渲染为图片，方便直接分享"),
            ).forEach { (kind, label, subtitle) ->
                ExportSheetItem(
                    icon = when (kind) {
                        "html" -> Icons.Filled.Code
                        "md" -> Icons.Filled.Title
                        else -> Icons.Filled.Image
                    },
                    title = label,
                    subtitle = subtitle,
                    busy = busy == kind,
                    enabled = busy == null && !loadingPosts && (scopeMode != "selected" || selectedIds.isNotEmpty()),
                    onClick = {
                        onExport(
                            kind,
                            scopeMode,
                            fromFloor.toIntOrNull() ?: 0,
                            toFloor.toIntOrNull() ?: Int.MAX_VALUE,
                            multiPage,
                            selectedIds,
                        )
                    }
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

/** MD3 列表项：圆角色调图标容器 + 标题/副标题（导出弹层用） */
@Composable
private fun ExportSheetItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    busy: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon, null, Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
        Column(Modifier.weight(1f)) {
            Text(if (busy) "导出中…" else title, style = MaterialTheme.typography.bodyLarge)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** 评论投币底部弹窗：金额档位来自源站，理由选填（最多 120 字）。 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CoinDialog(tiers: List<Int>, onDismiss: () -> Unit, onConfirm: (Int, String) -> Unit) {
    val availableTiers = remember(tiers) { tiers.filter { it > 0 }.distinct().ifEmpty { DEFAULT_COMMENT_REWARD_TIERS } }
    var amount by remember(availableTiers) { mutableIntStateOf(availableTiers.first()) }
    var reason by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("投币", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                "投币积分将赠送给作者，感谢你对优质内容的支持",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            // 源站只接受 data-tiers 声明的金额，放不下时自动换行。
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                availableTiers.forEach { value ->
                    FilterChip(
                        selected = amount == value,
                        onClick = { amount = value },
                        label = { Text(value.toString(), style = MaterialTheme.typography.labelMedium) }
                    )
                }
            }
            OutlinedTextField(
                value = reason,
                onValueChange = { if (it.length <= 120) reason = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("理由（选填）") },
                placeholder = { Text("例如：内容很有帮助") },
                shape = RoundedCornerShape(16.dp),
                singleLine = true,
                supportingText = { Text("${reason.length}/120") }
            )
            Button(
                onClick = { onConfirm(amount, reason.trim()) },
                enabled = amount in availableTiers,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    "投币 $amount 积分",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
