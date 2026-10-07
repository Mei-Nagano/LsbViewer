package sb.linux.client.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import sb.linux.client.ui.session.Session
import sb.linux.client.ui.*
import sb.linux.client.ui.feature.community.InviteCenterViewModel
import sb.linux.client.ui.feature.community.InviteCenterViewModelFactory

/** 邀请中心（/invite_center）：分享链接 + 奖励统计 + 已邀请用户列表 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InviteCenterScreen(session: Session, nav: NavHostController) {
    val factory = remember(session.inviteRepository) {
        InviteCenterViewModelFactory(session.inviteRepository)
    }
    val viewModel: InviteCenterViewModel = viewModel(factory = factory)
    val state by viewModel.uiState.collectAsState()
    var copied by remember { mutableStateOf(false) }
    val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current
    LaunchedEffect(viewModel) { viewModel.load() }

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
                    IconButton(onClick = viewModel::load) { Icon(Icons.Filled.Refresh, "刷新") }
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
            if (state.loading) { LoadingBox(); return@Column }
            val e = state.error
            if (e != null) {
                ErrorBox(e, viewModel::load)
            } else if (!session.account.loginState.loggedIn) {
                EmptyBox("登录后可查看邀请中心")
                Button(onClick = { nav.navigate("login") }) { Text("去登录") }
            } else {
                val i = state.data ?: return@Column
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
