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
import sb.linux.client.data.Endpoints
import sb.linux.client.data.LsbClient
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

    var captcha by remember { mutableStateOf<LsbClient.LoginCaptcha?>(null) }
    var captchaError by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    // 进入页面拉取登录页人机验证题目
    LaunchedEffect(Unit) {
        try {
            captcha = session.client.fetchLoginCaptcha()
        } catch (e: Exception) {
            captchaError = e.message ?: "验证码加载失败"
        }
    }

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

            // 人机验证：题目展示，答案由用户填写（PoW 由客户端自动计算）
            val cap = captcha
            if (cap != null) {
                Spacer(Modifier.height(6.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "人机验证",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = {
                        captchaAnswer = ""
                        scope.launch {
                            try { captcha = session.client.fetchLoginCaptcha() }
                            catch (e: Exception) { captchaError = e.message ?: "验证码加载失败" }
                        }
                    }) { Text("刷新验证码") }
                }
                if (cap.question.isNotBlank()) {
                    // 验证码题目：紧凑内联展示（不占额外空间）
                    Spacer(Modifier.height(4.dp))
                    Text(
                        cap.question,
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(4.dp))
                }
                OutlinedTextField(
                    value = captchaAnswer,
                    onValueChange = { if (it.length <= 8) captchaAnswer = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("验证码答案") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            } else if (captchaError != null) {
                Spacer(Modifier.height(6.dp))
                Text(captchaError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = {
                    captchaError = null
                    scope.launch {
                        try { captcha = session.client.fetchLoginCaptcha() }
                        catch (e: Exception) { captchaError = e.message ?: "验证码加载失败" }
                    }
                }) { Text("重新加载验证码") }
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
                        error = "请填写人机验证答案"; return@Button
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
                            // 验证码一次性：刷新题目让用户重新作答
                            captchaAnswer = ""
                            runCatching { captcha = session.client.fetchLoginCaptcha() }
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
