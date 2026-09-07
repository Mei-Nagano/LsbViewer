package sb.linux.client.data.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import sb.linux.client.data.LoginVerification

class LoginVerificationParserTest {
    @Test
    fun parsesCurrentCapWidget() {
        val result = LoginVerificationParser.parse(
            """
            <form method="post">
              <input type="hidden" name="_csrf" value="csrf-value">
              <div data-cap-verification
                data-cap-widget-script="https://cap.linux.sb/assets/widget.js"
                data-cap-wasm-url="https://cap.linux.sb/assets/cap_wasm_bg.wasm">
                <cap-widget required
                  data-cap-api-endpoint="https://cap.linux.sb/60c41af707/"
                  data-cap-hidden-field-name="cap_token"></cap-widget>
              </div>
            </form>
            """.trimIndent(),
            "https://linux.sb/login",
        )

        assertTrue(result is LoginVerification.Cap)
        result as LoginVerification.Cap
        assertEquals("csrf-value", result.csrf)
        assertEquals("https://cap.linux.sb/60c41af707/", result.endpoint)
        assertEquals("https://cap.linux.sb/assets/widget.js", result.scriptUrl)
        assertEquals("https://cap.linux.sb/assets/cap_wasm_bg.wasm", result.wasmUrl)
        assertEquals("cap_token", result.fieldName)
    }

    @Test
    fun keepsNativeCaptchaCompatibility() {
        val result = LoginVerificationParser.parse(
            """
            <form method="post">
              <input type="hidden" name="_csrf" value="csrf-value">
              <div data-native-captcha data-pow-prefix="pow" data-pow-zeroes="4">
                <span class="native-captcha-question">7 + 5 = ?</span>
                <input type="hidden" name="native_captcha_token" value="token-value">
              </div>
            </form>
            """.trimIndent(),
            "https://linux.sb/login",
        )

        assertTrue(result is LoginVerification.Native)
        result as LoginVerification.Native
        assertEquals("7 + 5 = ?", result.question)
        assertEquals("token-value", result.token)
        assertEquals(4, result.powZeros)
    }
}
