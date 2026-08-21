package sb.linux.client.data

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

/**
 * 应用设置（区别于源站个人设置）：
 * 滚动模式 / 快速点赞 / 屏蔽词 / AI 总结 / 浏览历史 / 签到摘要缓存。
 */
class AppSettings(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("lsb_app_settings", Context.MODE_PRIVATE)

    // ---------------- 偏好读写 ----------------

    /** 首页滚动模式：true = 无限滚动，false = 翻页 */
    var infiniteScroll: Boolean
        get() = prefs.getBoolean("infinite_scroll", true)
        set(v) = prefs.edit().putBoolean("infinite_scroll", v).apply()

    /** 打开应用时不自动刷新首页，直接显示缓存内容 */
    var homeKeepCache: Boolean
        get() = prefs.getBoolean("home_keep_cache", false)
        set(v) = prefs.edit().putBoolean("home_keep_cache", v).apply()

    /** 帖子内打赏弹幕开关 */
    var danmakuOn: Boolean
        get() = prefs.getBoolean("danmaku_on", true)
        set(v) = prefs.edit().putBoolean("danmaku_on", v).apply()

    /** 侧边栏双列显示（作用于快捷功能与版块列表） */
    var sidebarTwoColumns: Boolean
        get() = prefs.getBoolean("sidebar_two_columns", true)
        set(v) = prefs.edit().putBoolean("sidebar_two_columns", v).apply()

    /** 翻页模式下每页展示的帖子条数（默认 15） */
    var topicsPerPage: Int
        get() = prefs.getInt("topics_per_page", 15)
        set(v) = prefs.edit().putInt("topics_per_page", v.coerceIn(5, 50)).apply()

    /** 评论区排序：0 = 热度，1 = 正序，2 = 倒序（记住上次选择） */
    var commentSortOrder: Int
        get() = prefs.getInt("comment_sort_order", 0)
        set(v) = prefs.edit().putInt("comment_sort_order", v).apply()

    /** 评论翻页模式下每页展示的评论数量（默认 20，3.11） */
    var commentsPerPage: Int
        get() = prefs.getInt("comments_per_page", 20)
        set(v) = prefs.edit().putInt("comments_per_page", v.coerceIn(5, 100)).apply()

    /** 首页排序抽屉（全部/仅抽奖/仅发卡 上方的 新评论/新帖子/精华）折叠状态：true = 展开，false = 折叠 */
    var homeSortDrawerOpen: Boolean
        get() = prefs.getBoolean("home_sort_drawer_open", true)
        set(v) = prefs.edit().putBoolean("home_sort_drawer_open", v).apply()

    /** 无限滚动/翻页模式的分类覆盖（3.13）：key = home / forum / topic，值为该分类的无限滚动开关；
     *  未配置的分类跟随全局 infiniteScroll。 */
    var scrollModeOverrides: Map<String, Boolean>
        get() = runCatching {
            val o = JSONObject(prefs.getString("scroll_mode_overrides", "{}") ?: "{}")
            buildMap {
                for (k in o.keys()) if (k in setOf("home", "forum", "topic")) put(k, o.getBoolean(k))
            }
        }.getOrDefault(emptyMap())
        set(v) {
            val o = JSONObject()
            v.forEach { (k, b) -> o.put(k, b) }
            prefs.edit().putString("scroll_mode_overrides", o.toString()).apply()
        }

    // ---------------- 常规设置（3.20） ----------------

    /** 打开链接方式：0 = 应用内打开，1 = 外部浏览器打开 */
    var linkOpenMode: Int
        get() = prefs.getInt("link_open_mode", 0)
        set(v) = prefs.edit().putInt("link_open_mode", v).apply()

    /** 检查更新时机：0 = 每次打开应用，1 = 按间隔，2 = 从不 */
    var updateCheckMode: Int
        get() = prefs.getInt("update_check_mode", 0)
        set(v) = prefs.edit().putInt("update_check_mode", v).apply()

    /** 检查更新间隔（小时，默认 24） */
    var updateCheckIntervalHours: Int
        get() = prefs.getInt("update_check_interval_hours", 24)
        set(v) = prefs.edit().putInt("update_check_interval_hours", v.coerceIn(1, 24 * 30)).apply()

    /** 上次检查更新的时间戳 */
    var lastUpdateCheck: Long
        get() = prefs.getLong("last_update_check", 0L)
        set(v) = prefs.edit().putLong("last_update_check", v).apply()

    // ---------------- WebDAV 备份（3.19） ----------------

    var webdavUrl: String
        get() = prefs.getString("webdav_url", "") ?: ""
        set(v) = prefs.edit().putString("webdav_url", v).apply()

    var webdavUser: String
        get() = prefs.getString("webdav_user", "") ?: ""
        set(v) = prefs.edit().putString("webdav_user", v).apply()

    var webdavPass: String
        get() = prefs.getString("webdav_pass", "") ?: ""
        set(v) = prefs.edit().putString("webdav_pass", v).apply()

    var webdavDir: String
        get() = prefs.getString("webdav_dir", "/linuxsb/") ?: ""
        set(v) = prefs.edit().putString("webdav_dir", v).apply()

    // ---------------- 分类重置（3.9） ----------------

    /** 重置浏览设置 */
    fun resetBrowse() = prefs.edit()
        .remove("infinite_scroll").remove("home_keep_cache").remove("danmaku_on")
        .remove("sidebar_two_columns").remove("topics_per_page")
        .remove("comment_sort_order").remove("comments_per_page")
        .remove("scroll_mode_overrides").remove("home_sort_drawer_open")
        .apply()

    /** 重置 AI 设置 */
    fun resetAi() = prefs.edit()
        .remove("ai_auto").remove("ai_url").remove("ai_key").remove("ai_model")
        .remove("ai_temp").remove("ai_include_comments")
        .apply()

    /** 重置常规设置 */
    fun resetGeneral() = prefs.edit()
        .remove("link_open_mode").remove("update_check_mode")
        .remove("update_check_interval_hours")
        .apply()

    /** 重置 WebDAV 备份配置 */
    fun resetWebdav() = prefs.edit()
        .remove("webdav_url").remove("webdav_user").remove("webdav_pass").remove("webdav_dir")
        .apply()

    /** 打开帖子自动请求 AI 总结 */
    var aiAuto: Boolean
        get() = prefs.getBoolean("ai_auto", false)
        set(v) = prefs.edit().putBoolean("ai_auto", v).apply()

    var aiUrl: String
        get() = prefs.getString("ai_url", "") ?: ""
        set(v) = prefs.edit().putString("ai_url", v).apply()

    var aiKey: String
        get() = prefs.getString("ai_key", "") ?: ""
        set(v) = prefs.edit().putString("ai_key", v).apply()

    var aiModel: String
        get() = prefs.getString("ai_model", "gpt-4o-mini") ?: ""
        set(v) = prefs.edit().putString("ai_model", v).apply()

    var aiTemp: Float
        get() = prefs.getFloat("ai_temp", 0.3f)
        set(v) = prefs.edit().putFloat("ai_temp", v).apply()

    /** 是否解析评论区：false 时仅用楼主正文 */
    var aiIncludeComments: Boolean
        get() = prefs.getBoolean("ai_include_comments", true)
        set(v) = prefs.edit().putBoolean("ai_include_comments", v).apply()

    var blockedWords: Set<String>
        get() = prefs.getStringSet("blocked_words", emptySet())?.toSet() ?: emptySet()
        set(v) = prefs.edit().putStringSet("blocked_words", v).apply()

    var blockedUsers: Set<String>
        get() = prefs.getStringSet("blocked_users", emptySet())?.toSet() ?: emptySet()
        set(v) = prefs.edit().putStringSet("blocked_users", v).apply()

    // ---------------- 每日签到 ----------------

    private val todayKey: String
        get() {
            val c = java.util.Calendar.getInstance()
            return "%04d-%02d-%02d".format(c.get(java.util.Calendar.YEAR),
                c.get(java.util.Calendar.MONTH) + 1, c.get(java.util.Calendar.DAY_OF_MONTH))
        }

    /** 今日是否已自动签到 */
    fun checkinDoneToday(): Boolean = prefs.getString("last_checkin", "") == todayKey

    fun markCheckinDone() = prefs.edit().putString("last_checkin", todayKey).apply()

    /** 缓存本次签到摘要（连续/累计天数），跨天或未缓存时返回 null；用于避免每次启动重复查询源站 */
    fun saveCheckinSummary(streak: Int, total: Int) {
        prefs.edit()
            .putString("last_checkin_date", todayKey)
            .putInt("last_checkin_streak", streak)
            .putInt("last_checkin_total", total)
            .apply()
    }

    /** 今日已签到时的本地签到摘要；跨天或从未缓存返回 null */
    fun checkinSummaryToday(): Pair<Int, Int>? {
        if (!checkinDoneToday() || prefs.getString("last_checkin_date", "") != todayKey) return null
        val streak = prefs.getInt("last_checkin_streak", 0)
        val total = prefs.getInt("last_checkin_total", 0)
        if (streak == 0 && total == 0) return null
        return streak to total
    }

    // ---------------- 记住密码（登录页） ----------------

    var rememberPassword: Boolean
        get() = prefs.getBoolean("remember_password", false)
        set(v) = prefs.edit().putBoolean("remember_password", v).apply()

    var savedUsername: String
        get() = prefs.getString("saved_username", "") ?: ""
        set(v) = prefs.edit().putString("saved_username", v).apply()

    /** 仅做混淆存储（Base64），非加密保存 */
    var savedPassword: String
        get() = runCatching {
            String(android.util.Base64.decode(prefs.getString("saved_password", "") ?: "", android.util.Base64.NO_WRAP))
        }.getOrDefault("")
        set(v) = prefs.edit().putString(
            "saved_password",
            android.util.Base64.encodeToString(v.toByteArray(), android.util.Base64.NO_WRAP)
        ).apply()

    fun clearSavedPassword() =
        prefs.edit().remove("saved_password").remove("saved_username").apply()

    // ---------------- 本地浏览历史（不解析源站足迹） ----------------

    private fun historyArray(): JSONArray =
        runCatching { JSONArray(prefs.getString("local_history", "[]") ?: "[]") }.getOrDefault(JSONArray())

    /** 记录一次浏览：同一帖子去重置顶，最多保留 200 条 */
    fun addHistory(
        topicId: Long, title: String, authorId: Long, authorName: String,
        forumId: Long, forumName: String, avatarUrl: String, titleColor: String,
    ) {
        if (topicId <= 0) return
        val old = historyArray()
        val out = JSONArray()
        out.put(
            JSONObject()
                .put("topicId", topicId).put("title", title)
                .put("authorId", authorId).put("authorName", authorName)
                .put("forumId", forumId).put("forumName", forumName)
                .put("avatarUrl", avatarUrl).put("titleColor", titleColor)
                .put("at", System.currentTimeMillis())
        )
        for (i in 0 until old.length()) {
            val o = old.optJSONObject(i) ?: continue
            if (o.optLong("topicId") == topicId) continue
            out.put(o)
        }
        while (out.length() > 200) out.remove(out.length() - 1)
        prefs.edit().putString("local_history", out.toString()).apply()
    }

    /** 本地浏览历史（最新在前），timeText 为本地浏览时间 */
    fun historyList(): List<sb.linux.client.data.TopicCard> {
        val arr = historyArray()
        val fmt = java.text.SimpleDateFormat("MM-dd HH:mm", java.util.Locale.getDefault())
        return buildList {
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                val at = o.optLong("at", 0L)
                add(
                    TopicCard(
                        topicId = o.optLong("topicId"),
                        title = o.optString("title"),
                        authorId = o.optLong("authorId"),
                        authorName = o.optString("authorName"),
                        forumId = o.optLong("forumId"),
                        forumName = o.optString("forumName"),
                        replies = 0,
                        lastReplier = "",
                        timeText = if (at > 0) fmt.format(java.util.Date(at)) else "",
                        avatarUrl = o.optString("avatarUrl"),
                        titleColor = o.optString("titleColor"),
                    )
                )
            }
        }
    }

    fun removeHistory(topicId: Long) {
        val old = historyArray()
        val out = JSONArray()
        for (i in 0 until old.length()) {
            val o = old.optJSONObject(i) ?: continue
            if (o.optLong("topicId") != topicId) out.put(o)
        }
        prefs.edit().putString("local_history", out.toString()).apply()
    }

    fun clearHistory() = prefs.edit().remove("local_history").apply()

    // ---------------- 设置导出 / 导入 ----------------

    fun exportJson(): String {
        val o = JSONObject()
        o.put("infinite_scroll", infiniteScroll)
        o.put("danmaku_on", danmakuOn)
        o.put("sidebar_two_columns", sidebarTwoColumns)
        o.put("home_keep_cache", homeKeepCache)
        o.put("topics_per_page", topicsPerPage)
        o.put("comment_sort_order", commentSortOrder)
        o.put("comments_per_page", commentsPerPage)
        o.put("scroll_mode_overrides", JSONObject(scrollModeOverrides))
        o.put("home_sort_drawer_open", homeSortDrawerOpen)
        o.put("link_open_mode", linkOpenMode)
        o.put("update_check_mode", updateCheckMode)
        o.put("update_check_interval_hours", updateCheckIntervalHours)
        o.put("webdav_url", webdavUrl)
        o.put("webdav_user", webdavUser)
        o.put("webdav_pass", webdavPass)
        o.put("webdav_dir", webdavDir)
        o.put("ai_auto", aiAuto)
        o.put("ai_url", aiUrl)
        o.put("ai_key", aiKey)
        o.put("ai_model", aiModel)
        o.put("ai_temp", aiTemp.toDouble())
        o.put("ai_include_comments", aiIncludeComments)
        o.put("blocked_words", JSONArray(blockedWords))
        o.put("blocked_users", JSONArray(blockedUsers))
        return o.toString(2)
    }

    /** @return null 表示导入成功，否则为错误信息 */
    fun importJson(text: String): String? {
        return try {
            val o = JSONObject(text)
            val p = prefs.edit()
            if (o.has("infinite_scroll")) p.putBoolean("infinite_scroll", o.getBoolean("infinite_scroll"))
            if (o.has("danmaku_on")) p.putBoolean("danmaku_on", o.getBoolean("danmaku_on"))
            if (o.has("sidebar_two_columns")) p.putBoolean("sidebar_two_columns", o.getBoolean("sidebar_two_columns"))
            if (o.has("home_keep_cache")) p.putBoolean("home_keep_cache", o.getBoolean("home_keep_cache"))
            if (o.has("topics_per_page")) p.putInt("topics_per_page", o.optInt("topics_per_page", 15).coerceIn(5, 50))
            if (o.has("comment_sort_order")) p.putInt("comment_sort_order", o.optInt("comment_sort_order", 0).coerceIn(0, 2))
            if (o.has("comments_per_page")) p.putInt("comments_per_page", o.optInt("comments_per_page", 20).coerceIn(5, 100))
            if (o.has("scroll_mode_overrides")) p.putString("scroll_mode_overrides", o.getJSONObject("scroll_mode_overrides").toString())
            if (o.has("home_sort_drawer_open")) p.putBoolean("home_sort_drawer_open", o.getBoolean("home_sort_drawer_open"))
            if (o.has("link_open_mode")) p.putInt("link_open_mode", o.optInt("link_open_mode", 0).coerceIn(0, 1))
            if (o.has("update_check_mode")) p.putInt("update_check_mode", o.optInt("update_check_mode", 1).coerceIn(0, 1))
            if (o.has("update_check_interval_hours")) p.putInt("update_check_interval_hours", o.optInt("update_check_interval_hours", 24).coerceIn(1, 24 * 30))
            if (o.has("webdav_url")) p.putString("webdav_url", o.getString("webdav_url"))
            if (o.has("webdav_user")) p.putString("webdav_user", o.getString("webdav_user"))
            if (o.has("webdav_pass")) p.putString("webdav_pass", o.getString("webdav_pass"))
            if (o.has("webdav_dir")) p.putString("webdav_dir", o.getString("webdav_dir"))
            if (o.has("ai_auto")) p.putBoolean("ai_auto", o.getBoolean("ai_auto"))
            if (o.has("ai_url")) p.putString("ai_url", o.getString("ai_url"))
            if (o.has("ai_key")) p.putString("ai_key", o.getString("ai_key"))
            if (o.has("ai_model")) p.putString("ai_model", o.getString("ai_model"))
            if (o.has("ai_temp")) p.putFloat("ai_temp", o.getDouble("ai_temp").toFloat())
            if (o.has("ai_include_comments")) p.putBoolean("ai_include_comments", o.getBoolean("ai_include_comments"))
            if (o.has("blocked_words")) {
                val arr = o.getJSONArray("blocked_words")
                val set = (0 until arr.length()).map { arr.getString(it) }.toSet()
                p.putStringSet("blocked_words", set)
            }
            if (o.has("blocked_users")) {
                val arr = o.getJSONArray("blocked_users")
                val set = (0 until arr.length()).map { arr.getString(it) }.toSet()
                p.putStringSet("blocked_users", set)
            }
            p.apply()
            null
        } catch (e: Exception) {
            "导入失败：不是有效的设置 JSON"
        }
    }
}
