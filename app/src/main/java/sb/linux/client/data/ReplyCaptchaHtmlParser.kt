package sb.linux.client.data

import sb.linux.client.data.parser.LoginVerificationParser
import sb.linux.client.domain.account.LoginVerification
object ReplyCaptchaHtmlParser {
    /** 单独解析页面里的回复验证组件（回复弹窗刷新用，无需解析整个帖子）。 */
    fun parseReplyCaptcha(html: String, pageUrl: String): LoginVerification? =
        LoginVerificationParser.parse(html, pageUrl)

}
