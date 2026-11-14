package sb.linux.client.data

import org.jsoup.nodes.Element
import sb.linux.client.common.network.Endpoints

/** 只保留展示用作者栏，不复制源站的表单、事件或业务动作。 */
internal object SourceTopicMarkup {
    fun author(post: Element): String = post.children()
        .filter { it.hasClass("post-avatar") || it.hasClass("post-body") }
        .joinToString("") { source ->
            source.clone().apply {
                select(".post-ops").forEach { operations ->
                    val rewards = operations.select(".red-packet-reply-reward").map { it.clone() }
                    operations.empty()
                    rewards.forEach(operations::appendChild)
                    if (rewards.isEmpty()) operations.remove()
                }
                select(".post-floor, form, button, script, style, input").remove()
                getAllElements().forEach { element ->
                    element.attributes().asList().filter { it.key.startsWith("on", true) }
                        .forEach { element.removeAttr(it.key) }
                }
            }.outerHtml()
        }

    fun styles(document: Element): List<String> = document.select("link[rel=stylesheet][href]")
        .map { Endpoints.abs(it.attr("href")) }
        .filter { url -> runCatching {
            val uri = java.net.URI(url)
            uri.scheme == "https" && Endpoints.isForumHost(uri.host) && uri.path.startsWith("/app/assets/")
        }.getOrDefault(false) }.distinct()
}
