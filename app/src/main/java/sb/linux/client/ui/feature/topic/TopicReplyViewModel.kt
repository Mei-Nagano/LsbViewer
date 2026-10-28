package sb.linux.client.ui.feature.topic

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import sb.linux.client.domain.account.LoginVerification
import sb.linux.client.domain.topic.TopicPageData
import sb.linux.client.domain.topic.TopicReplyRepository
import sb.linux.client.domain.topic.TopicReplySubmission

class TopicReplyViewModel(
    private val repository: TopicReplyRepository,
) : ViewModel() {
    fun refreshCaptcha(
        topicId: Long,
        page: Int,
        onComplete: (Result<LoginVerification?>) -> Unit,
    ) = runRequest({ repository.refreshCaptcha(topicId, page) }, onComplete)

    fun submit(
        topicId: Long,
        page: TopicPageData?,
        body: String,
        captchaAnswer: String,
        captcha: LoginVerification?,
        onComplete: (Result<TopicReplySubmission>) -> Unit,
    ) = runRequest({
        repository.submit(topicId, page, body, captchaAnswer, captcha)
    }, onComplete)

    private fun <T> runRequest(request: suspend () -> T, onComplete: (Result<T>) -> Unit) {
        viewModelScope.launch {
            try {
                onComplete(Result.success(request()))
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                onComplete(Result.failure(error))
            }
        }
    }
}

class TopicReplyViewModelFactory(
    private val repository: TopicReplyRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        TopicReplyViewModel(repository) as T
}
