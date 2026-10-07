package sb.linux.client.data

import org.jsoup.nodes.Document
import sb.linux.client.util.TimeFormat
import sb.linux.client.domain.topic.TopicCard

/** 首页、收藏与版块列表使用的帖子卡片解析。 */
object TopicListParser {
    fun parseTopicList(html: String): Pair<List<TopicCard>, Pair<Int, Int>> {
        val d = SourceHtmlSupport.sourceDocument(html)
        val onlineIds = SourceHtmlSupport.onlineIdsOf(d)
        val items = d.select("ul.post-list > li.post-item").mapNotNull { li ->
            val titleA = li.selectFirst("a.post-title") ?: return@mapNotNull null
            val topicId = SourceHtmlSupport.idFrom(titleA.attr("href"))
            if (topicId == 0L) return@mapNotNull null
            val meta = li.selectFirst(".post-meta")
            val userLink = meta?.selectFirst("a[href^=/user/]")
            val forumLink = li.selectFirst("a.post-forum-badge") ?: meta?.selectFirst("a[href^=/forum/]")
            // 无 <a> 且非时间的 span：[回复数, 最后回复人]
            val plainSpans = meta?.select("span")
                ?.filter { !it.hasAttr("data-performance-time") && it.select("a").isEmpty() }
                ?.map { it.text().trim() }
                ?.filter { it.isNotBlank() } ?: emptyList()
            val replies = plainSpans.firstOrNull { it.all { c -> c.isDigit() } }?.toIntOrNull() ?: 0
            val lastReplier = plainSpans.lastOrNull { !it.all { c -> c.isDigit() } } ?: ""
            val timeSpan = meta?.selectFirst("span[data-performance-time]")
            val timeText = timeSpan?.attr("data-performance-time")?.toLongOrNull()
                ?.let { TimeFormat.relative(it) } ?: (timeSpan?.text() ?: "")
            val authorId = SourceHtmlSupport.idFrom(userLink?.attr("href"))
            TopicCard(
                topicId = topicId,
                title = titleA.text(),
                authorId = authorId,
                authorName = SourceHtmlSupport.textWithoutGachaTitle(userLink),
                forumId = SourceHtmlSupport.idFrom(forumLink?.attr("href")),
                forumName = forumLink?.text() ?: "",
                replies = replies,
                lastReplier = lastReplier,
                timeText = timeText,
                pinned = li.classNames().contains("topic-pinned") || li.selectFirst(".topic-badge.pinned") != null,
                hot = li.selectFirst(".topic-stamp-hot") != null,
                featured = li.selectFirst(".topic-management-featured-badge") != null,
                unread = li.classNames().any { it.contains("unread", ignoreCase = true) },
                titleColor = Regex("""(?i)color\s*:\s*(#[0-9a-fA-F]{3,8})""").find(titleA.attr("style"))?.groupValues?.get(1) ?: "",
                avatarUrl = SourceHtmlSupport.absUrl(SourceHtmlSupport.avatarOf(li)),
                pages = li.select(".topic-pages a").mapNotNull { it.text().toIntOrNull() },
                lotteryStatus = li.selectFirst(".community-lottery-title-status")?.text()?.trim() ?: "",
                cardStatus = li.selectFirst(".virtual-card-title-status")?.text()?.trim() ?: "",
                online = authorId in onlineIds || SourceHtmlSupport.onlineOf(li),
                titleBadge = SourceHtmlSupport.gachaTitleOf(meta ?: li),
            )
        }
        return items to parsePagination(d)
    }

    /** 返回 (当前页, 总页数) */
    fun parsePagination(d: Document): Pair<Int, Int> {
        val bar = d.selectFirst(".pagination-bar") ?: return 1 to 1
        val links = bar.select("a").mapNotNull {
            Regex("""[?&]p=(\d+)""").find(it.attr("href"))?.groupValues?.get(1)?.toIntOrNull()
        }
        val active = bar.selectFirst("li.active a")
            ?.attr("href")?.let { Regex("""[?&]p=(\d+)""").find(it)?.groupValues?.get(1)?.toIntOrNull() } ?: 1
        val maxPage = (links + active).maxOrNull() ?: 1
        return active to maxPage.coerceAtLeast(1)
    }

    // ---------------- 帖子详情 ----------------

}
