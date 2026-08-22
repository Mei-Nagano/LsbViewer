package sb.linux.client.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.CopyAll
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Title
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavHostController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import sb.linux.client.data.AiClient
import sb.linux.client.data.DanmakuItem
import sb.linux.client.data.HtmlParser
import sb.linux.client.data.LotteryPanel
import sb.linux.client.data.PostEntry
import sb.linux.client.data.Session
import sb.linux.client.data.TopicExport
import sb.linux.client.data.TopicPageData
import sb.linux.client.data.VirtualCard
import sb.linux.client.ui.*
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopicScreen(session: Session, nav: NavHostController) {
    val entry = nav.currentBackStackEntry ?: return
    val tid = entry.arguments?.getLong("tid") ?: return
    val initialPage = entry.arguments?.getInt("p") ?: 1
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

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
    var showExportDialog by remember { mutableStateOf(false) }
    var exportBusy by remember { mutableStateOf<String?>(null) }

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
    val listState = rememberLazyListState()

    // 楼层定位：高亮目标楼层 + 记住原位置以便返回（2.13）
    var highlightPostId by remember { mutableStateOf<Long?>(null) }
    var returnPos by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    // 沉浸模式：滚动时自动隐藏顶栏（顶栏为悬浮覆盖层，不参与布局，避免内容顶到状态栏的抖动）
    var topBarVisible by remember { mutableStateOf(true) }
    val derivedTopVisible by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            (info.visibleItemsInfo.firstOrNull()?.index ?: 0) == 0
        }
    }
    LaunchedEffect(derivedTopVisible) { topBarVisible = derivedTopVisible }

    // AI 总结状态
    var aiSummary by remember { mutableStateOf<String?>(null) }
    var aiBusy by remember { mutableStateOf(false) }
    var aiError by remember { mutableStateOf<String?>(null) }

    // 排序后的楼层列表（楼主正文固定首位）：热度档按点赞数降序（同赞按楼层号，顺序稳定）；
    // 正序档按楼层号升序（源站页面顺序）。未登录时源站不输出点赞计数（全 0），热度档自然退化为正序
    val sortedPosts = remember(data, sortOrder) {
        val d = data ?: return@remember emptyList()
        val main = d.posts.firstOrNull()
        val rest = when (sortOrder) {
            1 -> d.posts.drop(1).sortedBy { it.floor }
            // 倒序：楼层号降序
            2 -> d.posts.drop(1).sortedByDescending { it.floor }
            else -> d.posts.drop(1).sortedWith(
                compareByDescending<PostEntry> { it.likeCount }.thenBy { it.floor }
            )
        }
        if (main != null) listOf(main) + rest else rest
    }
    val mainPost = sortedPosts.firstOrNull()

    // 帖子分类生效的滚动模式（浏览设置可按分类覆盖，3.13）
    val topicInfinite = session.effectiveInfiniteScroll("topic")
    // 翻页模式：按设置的每页评论数本地切片分页（3.11）。
    // 用 rememberSaveable 使其在跳转到用户主页后返回本帖时得以保留，
    // 结合 loadedMaxPage 从缓存重建 data 即可恢复原来的评论滚动位置。
    var localReplyPage by rememberSaveable(tid) { mutableIntStateOf(1) }
    // 已载入 data 的源站分页范围（加载是顺序的，恒为 1..loadedMaxPage）。
    // 保存后返回本帖时据此从缓存重建 data，避免只加载第一页导致滚动位置丢失。
    var loadedMaxPage by rememberSaveable(tid) { mutableIntStateOf(1) }
    // 滚动模式切换的触发标记：scrollModeOverrides 存于 SharedPreferences（非 Compose 状态），
    // 切换后靠此标记强制重组，让 topicInfinite 从源设置重新读取，菜单选项与分页即时同步。
    var replyModeTick by remember { mutableIntStateOf(0) }
    val replies = sortedPosts.drop(1)
    // 楼号 -> 回复它的楼层数量：决定评论区是否显示该楼的「对话串联」入口
    val threadReplyCount = remember(sortedPosts) {
        val m = mutableMapOf<Int, Int>()
        sortedPosts.forEach { p -> p.referencedFloors.forEach { r -> if (r > 0) m[r] = (m[r] ?: 0) + 1 } }
        m
    }
    val repliesPer = session.settings.commentsPerPage.coerceAtLeast(1)
    val pagedReplies = if (topicInfinite) replies
    else replies.drop((localReplyPage - 1) * repliesPer).take(repliesPer)
    // 本地总页数：已加载评论的整页数；源站还有更多页时 +1（允许继续翻页触发加载）
    val localReplyTotal = if (topicInfinite) (data?.totalPages ?: 1) else run {
        val full = ((replies.size + repliesPer - 1) / repliesPer).coerceAtLeast(1)
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
                    val resp = session.client.get(if (p <= 1) "/topic/$tid" else "/topic/$tid?p=$p")
                    HtmlParser.parseTopicPage(resp.html, tid).also { session.saveTopicPage(tid, p, it) }
                }
                data = if (append && data != null) data!!.copy(
                    posts = (data!!.posts + d.posts).distinctBy { it.id },
                    page = d.page,
                    totalPages = d.totalPages,
                ) else d
                // 记录已载入的源站分页范围（1..loadedMaxPage）
                loadedMaxPage = if (append) maxOf(loadedMaxPage, d.page.coerceAtLeast(1))
                else d.page.coerceAtLeast(1)
                // 重新加载（非追加）时重置本地评论分页页码（3.11）
                if (!append) localReplyPage = 1
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
                try {
                    val np = (data?.page ?: 1) + 1
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
            }
            loadingMore = false
            localReplyPage = target.coerceAtLeast(1)
            // 本地翻页跳回评论区顶部
            listState.scrollToItem(if (mainPost != null) 2 else 1)
        }
    }

    /** 跳转到指定楼层：借助源站 ?floor=N 重定向定位页码（跨页时使用） */
    fun jumpFloor(floor: Int) {
        scope.launch {
            loading = true; error = null
            try {
                val resp = session.client.get("/topic/$tid?floor=$floor")
                val p = Regex("""[?&]p=(\d+)""").find(resp.url)?.groupValues?.get(1)?.toIntOrNull() ?: 1
                data = HtmlParser.parseTopicPage(resp.html, tid)
                sortOrder = 1 // 跨页跳楼层需按楼层序渲染，data.posts 索引才能对上滚动位置
                localReplyPage = 1
                loadedMaxPage = p.coerceAtLeast(1)
                loading = false
                // 滚动到目标楼层附近并高亮
                val idx = data?.posts?.indexOfFirst { it.floor == floor } ?: -1
                if (idx >= 0) {
                    highlightPostId = data?.posts?.get(idx)?.id
                    scope.launch {
                        listState.animateScrollToItem(idx + 1)
                        delay(1500)
                        highlightPostId = null
                    }
                }
            } catch (e: Exception) {
                error = e.message; loading = false
            }
        }
    }

    /** 定位楼层（本页优先）：直接滚动到目标楼层并高亮，同时记录原位置供返回（2.13） */
    fun jumpToFloor(floor: Int) {
        val posts = sortedPosts
        val idx = posts.indexOfFirst { it.floor == floor }
        if (idx >= 0) {
            returnPos = listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset
            highlightPostId = posts[idx].id
            scope.launch {
                listState.animateScrollToItem(idx + 1)
                delay(1500)
                highlightPostId = null
            }
        } else {
            jumpFloor(floor)
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
                val totalPages = (data?.totalPages ?: 1).coerceAtMost(10)
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
                session.showToast(json.optString("message", if (json.optInt("ok") == 1) "已点赞" else "操作失败"))
                load(d.page)
                if (points > 0) loadDanmaku()
            } catch (e: Exception) {
                session.showToast(e.message ?: "操作失败")
            }
        }
    }

    fun doFavorite() {
        val d = data ?: return
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
                    session.showToast("收藏状态已更新")
                    // 本地收藏快照：收藏即记入本地「收藏内容」，供离线检索，不额外访问源站（3.15）
                    val op = d.posts.firstOrNull { it.floor == 0 } ?: d.posts.firstOrNull()
                    session.settings.addFavorite(
                        topicId = d.topicId, title = d.title,
                        authorId = op?.authorId ?: 0, authorName = op?.authorName ?: "",
                        forumId = 0, forumName = "",
                        avatarUrl = op?.avatarUrl ?: "", titleColor = "",
                    )
                }
                load(d.page)
            } catch (e: Exception) { session.showToast(e.message ?: "失败") }
        }
    }

    // 长图所见即所得：捕获当前主题色（供导出使用，深色模式下导出深色长图）
    val colorScheme = MaterialTheme.colorScheme

    fun export(kind: String) {
        val d = data ?: return
        exportBusy = kind
        val exportTheme = TopicExport.ExportTheme.fromColorScheme(colorScheme)
        scope.launch {
            try {
                val url = sb.linux.client.data.Endpoints.abs("/topic/$tid")
                val file: File = withContext(Dispatchers.IO) {
                    when (kind) {
                        "html" -> TopicExport.exportHtml(context, d.title, d.posts, url)
                        "md" -> TopicExport.exportMarkdown(context, d.title, d.posts, url)
                        else -> TopicExport.renderLongImage(context, d.title, d.posts, url, exportTheme)
                    }
                }
                TopicExport.shareFile(
                    context, file,
                    when (kind) {
                        "html" -> "text/html"; "md" -> "text/markdown"; else -> "image/png"
                    }
                )
            } catch (e: Exception) {
                session.showToast("导出失败：${e.message}")
            } finally { exportBusy = null }
        }
    }

    fun runAi() {
        val d = data ?: return
        session.getAiSummary(tid)?.let {
            aiSummary = it
            return
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
                session.saveAiSummary(tid, summary)
            } catch (e: Exception) {
                aiError = e.message
            } finally { aiBusy = false }
        }
    }

    LaunchedEffect(tid) {
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
            load(cur)
            loadDanmaku()
        }
    }

    LaunchedEffect(data) {
        if (data != null && session.settings.aiAuto) runAi()
    }

    // 评论区无限滚动：接近末尾自动加载下一页（仅无限滚动模式）
    val listLast by remember {
        derivedStateOf { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1 }
    }
    LaunchedEffect(
        listLast, data?.posts?.size, data?.page, data?.totalPages,
        loadingMore, loading, topBarVisible
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

    Scaffold(
        bottomBar = {
            // 底部快捷栏：回复入口 + 点赞主楼 + 收藏（未登录点击跳转登录）
            if (data != null && error == null) {
                ReplyBar(
                    loggedIn = session.loginState.loggedIn,
                    liked = mainPost?.liked == true,
                    likeCount = mainPost?.likeCount ?: 0,
                    showLike = mainPost?.canLike == true,
                    showFavorite = data?.canFavorite == true,
                    onReply = {
                        if (session.loginState.loggedIn) { quoteText = ""; showReply = true }
                        else nav.navigate("login")
                    },
                    onLike = {
                        if (session.loginState.loggedIn) mainPost?.let { doLike(it, 0) }
                        else nav.navigate("login")
                    },
                    onFavorite = {
                        if (session.loginState.loggedIn) doFavorite()
                        else nav.navigate("login")
                    },
                )
            }
        }
    ) { pad ->
        val d = data
        Box(Modifier.padding(pad)) {
        when {
            loading -> LoadingBox()
            error != null -> ErrorBox(error!!) { load(initialPage) }
            d == null -> EmptyBox()
            else -> {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 72.dp, bottom = 8.dp)
                ) {
                    // ---------- 头部信息块（完整标题/统计/弹幕/AI/抽奖） ----------
                    item(key = "header") {
                        Column {
                            // 完整标题（不截断）+ 正文标签（已开奖 / 抽奖中等）
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    d.title,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    lineHeight = MaterialTheme.typography.titleLarge.lineHeight * 1.15f,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                if (d.titleTag.isNotBlank()) {
                                    val drawn = d.titleTag.contains("开奖")
                                    Badge(
                                        d.titleTag,
                                        if (drawn) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.primary,
                                        if (drawn) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onPrimary,
                                    )
                                }
                            }
                            // 浏览 / 回复统计 + 页码 胶囊
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                StatChip(Icons.Filled.Visibility, d.viewsText.ifBlank { "-" })
                                StatChip(Icons.AutoMirrored.Filled.Chat, d.repliesText.ifBlank { "${d.posts.size}" })
                                StatChip(
                                    null,
                                    if (topicInfinite) "${d.page} / ${d.totalPages} 页"
                                    else "$localReplyPage / $localReplyTotal 页"
                                )
                            }
                            // 打赏弹幕
                            if (danmakuLocalOn && danmaku.isNotEmpty()) {
                                DanmakuBar(danmaku)
                            }
                            // AI 总结：仅在开启「默认总结」时显示入口
                            if (session.settings.aiAuto) {
                                AiSummaryCard(
                                    summary = aiSummary,
                                    busy = aiBusy,
                                    error = aiError,
                                    enabled = AiClient.isConfigured(session.settings),
                                    onRun = { runAi() },
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
                                                val form = buildMap {
                                                    put("_csrf", d.csrf)
                                                    putAll(lp.joinFields)
                                                    if (option.isNotBlank()) put("option", option)
                                                }
                                                val resp = session.client.postForm(lp.joinAction, form)
                                                session.showToast(if (resp.url.contains("form_error")) HtmlParser.extractError(resp.html).ifBlank { "参与失败" } else "已参与抽奖")
                                                load(d.page, force = true)
                                            } catch (e: Exception) { session.showToast(e.message ?: "失败") }
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
                                    quoteText = "@${main.authorName}\n"
                                    showReply = true
                                },
                                onLike = { doLike(main, 0) },
                                onCoin = { coinTarget = main },
                                onReport = { nav.navigate("report/${main.likeCoinType.ifBlank { "topic" }}/${main.id}") },
                                onFloor = { f -> jumpToFloor(f) },
                                onThread = { openThread(main) },
                                threadReplyCount = threadReplyCount[main.floor] ?: 0,
                                onCopy = { copyPost = main },
                                onDonate = if (d.donateUrl != null) ({ nav.navigate("donate/$tid") }) else null,
                                onEditUser = { uid -> nav.navigate("user/$uid") },
                            )
                        }
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
                    itemsIndexed(pagedReplies, key = { _, p -> "r-${p.id}" }) { _, post ->
                        PostCard(
                            post = post,
                            loggedIn = session.loginState.loggedIn,
                            isOpAuthor = post.authorName == d.posts.firstOrNull()?.authorName,
                            isMainPost = false,
                            highlight = highlightPostId == post.id,
                            onUser = { nav.navigate("user/${post.authorId}") },
                            onQuote = {
                                quoteText = buildString {
                                    if (post.floor > 0) append("@${post.authorName} #${post.floor}\n")
                                    else append("@${post.authorName}\n")
                                }
                                showReply = true
                            },
                            onLike = { doLike(post, 0) },
                            onCoin = { coinTarget = post },
                            onReport = { nav.navigate("report/${post.likeCoinType.ifBlank { "reply" }}/${post.id}") },
                            onFloor = { f -> jumpToFloor(f) },
                            onThread = { openThread(post) },
                            threadReplyCount = threadReplyCount[post.floor] ?: 0,
                            onCopy = { copyPost = post },
                            onEditUser = { uid -> nav.navigate("user/$uid") },
                        )
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
                    item(key = "footer") { Spacer(Modifier.height(64.dp)) }
                }
            }
            }

            // 沉浸模式：顶栏隐藏后左上角悬浮返回按钮
            if (!topBarVisible) {
                SmallFloatingActionButton(
                    onClick = { nav.popBackStack() },
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp),
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.92f),
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                }
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
                    onClick = { scope.launch { listState.animateScrollToItem(0) } },
                    modifier = Modifier.padding(end = 16.dp, bottom = 16.dp),
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.92f),
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ) {
                    Icon(Icons.Filled.KeyboardArrowUp, "回到顶部")
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
                    Modifier.padding(bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SmallFloatingActionButton(
                        onClick = {
                            returnPos?.let { (idx, off) ->
                                scope.launch { listState.scrollToItem(idx, off) }
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

        // 悬浮顶栏（覆盖层，不参与布局，杜绝内容顶到状态栏的抖动问题 2.1）
        AnimatedVisibility(
            visible = topBarVisible,
            modifier = Modifier.align(Alignment.TopCenter),
            enter = slideInVertically { -it } + fadeIn(),
            exit = slideOutVertically { -it } + fadeOut(),
        ) {
            TopAppBar(
                // 顶栏不再显示标题：完整标题已在正文头部信息块展示，重复显示冗余
                title = {},
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
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f)
                ),
                // Scaffold 内容区已含状态栏内边距，这里置零避免双重叠加
                // （此前顶栏被垫高，遮住标题顶部并与悬浮菜单按钮重叠）
                windowInsets = WindowInsets(0, 0, 0, 0),
            )
        }

        // 右上角独立悬浮菜单按钮（类似返回 / 回顶按钮，2.11）
        val menuTop by animateDpAsState(
            targetValue = if (topBarVisible) 78.dp else 12.dp,
            animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy),
            label = "menuTop"
        )
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .padding(top = menuTop, end = 12.dp)
        ) {
            SmallFloatingActionButton(
                onClick = { menuOpen = true },
                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.92f),
                contentColor = MaterialTheme.colorScheme.onSurface,
            ) {
                Icon(Icons.Filled.MoreVert, "菜单")
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                // 跳转楼层移入菜单（2.10）
                DropdownMenuItem(
                    text = { Text("跳转楼层") },
                    leadingIcon = { Icon(Icons.Filled.FormatListNumbered, null) },
                    onClick = { menuOpen = false; showFloorDialog = true }
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
                if (session.loginState.loggedIn && data?.canFavorite == true) {
                    DropdownMenuItem(
                        text = { Text("收藏") },
                        leadingIcon = { Icon(Icons.Filled.FavoriteBorder, null) },
                        onClick = { menuOpen = false; doFavorite() }
                    )
                }
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
        }
    }

    // 回复弹窗（带人机验证时需用户作答，可「换一题」刷新验证码）
    if (showReply) {
        LaunchedEffect(Unit) { captchaOverride = null }
        val activeCaptcha = captchaOverride ?: data?.replyCaptcha
        ReplyDialog(
            initial = quoteText,
            captcha = activeCaptcha,
            onRefreshCaptcha = {
                scope.launch {
                    try {
                        // 重新拉取帖子页：源站每次渲染都会签发新的题目与 token
                        val resp = session.client.get("/topic/$tid?page=${data?.page ?: 1}")
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
                        val csrf = session.client.csrf()
                        val form = buildMap {
                            put("_csrf", csrf)
                            put("topic_id", tid.toString())
                            put("body", body)
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
                                    val r2 = session.client.get("/topic/$tid?page=${data?.page ?: 1}")
                                    HtmlParser.parseNativeCaptcha(r2.html)?.let { captchaOverride = it }
                                }
                            }
                        } else {
                            session.showToast("回复成功")
                            showReply = false
                            load(data?.totalPages ?: 1)
                        }
                    } catch (e: Exception) {
                        session.showToast(e.message ?: "回复失败")
                    } finally { onDone() }
                }
            }
        )
    }

    // 跳楼弹窗
    if (showFloorDialog) {
        FloorJumpDialog(
            maxFloor = data?.repliesText?.toIntOrNull() ?: ((data?.totalPages ?: 1) * 20),
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

    // 投币弹窗（档位跟随源站 data-tiers：1/5/10/50，可自定义，理由选填）
    coinTarget?.let { target ->
        CoinDialog(
            onDismiss = { coinTarget = null },
            tiers = listOf(1, 5, 10, 50),
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
            busy = exportBusy,
            onDismiss = { if (exportBusy == null) showExportDialog = false },
            onExport = {
                showExportDialog = false
                export(it)
            }
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
    likeCount: Int,
    showLike: Boolean,
    showFavorite: Boolean,
    onReply: () -> Unit,
    onLike: () -> Unit,
    onFavorite: () -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 3.dp) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // 伪输入条：点击弹出回复编辑
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(50))
                    .clickable(onClick = onReply)
            ) {
                Text(
                    if (loggedIn) "说点什么…" else "登录后参与回复",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 11.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (showLike) {
                // 点赞主楼
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onLike)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Icon(
                        if (liked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        if (liked) "已点赞" else "点赞",
                        Modifier.size(19.dp),
                        tint = if (liked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        if (likeCount > 0) "$likeCount" else "点赞",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (liked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
            if (showFavorite) {
                // 收藏
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onFavorite)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Icon(
                        Icons.Filled.BookmarkBorder, "收藏",
                        Modifier.size(19.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "收藏",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
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
    threadReplyCount: Int = 0,
) {
    if (isMainPost) {
        // ---------- 楼主正文：正文长按为系统原生文本选择，不再弹功能菜单（2.6） ----------
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.13f))
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
                threadReplyCount = threadReplyCount,
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
                    threadReplyCount = threadReplyCount,
                )
            }
        }
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
    threadReplyCount: Int = 0,
) {
    Column(modifier) {
        // 作者行：头像 + 名称/徽章 + 元信息，楼层号固定右上角
        Row(verticalAlignment = Alignment.CenterVertically) {
            Avatar(post.avatarUrl, avatarSize, Modifier.clickable(onClick = onUser), online = post.online)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                // 用户信息行（源站格式）：名字后面直接跟 UID，随后是组别/楼主徽章。
                // 用 FlowRow 在放不下时自动换行，避免徽章挤占导致用户名被截断
                FlowRow(
                    verticalArrangement = Arrangement.Center,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        post.authorName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (post.authorUid > 0) {
                        Text(
                            "UID ${post.authorUid}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                    if (isOpAuthor) {
                        Badge("楼主", MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                    if (post.userGroup.isNotBlank()) {
                        Badge(
                            post.userGroup,
                            MaterialTheme.colorScheme.secondaryContainer,
                            MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
                // 元信息行（源站格式）：称号徽章在发送时间前面显示
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    post.titleBadge?.let { TitleBadgeView(it) }
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
        // 正文：楼主正文支持系统原生长按选择文本（2.6）；#楼层号可点击跳转
        if (showFloorBadge) {
            HtmlContent(post.contentHtml, onFloor = onFloor)
        } else {
            SelectionContainer {
                HtmlContent(post.contentHtml, onFloor = onFloor)
            }
        }
        // 对话串联入口：本楼引用了其他楼层、或被其他楼层回复时显示
        val refFloors = post.referencedFloors.filter { it > 0 }
        if (refFloors.isNotEmpty() || threadReplyCount > 0) {
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
        // 操作行：楼主正文 = 点赞 / 投币 / 举报（2.9 去除收藏）；回复 = 点赞 / 投币 / 回复 / 举报
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.padding(bottom = 6.dp)
        ) {
            if (post.canLike) {
                PostAction(
                    icon = if (post.liked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    label = if (post.likeCount > 0) "赞 ${post.likeCount}" else "点赞",
                    onClick = { if (loggedIn) onLike() },
                    iconTint = if (post.liked) MaterialTheme.colorScheme.primary else null
                )
            }
            // 投币（金额可自定义，理由选填）：与点赞共用 donate-reaction 表单，主楼/未登录无表单时不显示
            if (post.canLike) {
                PostAction(
                    icon = Icons.Filled.Paid,
                    label = if (post.coined) "已投币" else "投币",
                    onClick = { if (loggedIn) onCoin() },
                    iconTint = if (post.coined) MaterialTheme.colorScheme.primary else null
                )
            }
            PostAction(icon = Icons.Filled.Flag, label = "举报", onClick = onReport)
            if (showFloorBadge) {
                // 回复的操作行：点赞 / 打赏 / 举报 / 回复（原"引用"统一为回复）
                PostAction(icon = Icons.Filled.FormatQuote, label = "回复", onClick = { if (loggedIn) onQuote() })
            }
            Spacer(Modifier.weight(1f))
        }
    }
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

/** AI 总结卡片（置顶显示） */
@Composable
fun AiSummaryCard(
    summary: String?,
    busy: Boolean,
    error: String?,
    enabled: Boolean,
    onRun: () -> Unit,
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
                    error != null -> Text(error, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    summary != null -> Text(summary, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
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
                Badge(
                    "楼主",
                    MaterialTheme.colorScheme.tertiaryContainer,
                    MaterialTheme.colorScheme.onTertiaryContainer
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
fun FloorJumpDialog(maxFloor: Int, onDismiss: () -> Unit, onJump: (Int) -> Unit) {
    var text by remember { mutableStateOf("") }
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
 *  title/submitLabel/showToolbar 可定制：私信等场景复用此编辑器但隐藏 Markdown 工具栏 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReplyDialog(
    initial: String,
    captcha: sb.linux.client.data.NativeCaptcha?,
    onDismiss: () -> Unit,
    onSubmit: (String, String, () -> Unit) -> Unit,
    title: String = "发表回复",
    submitLabel: String = "发布",
    showToolbar: Boolean = true,
    onRefreshCaptcha: (() -> Unit)? = null,
) {
    var body by remember { mutableStateOf(TextFieldValue(initial)) }
    var answer by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
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
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                TextButton(
                    onClick = { busy = true; onSubmit(body.text, answer) { busy = false } },
                    enabled = canSubmit
                ) { Text(if (busy) "发送中…" else submitLabel) }
            }
            Spacer(Modifier.height(4.dp))
            // Markdown 工具栏（横向滚动；私信等场景隐藏）
            if (showToolbar) Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    IconButton(onClick = { insert("**", "**", "粗体") }, modifier = Modifier.size(38.dp)) { Icon(Icons.Filled.FormatBold, "粗体", Modifier.size(18.dp)) }
                    IconButton(onClick = { insert("*", "*", "斜体") }, modifier = Modifier.size(38.dp)) { Icon(Icons.Filled.FormatItalic, "斜体", Modifier.size(18.dp)) }
                    IconButton(onClick = { insert("## ", "", "标题") }, modifier = Modifier.size(38.dp)) { Icon(Icons.Filled.Title, "标题", Modifier.size(18.dp)) }
                    IconButton(onClick = { insert("> ", "", "引用") }, modifier = Modifier.size(38.dp)) { Icon(Icons.Filled.FormatQuote, "引用", Modifier.size(18.dp)) }
                    IconButton(onClick = { insert("`", "`", "代码") }, modifier = Modifier.size(38.dp)) { Icon(Icons.Filled.Code, "代码", Modifier.size(18.dp)) }
                    IconButton(onClick = { insert("```\n", "\n```", "代码块") }, modifier = Modifier.size(38.dp)) { Icon(Icons.Filled.Code, "代码块", Modifier.size(18.dp)) }
                    IconButton(onClick = { insert("- ", "", "列表项") }, modifier = Modifier.size(38.dp)) { Icon(Icons.AutoMirrored.Filled.FormatListBulleted, "列表", Modifier.size(18.dp)) }
                    IconButton(onClick = { insert("[", "](https://)", "链接文字") }, modifier = Modifier.size(38.dp)) { Icon(Icons.Filled.Link, "链接", Modifier.size(18.dp)) }
                    IconButton(onClick = { insert("![", "](https://)", "图片描述") }, modifier = Modifier.size(38.dp)) { Icon(Icons.Filled.Image, "图片", Modifier.size(18.dp)) }
                }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = body,
                onValueChange = { body = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 150.dp),
                shape = RoundedCornerShape(14.dp)
            )
            // 人机验证（抽奖帖等）：题目展示，答案由用户填写；提供「换一题」刷新按钮
            if (captcha != null) {
                val refreshCap = onRefreshCaptcha
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = answer,
                    onValueChange = { if (it.length <= 8) answer = it },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    label = { Text("人机验证：${captcha.question}") },
                    singleLine = true,
                    trailingIcon = if (refreshCap != null) {
                        {
                            IconButton(onClick = refreshCap) {
                                Icon(Icons.Filled.Refresh, "换一题")
                            }
                        }
                    } else null,
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
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
            onCopyAll = { copyAll() },
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
    onCopyAll: () -> Unit,
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
                if (plain.isNotBlank()) {
                    Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 3.dp) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            Button(
                                onClick = onCopyAll,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("复制全部") }
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
fun ExportDialog(busy: String?, onDismiss: () -> Unit, onExport: (String) -> Unit) {
    ModalBottomSheet(onDismissRequest = { if (busy == null) onDismiss() }) {
        Column(Modifier.padding(horizontal = 16.dp)) {
            Row(
                Modifier.padding(start = 4.dp, top = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("导出帖子", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onDismiss, enabled = busy == null) { Text("关闭") }
            }
            Text(
                "选择导出格式，文件保存后可在「设置 → 已导出的帖子」查看",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
            )
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
                    enabled = busy == null,
                    onClick = { onExport(kind) }
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

/** 投币弹窗：默认金额 1/5/10/20，可自定义；理由选填（最多120字） */
@Composable
fun CoinDialog(tiers: List<Int>, onDismiss: () -> Unit, onConfirm: (Int, String) -> Unit) {
    var selected by remember { mutableIntStateOf(tiers.firstOrNull() ?: 1) }
    var custom by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }
    // 自定义金额生效时切换到自定义档
    val customNum = custom.toIntOrNull()
    if (customNum != null && customNum > 0 && customNum != selected) selected = customNum
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("投币") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    tiers.forEach { t ->
                        FilterChip(
                            selected = selected == t && custom.isBlank(),
                            onClick = {
                                custom = ""
                                selected = t
                            },
                            label = { Text("$t") }
                        )
                    }
                }
                OutlinedTextField(
                    value = custom,
                    onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) custom = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("自定义金额（积分）") },
                    placeholder = { Text("输入其他积分数") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = reason,
                    onValueChange = { if (it.length <= 120) reason = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("理由（选填）") },
                    placeholder = { Text("例如：内容很有帮助") },
                    singleLine = true,
                    supportingText = { Text("${reason.length}/120") }
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = selected > 0,
                onClick = { onConfirm(selected, reason.trim()) }
            ) { Text("投币") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}
