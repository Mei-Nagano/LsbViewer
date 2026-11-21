package sb.linux.client.data.parser

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import sb.linux.client.common.network.Endpoints
import sb.linux.client.domain.account.LoginVerification

/** 解析源站实际提供的 PoW、CAP 或旧版 native captcha 协议。 */
object LoginVerificationParser {
    fun parse(html: String, pageUrl: String): LoginVerification? =
        parse(Jsoup.parse(html, pageUrl.ifBlank { Endpoints.BASE }), pageUrl)

    fun parse(document: Element, pageUrl: String, resources: Element = document.ownerDocument() ?: document): LoginVerification? {
        // _csrf 只用于后续提交；页面结构变化时不应因为它缺失就整个放弃验证码组件，
        // 提交时还能回退到会话里现取的令牌（见 LsbAuthenticationClient.fetchLoginCaptcha）。
        val csrf = document.selectFirst("input[name=_csrf]")?.attr("value")?.trim().orEmpty()

        document.selectFirst("[data-human-verification][data-human-provider=pow]")?.let { widget ->
            return PowVerificationParser.parse(widget, resources, csrf, pageUrl)
        }

        document.selectFirst("cap-widget[data-cap-api-endpoint]")?.let { widget ->
            val endpoint = widget.absUrl("data-cap-api-endpoint").ifBlank {
                widget.attr("data-cap-api-endpoint").trim()
            }
            val wrapper = widget.closest("[data-cap-widget-script]")
                ?: document.selectFirst("[data-cap-widget-script]")
            val declaredScript = wrapper?.absUrl("data-cap-widget-script").orEmpty().ifBlank {
                wrapper?.attr("data-cap-widget-script")?.trim().orEmpty()
            }
            val scriptUrl = declaredScript.ifBlank {
                document.select("script[src]")
                    .firstOrNull { it.attr("src").contains("widget", ignoreCase = true) }
                    ?.absUrl("src").orEmpty()
            }.ifBlank {
                document.select("script[src]")
                    .firstOrNull { it.attr("src").contains("cap", ignoreCase = true) }
                    ?.absUrl("src").orEmpty()
            }
            val wasmUrl = wrapper?.absUrl("data-cap-wasm-url").orEmpty().ifBlank {
                wrapper?.attr("data-cap-wasm-url")?.trim().orEmpty()
            }
            if (endpoint.isNotBlank()) {
                // 页面没声明脚本地址时按源站约定回退到同一服务下的 widget 资源，
                // 否则重建的组件页没有任何可加载的脚本，组件只能显示为空白。
                val resolvedScript = scriptUrl.ifBlank {
                    endpoint.toHttpUrlOrNull()?.let { url ->
                        url.newBuilder().encodedPath("/assets/widget.js").query(null).build().toString()
                    }.orEmpty()
                }
                val scriptElement = document.select("script[src]").firstOrNull { element ->
                    val src = element.absUrl("src").ifBlank { element.attr("src").trim() }
                    src.isNotBlank() && resolvedScript.isNotBlank() &&
                        src.substringBefore('?') == resolvedScript.substringBefore('?')
                }
                return sb.linux.client.domain.account.LoginVerification.Cap(
                    csrf = csrf,
                    pageUrl = pageUrl,
                    endpoint = endpoint,
                    scriptUrl = resolvedScript,
                    wasmUrl = wasmUrl,
                    fieldName = widget.attr("data-cap-hidden-field-name").trim().ifBlank { "cap-token" },
                    scriptModule = scriptElement?.attr("type")
                        ?.contains("module", ignoreCase = true) == true,
                )
            }
        }

        val widget = document.selectFirst("[data-native-captcha]")
        val token = document.selectFirst("input[name=native_captcha_token]")?.attr("value")?.trim().orEmpty()
        val questionElement = widget?.selectFirst(".native-captcha-question")
            ?: document.selectFirst(".native-captcha-question")
        val question = questionElement?.text()?.trim().orEmpty()
        if (token.isBlank() || question.isBlank()) return null
        return sb.linux.client.domain.account.LoginVerification.Native(
            csrf = csrf,
            question = question,
            questionHtml = questionElement?.outerHtml().orEmpty(),
            token = token,
            powPrefix = widget?.attr("data-pow-prefix").orEmpty(),
            powZeros = widget?.attr("data-pow-zeroes")?.toIntOrNull() ?: 3,
        )
    }
}
