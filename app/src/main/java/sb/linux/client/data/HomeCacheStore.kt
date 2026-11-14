package sb.linux.client.data

import android.content.Context
import sb.linux.client.domain.community.HomeBrowseCache
import sb.linux.client.domain.community.HomeSidebar
import sb.linux.client.domain.community.ForumCount
import sb.linux.client.domain.community.HotTopic
import sb.linux.client.domain.community.HotTopicPeriod
import sb.linux.client.domain.community.NewUser
import sb.linux.client.domain.community.OnlineUser
import sb.linux.client.domain.community.OnlineUsers
import sb.linux.client.domain.community.SidebarAnnouncement
import sb.linux.client.domain.community.DailyStar
import sb.linux.client.domain.topic.TopicCard

internal class HomeCacheStore(context: Context) : HomeBrowseCache {
    private val homePrefs = context.getSharedPreferences("lsb_home_cache", Context.MODE_PRIVATE)
    private val sidebarPrefs = context.getSharedPreferences("lsb_sidebar", Context.MODE_PRIVATE)
    override fun saveHomeCache(sort: String, items: List<TopicCard>) {
        runCatching {
            val arr = org.json.JSONArray()
            items.forEach { t ->
                arr.put(
                    org.json.JSONObject()
                        .put("tid", t.topicId).put("title", t.title)
                        .put("aid", t.authorId).put("an", t.authorName)
                        .put("fid", t.forumId).put("fn", t.forumName)
                        .put("rp", t.replies).put("lp", t.lastReplier)
                        .put("tm", t.timeText).put("pin", t.pinned).put("hot", t.hot)
                        .put("fea", t.featured).put("tc", t.titleColor)
                        .put("av", t.avatarUrl)
                        .put("ls", t.lotteryStatus).put("cs", t.cardStatus)
                        .put("source_tags", org.json.JSONArray(t.sourceTags))
                        .put("styled_tags", org.json.JSONArray().apply { t.styledTags.forEach { tag ->
                            put(org.json.JSONObject().put("label", tag.label).put("classes", tag.classes).put("style", tag.style))
                        } })
                )
            }
            homePrefs.edit().putString("sort_$sort", arr.toString()).apply()
        }
    }

    override fun loadHomeCache(sort: String): List<TopicCard> = runCatching {
        val arr = org.json.JSONArray(homePrefs.getString("sort_$sort", "[]") ?: "[]")
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            TopicCard(
                topicId = o.optLong("tid"), title = o.optString("title"),
                authorId = o.optLong("aid"), authorName = o.optString("an"),
                forumId = o.optLong("fid"), forumName = o.optString("fn"),
                replies = o.optInt("rp"), lastReplier = o.optString("lp"),
                timeText = o.optString("tm"), pinned = o.optBoolean("pin"),
                hot = o.optBoolean("hot"), featured = o.optBoolean("fea"),
                titleColor = o.optString("tc"), avatarUrl = o.optString("av"),
                lotteryStatus = o.optString("ls"), cardStatus = o.optString("cs"),
                sourceTags = o.optJSONArray("source_tags")?.let { tags ->
                    (0 until tags.length()).map { tags.optString(it) }.filter(String::isNotBlank)
                }.orEmpty(),
                styledTags = o.optJSONArray("styled_tags")?.let { tags -> (0 until tags.length()).map { index ->
                    val tag = tags.getJSONObject(index)
                    sb.linux.client.domain.topic.SourceTopicTag(tag.optString("label"), tag.optString("classes"), tag.optString("style"))
                } }.orEmpty(),
            )
        }
    }.getOrDefault(emptyList())

    override fun clearHomeCache() = homePrefs.edit().clear().apply()

    // ---------------- 侧边栏本地永久缓存 ----------------


    override fun saveHomeSidebar(sidebar: HomeSidebar?) {
        val sb = sidebar
        if (sb == null) { sidebarPrefs.edit().clear().apply(); return }
        runCatching {
            val a = org.json.JSONArray()
            sb.forums.forEach { f ->
                a.put(org.json.JSONObject().put("id", f.id).put("name", f.name).put("count", f.count).put("color", f.color))
            }
            val h = encodeHotTopics(sb.hotTopics)
            val hotPeriods = org.json.JSONArray()
            sb.hotTopicPeriods.forEach { period ->
                hotPeriods.put(org.json.JSONObject().put("id", period.id).put("label", period.label)
                    .put("topics", encodeHotTopics(period.topics)))
            }
            val u = org.json.JSONArray()
            sb.newUsers.forEach { n ->
                u.put(org.json.JSONObject().put("uid", n.userId).put("name", n.username).put("av", n.avatarUrl))
            }
            // 在线用户（实时数据，缓存展示"上次已知在线状态"）
            val on = org.json.JSONArray()
            sb.onlineUsers.items.forEach { n ->
                on.put(org.json.JSONObject().put("uid", n.userId).put("name", n.username).put("av", n.avatarUrl))
            }
            val announcements = org.json.JSONArray()
            sb.announcements.forEach { notice ->
                announcements.put(org.json.JSONObject().put("title", notice.title).put("url", notice.url))
            }
            val stars = org.json.JSONArray()
            sb.dailyStars.forEach { star ->
                stars.put(
                    org.json.JSONObject()
                        .put("heading", star.heading).put("uid", star.userId)
                        .put("name", star.username).put("av", star.avatarUrl)
                        .put("description", star.description).put("online", star.online)
                )
            }
            val o = org.json.JSONObject()
                .put("forums", a).put("hot", h).put("users", u)
                .put("hotPeriods", hotPeriods)
                .put("stats", sb.statsText)
                .put("onCount", sb.onlineUsers.count).put("onItems", on)
                .put("announcements", announcements).put("dailyStars", stars)
            sidebarPrefs.edit().putString("sidebar", o.toString()).apply()
        }
    }

    override fun loadHomeSidebar(): HomeSidebar? = runCatching {
        val o = org.json.JSONObject(sidebarPrefs.getString("sidebar", "{}") ?: "{}")
        val forums = (0 until (o.optJSONArray("forums")?.length() ?: 0)).map { i ->
            val f = o.getJSONArray("forums").getJSONObject(i)
            ForumCount(f.optLong("id"), f.optString("name"), f.optString("count"), f.optString("color"))
        }
        val hot = decodeHotTopics(o.optJSONArray("hot"))
        val hotPeriods = (0 until (o.optJSONArray("hotPeriods")?.length() ?: 0)).map { i ->
            val period = o.getJSONArray("hotPeriods").getJSONObject(i)
            HotTopicPeriod(period.getString("id"), period.getString("label"), decodeHotTopics(period.getJSONArray("topics")))
        }
        val users = (0 until (o.optJSONArray("users")?.length() ?: 0)).map { i ->
            val n = o.getJSONArray("users").getJSONObject(i)
            NewUser(n.optLong("uid"), n.optString("name"), n.optString("av"))
        }
        val onItems = (0 until (o.optJSONArray("onItems")?.length() ?: 0)).map { i ->
            val n = o.getJSONArray("onItems").getJSONObject(i)
            OnlineUser(n.optLong("uid"), n.optString("name"), n.optString("av"))
        }
        val announcements = (0 until (o.optJSONArray("announcements")?.length() ?: 0)).map { i ->
            val notice = o.getJSONArray("announcements").getJSONObject(i)
            SidebarAnnouncement(notice.optString("title"), notice.optString("url"))
        }
        val stars = (0 until (o.optJSONArray("dailyStars")?.length() ?: 0)).map { i ->
            val star = o.getJSONArray("dailyStars").getJSONObject(i)
            DailyStar(
                heading = star.optString("heading"), userId = star.optLong("uid"),
                username = star.optString("name"), avatarUrl = star.optString("av"),
                description = star.optString("description"), online = star.optBoolean("online"),
            )
        }
        HomeSidebar(
            forums = forums, hotTopics = hot,
            statsText = o.optString("stats"), newUsers = users,
            onlineUsers = OnlineUsers(count = o.optString("onCount"), items = onItems),
            announcements = announcements, dailyStars = stars,
            hotTopicPeriods = hotPeriods,
        )
    }.getOrNull()

    private fun encodeHotTopics(topics: List<HotTopic>) = org.json.JSONArray().apply {
        topics.forEach { topic ->
            put(org.json.JSONObject().put("tid", topic.topicId).put("title", topic.title).put("meta", topic.meta))
        }
    }

    private fun decodeHotTopics(array: org.json.JSONArray?): List<HotTopic> = (0 until (array?.length() ?: 0)).map { index ->
        val topic = array!!.getJSONObject(index)
        HotTopic(topic.optLong("tid"), topic.optString("title"), topic.optString("meta"))
    }

}
