package sb.linux.client.data

import org.jsoup.nodes.Element
import sb.linux.client.domain.community.IdentityCenterData
import sb.linux.client.domain.community.IdentityRequirement
import sb.linux.client.domain.community.InviteCenter
import sb.linux.client.domain.community.InviteUser

object CommunityAccountHtmlParser {
    // ---------------- 认证中心 / 邀请中心 / 我的称号 ----------------

    /** 创作者认证中心（/identity_center）：权益、条件、邮箱状态与历史申请。 */
    fun parseIdentityCenter(html: String): IdentityCenterData {
        val d = SourceHtmlSupport.sourceDocument(html)
        fun heading(text: String) = d.select("h2,h3,h4").firstOrNull { it.text().contains(text) }
        fun nextElement(start: Element?, selector: String): Element? {
            var node = start?.nextElementSibling()
            while (node != null && node.tagName() !in setOf("h2", "h3", "h4")) {
                if (node.`is`(selector)) return node
                node.selectFirst(selector)?.let { return it }
                node = node.nextElementSibling()
            }
            return null
        }
        fun requirements(title: String): List<IdentityRequirement> {
            val table = nextElement(heading(title), "table") ?: return emptyList()
            return table.select("tbody tr, tr").mapNotNull { row ->
                val cells = row.select("td")
                if (cells.size < 2) return@mapNotNull null
                val label = cells[0].text().trim()
                val state = cells[1].text().trim()
                if (label.isBlank()) null else IdentityRequirement(label, state.contains("符合") && !state.contains("不符合"))
            }.distinctBy { it.label }
        }
        val benefits = nextElement(heading("创作者福利"), "ul")?.select("li")?.mapNotNull { item ->
            val name = item.selectFirst("strong")?.text()?.trim().orEmpty()
            if (name.isBlank()) return@mapNotNull null
            val description = item.text().removePrefix(name).trim()
            name to description
        }.orEmpty()
        val account = requirements("账号核验条件")
        val mainText = d.selectFirst("main")?.text().orEmpty()
        val email = Regex("当前邮箱[：:]\\s*(\\S+)").find(mainText)?.groupValues?.get(1).orEmpty()
        val records = nextElement(heading("我的申请"), "ul")?.select("li")?.map { it.text().trim() }
            ?.filter { it.isNotBlank() && !it.contains("暂无申请") }.orEmpty()
        val applyButton = d.selectFirst(".identity-center-next, form.identity-center-apply button[type=submit]")
        return IdentityCenterData(
            benefits = benefits,
            creatorRequirements = requirements("创作者认证条件"),
            accountRequirements = account,
            email = email,
            emailVerified = account.firstOrNull { it.label.contains("邮箱验证") }?.met == true,
            canApply = applyButton != null && !applyButton.hasAttr("disabled"),
            applicationRecords = records,
            notice = d.selectFirst(".identity-center-notice, .identity-center-rules, details")?.text()?.trim().orEmpty(),
        )
    }

    /**
     * 邀请中心（/invite_center，登录后可见）。
     * 结构（v8.7.5 插件）：.invite-center-link（说明 + 分享链接 input[data-invite-center-link]）、
     * .invite-center-stats（三个 strong+span 统计）、.invite-center-list ul li（被邀请用户）。
     */
    fun parseInviteCenter(html: String): InviteCenter {
        val d = SourceHtmlSupport.sourceDocument(html)
        fun statsAt(i: Int): String =
            d.select(".invite-center-stats > div")[i].selectFirst("strong")?.text()?.trim() ?: "0"
        val users = d.select(".invite-center-list ul li").mapNotNull { li ->
            // 空态 li.empty-state（"暂时还没有用户…"）不是用户条目
            if (li.hasClass("empty-state") || li.selectFirst("a[href^=/user/]") == null) return@mapNotNull null
            val a = li.selectFirst("a[href^=/user/]")
            val name = a?.text()?.trim() ?: return@mapNotNull null
            // 状态文本：条目全文去掉用户名后的剩余文本（注册时间/奖励发放等）
            val status = li.text().replace(name, "").replace(Regex("""\s+"""), " ").trim()
            InviteUser(
                name = name,
                userId = a?.attr("href")?.let { SourceHtmlSupport.idFrom(it) } ?: 0,
                statusText = status,
            )
        }
        return InviteCenter(
            link = d.selectFirst("input[data-invite-center-link]")?.attr("value")?.trim() ?: "",
            desc = d.selectFirst(".invite-center-link p.muted")?.text()?.trim() ?: "",
            rule = d.selectFirst(".invite-center-rule")?.text()?.trim() ?: "",
            invitedCount = statsAt(0),
            firstReward = d.select(".invite-center-stats > div").getOrNull(1)
                ?.selectFirst("span")?.text()?.trim() ?: "",
            secondReward = d.select(".invite-center-stats > div").getOrNull(2)
                ?.selectFirst("span")?.text()?.trim() ?: "",
            users = users,
        )
    }

    /**
     * 我的称号（/gacha_profile，登录后可见）。
     * 结构：.gacha-center-stat（"N 种称号"）+ .gacha-profile-titles 内称号条目。
     * 条目 DOM 未见于空态页面（测试账号无称号），按源站惯例宽松解析：
     * 容器直接子元素逐个取 .gacha-title-badge（icon/name/rarity），
     * 其余表单（装备/赠送等）按 action + 隐藏字段 + 按钮文本通用收集，不硬编码接口。
     */
}
