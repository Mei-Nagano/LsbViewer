package sb.linux.client.data

import org.jsoup.nodes.Element

internal data class BlockedPostMarker(val blocked: Boolean, val placeholder: String, val html: String)

internal object BlockedPostParser {
    fun parse(post: Element): BlockedPostMarker {
        // A quoted/nested post's notice must not mark its parent as blocked.
        val notice = post.select(".user-block-hidden-placeholder")
            .firstOrNull { it.closest("li.post-entry") == post }
        val placeholder = notice?.text().orEmpty()
        return BlockedPostMarker(
            blocked = post.hasClass("user-block-hidden-reply") ||
                post.hasClass("user-block-hidden-topic") || placeholder.isNotBlank(),
            placeholder = placeholder,
            html = notice?.html().orEmpty(),
        )
    }
}
