package sb.linux.client.data

import android.net.Uri
import kotlinx.coroutines.CancellationException
import sb.linux.client.domain.topic.TopicReplyRepository
import sb.linux.client.domain.topic.TopicReplySubmission
import sb.linux.client.domain.account.LoginVerification
import sb.linux.client.domain.topic.TopicPageData

internal class TopicReplyService(private val client: LsbClient) : TopicReplyRepository {
    override suspend fun refreshCaptcha(topicId: Long, page: Int): LoginVerification? {
        val response = client.get("/topic/$topicId?p=$page")
        return ReplyCaptchaHtmlParser.parseReplyCaptcha(response.html, response.url)
    }

    override suspend fun submit(
        topicId: Long,
        page: TopicPageData?,
        body: String,
        captchaAnswer: String,
        captcha: LoginVerification?,
    ): TopicReplySubmission {
        val csrf = page?.csrf?.takeIf { it.isNotBlank() } ?: client.csrf()
        val form = buildMap {
            put("_csrf", csrf)
            put("topic_id", topicId.toString())
            put("body", body)
            when (captcha) {
                is sb.linux.client.domain.account.LoginVerification.Native -> {
                    put("native_captcha_token", captcha.token)
                    put("native_captcha_answer", captchaAnswer)
                    put("native_captcha_pow", LsbClient.solvePow(captcha.powPrefix, captcha.powZeros))
                }
                is sb.linux.client.domain.account.LoginVerification.Cap -> put(captcha.fieldName, captchaAnswer)
                null -> Unit
            }
        }
        val response = client.postForm("/reply_edit", form)
        if (response.url.contains("form_error")) {
            val replacement = if (captcha == null) null else try {
                refreshCaptcha(topicId, page?.page ?: 1)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                null
            }
            return TopicReplySubmission(
                succeeded = false,
                message = HtmlTextParser.extractError(response.html).ifBlank { "回复失败" },
                submittedPage = page?.totalPages ?: 1,
                replacementCaptcha = replacement,
            )
        }
        val submittedPage = runCatching {
            Uri.parse(response.url).getQueryParameter("p")?.toIntOrNull()
        }.getOrNull() ?: (page?.totalPages ?: 1)
        return TopicReplySubmission(true, "回复成功", submittedPage)
    }
}
