package sb.linux.client.data
import sb.linux.client.common.network.Endpoints

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import sb.linux.client.common.content.HtmlTextConversion
import sb.linux.client.domain.topic.TitleBadge

internal object SourceHtmlSupport {
    /** 分域解析器共用的源站 Document 入口，统一基址和 Jsoup 配置。 */
    fun sourceDocument(html: String): Document = Jsoup.parse(html, Endpoints.BASE)

    /**
     * 提取正文中的楼层引用：优先取 ?floor=N 链接（源站渲染为 "#<a href=..?floor=N>N</a>"，
     * htmlToText 后 "#" 与数字之间隔着空格，纯文本正则 #N 不可靠），兜底纯文本 #N。
     */
    fun floorRefs(html: String): List<Int> {
        val linked = Regex("""[?&]floor=(\d+)""").findAll(html)
            .mapNotNull { it.groupValues[1].toIntOrNull() }.toList()
        if (linked.isNotEmpty()) return linked
        return Regex("""#(\d+)""").findAll(HtmlTextConversion.htmlToText(html))
            .mapNotNull { it.groupValues[1].toIntOrNull() }.toList()
    }

    internal fun idFrom(href: String?): Long =
        Regex("""/(\d+)""").find(href ?: "")?.groupValues?.get(1)?.toLongOrNull() ?: 0L

    fun absUrl(u: String): String = when {
        u.startsWith("//") -> "https:" + u
        u.startsWith("http") -> u
        else -> Endpoints.abs(u)
    }

    /** 兼容源站不同头像标签：优先 .avatar-img，其次带 avatar 的图片，最后任意 img */
    internal fun avatarOf(el: Element): String =
        el.selectFirst("img.avatar-img")?.attr("src")
            ?: el.selectFirst("img[src*='avatar']")?.attr("src")
            ?: el.selectFirst("img")?.attr("src") ?: ""

    /** 元素内头像是否带源站在线标记：
     *  online-users-dot 是子元素（<span class="online-users-dot">），
     *  online-users-host 是直接加在头像链接 class 上（class="avatar-profile-link online-users-host"） */
    internal fun onlineOf(el: Element): Boolean =
        el.selectFirst(".online-users-dot") != null ||
            el.selectFirst(".online-users-host") != null ||
            el.selectFirst("a.online-users-host") != null ||
            el.classNames().contains("online-users-host")

    /**
     * 源站在线用户 ID 集合。v8.6.5+ 源站不再把在线标记写进静态 HTML，
     * 而是通过 <span class="online-users-boot" data-online-users-ids="id,id,..."> 下发在线用户，
     * 由前端 JS 动态给头像加绿点。应用解析原始 HTML 需要自己读这个集合来判定某用户是否在线。
     */
    internal fun onlineIdsOf(d: Document): Set<Long> =
        d.selectFirst("span.online-users-boot[data-online-users-ids]")?.attr("data-online-users-ids")
            ?.split(",")
            ?.mapNotNull { it.trim().toLongOrNull() }
            ?.toSet() ?: emptySet()

    /**
     * 解析元素（或其子节点）内首个源站称号徽章 a.gacha-title-badge：
     * icon（emoji）、name（称号名）、rarity（稀有度，取自类名 gacha-title-<x>，兜底文本）。
     * 无称号徽章返回 null。
     */
    /** 称号既可能是容器内的子节点，也可能就是用户组节点本身。 */
    internal fun textWithoutGachaTitle(el: Element?): String {
        if (el == null || el.hasClass("gacha-title-badge") || el.hasClass("gacha-title-post-badge")) return ""
        return el.clone().also { it.select(".gacha-title-badge, .gacha-title-post-badge").remove() }.text()
    }

    internal fun gachaTitleOf(el: Element): TitleBadge? {
        val badge = el.selectFirst(".gacha-title-badge, .gacha-title-post-badge") ?: return null
        val rare = Regex("""gacha-title-(ur\+|ssr|sr|ur|r|n)(?:\s|$)""")
            .find(badge.className())?.groupValues?.get(1)?.uppercase()
        val rarityText = badge.selectFirst(".gacha-title-rarity")?.text()?.trim().orEmpty()
        return TitleBadge(
            icon = badge.selectFirst(".gacha-title-icon")?.text()?.trim() ?: "",
            sourceStyle = badge.attr("style"),
            name = badge.selectFirst(".gacha-title-name")?.text()?.trim() ?: "",
            rarity = rare ?: rarityText,
            // 个人主页把 UR 编号放在 rarity 节点，帖子中则使用 serial 节点。
            serial = badge.selectFirst(".gacha-title-serial")?.text()?.trim()?.takeIf { it.isNotBlank() }
                ?: rarityText.takeIf { rare?.startsWith("UR") == true && it.matches(Regex("[0-9]+")) }.orEmpty(),
        ).takeIf { it.name.isNotBlank() || it.icon.isNotBlank() }
    }

    /** 收集表单隐藏字段（兑换/参与时随 _csrf 一起提交） */
    internal fun hiddenFields(form: Element?): Map<String, String> =
        form?.select("input[type=hidden]")?.mapNotNull { i ->
            val n = i.attr("name")
            if (n.isNotBlank() && n != "_csrf") n to i.attr("value") else null
        }?.toMap() ?: emptyMap()

}

