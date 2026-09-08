package sb.linux.client.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TopicReplyCaptchaParserTest {
    @Test
    fun parsesCapVerificationFromLotteryReplyForm() {
        val page = HtmlParser.parseTopicPage(
            """
            <html><head><title>抽奖帖 - Linux.do</title></head><body>
              <input type="hidden" name="_csrf" value="csrf-value">
              <h1 class="post-content-title">抽奖帖</h1>
              <form action="/reply_edit">
                <div data-cap-verification
                  data-cap-widget-script="https://cap.linux.sb/assets/widget.js"
                  data-cap-wasm-url="https://cap.linux.sb/assets/cap_wasm_bg.wasm">
                  <cap-widget data-cap-api-endpoint="https://cap.linux.sb/60c41af707/"
                    data-cap-hidden-field-name="cap_token"></cap-widget>
                </div>
              </form>
            </body></html>
            """.trimIndent(),
            20439,
        )

        assertTrue(page.replyCaptcha is LoginVerification.Cap)
        val captcha = page.replyCaptcha as LoginVerification.Cap
        assertEquals("cap_token", captcha.fieldName)
        assertEquals("https://cap.linux.sb/60c41af707/", captcha.endpoint)
    }

    @Test
    fun keepsLegacyLotteryReplyCaptchaCompatibility() {
        val page = HtmlParser.parseTopicPage(
            """
            <html><body>
              <input type="hidden" name="_csrf" value="csrf-value">
              <h1 class="post-content-title">旧抽奖帖</h1>
              <div data-native-captcha data-pow-prefix="prefix" data-pow-zeroes="3">
                <span class="native-captcha-question">8 + 4 = ?</span>
                <input name="native_captcha_token" value="native-token">
              </div>
            </body></html>
            """.trimIndent(),
            1,
        )

        assertTrue(page.replyCaptcha is LoginVerification.Native)
        val captcha = page.replyCaptcha as LoginVerification.Native
        assertEquals("8 + 4 = ?", captcha.question)
        assertEquals("native-token", captcha.token)
    }
}
