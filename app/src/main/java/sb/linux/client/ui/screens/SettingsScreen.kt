package sb.linux.client.ui.screens
import sb.linux.client.common.network.Endpoints

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import sb.linux.client.ui.session.Session
import sb.linux.client.domain.profile.ProfileSettingsPage
import sb.linux.client.ui.*
import sb.linux.client.ui.feature.profile.ProfileSettingsViewModel
import sb.linux.client.ui.feature.profile.ProfileSettingsViewModelFactory
import java.io.File

/**
 * 个人信息设置（通用表单驱动）：
 * 解析源站 /profile 页的全部表单与字段，动态渲染为可编辑卡片；
 * 展示个人资料（用户名/UID/邮箱/注册时间/积分），支持上传头像、
 * 编辑简介、修改密码（点击展开）、修改邮箱/用户名、退出登录。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(session: Session, nav: NavHostController) {
    val context = LocalContext.current
    val factory = remember(session.profileSettingsRepository, session) {
        ProfileSettingsViewModelFactory(session.profileSettingsRepository, session::refreshSession)
    }
    val viewModel: ProfileSettingsViewModel = viewModel(factory = factory)
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(viewModel) { viewModel.load() }
    LaunchedEffect(state.message) {
        state.message?.let { session.showToast(it); viewModel.clearMessage() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("个人信息设置") },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.load() }, enabled = !state.loading) {
                        Icon(Icons.Filled.Refresh, "刷新")
                    }
                }
            )
        }
    ) { pad ->
        Box(Modifier.padding(pad)) {
            when {
                !session.account.loginState.loggedIn -> LoginRequiredBox(
                    "个人信息与源站账号绑定",
                    "登录后可管理头像、简介、密码与邮箱",
                ) { nav.navigate("login") }
                state.loading -> LoadingBox()
                state.error != null -> ErrorBox(state.error!!) { viewModel.load() }
                else -> {
                    val p = state.page ?: return@Box
                    LazyColumn(
                        Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // ---------- 个人资料卡 ----------
                        item {
                            ProfileHeaderCard(
                                avatarUrl = p.avatarUrl.ifBlank { session.account.loginState.avatarUrl },
                                username = session.account.loginState.username.ifBlank {
                                    p.info.firstOrNull { it.first.contains("用户") }?.second ?: ""
                                },
                                info = p.info,
                            )
                        }
                        // ---------- 屏蔽词管理 ----------
                        item {
                            GroupCard {
                                SettingMenuRow(
                                    "屏蔽词管理",
                                    "关键词与屏蔽用户，与网页端同步",
                                    Icons.Filled.Block,
                                ) { nav.navigate("blockWords") }
                            }
                        }
                        // ---------- 各表单卡片（上传头像/简介/密码…按源站实际动态渲染） ----------
                        // 过滤：纯复选框的个人资料表单、保存新邮箱设置、账号安全安全退出（按需求隐藏）
                        val forms = p.forms.filterNot { f ->
                            (f.title.contains("个人资料") &&
                                f.visibles.isNotEmpty() &&
                                f.visibles.all { it.type == "checkbox" } &&
                                f.passwords.isEmpty() && !f.hasFile) ||
                                f.title.contains("邮箱") || f.submitText.contains("新邮箱") ||
                                (f.title.contains("账号") && f.submitText.contains("退出")) ||
                                f.submitText.contains("安全退出")
                        }
                        items(forms.size) { i ->
                            val form = forms[i]
                            ProfileFormCard(
                                form = form,
                                busy = state.submitting,
                                context = context,
                                session = session,
                                avatarPicker = p.avatarPicker,
                                onSubmit = { fields, file, mime, done ->
                                    viewModel.submit(form, fields, file, mime, done)
                                },
                            )
                        }
                        item { Spacer(Modifier.height(60.dp)) }
                    }
                }
            }
        }
    }
}

// ---------------- 通用表单卡片 ----------------

