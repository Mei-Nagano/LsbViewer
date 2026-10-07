package sb.linux.client.ui.screens

import sb.linux.client.common.content.HtmlTextConversion
import sb.linux.client.domain.topic.PostEntry

internal fun findTopicSearchHits(posts: List<PostEntry>, rawQuery: String): List<TopicSearchHit> {
    val query = rawQuery.trim()
    if (query.isBlank()) return emptyList()
    return posts.sortedBy { it.floor }.mapNotNull { post ->
        val text = HtmlTextConversion.htmlToSearchText(post.contentHtml)
        val authorMatches = post.authorName.contains(query, ignoreCase = true)
        val matchIndex = text.indexOf(query, ignoreCase = true)
        if (!authorMatches && matchIndex < 0) return@mapNotNull null
        val center = matchIndex.coerceAtLeast(0)
        val start = (center - 45).coerceAtLeast(0)
        val end = (center + query.length + 90).coerceAtMost(text.length)
        val snippet = buildString {
            if (start > 0) append('…')
            append(text.substring(start, end).replace(Regex("""\s+"""), " "))
            if (end < text.length) append('…')
        }.ifBlank { "用户名匹配" }
        TopicSearchHit(post, snippet)
    }
}
