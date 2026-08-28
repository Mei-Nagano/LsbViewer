package sb.linux.client.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import sb.linux.client.data.CommentFavorite
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
    // WebView 内页面可能发生跳转，记录当前实际地址供「在浏览器打开」使用
    var currentUrl by remember { mutableStateOf(url) }
    val context = androidx.compose.ui.platform.LocalContext.current

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
                },
                actions = {
                    // 在浏览器打开：调起系统浏览器查看当前页面
                    IconButton(onClick = {
                        runCatching {
                            context.startActivity(
                                android.content.Intent(
                                    android.content.Intent.ACTION_VIEW,
                                    android.net.Uri.parse(currentUrl)
                                )
                            )
                        }
                    }) {
                        Icon(Icons.Filled.OpenInNew, "在浏览器打开")
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
                    // 注入已保存的登录会话，私信等需要登录的页面在应用内即可直接看到（避免跳登录拦截）
                    session.client.exportWebCookies()
                    webViewClient = object : android.webkit.WebViewClient() {
                        override fun doUpdateVisitedHistory(
                            view: android.webkit.WebView?,
                            u: String?,
                            isReload: Boolean,
                        ) {
                            pageTitle = view?.title?.takeIf { it.isNotBlank() } ?: u ?: url
                            currentUrl = u ?: url
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

// ---------------- 已导出帖子的应用内查看 ----------------

/** 本地导出 HTML 查看页：读取 filesDir/exports 下的导出文件并在 WebView 中离线展示 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportedHtmlScreen(session: Session, nav: NavHostController) {
    val entry = nav.currentBackStackEntry ?: return
    val rawPath = entry.arguments?.getString("path").orEmpty()
    var title by remember { mutableStateOf("已导出的帖子") }
    var html by remember(rawPath) { mutableStateOf<String?>(null) }
    var error by remember(rawPath) { mutableStateOf<String?>(null) }
    // 重试计数：文件读取失败后可重新读取（如文件刚被恢复）
    var retryKey by remember(rawPath) { mutableStateOf(0) }

    LaunchedEffect(rawPath, retryKey) {
        if (rawPath.isBlank()) {
            error = "文件路径为空"
            return@LaunchedEffect
        }
        runCatching {
            val f = java.io.File(rawPath)
            if (!f.isFile) throw IllegalStateException("文件不存在或已被删除")
            html = f.readText()
        }.onFailure { error = it.message ?: "读取失败" }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        title,
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
        Box(Modifier.padding(pad).fillMaxSize()) {
            when {
                error != null -> ErrorBox(error!!) { html = null; error = null; retryKey++ }
                html == null -> LoadingBox()
                else -> {
                    // delegated property 不能 smart cast：先取局部非空变量
                    val content = html ?: ""
                    // baseUrl 指向导出目录：导出 HTML 若引用同目录相对资源可正常加载
                    val baseUrl = java.io.File(rawPath).parentFile?.toURI()?.toString()
                    AndroidView(
                        factory = { ctx ->
                            android.webkit.WebView(ctx).apply {
                                settings.javaScriptEnabled = false
                                loadDataWithBaseURL(baseUrl, content, "text/html", "utf-8", null)
                                webViewClient = object : android.webkit.WebViewClient() {
                                    override fun onPageFinished(view: android.webkit.WebView?, url: String?) {
                                        val t = view?.title?.takeIf { it.isNotBlank() } ?: return
                                        if (t.startsWith("file:")) title = java.io.File(rawPath).nameWithoutExtension
                                        else title = t
                                    }
                                }
                            }
                        },
                        update = { w ->
                            // 配置变更重建后重新注入内容（html 只读一次，重载保证显示）
                            val u = w.url
                            if (u == null || u.startsWith("about:blank")) {
                                w.loadDataWithBaseURL(baseUrl, content, "text/html", "utf-8", null)
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
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
                // 进入签到页即刷新全局签到缓存（侧边栏/我的页同步显示最新连续/累计天数）
                info?.let { i ->
                    session.applyCheckinInfoPublic(i)
                }
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

// ---------------- 邀请中心 ----------------

/** 邀请中心（/invite_center）：分享链接 + 奖励统计 + 已邀请用户列表 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InviteCenterScreen(session: Session, nav: NavHostController) {
    var info by remember { mutableStateOf<sb.linux.client.data.InviteCenter?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var copied by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current

    fun load() {
        scope.launch {
            loading = true; error = null
            try {
                val resp = session.client.get("/invite_center")
                info = HtmlParser.parseInviteCenter(resp.html)
            } catch (e: Exception) { error = e.message } finally { loading = false }
        }
    }

    LaunchedEffect(Unit) { load() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("邀请中心") },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { load() }) { Icon(Icons.Filled.Refresh, "刷新") }
                }
            )
        }
    ) { pad ->
        Column(
            Modifier
                .padding(pad)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (loading) { LoadingBox(); return@Column }
            val e = error
            if (e != null) {
                ErrorBox(e) { load() }
            } else if (!session.loginState.loggedIn) {
                EmptyBox("登录后可查看邀请中心")
                Button(onClick = { nav.navigate("login") }) { Text("去登录") }
            } else {
                val i = info ?: return@Column
                // 分享链接卡：说明 + 链接 + 复制
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("我的分享链接", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        if (i.desc.isNotBlank()) {
                            Text(
                                i.desc,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.65f),
                        ) {
                            Row(
                                Modifier.padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    i.link.ifBlank { "暂无邀请链接" },
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                                )
                                TextButton(
                                    enabled = i.link.isNotBlank(),
                                    onClick = {
                                        clipboard.setText(androidx.compose.ui.text.AnnotatedString(i.link))
                                        copied = true
                                    },
                                ) { Text(if (copied) "已复制" else "复制") }
                            }
                        }
                        if (i.rule.isNotBlank()) {
                            Text(
                                i.rule,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                            )
                        }
                    }
                }
                // 统计三卡：已邀请 / 首奖 / 二奖
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf(
                        i.invitedCount to "已邀请用户",
                        i.firstReward.removePrefix("首奖积分").trim('（', '(', ')', '）') to i.firstReward,
                        i.secondReward.removePrefix("二奖积分").trim('（', '(', ')', '）') to i.secondReward,
                    ).forEach { (value, label) ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f),
                            modifier = Modifier.weight(1f),
                        ) {
                            Column(
                                Modifier.padding(vertical = 14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text(
                                    value.ifBlank { "-" },
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary,
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
                // 已邀请用户列表
                Text("我邀请到的用户", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                if (i.users.isEmpty()) {
                    Text(
                        "暂时还没有用户通过你的分享链接注册",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                    ) {
                        Column {
                            i.users.forEachIndexed { idx, u ->
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .then(if (u.userId > 0) Modifier.clickable { nav.navigate("user/${u.userId}") } else Modifier)
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(u.name, style = MaterialTheme.typography.bodyMedium)
                                        if (u.statusText.isNotBlank()) {
                                            Text(
                                                u.statusText,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    }
                                    if (u.userId > 0) {
                                        Icon(
                                            Icons.Filled.ChevronRight, null,
                                            Modifier.size(14.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                                if (idx != i.users.lastIndex) {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(40.dp))
        }
    }
}

// ---------------- 我的称号 ----------------

/** 我的称号（/gacha_profile）：称号收藏 + 装备/赠送操作；其余称号系统子页经应用内网页打开 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GachaProfileScreen(session: Session, nav: NavHostController) {
    var profile by remember { mutableStateOf<sb.linux.client.data.GachaProfile?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var busyAction by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun load() {
        scope.launch {
            loading = true; error = null
            try {
                val resp = session.client.get("/gacha_profile")
                profile = HtmlParser.parseGachaProfile(resp.html)
            } catch (e: Exception) { error = e.message } finally { loading = false }
        }
    }

    LaunchedEffect(Unit) { load() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("我的称号") },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { load() }) { Icon(Icons.Filled.Refresh, "刷新") }
                }
            )
        }
    ) { pad ->
        Column(
            Modifier
                .padding(pad)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (loading) { LoadingBox(); return@Column }
            val e = error
            if (e != null) {
                ErrorBox(e) { load() }
            } else if (!session.loginState.loggedIn) {
                EmptyBox("登录后可查看我的称号")
                Button(onClick = { nav.navigate("login") }) { Text("去登录") }
            } else {
                val p = profile ?: return@Column
                // 头部统计 + 称号系统其他子页入口（抽取/熔炼/回收/合成/交易：应用内网页打开）
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        p.stat.ifBlank { "${p.titles.size} 种称号" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                    )
                }
                // 称号系统子页导航（应用内网页）
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                ) {
                    Column(Modifier.padding(vertical = 6.dp)) {
                        listOf(
                            "称号抽取" to "/gacha",
                            "称号熔炼" to "/gacha_forge_center",
                            "称号回收" to "/gacha_recycle_center",
                            "UR 合成" to "/gacha_recipes",
                            "称号交易" to "/gacha_market",
                        ).forEach { (label, path) ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { nav.navigate("web?url=${android.net.Uri.encode(path)}") }
                                    .padding(horizontal = 14.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    label,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f),
                                )
                                Icon(
                                    Icons.Filled.OpenInNew, null,
                                    Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
                // 称号收藏列表
                Text("称号收藏", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                if (p.titles.isEmpty()) {
                    Text(
                        "暂无称号",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    p.titles.forEach { t ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (t.equipped) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            else MaterialTheme.colorScheme.surfaceContainerLow,
                        ) {
                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    TitleBadgeView(t.badge, small = true)
                                    Spacer(Modifier.weight(1f))
                                    if (t.count > 1) {
                                        Text(
                                            "×${t.count}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    if (t.equipped) {
                                        Badge("已装备", MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.onPrimary)
                                    }
                                }
                                // 操作表单（装备/卸下/赠送等）：直接 POST 源站表单
                                if (t.actions.isNotEmpty()) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        t.actions.forEach { act ->
                                            FilledTonalButton(
                                                enabled = busyAction == null,
                                                onClick = {
                                                    busyAction = act.label
                                                    scope.launch {
                                                        try {
                                                            val csrf = session.client.csrf()
                                                            val resp = session.client.postForm(
                                                                act.action, mapOf("_csrf" to csrf) + act.fields
                                                            )
                                                            session.showToast(
                                                                if (resp.url.contains("form_error"))
                                                                    HtmlParser.extractError(resp.html).ifBlank { "操作失败" }
                                                                else "已${act.label}"
                                                            )
                                                            // 操作后刷新装备状态
                                                            val fresh = session.client.get("/gacha_profile")
                                                            profile = HtmlParser.parseGachaProfile(fresh.html)
                                                        } catch (e2: Exception) {
                                                            session.showToast(e2.message ?: "操作失败")
                                                        } finally { busyAction = null }
                                                    }
                                                },
                                            ) { Text(if (busyAction == act.label) "处理中…" else act.label) }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(40.dp))
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
                                            Avatar(r.avatarUrl, 40, online = r.online)
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
                                    Avatar(r.avatarUrl, 38, online = r.online)
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
                // 本地聚合私信：把「收到的私信通知」记入本地会话（按内容去重），聊天界面据此离线展示。
                // 通知列表最新在前：倒序写入并带上解析到的消息时间，保证会话按时间正序、时间显示准确
                items.asReversed().forEach { n ->
                    if (n.partnerId > 0 && n.content.isNotBlank()) {
                        val ts = HtmlParser.parseRelativeTime(n.timeText)
                        // 对方回复引用了我方消息而本地没有（如在网页端发送）：
                        // 按引用内容补造一条我方记录并写在回复之前（seq 更小、同天），
                        // 会话顺序与真实往来一致，不会出现"我方消息缺失/错位"
                        if (n.quoteText.isNotBlank() && !session.settings.pmHasContent(n.partnerId, n.quoteText)) {
                            session.settings.addPmMessage(
                                partnerId = n.partnerId, partnerName = n.fromUser,
                                avatarUrl = n.partnerAvatar, incoming = false,
                                content = n.quoteText, ts = ts, synthetic = true,
                            )
                        }
                        session.settings.addPmMessage(
                            partnerId = n.partnerId, partnerName = n.fromUser,
                            avatarUrl = n.partnerAvatar, incoming = true, content = n.content,
                            ts = ts, quoteText = n.quoteText,
                        )
                    }
                }
                // 源站机制：GET 通知页即把全部通知标记为已读（已读状态同步到源站），红点随之清除
                session.updateNotifUnread(0)
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
                },
                actions = {
                    // 标记已读：源站在打开通知页时已同步标记为已读，这里仅更新界面展示
                    val anyUnread = items.any { it.unread }
                    TextButton(
                        onClick = {
                            items = items.map { it.copy(unread = false) }
                            session.updateNotifUnread(0)
                            session.showToast("已标记为已读")
                        },
                        enabled = items.isNotEmpty() && anyUnread,
                    ) { Text("标记已读") }
                    // 刷新：重新请求源站通知列表
                    IconButton(onClick = { load() }, enabled = !loading) {
                        Icon(Icons.Filled.Refresh, "刷新")
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
                                // 通知发起者头像：解析到 URL 时加载真实头像（用户头像/系统 dylan），
                                // 无头像时退回首字占位圆
                                if (n.partnerAvatar.isNotBlank()) {
                                    Avatar(n.partnerAvatar, 34)
                                } else {
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
                                }
                                Column(Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(n.fromUser, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                                        // 源站未读标记（与源站「未读」标签一致）
                                        if (n.unread) {
                                            Text(
                                                "未读",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onError,
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(MaterialTheme.colorScheme.error)
                                                    .padding(horizontal = 6.dp, vertical = 1.dp),
                                            )
                                        }
                                    }
                                    Text(n.timeText, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Spacer(Modifier.height(10.dp))
                            Text(n.content, style = MaterialTheme.typography.bodyMedium, maxLines = 4)
                            // 操作入口：
                            // 帖子/系统通知带 /topic/ 链接 → 原生打开主题
                            // 私信通知带识别到的对方 uid → 打开聊天界面（本地聚合 + 可直接发送）
                            val topicId = Regex("""/topic/(\d+)""").find(n.link)?.groupValues?.get(1)
                            when {
                                topicId != null -> {
                                    Spacer(Modifier.height(4.dp))
                                    TextButton(onClick = { nav.navigate("topic/$topicId") }) { Text("查看主题") }
                                }
                                n.partnerId > 0 || n.link.contains("/notify/") -> {
                                    val pid = n.partnerId.takeIf { it > 0 }
                                        ?: Regex("""/notify/(\d+)""").find(n.link)?.groupValues?.get(1)?.toLongOrNull() ?: 0L
                                    if (pid > 0) {
                                        Spacer(Modifier.height(4.dp))
                                        TextButton(onClick = {
                                            nav.navigate("chat/$pid?name=${android.net.Uri.encode(n.fromUser)}&avatar=${android.net.Uri.encode(n.partnerAvatar)}")
                                        }) { Text("回复") }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------- 本地聚合私信聊天（QQ/微信式） ----------------

/** 私信聊天界面：本地聚合会话（收到的私信通知 + 我发出的记录），气泡左右分列，输入框直接回复源站 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(session: Session, nav: NavHostController) {
    val entry = nav.currentBackStackEntry ?: return
    val partnerId = entry.arguments?.getString("userId")?.toLongOrNull() ?: 0L
    var name by remember { mutableStateOf(entry.arguments?.getString("name") ?: "") }
    var avatarUrl by remember { mutableStateOf(entry.arguments?.getString("avatar") ?: "") }
    var msgs by remember { mutableStateOf(session.settings.pmThread(partnerId)) }
    var text by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    fun refresh() { msgs = session.settings.pmThread(partnerId) }

    // 进入即标记该会话已读
    LaunchedEffect(partnerId) { if (partnerId > 0) session.settings.markPmRead(partnerId) }
    // 消息变化自动滚到底部
    LaunchedEffect(msgs.size) { if (msgs.isNotEmpty()) listState.scrollToItem(msgs.size - 1) }

    fun send() {
        val body = text.trim()
        if (body.isBlank() || partnerId <= 0 || sending) return
        sending = true
        scope.launch {
            try {
                val csrf = session.client.csrf()
                val resp = session.client.postForm("/notify/$partnerId", mapOf("_csrf" to csrf, "content" to body))
                if (resp.url.contains("form_error")) {
                    session.showToast(HtmlParser.extractError(resp.html).ifBlank { "发送失败" })
                } else {
                    session.settings.addPmMessage(partnerId, name, avatarUrl, incoming = false, content = body)
                    text = ""
                    refresh()
                    session.showToast("已发送")
                }
            } catch (e: Exception) { session.showToast(e.message ?: "发送失败") }
            finally { sending = false }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        // 顶栏对方头像同样可点击进入其个人主页
                        Box(
                            Modifier.clip(CircleShape).clickable(enabled = partnerId > 0) {
                                nav.navigate("user/$partnerId")
                            }
                        ) { Avatar(avatarUrl, 30) }
                        Text(name.ifBlank { "用户 $partnerId" }, style = MaterialTheme.typography.titleMedium,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") }
                }
            )
        },
        bottomBar = {
            Surface(shadowElevation = 6.dp) {
                Row(
                    Modifier.fillMaxWidth().navigationBarsPadding().imePadding().padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = text, onValueChange = { text = it },
                        placeholder = { Text("输入私信内容") },
                        modifier = Modifier.weight(1f),
                        maxLines = 4,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = { send() }),
                        shape = RoundedCornerShape(20.dp),
                    )
                    FilledIconButton(onClick = { send() }, enabled = text.isNotBlank() && !sending && partnerId > 0) {
                        Icon(Icons.Filled.Send, "发送")
                    }
                }
            }
        }
    ) { pad ->
        when {
            partnerId <= 0 -> Box(Modifier.padding(pad).fillMaxSize(), contentAlignment = Alignment.Center) { Text("无效的会话") }
            msgs.isEmpty() -> Column(
                Modifier.padding(pad).fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
            ) {
                Text("还没有消息", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(6.dp))
                Text("在这里发送第一条，或对方发来私信后，往来消息会显示在此", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            else -> LazyColumn(
                Modifier.padding(pad).fillMaxSize(),
                state = listState,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
            ) {
                items(msgs) { m ->
                    ChatBubble(m, onPartnerClick = { if (m.partnerId > 0) nav.navigate("user/${m.partnerId}") })
                }
            }
        }
    }
}

/** 单条气泡：对方靠左、头像在消息左侧（可点头像进对方主页），我方靠右无头像，气泡着色区分 */
@Composable
private fun ChatBubble(m: sb.linux.client.data.PmMessage, onPartnerClick: () -> Unit = {}) {
    val mine = !m.incoming
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        // 对方头像固定在消息左侧（QQ/微信式），点击进入对方个人主页
        if (!mine) {
            Box(Modifier.clip(CircleShape).clickable(onClick = onPartnerClick)) {
                Avatar(m.avatarUrl, 32)
            }
            Spacer(Modifier.width(8.dp))
        }
        Column(horizontalAlignment = if (mine) Alignment.End else Alignment.Start) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (mine) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.widthIn(max = 280.dp)
            ) {
                Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    // 回复引用预览（QQ/微信式）：对方回复时引用的我方消息
                    if (m.quoteText.isNotBlank()) {
                        Text(
                            m.quoteText,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (mine) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.72f)
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(bottom = 3.dp),
                        )
                    }
                    Text(
                        m.content,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (mine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
            if (m.timeText.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(m.timeText, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
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
    var query by remember { mutableStateOf("") }

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
                    TextButton(
                        onClick = { showClearDialog = true },
                        enabled = topics.isNotEmpty(),
                    ) { Text("清空") }
                }
            )
        }
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            // 本地搜索（只过滤标题，纯本地无需访问源站）
            if (topics.isNotEmpty()) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("搜索浏览历史") },
                    leadingIcon = { Icon(Icons.Filled.Search, null) },
                    trailingIcon = {
                        if (query.isNotBlank()) Icon(
                            Icons.Filled.Close, "清空",
                            Modifier.clickable { query = "" }
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    textStyle = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
            if (topics.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "暂无浏览历史",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(24.dp)
                    )
                }
            } else {
                val filtered = topics.filter {
                    query.isBlank() || it.title.contains(query.trim(), ignoreCase = true)
                }
                if (filtered.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            "无匹配“${query}”的记录",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(24.dp)
                        )
                    }
                } else {
                    LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
                        items(filtered, key = { it.topicId }) { t ->
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
                            showComments = false,   // 浏览历史只展示浏览时间，不显示评论数
                            showForum = false,      // 浏览历史不显示所属板块
                        )
                    }
                    }
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

// ---------------- 收藏内容（本地快照，收藏时更新，可本地搜索；34 增加评论分类） ----------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(session: Session, nav: NavHostController) {
    // 本地收藏：收藏帖子时写入本地快照，纯本地筛选（3.15）；
    // 进入页面时从源站合并一次收藏（本地已有快照的不覆盖），之后全部本地展示
    var topics by remember { mutableStateOf(session.settings.favoriteList()) }
    // 本地收藏的评论（34）：纯本地快照，与源站无关
    var comments by remember { mutableStateOf(session.settings.commentFavoriteList()) }
    // 分类 Tab：0 = 帖子，1 = 评论
    var tab by remember { mutableIntStateOf(0) }
    var showClearDialog by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var syncing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun refresh() {
        topics = session.settings.favoriteList()
        comments = session.settings.commentFavoriteList()
    }

    // 同步源站收藏：合并进本地，静默失败（本地快照仍可正常展示）
    fun syncFromSource() {
        val uid = session.loginState.userId
        if (!session.loginState.loggedIn || uid <= 0 || syncing) return
        scope.launch {
            syncing = true
            try {
                val resp = session.client.get("/user/$uid?tab=favorites")
                session.settings.mergeFavorites(HtmlParser.parseTopicList(resp.html).first)
                refresh()
            } catch (_: Exception) {
            } finally { syncing = false }
        }
    }

    // 每次进入页面同步一次（之后搜索/浏览全部走本地，不再访问源站）
    LaunchedEffect(Unit) { syncFromSource() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("收藏内容") },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
                actions = {
                    if (tab == 0) {
                        TextButton(onClick = { syncFromSource() }, enabled = !syncing) {
                            Text(if (syncing) "同步中…" else "刷新")
                        }
                    }
                    TextButton(
                        onClick = { showClearDialog = true },
                        enabled = if (tab == 0) topics.isNotEmpty() else comments.isNotEmpty(),
                    ) { Text("清空") }
                }
            )
        }
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            // 分类选择（34）：帖子 / 评论
            TabRow(selectedTabIndex = tab) {
                Tab(
                    selected = tab == 0,
                    onClick = { tab = 0; query = "" },
                    text = { Text("帖子${if (topics.isNotEmpty()) " (${topics.size})" else ""}") }
                )
                Tab(
                    selected = tab == 1,
                    onClick = { tab = 1; query = "" },
                    text = { Text("评论${if (comments.isNotEmpty()) " (${comments.size})" else ""}") }
                )
            }
            val listEmpty = if (tab == 0) topics.isEmpty() else comments.isEmpty()
            if (!listEmpty) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text(if (tab == 0) "搜索收藏帖子" else "搜索收藏评论") },
                    leadingIcon = { Icon(Icons.Filled.Search, null) },
                    trailingIcon = {
                        if (query.isNotBlank()) Icon(
                            Icons.Filled.Close, "清空",
                            Modifier.clickable { query = "" }
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    textStyle = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
            if (tab == 0) {
                if (topics.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            if (syncing) "正在从源站同步收藏…"
                            else "收藏的帖子会显示在这里（在帖子菜单里点「收藏」，登录后进入本页会自动同步源站收藏）",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(24.dp)
                        )
                    }
                } else {
                    val filtered = topics.filter {
                        query.isBlank() || it.title.contains(query.trim(), ignoreCase = true)
                    }
                    if (filtered.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                "无匹配“${query}”的收藏",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(24.dp)
                            )
                        }
                    } else {
                        LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
                            items(filtered, key = { it.topicId }) { t ->
                                val state = rememberSwipeToDismissBoxState(
                                    confirmValueChange = { v ->
                                        if (v == SwipeToDismissBoxValue.EndToStart) {
                                            session.settings.removeFavorite(t.topicId)
                                            refresh()
                                            session.showToast("已取消收藏")
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
                                                Icons.Filled.Delete, "取消收藏",
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
            } else {
                if (comments.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            "收藏的评论会显示在这里（在评论操作行点「收藏」，点击收藏项可跳回原楼层）",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(24.dp)
                        )
                    }
                } else {
                    val filtered = comments.filter {
                        query.isBlank() || it.content.contains(query.trim(), ignoreCase = true) ||
                            it.topicTitle.contains(query.trim(), ignoreCase = true) ||
                            it.authorName.contains(query.trim(), ignoreCase = true)
                    }
                    if (filtered.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                "无匹配“${query}”的收藏",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(24.dp)
                            )
                        }
                    } else {
                        LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
                            items(filtered, key = { it.replyId }) { c ->
                                val state = rememberSwipeToDismissBoxState(
                                    confirmValueChange = { v ->
                                        if (v == SwipeToDismissBoxValue.EndToStart) {
                                            session.settings.removeCommentFavorite(c.replyId)
                                            refresh()
                                            session.showToast("已取消收藏")
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
                                                Icons.Filled.Delete, "取消收藏",
                                                Modifier.padding(end = 22.dp),
                                                tint = MaterialTheme.colorScheme.onErrorContainer
                                            )
                                        }
                                    }
                                ) {
                                    CommentFavCard(c) {
                                        nav.navigate("topic/${c.topicId}?floor=${c.floor}")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text(if (tab == 0) "清空收藏帖子" else "清空收藏评论") },
            text = {
                Text(
                    if (tab == 0) "确定清空全部 ${topics.size} 条本地收藏记录吗？"
                    else "确定清空全部 ${comments.size} 条收藏的评论吗？"
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (tab == 0) session.settings.clearFavorites()
                    else session.settings.clearCommentFavorites()
                    refresh()
                    showClearDialog = false
                    session.showToast("收藏内容已清空")
                }) { Text("清空") }
            },
            dismissButton = { TextButton(onClick = { showClearDialog = false }) { Text("取消") } }
        )
    }
}

/** 收藏评论卡片（34）：作者 + 摘要 + 所属帖子；点击跳回原帖原楼层 */
@Composable
private fun CommentFavCard(c: CommentFavorite, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 5.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
    ) {
        Column(
            Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                Avatar(c.avatarUrl, 30)
                Column(Modifier.weight(1f)) {
                    Text(
                        c.authorName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (c.timeText.isNotBlank()) {
                        Text(
                            c.timeText,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    Text(
                        "#${c.floor}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                    )
                }
            }
            Text(
                c.content,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Chat, null,
                    Modifier.size(13.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    c.topicTitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
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
