package sb.linux.client.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import sb.linux.client.data.LoginVerification
import sb.linux.client.data.Session
import sb.linux.client.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(session: Session, nav: NavHostController) {
    // 记住密码：进入时回填已保存的账号
    var username by remember { mutableStateOf(session.settings.savedUsername) }
    var password by remember { mutableStateOf(session.settings.savedPassword) }
    var rememberPwd by remember { mutableStateOf(session.settings.rememberPassword) }
    var showPassword by remember { mutableStateOf(false) }
    var captchaAnswer by remember { mutableStateOf("") }

    var captcha by remember { mutableStateOf<LoginVerification?>(null) }
    var captchaRevision by remember { mutableIntStateOf(0) }
    var captchaError by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    suspend fun reloadCaptcha() {
        captchaError = null
        captchaAnswer = ""
        try {
            captcha = session.client.fetchLoginCaptcha()
            captchaRevision++
        } catch (e: Exception) {
            captcha = null
            captchaError = e.message ?: "验证码加载失败"
        }
    }

    // 进入页面读取源站当前使用的验证协议（CAP 或旧版数学题）。
    LaunchedEffect(Unit) { reloadCaptcha() }

    Scaffold(topBar = { TopAppBar(title = { Text("登录烧饼社区") }) }) { pad ->
        Column(
            Modifier
                .padding(pad)
                .fillMaxSize()
                // 键盘弹出时收缩可视区域，保证底部的人机验证输入框不被遮挡
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(36.dp))
            // 品牌头部：源站真实图标 + 标题 + 副标题
            Box(
                Modifier
                    .size(76.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_launcher),
                    contentDescription = "烧饼社区图标",
                    modifier = Modifier.size(64.dp),
                    contentScale = ContentScale.Fit,
                )
            }
            Spacer(Modifier.height(16.dp))
            Text("烧饼社区", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(
                "人人都有饼吃的AI社区",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(34.dp))

            OutlinedTextField(
                value = username, onValueChange = { username = it },
                label = { Text("用户名或邮箱") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                leadingIcon = { Icon(Icons.Filled.Person, null, Modifier.size(18.dp)) }
            )
            Spacer(Modifier.height(14.dp))
            OutlinedTextField(
                value = password, onValueChange = { password = it },
                label = { Text("密码") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                leadingIcon = { Icon(Icons.Filled.Lock, null, Modifier.size(18.dp)) },
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    IconButton(onClick = { showPassword = !showPassword }) {
                        Icon(
                            if (showPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            if (showPassword) "隐藏密码" else "显示密码",
                            Modifier.size(18.dp)
                        )
                    }
                }
            )
            Spacer(Modifier.height(4.dp))
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(checked = rememberPwd, onCheckedChange = { rememberPwd = it })
                Text("记住密码", style = MaterialTheme.typography.bodyMedium)
            }

            // 人机验证：新源站使用 CAP Web Component，旧数学题保留兼容。
            val cap = captcha
            when (cap) {
                is LoginVerification.Native -> {
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = captchaAnswer,
                        onValueChange = { if (it.length <= 8) captchaAnswer = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("人机验证：${cap.question}") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        trailingIcon = {
                            IconButton(onClick = { scope.launch { reloadCaptcha() } }) {
                                Icon(Icons.Filled.Refresh, "刷新验证码")
                            }
                        },
                    )
                }
                is LoginVerification.Cap -> {
                    Spacer(Modifier.height(6.dp))
                    CapLoginWidget(
                        verification = cap,
                        revision = captchaRevision,
                        onToken = { captchaAnswer = it; captchaError = null },
                        onStatus = { status = it },
                        onError = { captchaAnswer = ""; captchaError = it },
                    )
                    captchaError?.let { message ->
                        Text(message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        TextButton(onClick = { scope.launch { reloadCaptcha() } }) { Text("重新加载验证码") }
                    }
                }
                null -> {
                    Spacer(Modifier.height(6.dp))
                    captchaError?.let { message ->
                        Text(message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        TextButton(onClick = { scope.launch { reloadCaptcha() } }) { Text("重新加载验证码") }
                    } ?: CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                }
            }

            Spacer(Modifier.height(16.dp))
            error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))
            }
            if (busy) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(10.dp))
                    Text(status, style = MaterialTheme.typography.bodySmall)
                }
                Spacer(Modifier.height(12.dp))
            }

            Button(
                onClick = {
                    val c = captcha
                    if (username.isBlank() || password.isBlank()) {
                        error = "请输入用户名和密码"; return@Button
                    }
                    if (c == null) {
                        error = "验证码未加载，请稍候"; return@Button
                    }
                    if (captchaAnswer.isBlank()) {
                        error = if (c is LoginVerification.Cap) "请先完成人机验证" else "请填写人机验证答案"
                        return@Button
                    }
                    busy = true; error = null
                    scope.launch {
                        try {
                            session.client.login(username, password, c, captchaAnswer) { status = it }
                            // 记住密码：保存 / 清除已存凭据
                            if (rememberPwd) {
                                session.settings.rememberPassword = true
                                session.settings.savedUsername = username
                                session.settings.savedPassword = password
                            } else {
                                session.settings.rememberPassword = false
                                session.settings.savedUsername = ""
                                session.settings.savedPassword = ""
                            }
                            session.refreshSession()
                            nav.popBackStack()
                        } catch (e: Exception) {
                            error = e.message ?: "登录失败"
                            // 验证令牌一次性，失败后重新加载组件或题目。
                            reloadCaptcha()
                        } finally {
                            busy = false
                        }
                    }
                },
                enabled = !busy,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp)
            ) { Text(if (busy) "登录中…" else "登录", style = MaterialTheme.typography.titleMedium) }
        }
    }
}
