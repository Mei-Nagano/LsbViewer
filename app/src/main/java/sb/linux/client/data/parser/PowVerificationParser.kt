package sb.linux.client.data.parser

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import sb.linux.client.common.network.Endpoints
import sb.linux.client.domain.account.LoginVerification

/** 保留源页面的 DOM 和脚本顺序，只标记要展示的原组件。 */
internal object PowVerificationParser {
    private const val selector = "[data-human-verification][data-human-provider=pow]"

    fun parse(widget: Element, resources: Element, csrf: String, pageUrl: String): LoginVerification.Pow? {
        val base = pageUrl.toHttpUrlOrNull() ?: return null
        if (base.scheme != "https" || !Endpoints.isForumHost(base.host)) return null
        val source = resources.ownerDocument() ?: resources
        val page = if (source is Document) source.clone() else Jsoup.parse(source.outerHtml(), pageUrl)
        val widgetHtml = widget.outerHtml()
        val index = source.select(selector).indexOfFirst {
            (it === widget || it.outerHtml() == widgetHtml) && it.parents().none { parent -> parent.normalName() == "template" }
        }
        val component = if (index >= 0) page.select(selector)[index] else {
            // 称号赠送等表单由调用方从源文档克隆，原页面仍提供初始化资源。
            val form = widget.closest("form")?.shallowClone() ?: Element("form")
            page.body().appendChild(form)
            form.appendChild(widget.clone())
            form.selectFirst(selector)!!
        }
        val form = component.closest("form") ?: Element("form").also {
            component.replaceWith(it)
            it.appendChild(component)
        }
        form.attr("data-lsb-pow-form", "true")
        if (form.selectFirst("input[name=_csrf]") == null) {
            form.appendElement("input").attr("type", "hidden").attr("name", "_csrf").attr("value", csrf)
        }
        component.attr("data-lsb-pow-widget", "true")
        var parent = component.parent()
        while (parent != null && parent.normalName() != "html") {
            parent.attr("data-lsb-pow-container", "true")
            parent = parent.parent()
        }
        return LoginVerification.Pow(csrf, pageUrl, page.outerHtml())
    }
}
