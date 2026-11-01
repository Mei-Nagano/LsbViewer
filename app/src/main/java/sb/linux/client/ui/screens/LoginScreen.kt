package sb.linux.client.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import sb.linux.client.ui.components.feedback.AppInlineLoading
import sb.linux.client.ui.components.navigation.AppPageScaffold
import sb.linux.client.ui.session.Session
import sb.linux.client.domain.account.LoginVerification
import sb.linux.client.ui.feature.account.LoginViewModel
import sb.linux.client.ui.feature.account.LoginViewModelFactory
import sb.linux.client.ui.theme.LocalAppSizes
import sb.linux.client.ui.theme.LocalAppSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(session: Session, nav: NavHostController) {
    val factory = remember(session.accountAuthenticationRepository, nav) {
        LoginViewModelFactory(session.accountAuthenticationRepository) { nav.popBackStack() }
    }
    val viewModel: LoginViewModel = viewModel(factory = factory)
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(viewModel) { viewModel.loadChallenge() }

    AppPageScaffold(
        title = "登录烧饼社区",
        onBack = { nav.popBackStack() },
        // 登录是可取消的门禁：返回图标保留「取消登录」语义，避免把用户困在登录页
        navigationIcon = {
            IconButton(onClick = { nav.popBackStack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "取消登录")
            }
        },
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier
                .widthIn(max = LocalAppSizes.current.formMaxWidth)
                .fillMaxHeight()
                // 键盘弹出时收缩可视区域，保证底部的人机验证输入框不被遮挡
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = LocalAppSpacing.current.pageHorizontal),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(36.dp))
            LoginBrand()

            OutlinedTextField(
                value = state.username, onValueChange = viewModel::updateUsername,
                label = { Text("用户名或邮箱") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = MaterialTheme.shapes.large,
                leadingIcon = { Icon(Icons.Filled.Person, null, Modifier.size(18.dp)) }
            )
            Spacer(Modifier.height(14.dp))
            OutlinedTextField(
                value = state.password, onValueChange = viewModel::updatePassword,
                label = { Text("密码") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = MaterialTheme.shapes.large,
                leadingIcon = { Icon(Icons.Filled.Lock, null, Modifier.size(18.dp)) },
                visualTransformation = if (state.showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    IconButton(onClick = viewModel::togglePasswordVisibility) {
                        Icon(
                            if (state.showPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            if (state.showPassword) "隐藏密码" else "显示密码",
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
                Checkbox(checked = state.rememberPassword, onCheckedChange = viewModel::updateRememberPassword)
                Text("记住密码", style = MaterialTheme.typography.bodyMedium)
            }

            // 人机验证：新源站使用 CAP Web Component，旧数学题保留兼容。
            val cap = state.captcha
            when (cap) {
                is sb.linux.client.domain.account.LoginVerification.Native -> {
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = state.captchaAnswer,
                        onValueChange = { if (it.length <= 8) viewModel.updateCaptchaAnswer(it) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("人机验证：${cap.question}") },
                        singleLine = true,
                        shape = MaterialTheme.shapes.large,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        trailingIcon = {
                            IconButton(onClick = viewModel::loadChallenge) {
                                Icon(Icons.Filled.Refresh, "刷新验证码")
                            }
                        },
                    )
                }
                is sb.linux.client.domain.account.LoginVerification.Cap -> {
                    Spacer(Modifier.height(6.dp))
                    CapVerificationWidget(
                        verification = cap,
                        revision = state.captchaRevision,
                        gateway = session.capVerificationGateway,
                        onToken = viewModel::updateCaptchaAnswer,
                        onStatus = viewModel::updateStatus,
                        onError = viewModel::showCaptchaError,
                    )
                    state.captchaError?.let { message ->
                        Text(message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        TextButton(onClick = viewModel::loadChallenge) { Text("重新加载验证码") }
                    }
                }
                null -> {
                    Spacer(Modifier.height(6.dp))
                    state.captchaError?.let { message ->
                        Text(message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        TextButton(onClick = viewModel::loadChallenge) { Text("重新加载验证码") }
                    } ?: AppInlineLoading(size = 24.dp)
                }
            }

            Spacer(Modifier.height(16.dp))
            state.error?.let {
                sb.linux.client.ui.components.feedback.AppNotice(it)
                Spacer(Modifier.height(8.dp))
            }
            if (state.busy) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AppInlineLoading(size = 18.dp)
                    Spacer(Modifier.width(10.dp))
                    Text(state.status, style = MaterialTheme.typography.bodySmall)
                }
                Spacer(Modifier.height(12.dp))
            }

            Button(
                onClick = viewModel::submit,
                enabled = !state.busy,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = MaterialTheme.shapes.large
            ) { Text(if (state.busy) "登录中…" else "登录", style = MaterialTheme.typography.titleMedium) }
        }
        }
    }
}
