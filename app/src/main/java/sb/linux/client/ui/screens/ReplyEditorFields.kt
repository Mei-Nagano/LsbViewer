package sb.linux.client.ui.screens
import sb.linux.client.ui.session.Session


import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.*
import androidx.compose.foundation.text.selection.*
import androidx.compose.material.icons.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.*
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.*
import sb.linux.client.ui.*
import sb.linux.client.domain.account.LoginVerification
/** Markdown 工具栏（横向滚动；私信等场景隐藏），onInsert(before, after, placeholder) */
@Composable
internal fun ReplyToolbar(
    onInsert: (String, String, String) -> Unit,
    onUploadImage: (() -> Unit)? = null,
    imageUploadBusy: Boolean = false,
) {
    sb.linux.client.ui.MarkdownEditorTools(
        onInsert = onInsert, onUpload = { onUploadImage?.invoke() ?: onInsert("![", "](https://)", "图片描述") },
        uploading = imageUploadBusy,
    )
}

/** 抽奖回复验证：新版显示 CAP 组件，旧版保留数学题输入与换题按钮。 */
@Composable
internal fun ReplyCaptchaField(
    session: Session,
    captcha: LoginVerification?,
    answer: String,
    onAnswerChange: (String) -> Unit,
    onRefresh: (() -> Unit)?,
    topPadding: androidx.compose.ui.unit.Dp,
    revision: Int,
    onReloadCap: () -> Unit,
) {
    if (captcha == null) return
    Spacer(Modifier.height(topPadding))
    when (captcha) {
        is sb.linux.client.domain.account.LoginVerification.Native -> OutlinedTextField(
            value = answer,
            onValueChange = onAnswerChange,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            label = { Text("人机验证：${captcha.question}") },
            singleLine = true,
            trailingIcon = if (onRefresh != null) {
                {
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Filled.Refresh, "换一题")
                    }
                }
            } else null,
        )
        is sb.linux.client.domain.account.LoginVerification.Cap -> {
            var capError by remember(captcha, revision) { mutableStateOf<String?>(null) }
            CapVerificationWidget(
                verification = captcha,
                revision = revision,
                gateway = session.capVerificationGateway,
                onToken = onAnswerChange,
                onStatus = {},
                onError = { capError = it; onAnswerChange("") },
            )
            capError?.let { message ->
                Text(message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = onReloadCap) { Text("重新加载验证码") }
            }
        }
    }
}
