package sb.linux.client.data

import org.jsoup.nodes.Element

internal object PostSignatureParser {
    const val SELECTOR = ".post-signature-content, .post-signature, .user-signature, [data-post-signature]"

    fun parse(post: Element): String {
        val signature = post.selectFirst(SELECTOR)?.clone() ?: return ""
        signature.select("button, script, style, .signature-toggle").remove()
        return signature.html().takeIf { signature.text().isNotBlank() }.orEmpty()
    }
}
