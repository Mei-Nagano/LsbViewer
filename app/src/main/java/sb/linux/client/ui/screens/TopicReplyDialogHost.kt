package sb.linux.client.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import sb.linux.client.domain.account.LoginVerification
import sb.linux.client.domain.topic.TopicPageData
import sb.linux.client.ui.feature.topic.TopicReplyViewModel
import sb.linux.client.ui.session.Session

@Composable
internal fun TopicReplyDialogHost(
    isOpen: Boolean,
    session: Session,
    topicId: Long,
    page: TopicPageData?,
    initial: String,
    captchaOverride: MutableState<LoginVerification?>,
    viewModel: TopicReplyViewModel,
    onDismiss: () -> Unit,
    onLoad: (Int) -> Unit,
) {
    if (isOpen) {
        LaunchedEffect(Unit) { captchaOverride.value = null }
        val activeCaptcha = captchaOverride.value ?: page?.replyCaptcha
        ReplyDialog(
            session = session,
            initial = initial,
            captcha = activeCaptcha,
            onRefreshCaptcha = {
                viewModel.refreshCaptcha(topicId, page?.page ?: 1) { result ->
                    result.onSuccess { captcha ->
                        if (captcha != null) captchaOverride.value = captcha
                        else session.showToast("未找到人机验证")
                    }.onFailure { session.showToast(it.message ?: "刷新失败") }
                }
            },
            onDismiss = onDismiss,
            onSubmit = { body, captchaAnswer, onDone ->
                viewModel.submit(topicId, page, body, captchaAnswer, activeCaptcha) { result ->
                    var succeeded = false
                    result.onSuccess { submission ->
                        if (!submission.succeeded) {
                            session.showToast(submission.message)
                            submission.replacementCaptcha?.let { captchaOverride.value = it }
                        } else {
                            succeeded = true
                            session.showToast(submission.message)
                            onDismiss()
                            // 合并刷新提交所在页，保留树形回复依赖的前序楼层。
                            onLoad(submission.submittedPage)
                        }
                    }.onFailure { session.showToast(it.message ?: "回复失败") }
                    onDone(succeeded)
                }
            },
        )
    }
}
