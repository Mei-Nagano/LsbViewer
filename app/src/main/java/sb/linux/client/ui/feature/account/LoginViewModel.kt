package sb.linux.client.ui.feature.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import sb.linux.client.domain.account.AccountAuthenticationRepository
import sb.linux.client.domain.account.LoginVerification

data class LoginUiState(
    val username: String = "",
    val password: String = "",
    val rememberPassword: Boolean = true,
    val showPassword: Boolean = false,
    val captchaAnswer: String = "",
    val captcha: LoginVerification? = null,
    val captchaRevision: Int = 0,
    val captchaError: String? = null,
    val busy: Boolean = false,
    val status: String = "",
    val error: String? = null,
)

class LoginViewModel(
    private val repository: AccountAuthenticationRepository,
    private val onSuccess: () -> Unit,
) : ViewModel() {
    private val saved = repository.savedCredentials()
    private val mutableState = MutableStateFlow(
        LoginUiState(saved.username, saved.password, saved.rememberPassword),
    )
    val uiState = mutableState.asStateFlow()

    fun loadChallenge() {
        viewModelScope.launch {
            mutableState.update {
                it.copy(captcha = null, captchaAnswer = "", captchaError = null, captchaRevision = it.captchaRevision + 1)
            }
            try {
                val captcha = repository.loadLoginChallenge()
                mutableState.update { it.copy(captcha = captcha, captchaRevision = it.captchaRevision + 1) }
            } catch (error: Exception) {
                mutableState.update { it.copy(captchaError = error.message ?: "验证码加载失败") }
            }
        }
    }

    fun updateUsername(value: String) = mutableState.update { it.copy(username = value) }
    fun updatePassword(value: String) = mutableState.update { it.copy(password = value) }
    fun updateRememberPassword(value: Boolean) = mutableState.update { it.copy(rememberPassword = value) }
    fun togglePasswordVisibility() = mutableState.update { it.copy(showPassword = !it.showPassword) }
    fun updateCaptchaAnswer(value: String) = mutableState.update { it.copy(captchaAnswer = value, captchaError = null) }
    fun updateStatus(value: String) = mutableState.update { it.copy(status = value) }
    fun showCaptchaError(value: String) = mutableState.update { it.copy(captchaAnswer = "", captchaError = value) }

    fun submit() {
        val current = mutableState.value
        val challenge = current.captcha
        val validation = when {
            current.username.isBlank() || current.password.isBlank() -> "请输入用户名和密码"
            challenge == null -> "验证码未加载，请稍候"
            current.captchaAnswer.isBlank() -> if (challenge is sb.linux.client.domain.account.LoginVerification.Cap) "请先完成人机验证" else "请填写人机验证答案"
            else -> null
        }
        if (validation != null) {
            mutableState.update { it.copy(error = validation) }
            return
        }
        viewModelScope.launch {
            mutableState.update { it.copy(busy = true, error = null) }
            try {
                repository.login(
                    current.username, current.password, challenge!!, current.captchaAnswer,
                    current.rememberPassword, ::updateStatus,
                )
                onSuccess()
            } catch (error: Exception) {
                mutableState.update { it.copy(error = error.message ?: "登录失败") }
                loadChallenge()
            } finally {
                mutableState.update { it.copy(busy = false) }
            }
        }
    }
}

class LoginViewModelFactory(
    private val repository: AccountAuthenticationRepository,
    private val onSuccess: () -> Unit,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        LoginViewModel(repository, onSuccess) as T
}
