package sb.linux.client.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import sb.linux.client.LocalMasterNav
import sb.linux.client.data.HtmlParser
import sb.linux.client.data.Session
import sb.linux.client.data.UserProfile
import sb.linux.client.ui.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserScreen(session: Session, nav: NavHostController) {
    val entry = nav.currentBackStackEntry ?: return
    val uid = entry.arguments?.getLong("uid") ?: return
    var tab by remember { mutableStateOf(entry.arguments?.getString("tab") ?: "topics") }
    var profile by remember { mutableStateOf<UserProfile?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var showAvatar by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val userListState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }

    var topicCards by remember { mutableStateOf<List<sb.linux.client.data.TopicCard>>(emptyList()) }
    var replies by remember { mutableStateOf<List<sb.linux.client.data.ReplyItem>>(emptyList()) }
    var pointRows by remember { mutableStateOf<List<sb.linux.client.data.PointRow>>(emptyList()) }
    // 通知（仅本人主页）：私信/帖子/系统通知，不能按主题列表解析否则私信会被当成帖子
    var notifications by remember { mutableStateOf<List<sb.linux.client.data.NotificationItem>>(emptyList()) }
    val isSelf = session.loginState.userId == uid
    // 平板双栏的主栏控制器（手机为 null）：退出登录时切主栏并清空右栏
    val masterNav = LocalMasterNav.current

    fun load(t: String) {
        scope.launch {
            loading = true; error = null
            try {
                val resp = session.client.get("/user/$uid?tab=$t")
                // 解析失败不阻塞列表
                if (profile == null) runCatching { profile = HtmlParser.parseUserProfile(resp.html) }
                when (t) {
                    "replies" -> {
                        replies = runCatching { HtmlParser.parseReplyList(resp.html) }.getOrDefault(emptyList())
                        topicCards = emptyList(); pointRows = emptyList(); notifications = emptyList()
                    }
                    "points_rewards" -> {
                        pointRows = runCatching { HtmlParser.parsePointRows(resp.html) }.getOrDefault(emptyList())
                        replies = emptyList(); topicCards = emptyList(); notifications = emptyList()
                    }
                    "notifications" -> {
                        notifications = runCatching { HtmlParser.parseNotifications(resp.html) }.getOrDefault(emptyList())
                        // 与「我的通知」页一致：倒序写入并带上解析到的消息时间，保证会话按时间正序；
                        // 对方回复引用了我方消息而本地没有时，按引用补造我方记录（写在回复之前）
                        notifications.asReversed().forEach { n ->
                            if (n.partnerId > 0 && n.content.isNotBlank()) {
                                val ts = HtmlParser.parseRelativeTime(n.timeText)
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
                        replies = emptyList(); topicCards = emptyList(); pointRows = emptyList()
                    }
                    else -> {
                        topicCards = runCatching { HtmlParser.parseTopicList(resp.html).first }.getOrDefault(emptyList())
                        replies = emptyList(); pointRows = emptyList(); notifications = emptyList()
                    }
                }
            } catch (e: Exception) {
                error = e.message ?: "加载失败"
                replies = emptyList(); topicCards = emptyList(); pointRows = emptyList(); notifications = emptyList()
            } finally { loading = false }
        }
    }

    LaunchedEffect(uid) { load(tab) }

    // 点进该用户主页即刷新其头像缓存：清掉 Coil 内存/磁盘里该 URL 的旧图，
    // 大头像随后以新拉取的字节解码，侧栏等处的旧头像也能随之更新。
    val context = LocalContext.current
    LaunchedEffect(profile?.avatarUrl) {
        val url = profile?.avatarUrl ?: return@LaunchedEffect
        if (url.isNotBlank()) {
            val loader = coil.Coil.imageLoader(context)
            val memCache = loader.memoryCache
            memCache?.keys?.filter { it.key == url }?.forEach { key -> memCache.remove(key) }
            loader.diskCache?.remove(url)
        }
    }

    val tabs = buildList {
        add("topics" to "主题")
        add("replies" to "回帖")
        add("points_rewards" to "积分")
        add("favorites" to "收藏")
        if (isSelf) add("notifications" to "通知")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(profile?.username ?: "用户") },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
                actions = {
                    if (session.loginState.loggedIn && !isSelf) {
                        IconButton(onClick = {
                            // 进入「通知-私信」同一套聊天界面（本地聚合会话 + 输入框直接回复源站）
                            nav.navigate(
                                "chat/$uid?name=${android.net.Uri.encode(profile?.username ?: "")}" +
                                    "&avatar=${android.net.Uri.encode(profile?.avatarUrl ?: "")}"
                            )
                        }) {
                            Icon(Icons.AutoMirrored.Filled.Send, "私信")
                        }
                    }
                }
            )
        }
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            // 用户信息头：品牌色横幅 + 大头像 + 信息胶囊
            profile?.let { p ->
                Column(Modifier.fillMaxWidth()) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(22.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                    ) {
                        Column(Modifier.padding(18.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Avatar(
                                    p.avatarUrl, 60, online = p.online,
                                    modifier = Modifier.clickable { showAvatar = true }
                                )
                                Spacer(Modifier.width(14.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(p.username, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                    // 称号（源站称号系统徽章）+ 用户组
                                    if (p.titleBadge != null || p.userGroup.isNotBlank()) {
                                        Spacer(Modifier.height(4.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            p.titleBadge?.let { TitleBadgeView(it) }
                                            if (p.userGroup.isNotBlank()) {
                                                if (p.titleBadge != null) Spacer(Modifier.width(6.dp))
                                                Surface(
                                                    shape = androidx.compose.foundation.shape.RoundedCornerShape(50),
                                                    color = MaterialTheme.colorScheme.secondaryContainer
                                                ) {
                                                    Text(
                                                        p.userGroup,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Medium,
                                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            Spacer(Modifier.height(14.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.horizontalScroll(rememberScrollState())
                            ) {
                                UserInfoPill(Icons.Filled.Star, "积分 ${p.points.ifBlank { "-" }}")
                                UserInfoPill(Icons.Filled.Badge, "UID ${p.userId}")
                                if (p.rankText.isNotBlank() && p.userGroup.isBlank()) {
                                    UserInfoPill(Icons.Filled.MilitaryTech, p.rankText)
                                }
                            }
                            if (p.bio.isNotBlank()) {
                                Spacer(Modifier.height(12.dp))
                                // 过长简介默认折叠，可展开/收起；不长的正常展示且不显示按钮。
                                // 简介为源站 HTML（markdown 渲染结果），用 HtmlContent 渲染以保留原文格式
                                val plainLen = org.jsoup.Jsoup.parse(p.bio).text().length
                                val bioIsLong = plainLen > 80
                                var bioExpanded by remember(plainLen) { mutableStateOf(!bioIsLong) }
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .then(if (bioIsLong && !bioExpanded) Modifier.height(96.dp) else Modifier)
                                        .then(if (bioIsLong && !bioExpanded) Modifier.clipToBounds() else Modifier)
                                ) {
                                    HtmlContent(
                                        p.bio,
                                        Modifier.fillMaxWidth(),
                                        onFloor = {}
                                    )
                                }
                                if (bioIsLong) {
                                    TextButton(onClick = { bioExpanded = !bioExpanded }) {
                                        Text(if (bioExpanded) "收起" else "展开/收起")
                                    }
                                }
                            }
                        }
                    }
                }
            }
            // 本人主页：个人设置与退出登录入口（原"我的"页账号分类移入，3.1）
            var confirmLogout by remember { mutableStateOf(false) }
            if (isSelf && session.loginState.loggedIn) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow
                ) {
                    Column {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { nav.navigate("settings") }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.Settings, null, Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(12.dp))
                            Text("个人设置", style = MaterialTheme.typography.bodyLarge)
                        }
                        HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { confirmLogout = true }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Logout, null, Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(Modifier.width(12.dp))
                            Text("退出登录", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
            // 退出登录确认
            if (confirmLogout) {
                AlertDialog(
                    onDismissRequest = { confirmLogout = false },
                    title = { Text("退出登录") },
                    text = { Text("确定要退出当前账号吗？") },
                    confirmButton = {
                        TextButton(onClick = {
                            confirmLogout = false
                            session.onLoggedOut()
                            // 平板双栏：主栏切回首页并清空右栏；手机：回首页重建栈
                            if (masterNav != null) {
                                masterNav.navigate("home") {
                                    popUpTo(masterNav.graph.findStartDestination().id) { inclusive = true }
                                }
                                nav.popBackStack("detailEmpty", false)
                            } else {
                                nav.navigate("home") { popUpTo("home") { inclusive = true } }
                            }
                        }) { Text("退出", color = MaterialTheme.colorScheme.error) }
                    },
                    dismissButton = {
                        TextButton(onClick = { confirmLogout = false }) { Text("取消") }
                    }
                )
            }
            ScrollableTabRow(selectedTabIndex = tabs.indexOfFirst { it.first == tab }.coerceAtLeast(0), edgePadding = 8.dp) {
                tabs.forEach { (t, label) ->
                    Tab(selected = tab == t, onClick = { tab = t; load(t) }, text = { Text(label) })
                }
            }
            Box(Modifier.weight(1f).fillMaxWidth()) {
                when {
                    loading -> LoadingBox()
                    error != null -> ErrorBox(error!!) { load(tab) }
                    else -> LazyColumn(
                        state = userListState,
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        when (tab) {
                            "replies" -> if (replies.isEmpty()) {
                                item { EmptyBox("暂无回帖") }
                            } else itemsIndexed(replies, key = { i, r -> "${r.replyId}-${r.topicId}-$i" }) { _, r ->
                                Surface(
                                    shape = RoundedCornerShape(18.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                                    onClick = { nav.navigate("topic/${r.topicId}") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 5.dp),
                                ) {
                                    Column(Modifier.padding(14.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                r.title,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f)
                                            )
                                            if (r.forumName.isNotBlank()) {
                                                Spacer(Modifier.width(8.dp))
                                                Badge(
                                                    r.forumName,
                                                    MaterialTheme.colorScheme.secondaryContainer,
                                                    MaterialTheme.colorScheme.onSecondaryContainer
                                                )
                                            }
                                        }
                                        if (r.excerpt.isNotBlank()) {
                                            Spacer(Modifier.height(6.dp))
                                            Text(
                                                r.excerpt,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 3,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Spacer(Modifier.height(6.dp))
                                        Text(
                                            r.timeText,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                            "points_rewards" -> if (pointRows.isEmpty()) {
                                item { EmptyBox("暂无积分记录") }
                            } else itemsIndexed(pointRows, key = { i, row -> "${row.timeText}-${row.reason}-$i" }) { _, row ->
                                ListItem(
                                    headlineContent = { Text(row.reason) },
                                    supportingContent = { Text(row.timeText) },
                                    trailingContent = {
                                        Text(
                                            "${if (row.positive) "+" else ""}${row.change} 积分",
                                            color = if (row.positive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                            style = MaterialTheme.typography.labelLarge
                                        )
                                    }
                                )
                            }
                            "notifications" -> if (notifications.isEmpty()) {
                                item { EmptyBox("暂无通知") }
                            } else itemsIndexed(notifications, key = { i, n -> "notif-${n.partnerId}-${n.link}-$i" }) { _, n ->
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
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                n.fromUser,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.weight(1f),
                                                maxLines = 1, overflow = TextOverflow.Ellipsis
                                            )
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
                                        Spacer(Modifier.height(4.dp))
                                        Text(n.content, style = MaterialTheme.typography.bodyMedium, maxLines = 4,
                                            overflow = TextOverflow.Ellipsis)
                                        Spacer(Modifier.height(4.dp))
                                        Text(n.timeText, style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        // 操作入口：帖子通知 → 打开主题；私信通知 → 打开聊天界面（QQ/微信式）
                                        val topicId = Regex("""/topic/(\d+)""").find(n.link)?.groupValues?.get(1)
                                        when {
                                            topicId != null -> TextButton(
                                                onClick = { nav.navigate("topic/$topicId") }
                                            ) { Text("查看主题") }
                                            else -> {
                                                val pid = n.partnerId.takeIf { it > 0 }
                                                    ?: Regex("""/notify/(\d+)""").find(n.link)?.groupValues?.get(1)?.toLongOrNull() ?: 0L
                                                if (pid > 0) TextButton(
                                                    onClick = {
                                                        nav.navigate("chat/$pid?name=${android.net.Uri.encode(n.fromUser)}&avatar=${android.net.Uri.encode(n.partnerAvatar)}")
                                                    }
                                                ) { Text("回复") }
                                            }
                                        }
                                    }
                                }
                            }
                            else -> if (topicCards.isEmpty()) {
                                item {
                                    Spacer(Modifier.height(28.dp))
                                    EmptyBox("暂无主题")
                                }
                            } else itemsIndexed(topicCards, key = { i, t -> "${t.topicId}-${t.pinned}-$i" }) { _, t ->
                                TopicCardView(t, onClick = { nav.navigate("topic/${t.topicId}") },
                                    onForumClick = { fid -> if (fid > 0) nav.navigate("forum/$fid") })
                            }
                        }
                    }
                }
                // 返回顶部悬浮按钮：滚过前几条后出现
                val showBackTop by remember {
                    derivedStateOf { userListState.firstVisibleItemIndex > 2 }
                }
                if (showBackTop) {
                    SmallFloatingActionButton(
                        onClick = { scope.launch { userListState.animateScrollToItem(0) } },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 16.dp, bottom = 24.dp),
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.92f),
                        contentColor = MaterialTheme.colorScheme.onSurface,
                    ) {
                        Icon(Icons.Filled.KeyboardArrowUp, "回到顶部")
                    }
                }
            }
        }
    }

    // 点击头像后全屏查看大图（SVG 头像走 resvg 归一化渲染）
    if (showAvatar) {
        val a = profile?.avatarUrl
        if (!a.isNullOrBlank()) AvatarViewer(a) { showAvatar = false }
    }
}

/** 用户信息胶囊：图标 + 文本 */
@Composable
private fun UserInfoPill(icon: ImageVector, text: String) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.65f)
    ) {
        Row(
            Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(icon, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
            Text(
                text,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
    }
}
