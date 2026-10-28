package sb.linux.client.data

import android.content.Context
import sb.linux.client.domain.community.HomeBrowseCache
import sb.linux.client.domain.community.HomeSidebar
import sb.linux.client.domain.community.ForumCount
import sb.linux.client.domain.community.HotTopic
import sb.linux.client.domain.community.NewUser
import sb.linux.client.domain.community.OnlineUser
import sb.linux.client.domain.community.OnlineUsers
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
            val h = org.json.JSONArray()
            sb.hotTopics.forEach { t ->
                h.put(org.json.JSONObject().put("tid", t.topicId).put("title", t.title).put("meta", t.meta))
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
            val o = org.json.JSONObject()
                .put("forums", a).put("hot", h).put("users", u)
                .put("stats", sb.statsText)
                .put("onCount", sb.onlineUsers.count).put("onItems", on)
            sidebarPrefs.edit().putString("sidebar", o.toString()).apply()
        }
    }

    override fun loadHomeSidebar(): HomeSidebar? = runCatching {
        val o = org.json.JSONObject(sidebarPrefs.getString("sidebar", "{}") ?: "{}")
        val forums = (0 until (o.optJSONArray("forums")?.length() ?: 0)).map { i ->
            val f = o.getJSONArray("forums").getJSONObject(i)
            ForumCount(f.optLong("id"), f.optString("name"), f.optString("count"), f.optString("color"))
        }
        val hot = (0 until (o.optJSONArray("hot")?.length() ?: 0)).map { i ->
            val t = o.getJSONArray("hot").getJSONObject(i)
            HotTopic(t.optLong("tid"), t.optString("title"), t.optString("meta"))
        }
        val users = (0 until (o.optJSONArray("users")?.length() ?: 0)).map { i ->
            val n = o.getJSONArray("users").getJSONObject(i)
            NewUser(n.optLong("uid"), n.optString("name"), n.optString("av"))
        }
        val onItems = (0 until (o.optJSONArray("onItems")?.length() ?: 0)).map { i ->
            val n = o.getJSONArray("onItems").getJSONObject(i)
            OnlineUser(n.optLong("uid"), n.optString("name"), n.optString("av"))
        }
        HomeSidebar(
            forums = forums, hotTopics = hot,
            statsText = o.optString("stats"), newUsers = users,
            onlineUsers = OnlineUsers(count = o.optString("onCount"), items = onItems),
        )
    }.getOrNull()

}
