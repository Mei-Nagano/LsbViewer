package sb.linux.client.data

import sb.linux.client.util.TimeFormat
import sb.linux.client.domain.community.LeaderRow
import sb.linux.client.domain.community.ReplyItem


object CommunityFeedHtmlParser {
    // ---------------- 榜单 ----------------

    fun parseLeaderboard(html: String): List<LeaderRow> {
        val d = SourceHtmlSupport.sourceDocument(html)
        val onlineIds = SourceHtmlSupport.onlineIdsOf(d)
        val out = mutableListOf<LeaderRow>()
        // 前三名领奖台
        d.select("a.leaderboard-podium-col").forEach { a ->
            out.add(
                LeaderRow(
                    rank = a.selectFirst(".leaderboard-podium-step span")?.text()?.toIntOrNull() ?: 0,
                    userId = SourceHtmlSupport.idFrom(a.attr("href")),
                    username = SourceHtmlSupport.textWithoutGachaTitle(a.selectFirst(".leaderboard-podium-name")),
                    userGroup = SourceHtmlSupport.textWithoutGachaTitle(a.selectFirst(".leaderboard-podium-group")),
                    valueText = a.selectFirst(".leaderboard-podium-count")?.text() ?: "",
                    avatarUrl = SourceHtmlSupport.absUrl(a.selectFirst("img")?.attr("src") ?: ""),
                    online = SourceHtmlSupport.idFrom(a.attr("href")) in onlineIds || SourceHtmlSupport.onlineOf(a),
                    titleBadge = SourceHtmlSupport.gachaTitleOf(a),
                )
            )
        }
        // 列表部分
        d.select("a.leaderboard-item").forEach { a ->
            out.add(
                LeaderRow(
                    rank = a.selectFirst(".leaderboard-rank-badge")?.text()?.toIntOrNull() ?: 0,
                    userId = SourceHtmlSupport.idFrom(a.attr("href")),
                    username = SourceHtmlSupport.textWithoutGachaTitle(a.selectFirst(".leaderboard-name")),
                    userGroup = SourceHtmlSupport.textWithoutGachaTitle(a.selectFirst(".leaderboard-group")),
                    valueText = a.selectFirst(".leaderboard-count")?.text() ?: "",
                    avatarUrl = SourceHtmlSupport.absUrl(a.selectFirst("img")?.attr("src") ?: ""),
                    online = SourceHtmlSupport.idFrom(a.attr("href")) in onlineIds || SourceHtmlSupport.onlineOf(a),
                    titleBadge = SourceHtmlSupport.gachaTitleOf(a),
                )
            )
        }
        return out.sortedBy { if (it.rank == 0) 999 else it.rank }
    }

    // ---------------- 回帖列表（我的回帖 tab） ----------------

    fun parseReplyList(html: String): List<ReplyItem> {
        val d = SourceHtmlSupport.sourceDocument(html)
        return d.select("li.post-item").mapNotNull { li ->
            val a = li.selectFirst("a.post-title") ?: li.selectFirst("a[href^=/topic/]") ?: return@mapNotNull null
            val href = a.attr("href")
            val tid = SourceHtmlSupport.idFrom(href)
            if (tid == 0L) return@mapNotNull null
            val replyId = Regex("""[?&]replyid=(\d+)""").find(href)?.groupValues?.get(1)?.toLongOrNull() ?: 0L
            ReplyItem(
                topicId = tid,
                title = a.text(),
                excerpt = li.selectFirst(".profile-reply-excerpt, .post-content, .reply-excerpt")?.text() ?: "",
                timeText = li.selectFirst("span[data-performance-time]")?.attr("data-performance-time")?.toLongOrNull()
                    ?.let { TimeFormat.relative(it) } ?: "",
                replyId = replyId,
                forumName = li.selectFirst("a.post-forum-badge")?.text() ?: "",
            )
        }
    }

}
