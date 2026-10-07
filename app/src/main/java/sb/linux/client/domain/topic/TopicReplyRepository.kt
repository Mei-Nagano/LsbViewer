package sb.linux.client.domain.topic

import sb.linux.client.domain.account.LoginVerification

data class TopicReplySubmission(
    val succeeded: Boolean,
    val message: String,
    val submittedPage: Int,
    val replacementCaptcha: LoginVerification? = null,
)

interface TopicReplyRepository {
    suspend fun refreshCaptcha(topicId: Long, page: Int): LoginVerification?
    suspend fun submit(
        topicId: Long,
        page: TopicPageData?,
        body: String,
        captchaAnswer: String,
        captcha: LoginVerification?,
    ): TopicReplySubmission
}
