package sb.linux.client.ui.screens

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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import sb.linux.client.data.Endpoints
import sb.linux.client.data.HtmlParser
import sb.linux.client.data.HtmlParser.ProfileFormData
import sb.linux.client.data.HtmlParser.ProfileField
import sb.linux.client.data.Session
import sb.linux.client.ui.*
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
    var page by remember { mutableStateOf<HtmlParser.ProfilePageData?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var msg by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun load() {
        scope.launch {
            loading = true; error = null; msg = null
            try {
                val resp = session.client.get("/profile")
                if (resp.url.contains("/login")) {
                    error = "请先登录后管理个人信息"
                } else {
                    page = HtmlParser.parseProfilePage(resp.html)
                    if (page?.forms.isNullOrEmpty() && page?.info.isNullOrEmpty())
                        error = "未解析到设置内容，源站页面结构可能已变化"
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
                title = { Text("个人信息设置") },
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
                    Text("个人信息与源站账号绑定", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(6.dp))
                    Text("登录后可管理头像、简介、密码与邮箱", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { nav.navigate("login") }) { Text("去登录") }
                }
                loading -> LoadingBox()
                error != null -> Column(
                    Modifier.fillMaxSize().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    ErrorBox(error!!) { load() }
                }
                else -> {
                    val p = page ?: return@Box
                    LazyColumn(
                        Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        msg?.let { m ->
                            item {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (m.startsWith("✓")) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.errorContainer,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        m,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (m.startsWith("✓")) MaterialTheme.colorScheme.onPrimaryContainer
                                        else MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                                    )
                                }
                            }
                        }
                        // ---------- 个人资料卡 ----------
                        item {
                            ProfileHeaderCard(
                                avatarUrl = p.avatarUrl.ifBlank { session.loginState.avatarUrl },
                                username = session.loginState.username.ifBlank {
                                    p.info.firstOrNull { it.first.contains("用户") }?.second ?: ""
                                },
                                info = p.info,
                                userId = session.loginState.userId,
                            )
                        }
                        // ---------- 屏蔽词管理 ----------
                        item {
                            OutlinedButton(
                                onClick = { nav.navigate("blockWords") },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Filled.Block, null, Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("屏蔽词管理")
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
                                busy = busy,
                                context = context,
                                session = session,
                                onSubmit = { fields, file, mime, done ->
                                    scope.launch {
                                        busy = true; msg = null
                                        try {
                                            val csrf = session.client.csrf()
                                            val all = fields + ("_csrf" to csrf) + form.hidden
                                            val resp = if (file != null && form.hasFile)
                                                session.client.postMultipart(form.action, all, form.fileFieldName, file, mime)
                                            else session.client.postForm(form.action, all)
                                            msg = if (resp.url.contains("form_error") || resp.url.contains("error"))
                                                HtmlParser.extractError(resp.html).ifBlank { "提交失败" }
                                            else "✓ ${form.submitText}成功"
                                            if (!resp.url.contains("form_error")) {
                                                session.refreshSession()
                                                load()
                                            }
                                        } catch (e: Exception) {
                                            msg = e.message ?: "提交失败"
                                        } finally { busy = false; done() }
                                    }
                                }
                            )
                        }
                        item { Spacer(Modifier.height(60.dp)) }
                    }
                }
            }
        }
    }
}

// ---------------- 个人资料卡 ----------------

@Composable
private fun ProfileHeaderCard(avatarUrl: String, username: String, info: List<Pair<String, String>>, userId: Long = 0) {
    Surface(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Avatar(avatarUrl, 64, userId = userId, seed = username)
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(username.ifBlank { "未命名" }, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            // 关键信息优先展示：UID/邮箱/注册时间/积分
            val priority = listOf("UID", "邮箱", "注册", "积分", "用户名")
            val sorted = info.sortedByDescending { (k, _) ->
                val idx = priority.indexOfFirst { k.contains(it) }
                if (idx < 0) -1 else priority.size - idx
            }
            sorted.forEach { (k, v) ->
                Row(Modifier.fillMaxWidth()) {
                    Text(k, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(88.dp))
                    Text(
                        v.removePrefix(k).removePrefix(":").removePrefix("：").trim(),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

// ---------------- 通用表单卡片 ----------------

@Composable
private fun ProfileFormCard(
    form: ProfileFormData,
    busy: Boolean,
    context: android.content.Context,
    session: Session,
    onSubmit: (Map<String, String>, File?, String, () -> Unit) -> Unit,
) {
    // 各字段当前值（初始为解析值）
    val values = remember(form) { mutableStateMapOf(*form.fields.map { it.name to it.value }.toTypedArray()) }
    // 密码组默认折叠：点击"修改密码"才显示输入框
    var pwdExpanded by remember(form) { mutableStateOf(false) }
    // 头像上传：选中的临时文件
    var pickedFile by remember(form) { mutableStateOf<File?>(null) }
    var pickedMime by remember(form) { mutableStateOf("image/jpeg") }
    var localBusy by remember(form) { mutableStateOf(false) }
    // 源站预置头像：选中的序号（点击即下载为本地文件，随表单一起上传）
    var pickedPreset by remember(form) { mutableStateOf(0) }
    val scope = rememberCoroutineScope()

    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            try {
                val mime = context.contentResolver.getType(uri) ?: "image/jpeg"
                val ext = when (mime) {
                    "image/png" -> "png"; "image/webp" -> "webp"; "image/gif" -> "gif"; else -> "jpg"
                }
                val f = File(context.cacheDir, "avatar_upload_${System.currentTimeMillis()}.$ext")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    f.outputStream().use { input.copyTo(it) }
                }
                pickedFile = f; pickedMime = mime; pickedPreset = 0
            } catch (_: Exception) {
                session.showToast("读取图片失败")
            }
        }
    }

    // 下载源站预置头像到本地缓存，作为头像文件上传
    fun pickPreset(n: Int) {
        if (localBusy) return
        localBusy = true
        scope.launch {
            try {
                val url = Endpoints.abs("/app/avatars/bottts-neutral_$n.svg")
                val bytes = session.client.fetchBytes(url)
                if (bytes.isNotEmpty()) {
                    val f = File(context.cacheDir, "avatar_preset_$n.svg")
                    f.writeBytes(bytes)
                    pickedFile = f; pickedMime = "image/svg+xml"; pickedPreset = n
                    session.showToast("已选择预置头像 $n")
                } else session.showToast("预置头像下载失败")
            } catch (e: Exception) {
                session.showToast("预置头像下载失败：${e.message}")
            } finally { localBusy = false }
        }
    }

    Surface(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (!(form.title.contains("账号") && form.submitText.contains("退出"))) {
                Text(
                    form.title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // 头像上传（file 字段）
            if (form.hasFile) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = { pickImage.launch("image/*") }, enabled = !localBusy) {
                        Icon(Icons.Filled.Upload, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (pickedFile == null) "选择头像图片" else "重新选择")
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(
                        if (pickedFile == null) "未选择图片时提交不修改头像"
                        else if (pickedPreset > 0) "已选择预置头像 ${pickedPreset}"
                        else "已选择：${pickedFile?.name}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                // 源站预置头像：横向选择列表（与源站默认头像池一致）
                Spacer(Modifier.height(4.dp))
                Text(
                    "或选择源站预置头像",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items((1..46).toList()) { n ->
                        val url = Endpoints.abs("/app/avatars/bottts-neutral_$n.svg")
                        val selected = pickedPreset == n
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable(enabled = !localBusy) { pickPreset(n) }
                        ) {
                            Box(
                                Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                    .border(
                                        width = if (selected) 2.dp else 1.dp,
                                        color = if (selected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.outlineVariant,
                                        shape = CircleShape
                                    )
                            ) {
                                AsyncImage(
                                    model = url,
                                    contentDescription = "预置头像 $n",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                )
                            }
                            Text(
                                "$n",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // 普通字段（text/email/number/select/textarea/checkbox…）
            form.visibles.forEach { f ->
                FieldInput(f, values[f.name] ?: f.value) { values[f.name] = it }
            }

            // 密码字段：点击"修改密码"展开
            if (form.passwords.isNotEmpty()) {
                if (!pwdExpanded) {
                    TextButton(onClick = { pwdExpanded = true }) { Text("修改密码") }
                } else {
                    form.passwords.forEach { f ->
                        OutlinedTextField(
                            value = values[f.name] ?: "",
                            onValueChange = { values[f.name] = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(f.label) },
                            shape = RoundedCornerShape(14.dp),
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation()
                        )
                    }
                    TextButton(onClick = {
                        form.passwords.forEach { values[it.name] = "" }
                        pwdExpanded = false
                    }) { Text("收起") }
                }
            }

            Button(
                onClick = {
                    localBusy = true
                    val payload = form.fields.filter { it.type != "hidden" }
                        .associate { it.name to (values[it.name] ?: it.value) }
                    onSubmit(payload, pickedFile, pickedMime) { localBusy = false; pickedFile = null; pickedPreset = 0 }
                },
                enabled = !busy && !localBusy,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (localBusy) "提交中…" else form.submitText) }
        }
    }
}

@Composable
private fun FieldInput(f: ProfileField, value: String, onChange: (String) -> Unit) {
    when (f.type) {
        "textarea" -> OutlinedTextField(
            value = value, onValueChange = onChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(f.label) },
            shape = RoundedCornerShape(14.dp),
            minLines = 2
        )
        "select" -> {
            var expanded by remember { mutableStateOf(false) }
            val current = f.options.firstOrNull { it.first == value }?.second ?: value
            OutlinedTextField(
                value = current,
                onValueChange = {},
                readOnly = true,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(f.label) },
                shape = RoundedCornerShape(14.dp),
                trailingIcon = {
                    TextButton(onClick = { expanded = true }) { Text("选择") }
                }
            )
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                f.options.forEach { (v, label) ->
                    DropdownMenuItem(
                        text = { Text(label) },
                        onClick = { onChange(v); expanded = false }
                    )
                }
            }
        }
        "checkbox" -> Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Switch(checked = value == "1", onCheckedChange = { onChange(if (it) "1" else "0") })
            Spacer(Modifier.width(10.dp))
            Text(f.label)
        }
        else -> OutlinedTextField(
            value = value, onValueChange = onChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(f.label) },
            shape = RoundedCornerShape(14.dp),
            singleLine = true
        )
    }
}
