package sb.linux.client.data

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import sb.linux.client.common.network.Endpoints
import sb.linux.client.domain.account.LoginState
import sb.linux.client.domain.account.UserProfile

object UserAccountHtmlParser {
    private fun rankWithoutUid(element: org.jsoup.nodes.Element?): String =
        SourceHtmlSupport.textWithoutGachaTitle(element?.clone()?.also {
            it.select(".user-uid-badge, .user-follow-panel-uid").remove()
        }).replace(Regex("""\bUID\s*\d+""", RegexOption.IGNORE_CASE), "").trim(' ', '·')

    /** 源站 b.title 保存完整数字，b.text 可能是 1K 等缩写。 */
    private fun accountStat(card: org.jsoup.nodes.Element?, field: String): String {
        val value = card?.selectFirst("[data-user-follow-stat=$field] b") ?: return ""
        return value.attr("title").trim().ifBlank { value.text().trim() }
    }
    // ---------------- 用户 ----------------

    fun parseUserProfile(html: String): UserProfile {
        val d = SourceHtmlSupport.sourceDocument(html)
        val onlineIds = SourceHtmlSupport.onlineIdsOf(d)
        val profileCard = d.selectFirst(".user-card")
        val name = d.selectFirst("a.user-name")?.text()
            ?: d.selectFirst(".user-name")?.text()
            ?: d.selectFirst(".profile-toolbar")?.previousElementSibling()?.text()
            ?: ""
        val uidHref = d.selectFirst("a.user-avatar-big[href^=/user/]")?.attr("href")
            ?: d.selectFirst("a[href^=/user/]")?.attr("href")
        val avatar = d.selectFirst("a.user-avatar-big img, .user-header img")?.attr("src")
            ?: d.selectFirst("img.avatar-img, img[src*='avatar']")?.attr("src") ?: ""
        val rank = rankWithoutUid(d.selectFirst(".user-rank"))
        // rank 形如 "饼友 · 积分 1324"：拆出用户组与积分
        val groupBadge = SourceHtmlSupport.groupBadgeOf(profileCard)
        val group = groupBadge?.name ?: rank.substringBefore("积分").trim(' ', '·')
        val points = accountStat(profileCard, "points").ifBlank {
            Regex("""积分\s*([\d.,kw万K W]+)""", RegexOption.IGNORE_CASE).find(rank)?.groupValues?.get(1) ?: ""
        }
        // 简介保留源站格式：直接取原始 HTML（含 markdown 渲染后的 <strong>/<a>/<br>/<pre> 等），
        // 交由 HtmlContent 渲染，保留加粗、链接、换行与代码块等排版
        val bio = d.selectFirst(".sidebar-bio")?.html()?.trim() ?: ""
        val stats = d.select(".sidebar-card .stat-row, .sidebar .card").mapNotNull { c ->
            val t = c.selectFirst(".quick-title")?.text() ?: return@mapNotNull null
            val v = c.selectFirst(".quick-value, .sidebar-value")?.text() ?: ""
            t to v
        }
        val blockLink = d.selectFirst("a[href^=/user_block]")
        return UserProfile(
            userId = SourceHtmlSupport.idFrom(uidHref),
            username = name,
            avatarUrl = SourceHtmlSupport.absUrl(avatar),
            rankText = rank,
            userGroup = group,
            points = points,
            bio = bio,
            stats = stats,
            // 在线：源站 data-online-users-ids 在线集合命中，兜底静态标记
            online = SourceHtmlSupport.idFrom(uidHref) in onlineIds ||
                d.selectFirst("a.user-avatar-big")?.let { SourceHtmlSupport.onlineOf(it) } == true,
            titleBadge = d.selectFirst(".user-header, .user-card")?.let(SourceHtmlSupport::gachaTitleOf),
            blockUrl = blockLink?.attr("href").orEmpty(),
            blockLabel = blockLink?.text().orEmpty(),
            following = accountStat(profileCard, "following"),
            followers = accountStat(profileCard, "followers"),
            wallet = accountStat(profileCard, "wallet"),
            groupBadge = groupBadge,
        )
    }

    fun parseMySidebar(html: String): LoginState {
        val d = SourceHtmlSupport.sourceDocument(html)
        val nameEl = d.select(".user-card a.user-name[href]").firstOrNull { element ->
            val url = element.absUrl("href").toHttpUrlOrNull()
            url != null && Endpoints.isForumHost(url.host) &&
                Regex("""/user/[1-9]\d*/?""").matches(url.encodedPath)
        }
        val card = nameEl?.parents()?.firstOrNull { it.hasClass("user-card") }
        val rank = rankWithoutUid(card?.selectFirst(".user-rank"))
        val points = accountStat(card, "points").ifBlank {
            Regex("""积分\s*([\d.,kw万]+)""", RegexOption.IGNORE_CASE).find(rank)?.groupValues?.get(1) ?: ""
        }
        return LoginState(
            loggedIn = nameEl != null,
            userId = SourceHtmlSupport.idFrom(nameEl?.attr("href")),
            username = nameEl?.text() ?: "",
            points = points,
            avatarUrl = SourceHtmlSupport.absUrl(SourceHtmlSupport.avatarOf(card ?: d)),
            userGroup = SourceHtmlSupport.groupBadgeOf(card)?.name ?: rank.substringBefore("积分").trim(' ', '·'),
            titleBadge = card?.let(SourceHtmlSupport::gachaTitleOf),
            following = accountStat(card, "following"),
            followers = accountStat(card, "followers"),
            wallet = accountStat(card, "wallet"),
            groupBadge = SourceHtmlSupport.groupBadgeOf(card),
        )
    }

}
