package sb.linux.client.data
import sb.linux.client.domain.community.ForumCount
import sb.linux.client.domain.community.HomeSidebar
import sb.linux.client.domain.community.HotTopic
import sb.linux.client.domain.community.NewUser
import sb.linux.client.domain.community.OnlineUser
import sb.linux.client.domain.community.OnlineUsers


object HomeSidebarHtmlParser {
    // ---------------- 首页侧板（源站 aside.sidebar） ----------------

    fun parseHomeSidebar(html: String): HomeSidebar {
        val d = SourceHtmlSupport.sourceDocument(html)
        val aside = d.selectFirst("aside.sidebar") ?: d

        // 版块列表：与底栏「板块导航」使用同一解析（全文档抓取 a[href^=/forum/]），保证两处板块一致
        val forums = ForumParser.parseForumList(html).map { ForumCount(id = it.id, name = it.name, count = it.topicCount) }

        // 每日热帖
        val hot = aside.select(".daily-hot-topics-list a[href^=/topic/]").mapNotNull { a ->
            val tid = SourceHtmlSupport.idFrom(a.attr("href"))
            if (tid <= 0) null else HotTopic(
                topicId = tid,
                title = a.selectFirst(".daily-hot-topics-title")?.text() ?: a.text().trim(),
                meta = a.selectFirst(".daily-hot-topics-count")?.text()?.replace(Regex("""\s+"""), " ")?.trim() ?: "",
            )
        }.distinctBy { it.topicId }

        // 站点统计
        val stats = aside.selectFirst(".stats-sub")?.text()?.replace(Regex("""\s+"""), " ")?.trim() ?: ""

        // 最新用户
        val newUsers = aside.select("a.nu-item").mapNotNull { a ->
            val uid = SourceHtmlSupport.idFrom(a.attr("href"))
            if (uid <= 0) null else NewUser(
                userId = uid,
                username = a.selectFirst(".nu-name")?.text() ?: a.text().trim(),
                avatarUrl = SourceHtmlSupport.absUrl(SourceHtmlSupport.avatarOf(a)),
            )
        }.distinctBy { it.userId }

        // 当前在线：源站现已服务端渲染 .online-users-card（总数 + 头像网格），直接解析；
        // 卡片缺失（旧版/异常页）时回退：读 online-users-boot 的 ID 集，人数 = ID 数，
        // 在线用户项交叉匹配本页帖子作者（拿名字/头像）。
        val card = d.selectFirst(".online-users-card")
        val onlineUsers = if (card != null) {
            OnlineUsers(
                count = card.selectFirst(".online-users-count")?.text()?.trim() ?: "",
                items = card.select(".online-users-grid a.online-users-item[href]").mapNotNull { a ->
                    val uid = SourceHtmlSupport.idFrom(a.attr("href"))
                    if (uid <= 0) null else OnlineUser(
                        userId = uid,
                        username = a.attr("title").ifBlank {
                            a.selectFirst(".online-users-name")?.text() ?: ""
                        }.trim(),
                        avatarUrl = SourceHtmlSupport.absUrl(a.selectFirst("img[src]")?.attr("src") ?: ""),
                    )
                }.distinctBy { it.userId },
                moreText = "",
            )
        } else {
            val onlineIds = SourceHtmlSupport.onlineIdsOf(d)
            if (onlineIds.isNotEmpty()) {
                val fromPage = d.select("ul.post-list > li.post-item").mapNotNull { li ->
                    val userLink = li.selectFirst(".post-meta a[href^=/user/]") ?: return@mapNotNull null
                    val uid = SourceHtmlSupport.idFrom(userLink.attr("href"))
                    if (uid > 0 && uid in onlineIds) OnlineUser(
                        userId = uid,
                        username = userLink.text().trim(),
                        avatarUrl = SourceHtmlSupport.absUrl(SourceHtmlSupport.avatarOf(li)),
                    ) else null
                }.distinctBy { it.userId }
                OnlineUsers(
                    count = "${onlineIds.size} 人",
                    items = fromPage,
                    moreText = "",
                )
            } else OnlineUsers()
        }

        return HomeSidebar(
            forums = forums,
            hotTopics = hot,
            statsText = stats,
            newUsers = newUsers,
            onlineUsers = onlineUsers,
        )
    }

}
