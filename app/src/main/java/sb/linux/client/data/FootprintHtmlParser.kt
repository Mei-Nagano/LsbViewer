package sb.linux.client.data

import sb.linux.client.util.TimeFormat
import sb.linux.client.domain.topic.TopicCard

object FootprintHtmlParser {
    fun parseFootprintList(html: String): List<TopicCard> {
        runCatching { TopicListParser.parseTopicList(html).first }.getOrDefault(emptyList()).takeIf { it.isNotEmpty() }?.let {
            return it
        }
        val d = SourceHtmlSupport.sourceDocument(html)
        val seen = mutableSetOf<Long>()
        return d.select("a[href^=/topic/]").mapNotNull { a ->
            val tid = SourceHtmlSupport.idFrom(a.attr("href"))
            if (tid == 0L || !seen.add(tid)) return@mapNotNull null
            // 跳过分页/翻页链接避免把 2、3 当成标题
            if (a.parent()?.selectFirst(".topic-pages") != null || a.text().trim().all { it.isDigit() }) return@mapNotNull null
            val item = a.parents().firstOrNull {
                it.tagName() == "li" || it.hasClass("post-item") || it.hasClass("notice-item") ||
                    it.hasClass("footprint-item") || it.hasClass("history-item")
            } ?: a.parent()
            val meta = item?.selectFirst(".post-meta")
            val user = meta?.selectFirst("a[href^=/user/]") ?: item?.selectFirst("a[href^=/user/]")
            val forum = meta?.selectFirst("a[href^=/forum/]")
                ?: item?.selectFirst(".forum-name, .post-forum-badge")
            val time = meta?.selectFirst("span[data-performance-time]")
            TopicCard(
                topicId = tid,
                title = a.text().trim(),
                authorId = SourceHtmlSupport.idFrom(user?.attr("href")),
                authorName = user?.text() ?: "",
                forumId = SourceHtmlSupport.idFrom(forum?.attr("href")),
                forumName = forum?.text() ?: "",
                replies = 0,
                lastReplier = "",
                timeText = time?.attr("data-performance-time")?.toLongOrNull()
                    ?.let { TimeFormat.relative(it) } ?: (time?.text() ?: ""),
            )
        }
    }

    /** 打赏弹幕：解析 /donate_feed 返回的 area_html（.donate-barrage-item） */
}
