package sb.linux.client.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import sb.linux.client.data.HtmlParser
import sb.linux.client.data.InviteCenter
import sb.linux.client.data.Session
import sb.linux.client.ui.Avatar
import sb.linux.client.ui.ErrorBox
import sb.linux.client.ui.LoadingBox

/** 邀请中心：完成自身邀请码管理，显示邀请码列表并提供兑换入口 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InviteCenterScreen(session: Session, nav: NavHostController) {
    val clipboard = LocalClipboardManager.current
    var page by remember { mutableStateOf<InviteCenter?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun load() {
        scope.launch {
            loading = true; error = null
            try {
                val resp = session.client.get("/invite_code")
                if (resp.url.contains("/login")) {
                    error = "请先登录后管理邀请码"
                } else {
                    val parsed = HtmlParser.parseInviteCenter(resp.html)
                    page = parsed
                    if (parsed.canExchange == false && parsed.codes.isEmpty() && parsed.invited.isEmpty() && !session.loginState.loggedIn)
                        error = "请先登录后管理邀请码"
                }
            } catch (e: Exception) {
                error = e.message
            } finally { loading = false }
        }
    }

    LaunchedEffect(Unit) { load() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("邀请码管理") },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { load() }, enabled = !loading) {
                        Icon(Icons.Filled.Refresh, "刷新")
                    }
                }
            )
        }
    ) { pad ->
        Box(Modifier.padding(pad)) {
            when {
                !session.loginState.loggedIn -> Column(
                    Modifier.fillMaxSize().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("邀请码与源站账号绑定", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(6.dp))
                    Text("登录后可查看、兑换并复制邀请码", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { nav.navigate("login") }) { Text("去登录") }
                }
                loading -> LoadingBox()
                error != null && page == null -> Column(
                    Modifier.fillMaxSize().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    ErrorBox(error!!) { load() }
                }
                else -> {
                    val p = page ?: run { loading = true; return@Box }
                    val desc = p.description
                    LazyColumn(
                        Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // 兑换卡片
                        item {
                            Surface(
                                Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerLow
                            ) {
                                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text(
                                        "邀请中心${p.subtitle.takeIf { it.isNotBlank() }?.let { " · $it" } ?: ""}",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    if (desc.isNotBlank()) {
                                        Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    if (session.loginState.loggedIn && p.canExchange) {
                                        Button(
                                            onClick = {
                                                busy = true
                                                scope.launch {
                                                    try {
                                                        val csrf = session.client.csrf()
                                                        val resp = session.client.postForm(
                                                            "/invite_code",
                                                            mapOf("_csrf" to csrf, "invite_action" to "exchange")
                                                        )
                                                        if (resp.url.contains("form_error") || resp.url.contains("error")) {
                                                            session.showToast(HtmlParser.extractError(resp.html).ifBlank { "兑换失败" })
                                                        } else {
                                                            val r = HtmlParser.parseInviteCenter(resp.html)
                                                            session.showToast("已兑换并新增一枚邀请码")
                                                            page = r
                                                        }
                                                    } catch (e: Exception) {
                                                        session.showToast(e.message ?: "兑换失败")
                                                    } finally {
                                                        busy = false
                                                        load()
                                                    }
                                                }
                                            },
                                            enabled = !busy,
                                            shape = RoundedCornerShape(14.dp),
                                            modifier = Modifier.fillMaxWidth().height(48.dp)
                                        ) { Text(if (busy) "兑换中…" else "兑换邀请码") }
                                    } else if (!session.loginState.loggedIn) {
                                        Button(onClick = { nav.navigate("login") }, modifier = Modifier.fillMaxWidth()) { Text("登录后兑换") }
                                    }
                                }
                            }
                        }
                        // 可用邀请码
                        item {
                            Text("可用邀请码", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        }
                        if (p.codes.isEmpty()) {
                            item { Text(p.emptyCodesText.ifBlank { "暂无邀请码，可先兑换一个。" }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 4.dp)) }
                        } else {
                            items(p.codes) { c ->
                                Surface(
                                    Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerLow
                                ) {
                                    Row(
                                        Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            c.code,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        IconButton(onClick = {
                                            clipboard.setText(AnnotatedString(c.code))
                                            session.showToast("已复制邀请码")
                                        }) {
                                            Icon(Icons.Filled.ContentCopy, "复制")
                                        }
                                    }
                                }
                            }
                        }
                        // 我邀请到的用户
                        item { Spacer(Modifier.height(4.dp)) }
                        item {
                            Text("我邀请到的用户", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        }
                        if (p.invited.isEmpty()) {
                            item { Text(p.emptyInvitedText.ifBlank { "暂时还没有用户通过你的邀请码注册。" }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 4.dp)) }
                        } else {
                            items(p.invited) { u ->
                                Surface(
                                    Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerLow
                                ) {
                                    Row(
                                        Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Avatar(u.avatarUrl, 38)
                                        Spacer(Modifier.width(12.dp))
                                        Column(Modifier.weight(1f)) {
                                            Text(u.username.ifBlank { u.statusText.ifBlank { "用户 ${u.userId}" } }, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            if (u.statusText.isNotBlank() && u.statusText != u.username) {
                                                Text(u.statusText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            }
                                        }
                                        if (u.userId > 0) {
                                            Text(
                                                "查看",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.clickable { nav.navigate("user/${u.userId}") }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        item { Spacer(Modifier.height(40.dp)) }
                    }
                }
            }
        }
    }
}