package sb.linux.client.data

import sb.linux.client.domain.account.LoginState
import sb.linux.client.domain.account.UserProfile

object UserAccountHtmlParser {
    // ---------------- 用户 ----------------

    fun parseUserProfile(html: String): UserProfile {
        val d = SourceHtmlSupport.sourceDocument(html)
        val onlineIds = SourceHtmlSupport.onlineIdsOf(d)
        val name = d.selectFirst("a.user-name")?.text()
            ?: d.selectFirst(".user-name")?.text()
            ?: d.selectFirst(".profile-toolbar")?.previousElementSibling()?.text()
            ?: ""
        val uidHref = d.selectFirst("a.user-avatar-big[href^=/user/]")?.attr("href")
            ?: d.selectFirst("a[href^=/user/]")?.attr("href")
        val avatar = d.selectFirst("a.user-avatar-big img, .user-header img")?.attr("src")
            ?: d.selectFirst("img.avatar-img, img[src*='avatar']")?.attr("src") ?: ""
        val rank = SourceHtmlSupport.textWithoutGachaTitle(d.selectFirst(".user-rank"))
        // rank 形如 "饼友 · 积分 1324"：拆出用户组与积分
        val group = rank.substringBefore("积分").trim(' ', '·')
        val points = Regex("""积分\s*([\d.,kw万K W]+)""", RegexOption.IGNORE_CASE).find(rank)?.groupValues?.get(1) ?: ""
        // 简介保留源站格式：直接取原始 HTML（含 markdown 渲染后的 <strong>/<a>/<br>/<pre> 等），
        // 交由 HtmlContent 渲染，保留加粗、链接、换行与代码块等排版
        val bio = d.selectFirst(".sidebar-bio")?.html()?.trim() ?: ""
        val stats = d.select(".sidebar-card .stat-row, .sidebar .card").mapNotNull { c ->
            val t = c.selectFirst(".quick-title")?.text() ?: return@mapNotNull null
            val v = c.selectFirst(".quick-value, .sidebar-value")?.text() ?: ""
            t to v
        }
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
        )
    }

    fun parseMySidebar(html: String): LoginState {
        val d = SourceHtmlSupport.sourceDocument(html)
        val card = d.selectFirst(".user-card")
        val nameEl = card?.selectFirst("a.user-name")
        val rank = SourceHtmlSupport.textWithoutGachaTitle(card?.selectFirst(".user-rank"))
        val points = Regex("""积分\s*([\d.kw万]+)""").find(rank)?.groupValues?.get(1) ?: ""
        return LoginState(
            loggedIn = nameEl != null,
            userId = SourceHtmlSupport.idFrom(nameEl?.attr("href")),
            username = nameEl?.text() ?: "",
            points = points,
            avatarUrl = SourceHtmlSupport.absUrl(SourceHtmlSupport.avatarOf(card ?: d)),
            userGroup = rank.substringBefore("积分").trim(' ', '·'),
            titleBadge = card?.let(SourceHtmlSupport::gachaTitleOf),
        )
    }

}
