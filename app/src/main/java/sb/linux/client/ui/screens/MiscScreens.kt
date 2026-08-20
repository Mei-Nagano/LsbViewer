package sb.linux.client.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import sb.linux.client.data.HtmlParser
import sb.linux.client.data.LeaderRow
import sb.linux.client.data.Session
import sb.linux.client.data.TopicCard
import sb.linux.client.ui.*

// ---------------- 应用内网页（常规设置「应用内打开」，3.20） ----------------

/** 应用内 WebView 页面：常规设置选择「应用内打开」时，站内未映射页面与外部链接在此展示 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebScreen(session: Session, nav: NavHostController) {
    val entry = nav.currentBackStackEntry ?: return
    val rawUrl = entry.arguments?.getString("url").orEmpty()
    val url = sb.linux.client.data.Endpoints.abs(rawUrl)
    var pageTitle by remember { mutableStateOf(url) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        pageTitle,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                }
            )
        }
    ) { pad ->
        AndroidView(
            factory = { ctx ->
                android.webkit.WebView(ctx).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    webViewClient = object : android.webkit.WebViewClient() {
                        override fun doUpdateVisitedHistory(
                            view: android.webkit.WebView?,
                            u: String?,
                            isReload: Boolean,
                        ) {
                            pageTitle = view?.title?.takeIf { it.isNotBlank() } ?: u ?: url
                        }
                    }
                    loadUrl(url)
                }
            },
            modifier = Modifier
                .padding(pad)
                .fillMaxSize()
        )
    }
}

// ---------------- 每日签到 ----------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckinScreen(session: Session, nav: NavHostController) {
    var info by remember { mutableStateOf<HtmlParser.CheckinInfo?>(null) }
    var loading by remember { mutableStateOf(true) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun load() {
        scope.launch {
            loading = true
            try {
                val resp = session.client.get("/daily_checkin")
                info = HtmlParser.parseCheckin(resp.html)
            } catch (_: Exception) {
                info = null
            } finally { loading = false }
        }
    }

    LaunchedEffect(Unit) { load() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("每日签到") },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                }
            )
        }
    ) { pad ->
        Column(
            Modifier
                .padding(pad)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (loading) { LoadingBox(); return@Column }
            val i = info
            if (i == null) {
                Text("加载失败，请重试", style = MaterialTheme.typography.bodyMedium)
            } else {
                // 连续 / 累计签到天数：双统计卡片
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            Modifier.padding(vertical = 22.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                if (i.streak > 0) "${i.streak}" else "-",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.height(2.dp))
                            Text("连续签到", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            Modifier.padding(vertical = 22.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                if (i.total > 0) "${i.total}" else "-",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(Modifier.height(2.dp))
                            Text("累计签到", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                if (i.message.isNotBlank()) {
                    Text(i.message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (session.loginState.loggedIn && i?.canCheckin == true) {
                Button(
                    onClick = {
                        busy = true
                        scope.launch {
                            try {
                                val csrf = session.client.csrf()
                                val resp = session.client.postForm("/daily_checkin", mapOf("_csrf" to csrf))
                                if (resp.url.contains("form_error")) {
                                    session.showToast(HtmlParser.extractError(resp.html).ifBlank { "签到失败" })
                                } else {
                                    val r = HtmlParser.parseCheckin(resp.html)
                                    session.showToast(r.message.ifBlank { "签到成功" })
                                    // 立即同步签到状态缓存（侧边栏/我的页实时显示"已签到"）
                                    session.onCheckinDone()
                                }
                            } catch (e: Exception) {
                                session.showToast(e.message ?: "签到失败")
                            } finally { busy = false; load() }
                        }
                    },
                    enabled = !busy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(16.dp)
                ) { Text(if (busy) "签到中…" else "立即签到", style = MaterialTheme.typography.titleMedium) }
            } else if (!session.loginState.loggedIn) {
                Button(onClick = { nav.navigate("login") }) { Text("登录后签到") }
            }
        }
    }
}

// ---------------- 用户榜单 ----------------

private val LeaderTabs = listOf(
    "points" to "富豪榜",
    "replies" to "评论榜",
    "topics" to "发帖榜",
    "checkin" to "签到榜",
    "donation" to "打赏榜",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardScreen(session: Session, nav: NavHostController) {
    val entry = nav.currentBackStackEntry ?: return
    var type by remember { mutableStateOf(entry.arguments?.getString("type") ?: "points") }
    var rows by remember { mutableStateOf<List<LeaderRow>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun load(t: String) {
        scope.launch {
            loading = true; error = null
            try {
                val resp = session.client.get("/leaderboard?type=$t")
                rows = HtmlParser.parseLeaderboard(resp.html)
            } catch (e: Exception) { error = e.message } finally { loading = false }
        }
    }

    LaunchedEffect(Unit) { load(type) }

    Scaffold(topBar = { TopAppBar(title = { Text("用户榜单") }) }) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            ScrollableTabRow(selectedTabIndex = LeaderTabs.indexOfFirst { it.first == type }.coerceAtLeast(0), edgePadding = 8.dp) {
                LeaderTabs.forEach { (t, label) ->
                    Tab(selected = type == t, onClick = { type = t; load(t) }, text = { Text(label) })
                }
            }
            when {
                loading -> LoadingBox()
                error != null -> ErrorBox(error!!) { load(type) }
                rows.isEmpty() -> EmptyBox("暂无数据")
                else -> LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
                    // 前三名：奖牌卡片（金银铜色调）
                    if (rows.size >= 3) {
                        item(key = "top3") {
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                rows.take(3).forEach { r ->
                                    val (bg, fg, medal) = when (r.rank) {
                                        1 -> Triple(Color(0xFFFFF3CD), Color(0xFF7A5900), "1")
                                        2 -> Triple(Color(0xFFECEFF4), Color(0xFF444A54), "2")
                                        else -> Triple(Color(0xFFFBE2D5), Color(0xFF7A3B00), "3")
                                    }
                                    val dark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
                                    val cardBg = if (dark) MaterialTheme.colorScheme.surfaceContainerHigh else bg
                                    val cardFg = if (dark) MaterialTheme.colorScheme.onSurface else fg
                                    Surface(
                                        shape = RoundedCornerShape(18.dp),
                                        color = cardBg,
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(18.dp))
                                            .clickable { nav.navigate("user/${r.userId}") },
                                    ) {
                                        Column(
                                            Modifier.padding(vertical = 14.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            // 奖牌徽记
                                            Box(
                                                Modifier
                                                    .size(30.dp)
                                                    .clip(CircleShape)
                                                    .background(cardFg.copy(alpha = 0.14f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(medal, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = cardFg)
                                            }
                                            Spacer(Modifier.height(8.dp))
                                            Avatar(r.avatarUrl, 40)
                                            Spacer(Modifier.height(6.dp))
                                            Text(
                                                r.username,
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1, overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                r.valueText,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    // 其余名次：扁平列表
                    items(rows.drop(3), key = { "${it.rank}-${it.userId}" }) { r ->
                        ListItem(
                            leadingContent = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        "${r.rank}",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.width(26.dp)
                                    )
                                    Avatar(r.avatarUrl, 38)
                                }
                            },
                            headlineContent = { Text(r.username, style = MaterialTheme.typography.bodyLarge) },
                            supportingContent = { if (r.userGroup.isNotBlank()) Text(r.userGroup) },
                            trailingContent = { Text(r.valueText, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold) },
                            modifier = Modifier.clickable { nav.navigate("user/${r.userId}") }
                        )
                    }
                }
            }
        }
    }
}

// ---------------- 我的通知 ----------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(session: Session, nav: NavHostController) {
    var items by remember { mutableStateOf<List<sb.linux.client.data.NotificationItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val uid = session.loginState.userId

    fun load() {
        scope.launch {
            loading = true; error = null
            try {
                val resp = session.client.get("/user/$uid?tab=notifications")
                items = HtmlParser.parseNotifications(resp.html)
            } catch (e: Exception) { error = e.message } finally { loading = false }
        }
    }

    LaunchedEffect(Unit) { if (session.loginState.loggedIn) load() else loading = false }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("我的通知") },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                }
            )
        }
    ) { pad ->
        when {
            !session.loginState.loggedIn -> Column(
                Modifier.padding(pad).fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("通知与源站账号绑定", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(6.dp))
                Text("登录后可查看系统与互动通知", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))
                Button(onClick = { nav.navigate("login") }) { Text("去登录") }
            }
            loading -> LoadingBox()
            error != null -> ErrorBox(error!!) { load() }
            items.isEmpty() -> EmptyBox("暂无通知")
            else -> LazyColumn(Modifier.padding(pad), contentPadding = PaddingValues(vertical = 8.dp)) {
                // 通知内容可能完全重复：key 必须带 index 防止重复 key 崩溃
                itemsIndexed(items) { idx, n ->
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 5.dp),
                    ) {
                        Column(Modifier.padding(14.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(9.dp)
                            ) {
                                // 通知发起者头像占位（色调圆）
                                Box(
                                    Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        n.fromUser.take(1).ifBlank { "通" },
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                                Column(Modifier.weight(1f)) {
                                    Text(n.fromUser, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                                    Text(n.timeText, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Spacer(Modifier.height(10.dp))
                            Text(n.content, style = MaterialTheme.typography.bodyMedium, maxLines = 4)
                            if (n.link.isNotBlank()) {
                                Spacer(Modifier.height(4.dp))
                                TextButton(onClick = {
                                    val tid = Regex("""/topic/(\d+)""").find(n.link)?.groupValues?.get(1)
                                    if (tid != null) nav.navigate("topic/$tid")
                                }) { Text("查看详情") }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------- 浏览历史（本地） ----------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FootprintScreen(session: Session, nav: NavHostController) {
    // 本地浏览历史：不再解析源站足迹，左划卡片可删除（3.2）
    var topics by remember { mutableStateOf(session.settings.historyList()) }
    var showClearDialog by remember { mutableStateOf(false) }

    fun refresh() { topics = session.settings.historyList() }

    // 从帖子返回时刷新（浏览时间可能变化）
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val obs = androidx.lifecycle.LifecycleEventObserver { _, e ->
            if (e == androidx.lifecycle.Lifecycle.Event.ON_RESUME) refresh()
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("浏览历史") },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
                actions = {
                    TextButton(onClick = { refresh() }) { Text("刷新") }
                    TextButton(
                        onClick = { showClearDialog = true },
                        enabled = topics.isNotEmpty(),
                    ) { Text("清空") }
                }
            )
        }
    ) { pad ->
        if (topics.isEmpty()) {
            Box(Modifier.padding(pad).fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "暂无浏览历史",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(24.dp)
                )
            }
        } else {
            LazyColumn(Modifier.padding(pad), contentPadding = PaddingValues(vertical = 8.dp)) {
                items(topics, key = { it.topicId }) { t ->
                    val state = rememberSwipeToDismissBoxState(
                        confirmValueChange = { v ->
                            if (v == SwipeToDismissBoxValue.EndToStart) {
                                session.settings.removeHistory(t.topicId)
                                refresh()
                                session.showToast("已删除该记录")
                                true
                            } else false
                        }
                    )
                    SwipeToDismissBox(
                        state = state,
                        enableDismissFromStartToEnd = false,
                        backgroundContent = {
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 14.dp, vertical = 5.dp)
                                    .background(
                                        MaterialTheme.colorScheme.errorContainer,
                                        RoundedCornerShape(18.dp)
                                    ),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                Icon(
                                    Icons.Filled.Delete, "删除",
                                    Modifier.padding(end = 22.dp),
                                    tint = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    ) {
                        TopicCardView(
                            t,
                            onClick = { nav.navigate("topic/${t.topicId}") },
                            onForumClick = { fid -> if (fid > 0) nav.navigate("forum/$fid") },
                        )
                    }
                }
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("清空浏览历史") },
            text = { Text("确定清空全部 ${topics.size} 条本地浏览记录吗？") },
            confirmButton = {
                TextButton(onClick = {
                    session.settings.clearHistory()
                    refresh()
                    showClearDialog = false
                    session.showToast("浏览历史已清空")
                }) { Text("清空") }
            },
            dismissButton = { TextButton(onClick = { showClearDialog = false }) { Text("取消") } }
        )
    }
}

// ---------------- 社区保释所 ----------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BailScreen(session: Session, nav: NavHostController) {
    var rows by remember { mutableStateOf<List<HtmlParser.BailRow>>(emptyList()) }
    var myPoints by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        try {
            val resp = session.client.get("/community_bail")
            val (pts, list) = HtmlParser.parseBail(resp.html)
            myPoints = pts; rows = list
        } catch (_: Exception) {} finally { loading = false }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("社区保释所") },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                }
            )
        }
    ) { pad ->
        if (loading) { LoadingBox(); return@Scaffold }
        Column(Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
            if (myPoints.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("我的积分", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(2.dp))
                        Text(
                            myPoints,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
            }
            Text(
                "最近释放",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 6.dp)
            )
            Spacer(Modifier.height(8.dp))
            if (rows.isEmpty()) {
                val resp = remember { mutableStateOf("") }
                LaunchedEffect(Unit) {
                    runCatching {
                        resp.value = HtmlParser.mainText(session.client.get("/community_bail").html)
                    }
                }
                Text(resp.value, style = MaterialTheme.typography.bodySmall)
            }
            rows.forEach { r ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            r.helperName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            r.text,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

// ---------------- 打赏 ----------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DonateScreen(session: Session, nav: NavHostController) {
    val tid = nav.currentBackStackEntry?.arguments?.getLong("tid") ?: return
    var info by remember { mutableStateOf<HtmlParser.DonateInfo?>(null) }
    var amount by remember { mutableStateOf("10") }
    var message by remember { mutableStateOf("好帖！赞一个！") }
    var busy by remember { mutableStateOf(false) }
    var msg by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(tid) {
        runCatching {
            info = HtmlParser.parseDonate(session.client.get("/donate?topic_id=$tid").html)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("打赏作者") },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                }
            )
        }
    ) { pad ->
        val i = info ?: run { LoadingBox(); return@Scaffold }
        Column(
            Modifier
                .padding(pad)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
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
            // 顶部提示卡
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "选择打赏金额",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "打赏积分将直接赠送给楼主，感谢你对优质内容的支持",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            // 金额选择
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("10", "50", "100", "500").forEach { v ->
                    FilterChip(
                        selected = amount == v,
                        onClick = { amount = v },
                        label = { Text("$v 积分", style = MaterialTheme.typography.labelMedium) }
                    )
                }
            }
            OutlinedTextField(
                value = message, onValueChange = { message = it },
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
                                session.showToast("打赏成功")
                                nav.popBackStack()
                            }
                        } catch (e: Exception) { msg = e.message } finally { busy = false }
                    }
                },
                enabled = !busy,
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

// ---------------- 举报 ----------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(session: Session, nav: NavHostController) {
    val entry = nav.currentBackStackEntry ?: return
    val rtype = entry.arguments?.getString("type") ?: "reply"
    val rid = entry.arguments?.getLong("id") ?: return
    var info by remember { mutableStateOf<HtmlParser.ReportInfo?>(null) }
    var reason by remember { mutableStateOf<String?>(null) }
    var details by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var msg by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(rid) {
        runCatching {
            info = HtmlParser.parseReport(session.client.get("/content_report/$rid?type=$rtype").html)
            reason = info?.reasons?.firstOrNull()?.first
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("举报") },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                }
            )
        }
    ) { pad ->
        val i = info ?: run { LoadingBox(); return@Scaffold }
        Column(
            Modifier
                .padding(pad)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
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
            // 押金说明卡
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "举报押金 100 积分，举报成立原额退回",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                )
            }
            // 举报理由：可选中卡片
            i.reasons.forEach { (v, label) ->
                val selected = reason == v
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                    else MaterialTheme.colorScheme.surfaceContainerLow,
                    border = if (selected) androidx.compose.foundation.BorderStroke(
                        1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    ) else null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { reason = v },
                ) {
                    Row(
                        Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        RadioButton(selected = selected, onClick = { reason = v })
                        Text(
                            label.take(30),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
            OutlinedTextField(
                value = details, onValueChange = { details = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("补充说明（选填）") },
                shape = RoundedCornerShape(16.dp),
                minLines = 3
            )
            Button(
                onClick = {
                    val r = reason ?: run { msg = "请选择举报理由"; return@Button }
                    busy = true; msg = null
                    scope.launch {
                        try {
                            val csrf = session.client.csrf()
                            val resp = session.client.postForm(
                                "/content_report/$rid?type=$rtype", mapOf(
                                    "_csrf" to csrf,
                                    "target_type" to i.targetType.ifBlank { rtype },
                                    "target_id" to i.targetId.ifBlank { rid.toString() },
                                    "reason_type" to r,
                                    "reason_details" to details,
                                )
                            )
                            if (resp.url.contains("form_error")) {
                                msg = HtmlParser.extractError(resp.html).ifBlank { "提交失败" }
                            } else {
                                session.showToast("举报已提交")
                                nav.popBackStack()
                            }
                        } catch (e: Exception) { msg = e.message } finally { busy = false }
                    }
                },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    if (busy) "提交中…" else "提交举报",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
