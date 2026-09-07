package sb.linux.client.data.parser

import org.jsoup.Jsoup
import sb.linux.client.data.Endpoints
import sb.linux.client.data.LoginVerification

/** 解析登录页实际提供的验证码协议，CAP 优先，旧版 native captcha 作为兼容回退。 */
object LoginVerificationParser {
    fun parse(html: String, pageUrl: String): LoginVerification? {
        val document = Jsoup.parse(html, pageUrl.ifBlank { Endpoints.BASE })
        val csrf = document.selectFirst("input[name=_csrf]")?.attr("value")?.trim()
            ?.takeIf { it.isNotBlank() } ?: return null

        document.selectFirst("cap-widget[data-cap-api-endpoint]")?.let { widget ->
            val endpoint = widget.absUrl("data-cap-api-endpoint").ifBlank {
                widget.attr("data-cap-api-endpoint").trim()
            }
            val wrapper = widget.closest("[data-cap-widget-script]")
                ?: document.selectFirst("[data-cap-widget-script]")
            val scriptUrl = wrapper?.absUrl("data-cap-widget-script").orEmpty().ifBlank {
                wrapper?.attr("data-cap-widget-script")?.trim().orEmpty()
            }.ifBlank {
                document.select("script[src]")
                    .firstOrNull { it.attr("src").contains("widget", ignoreCase = true) }
                    ?.absUrl("src").orEmpty()
            }
            val wasmUrl = wrapper?.absUrl("data-cap-wasm-url").orEmpty().ifBlank {
                wrapper?.attr("data-cap-wasm-url")?.trim().orEmpty()
            }
            if (endpoint.isNotBlank() && scriptUrl.isNotBlank()) {
                return LoginVerification.Cap(
                    csrf = csrf,
                    pageUrl = pageUrl,
                    endpoint = endpoint,
                    scriptUrl = scriptUrl,
                    wasmUrl = wasmUrl,
                    fieldName = widget.attr("data-cap-hidden-field-name").trim().ifBlank { "cap-token" },
                )
            }
        }

        val widget = document.selectFirst("[data-native-captcha]")
        val token = document.selectFirst("input[name=native_captcha_token]")?.attr("value")?.trim().orEmpty()
        val questionElement = widget?.selectFirst(".native-captcha-question")
            ?: document.selectFirst(".native-captcha-question")
        val question = questionElement?.text()?.trim().orEmpty()
        if (token.isBlank() || question.isBlank()) return null
        return LoginVerification.Native(
            csrf = csrf,
            question = question,
            questionHtml = questionElement?.outerHtml().orEmpty(),
            token = token,
            powPrefix = widget?.attr("data-pow-prefix").orEmpty(),
            powZeros = widget?.attr("data-pow-zeroes")?.toIntOrNull() ?: 3,
        )
    }
}
