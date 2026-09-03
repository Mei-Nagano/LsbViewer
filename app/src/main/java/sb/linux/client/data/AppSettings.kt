package sb.linux.client.data

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

data class AiConfigPreset(
    val name: String,
    val url: String,
    val key: String,
    val model: String,
    val temperature: Float,
    val prompt: String,
    val includeComments: Boolean,
)

data class CardRedemptionRecord(
    val topicId: Long,
    val topicTitle: String,
    val cardTitle: String,
    val code: String,
    val price: String,
    val sourceTime: String,
    val recordedAt: Long,
)

data class UsageEvent(
    val type: String,
    val topicId: Long,
    val title: String,
    val value: Int,
    val at: Long,
)

/**
 * 应用设置（区别于源站个人设置）：
 * 滚动模式 / 快速点赞 / 屏蔽词 / AI 总结 / 浏览历史 / 签到摘要缓存。
 */
class AppSettings(context: Context) {
    private val aiRecords = context.getSharedPreferences("lsb_ai_summaries", Context.MODE_PRIVATE)
    private val drafts = context.getSharedPreferences("lsb_topic_drafts", Context.MODE_PRIVATE)
    private val readingRecords = context.getSharedPreferences("lsb_reading_positions", Context.MODE_PRIVATE)

    private val prefs: SharedPreferences =
        context.getSharedPreferences("lsb_app_settings", Context.MODE_PRIVATE)

    /** 主题/字体等外观偏好（与 Session 共用的 lsb_prefs） */
    private val themePrefs: SharedPreferences =
        context.getSharedPreferences("lsb_prefs", Context.MODE_PRIVATE)

    // ---------------- 偏好读写 ----------------

    /** 首页滚动模式：true = 无限滚动，false = 翻页 */
    var infiniteScroll: Boolean
        get() = prefs.getBoolean("infinite_scroll", true)
        set(v) = prefs.edit().putBoolean("infinite_scroll", v).apply()

    /** 打开应用时不自动刷新首页，直接显示缓存内容 */
    var homeKeepCache: Boolean
        get() = prefs.getBoolean("home_keep_cache", false)
        set(v) = prefs.edit().putBoolean("home_keep_cache", v).apply()

    /** 刷新首页时把新增帖子折叠为顶部提示，不改变当前阅读位置。 */
    var collapseNewTopics: Boolean
        get() = prefs.getBoolean("collapse_new_topics", false)
        set(v) = prefs.edit().putBoolean("collapse_new_topics", v).apply()

    /** 帖子内打赏弹幕开关 */
    var danmakuOn: Boolean
        get() = prefs.getBoolean("danmaku_on", true)
        set(v) = prefs.edit().putBoolean("danmaku_on", v).apply()

    /** 评论「对话串联」(#N 引用链弹窗) 开关：源站已支持树形结构评论
     *  (data-quote-threads-parent-floor)，应用改为直接展示回复目标楼层，
     *  此功能默认关闭、仅作兼容保留 */
    var threadDialogEnabled: Boolean
        get() = prefs.getBoolean("thread_dialog_enabled", false)
        set(v) = prefs.edit().putBoolean("thread_dialog_enabled", v).apply()

    /** 取消收藏前弹确认框：关闭后已收藏帖子点击收藏图标直接取消收藏，不再提示 */
    var unfavoriteConfirm: Boolean
        get() = prefs.getBoolean("unfavorite_confirm", true)
        set(v) = prefs.edit().putBoolean("unfavorite_confirm", v).apply()

    /** 侧边栏双列显示（作用于快捷功能与版块列表） */
    var sidebarTwoColumns: Boolean
        get() = prefs.getBoolean("sidebar_two_columns", true)
        set(v) = prefs.edit().putBoolean("sidebar_two_columns", v).apply()

    /** 侧边栏展示源站在线用户与人数 */
    var sidebarShowOnlineUsers: Boolean
        get() = prefs.getBoolean("sidebar_show_online_users", true)
        set(v) = prefs.edit().putBoolean("sidebar_show_online_users", v).apply()

    /** 翻页模式下每页展示的帖子条数（默认 15） */
    var topicsPerPage: Int
        get() = prefs.getInt("topics_per_page", 15)
        set(v) = prefs.edit().putInt("topics_per_page", v.coerceIn(5, 50)).apply()

    /** 评论区排序：0 = 热度，1 = 正序，2 = 倒序（记住上次选择） */
    var commentSortOrder: Int
        get() = prefs.getInt("comment_sort_order", 1)
        set(v) = prefs.edit().putInt("comment_sort_order", v).apply()

    /** 评论翻页模式下每页展示的评论数量（默认 20，3.11） */
    var commentsPerPage: Int
        get() = prefs.getInt("comments_per_page", 20)
        set(v) = prefs.edit().putInt("comments_per_page", v.coerceIn(5, 100)).apply()

    /** Markdown 表格展示：0 = 自适应屏幕等分列，1 = 保持可读列宽并横向滑动 */
    var tableDisplayMode: Int
        get() = prefs.getInt("table_display_mode", 0).coerceIn(0, 1)
        set(v) = prefs.edit().putInt("table_display_mode", v.coerceIn(0, 1)).apply()

    /** 帖内头像下方显示纯数字 UID */
    var showUid: Boolean
        get() = prefs.getBoolean("show_uid", true)
        set(v) = prefs.edit().putBoolean("show_uid", v).apply()

    var smartDecodeEnabled: Boolean
        get() = prefs.getBoolean("smart_decode_enabled", false)
        set(v) = prefs.edit().putBoolean("smart_decode_enabled", v).apply()

    var autoClearItems: Set<String>
        get() = prefs.getStringSet("auto_clear_items", emptySet())?.toSet() ?: emptySet()
        set(v) = prefs.edit().putStringSet("auto_clear_items", v).apply()

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

    var linkPreviewEnabled: Boolean
        get() = prefs.getBoolean("link_preview_enabled", true)
        set(v) = prefs.edit().putBoolean("link_preview_enabled", v).apply()

    /** DNS over HTTPS。VPN 存在时可自动旁路，防止与 VPN 自身 DNS 策略冲突。 */
    var dohEnabled: Boolean
        get() = prefs.getBoolean("doh_enabled", false)
        set(v) = prefs.edit().putBoolean("doh_enabled", v).apply()

    var dohUrl: String
        get() = prefs.getString("doh_url", null) ?: DohServers.defaults.first().url
        set(v) = prefs.edit().putString("doh_url", v.trim()).apply()

    fun dohServers(): List<DohServer> {
        val saved = runCatching {
            val array = JSONArray(prefs.getString("doh_servers", "[]"))
            (0 until array.length()).map { array.getJSONObject(it) }.map {
                DohServer(it.getString("id"), it.getString("name"), it.getString("url"), it.optString("note"))
            }
        }.getOrDefault(emptyList())
        return DohServers.merge(saved, dohUrl)
    }

    fun saveDohServers(servers: List<DohServer>) {
        val array = JSONArray()
        servers.forEach { server ->
            require(server.name.isNotBlank() && AppNetwork.isValidEndpoint(server.url))
            array.put(JSONObject().put("id", server.id).put("name", server.name.trim()).put("url", server.url.trim()).put("note", server.note.trim()))
        }
        prefs.edit().putString("doh_servers", array.toString()).apply()
    }

    var dohDisableOnVpn: Boolean
        get() = prefs.getBoolean("doh_disable_on_vpn", true)
        set(v) = prefs.edit().putBoolean("doh_disable_on_vpn", v).apply()

    /** 默认使用应用内置的 Catbox 匿名上传；关闭后才读取下面的自定义接口配置。 */
    var useBuiltInImageHost: Boolean
        get() = prefs.getBoolean("use_builtin_image_host", true)
        set(v) = prefs.edit().putBoolean("use_builtin_image_host", v).apply()

    var imageHostUrl: String
        get() = prefs.getString("image_host_url", "") ?: ""
        set(v) = prefs.edit().putString("image_host_url", v).apply()

    var imageHostToken: String
        get() = prefs.getString("image_host_token", "") ?: ""
        set(v) = prefs.edit().putString("image_host_token", v).apply()

    var imageHostField: String
        get() = prefs.getString("image_host_field", "file") ?: "file"
        set(v) = prefs.edit().putString("image_host_field", v.ifBlank { "file" }).apply()

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

    /** 平板模式：宽屏设备启用左右双栏布局（导航栏居左，主页/设置 1/3 + 详情 2/3） */
    var tabletMode: Boolean
        get() = prefs.getBoolean("tablet_mode", false)
        set(v) = prefs.edit().putBoolean("tablet_mode", v).apply()

    /** 底栏样式：0 = 经典（通栏 NavigationBar），1 = 液态玻璃悬浮胶囊。
     *  读取时夹取到 0..1：原「贴底通栏」铺满样式（2）已移除，旧值自动回落到悬浮胶囊 */
    var bottomBarStyle: Int
        get() = prefs.getInt("bottom_bar_style", 0).coerceIn(0, 1)
        set(v) = prefs.edit().putInt("bottom_bar_style", v.coerceIn(0, 1)).apply()

    /** 主底栏项目及顺序；至少保留两项，旧版默认为首页+我的。 */
    var bottomBarItems: List<String>
        get() {
            val allowed = setOf("home", "topicCollections", "directMessages", "newTopic", "me")
            val value = (prefs.getString("bottom_bar_items", "home,topicCollections,directMessages,me") ?: "")
                .split(',').map { if (it.trim() == "forums") "topicCollections" else it.trim() }.filter { it in allowed }.distinct()
            val valid = value.takeIf { it.size >= 2 } ?: listOf("home", "topicCollections", "directMessages", "me")
            if (prefs.getInt("bottom_navigation_version", 0) < 2) {
                val migrated = valid.toMutableList()
                if ("topicCollections" !in migrated) migrated.add((migrated.indexOf("me").takeIf { it >= 0 } ?: migrated.size), "topicCollections")
                if ("directMessages" !in migrated) migrated.add((migrated.indexOf("me").takeIf { it >= 0 } ?: migrated.size), "directMessages")
                prefs.edit().putString("bottom_bar_items", migrated.joinToString(",")).putInt("bottom_navigation_version", 2).apply()
                return migrated
            }
            return valid
        }
        set(v) {
            val allowed = setOf("home", "topicCollections", "directMessages", "newTopic", "me")
            val clean = v.filter { it in allowed }.distinct().takeIf { it.size >= 2 } ?: listOf("home", "me")
            prefs.edit().putString("bottom_bar_items", clean.joinToString(",")).putInt("bottom_navigation_version", 2).apply()
        }

    /** 评论底栏样式（帖子内底部快捷回复栏）：0 = 经典（通栏 Surface），
     *  1 = 液态玻璃悬浮胶囊（实时折射身后滚动内容） */
    var replyBarStyle: Int
        get() = prefs.getInt("reply_bar_style", 0).coerceIn(0, 1)
        set(v) = prefs.edit().putInt("reply_bar_style", v.coerceIn(0, 1)).apply()

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

    /** 本地导出与 WebDAV 共用的备份范围。 */
    var backupItems: Set<String>
        get() {
            val allowed = setOf("settings", "history", "favorites", "ai", "usage", "cards", "drafts")
            return (prefs.getStringSet("backup_items", allowed) ?: allowed).filter { it in allowed }.toSet().ifEmpty { allowed }
        }
        set(v) {
            val allowed = setOf("settings", "history", "favorites", "ai", "usage", "cards", "drafts")
            prefs.edit().putStringSet("backup_items", v.filter { it in allowed }.toSet().ifEmpty { setOf("settings") }).apply()
        }

    // ---------------- 分类重置（3.9） ----------------

    /** 重置浏览设置 */
    fun resetBrowse() = prefs.edit()
        .remove("infinite_scroll").remove("home_keep_cache").remove("collapse_new_topics").remove("danmaku_on")
        .remove("home_topic_preview_enabled")
        .remove("sidebar_two_columns").remove("sidebar_show_online_users").remove("topics_per_page")
        .remove("comment_sort_order").remove("comments_per_page")
        .remove("table_display_mode")
        .remove("show_uid")
        .remove("smart_decode_enabled")
        .remove("selection_menu_items")
        .remove("scroll_mode_overrides").remove("home_sort_drawer_open")
        .remove("unfavorite_confirm")
        .apply()

    /** 重置 AI 设置 */
    fun resetAi() = prefs.edit()
        .remove("ai_auto").remove("ai_auto_run").remove("ai_url").remove("ai_key")
        .remove("ai_model").remove("ai_prompt").remove("ai_temp").remove("ai_include_comments")
        .remove("ai_config_presets")
        .apply()

    /** 重置常规设置 */
    fun resetGeneral() = prefs.edit()
        .remove("link_open_mode").remove("update_check_mode")
        .remove("link_preview_enabled")
        .remove("doh_enabled").remove("doh_url").remove("doh_disable_on_vpn")
        .remove("doh_servers")
        .remove("use_builtin_image_host")
        .remove("image_host_url").remove("image_host_token").remove("image_host_field")
        .remove("update_check_interval_hours")
        .apply()

    /** 重置 WebDAV 备份配置 */
    fun resetWebdav() = prefs.edit()
        .remove("webdav_url").remove("webdav_user").remove("webdav_pass").remove("webdav_dir")
        .apply()

    /**
     * AI 总结总开关：关闭后帖子内不显示 AI 入口。
     * 键名沿用历史 ai_auto（当初这个开关兼任「自动总结」），改名会让老用户/旧备份丢配置。
     */
    var aiEnabled: Boolean
        get() = prefs.getBoolean("ai_auto", false)
        set(v) = prefs.edit().putBoolean("ai_auto", v).apply()

    /**
     * 打开帖子自动请求总结。默认关闭——水贴没有总结价值，还白烧 token，
     * 改为帖子内点按钮手动触发（结果按帖缓存，同一帖不会重复请求）。
     */
    var aiAutoRun: Boolean
        get() = prefs.getBoolean("ai_auto_run", false)
        set(v) = prefs.edit().putBoolean("ai_auto_run", v).apply()

    var aiUrl: String
        get() = prefs.getString("ai_url", "") ?: ""
        set(v) = prefs.edit().putString("ai_url", v).apply()

    var aiKey: String
        get() = prefs.getString("ai_key", "") ?: ""
        set(v) = prefs.edit().putString("ai_key", v).apply()

    var aiModel: String
        get() = prefs.getString("ai_model", "gpt-4o-mini") ?: ""
        set(v) = prefs.edit().putString("ai_model", v).apply()

    var aiPrompt: String
        get() = prefs.getString("ai_prompt", "") ?: ""
        set(v) = prefs.edit().putString("ai_prompt", v).apply()

    fun aiConfigPresets(): List<AiConfigPreset> = runCatching {
        val arr = JSONArray(prefs.getString("ai_config_presets", "[]") ?: "[]")
        (0 until arr.length()).mapNotNull { i ->
            val o = arr.optJSONObject(i) ?: return@mapNotNull null
            val name = o.optString("name").trim()
            if (name.isBlank()) return@mapNotNull null
            AiConfigPreset(
                name, o.optString("url"), o.optString("key"), o.optString("model"),
                o.optDouble("temperature", 0.3).toFloat(), o.optString("prompt"),
                o.optBoolean("includeComments", true),
            )
        }
    }.getOrDefault(emptyList())

    fun saveCurrentAiPreset(name: String) {
        val clean = name.trim()
        if (clean.isBlank()) return
        val all = aiConfigPresets().filterNot { it.name == clean } + AiConfigPreset(
            clean, aiUrl, aiKey, aiModel, aiTemp, aiPrompt, aiIncludeComments,
        )
        val arr = JSONArray()
        all.forEach { p ->
            arr.put(JSONObject().put("name", p.name).put("url", p.url).put("key", p.key)
                .put("model", p.model).put("temperature", p.temperature.toDouble())
                .put("prompt", p.prompt).put("includeComments", p.includeComments))
        }
        prefs.edit().putString("ai_config_presets", arr.toString()).apply()
    }

    fun applyAiPreset(preset: AiConfigPreset) {
        aiUrl = preset.url; aiKey = preset.key; aiModel = preset.model
        aiTemp = preset.temperature; aiPrompt = preset.prompt
        aiIncludeComments = preset.includeComments
    }

    fun removeAiPreset(name: String) {
        val keep = aiConfigPresets().filterNot { it.name == name }
        val arr = JSONArray()
        keep.forEach { p ->
            arr.put(JSONObject().put("name", p.name).put("url", p.url).put("key", p.key)
                .put("model", p.model).put("temperature", p.temperature.toDouble())
                .put("prompt", p.prompt).put("includeComments", p.includeComments))
        }
        prefs.edit().putString("ai_config_presets", arr.toString()).apply()
    }

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

    /** 清除签到缓存（切换账号时调用，避免显示旧账号的签到信息） */
    fun clearCheckinCache() {
        prefs.edit()
            .remove("last_checkin")
            .remove("last_checkin_date")
            .remove("last_checkin_streak")
            .remove("last_checkin_total")
            .apply()
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

    // ---------------- 发卡兑换自动记录 ----------------

    private fun cardRedemptionArray(): JSONArray = runCatching {
        JSONArray(prefs.getString("card_redemptions", "[]") ?: "[]")
    }.getOrDefault(JSONArray())

    /** 从帖子“我的购买记录”同步本机档案。按帖子 + 兑换码去重，最新发现的记录置顶。 */
    fun recordCardRedemptions(topicId: Long, topicTitle: String, card: VirtualCard?) {
        if (topicId <= 0 || card == null || card.orders.isEmpty()) return
        val old = cardRedemptionArray()
        val incomingKeys = card.orders.map { "$topicId\u0000${it.code}" }.toSet()
        val out = JSONArray()
        val now = System.currentTimeMillis()
        card.orders.forEach { order ->
            out.put(
                JSONObject()
                    .put("topicId", topicId).put("topicTitle", topicTitle)
                    .put("cardTitle", card.title).put("code", order.code)
                    .put("price", order.price).put("sourceTime", order.time)
                    .put("recordedAt", now)
            )
        }
        for (i in 0 until old.length()) {
            val item = old.optJSONObject(i) ?: continue
            val key = "${item.optLong("topicId")}\u0000${item.optString("code")}"
            if (key !in incomingKeys) out.put(item)
        }
        while (out.length() > 500) out.remove(out.length() - 1)
        prefs.edit().putString("card_redemptions", out.toString()).apply()
    }

    fun cardRedemptions(): List<CardRedemptionRecord> {
        val arr = cardRedemptionArray()
        return buildList {
            for (i in 0 until arr.length()) {
                val item = arr.optJSONObject(i) ?: continue
                add(
                    CardRedemptionRecord(
                        topicId = item.optLong("topicId"),
                        topicTitle = item.optString("topicTitle"),
                        cardTitle = item.optString("cardTitle"),
                        code = item.optString("code"),
                        price = item.optString("price"),
                        sourceTime = item.optString("sourceTime"),
                        recordedAt = item.optLong("recordedAt"),
                    )
                )
            }
        }
    }

    fun clearCardRedemptions() = prefs.edit().remove("card_redemptions").apply()

    // ---------------- 使用统计 ----------------

    private fun usageEventArray(): JSONArray = runCatching {
        JSONArray(prefs.getString("usage_events", "[]") ?: "[]")
    }.getOrDefault(JSONArray())

    /** 仅记录应用内实际发生的操作；最多保留 2000 条，避免统计数据无限增长。 */
    fun recordUsageEvent(type: String, topicId: Long = 0, title: String = "", value: Int = 0) {
        if (type !in setOf("view", "favorite", "topic", "reply", "coin", "donate")) return
        val old = usageEventArray()
        val out = JSONArray().put(
            JSONObject().put("type", type).put("topicId", topicId).put("title", title)
                .put("value", value).put("at", System.currentTimeMillis())
        )
        for (i in 0 until old.length().coerceAtMost(1999)) old.optJSONObject(i)?.let(out::put)
        prefs.edit().putString("usage_events", out.toString()).apply()
    }

    fun usageEvents(): List<UsageEvent> {
        val arr = usageEventArray()
        return buildList {
            for (i in 0 until arr.length()) {
                val item = arr.optJSONObject(i) ?: continue
                add(
                    UsageEvent(
                        type = item.optString("type"), topicId = item.optLong("topicId"),
                        title = item.optString("title"), value = item.optInt("value"),
                        at = item.optLong("at"),
                    )
                )
            }
        }
    }

    var usageAiSummary: String
        get() = prefs.getString("usage_ai_summary", "") ?: ""
        set(v) = prefs.edit().putString("usage_ai_summary", v).apply()

    var sourceUsageJson: String
        get() = prefs.getString("source_usage", "{}") ?: "{}"
        set(value) = prefs.edit().putString("source_usage", value).apply()
    var builtInImageHostProvider: String
        get() = prefs.getString("builtin_image_provider", "pixhost") ?: "pixhost"
        set(value) = prefs.edit().putString("builtin_image_provider", value).apply()

    var confirmGachaPull: Boolean
        get() = prefs.getBoolean("confirm_gacha_pull", true)
        set(value) = prefs.edit().putBoolean("confirm_gacha_pull", value).apply()

    fun saveSourcePoints(rows: List<PointRow>) {
        val array = JSONArray()
        rows.forEach { row -> array.put(JSONObject().put("time", row.timeText).put("reason", row.reason)
            .put("change", row.change).put("positive", row.positive).put("timestamp", row.timestamp).put("topicId", row.topicId)) }
        prefs.edit().putString("source_points", array.toString()).putLong("source_points_synced_at", System.currentTimeMillis()).apply()
    }

    fun sourcePoints(): List<PointRow> = runCatching {
        val array = JSONArray(prefs.getString("source_points", "[]"))
        (0 until array.length()).map { array.getJSONObject(it) }.map {
            PointRow(it.optString("time"), it.optString("reason"), it.optString("change"), it.optBoolean("positive"), it.optLong("timestamp"), it.optLong("topicId"))
        }
    }.getOrDefault(emptyList())

    val sourcePointsSyncedAt: Long get() = prefs.getLong("source_points_synced_at", 0L)

    fun clearUsageStats() = prefs.edit().remove("usage_events").remove("usage_ai_summary")
        .remove("source_points").remove("source_points_synced_at").remove("source_usage").apply()

    // ---------------- 本地收藏内容（收藏时本地快照，不额外访问源站） ----------------

    private fun favoriteArray(): JSONArray =
        runCatching { JSONArray(prefs.getString("local_favorites", "[]") ?: "[]") }.getOrDefault(JSONArray())

    /** 收藏一个帖子：同帖去重置顶，最多保留 500 条 */
    fun addFavorite(
        topicId: Long, title: String, authorId: Long, authorName: String,
        forumId: Long, forumName: String, avatarUrl: String, titleColor: String,
    ) {
        if (topicId <= 0) return
        val old = favoriteArray()
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
        while (out.length() > 500) out.remove(out.length() - 1)
        prefs.edit().putString("local_favorites", out.toString()).apply()
    }

    /** 本地收藏内容（最新收藏在前；源站同步来的条目沿用源站时间/回复数） */
    fun favoriteList(): List<sb.linux.client.data.TopicCard> {
        val arr = favoriteArray()
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
                        replies = o.optInt("replies", 0),
                        lastReplier = "",
                        timeText = if (at > 0) fmt.format(java.util.Date(at)) else o.optString("timeText"),
                        avatarUrl = o.optString("avatarUrl"),
                        titleColor = o.optString("titleColor"),
                    )
                )
            }
        }
    }

    /** 合并源站收藏（用户页 tab=favorites 解析结果）：本地没有的追加到末尾，已有的不覆盖 */
    fun mergeFavorites(cards: List<TopicCard>) {
        if (cards.isEmpty()) return
        val old = favoriteArray()
        val have = buildSet {
            for (i in 0 until old.length()) add(old.optJSONObject(i)?.optLong("topicId") ?: -1L)
        }
        val out = JSONArray()
        for (i in 0 until old.length()) out.put(old.optJSONObject(i))
        cards.forEach { c ->
            if (c.topicId > 0 && c.topicId !in have) {
                out.put(
                    JSONObject()
                        .put("topicId", c.topicId).put("title", c.title)
                        .put("authorId", c.authorId).put("authorName", c.authorName)
                        .put("forumId", c.forumId).put("forumName", c.forumName)
                        .put("avatarUrl", c.avatarUrl).put("titleColor", c.titleColor)
                        .put("at", 0L)
                        .put("timeText", c.timeText)
                        .put("replies", c.replies)
                )
            }
        }
        prefs.edit().putString("local_favorites", out.toString()).apply()
    }

    fun removeFavorite(topicId: Long) {
        val old = favoriteArray()
        val out = JSONArray()
        for (i in 0 until old.length()) {
            val o = old.optJSONObject(i) ?: continue
            if (o.optLong("topicId") != topicId) out.put(o)
        }
        prefs.edit().putString("local_favorites", out.toString()).apply()
    }

    fun clearFavorites() = prefs.edit().remove("local_favorites").apply()

    // ---------------- 本地收藏评论（纯本地快照，34） ----------------

    private fun commentFavArray(): JSONArray =
        runCatching { JSONArray(prefs.getString("local_comment_favorites", "[]") ?: "[]") }.getOrDefault(JSONArray())

    /** 收藏一条评论：同条去重置顶，最多保留 500 条 */
    fun addCommentFavorite(
        replyId: Long, topicId: Long, topicTitle: String, floor: Int,
        authorId: Long, authorName: String, avatarUrl: String, content: String,
    ) {
        if (replyId <= 0 || topicId <= 0) return
        val old = commentFavArray()
        val out = JSONArray()
        out.put(
            JSONObject()
                .put("replyId", replyId).put("topicId", topicId)
                .put("topicTitle", topicTitle).put("floor", floor)
                .put("authorId", authorId).put("authorName", authorName)
                .put("avatarUrl", avatarUrl).put("content", content)
                .put("at", System.currentTimeMillis())
        )
        for (i in 0 until old.length()) {
            val o = old.optJSONObject(i) ?: continue
            if (o.optLong("replyId") == replyId) continue
            out.put(o)
        }
        while (out.length() > 500) out.remove(out.length() - 1)
        prefs.edit().putString("local_comment_favorites", out.toString()).apply()
    }

    fun commentFavoriteList(): List<CommentFavorite> {
        val arr = commentFavArray()
        val fmt = java.text.SimpleDateFormat("MM-dd HH:mm", java.util.Locale.getDefault())
        return buildList {
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                val at = o.optLong("at", 0L)
                add(
                    CommentFavorite(
                        replyId = o.optLong("replyId"),
                        topicId = o.optLong("topicId"),
                        topicTitle = o.optString("topicTitle"),
                        floor = o.optInt("floor", 0),
                        authorId = o.optLong("authorId"),
                        authorName = o.optString("authorName"),
                        avatarUrl = o.optString("avatarUrl"),
                        content = o.optString("content"),
                        at = at,
                        timeText = if (at > 0) fmt.format(java.util.Date(at)) else "",
                    )
                )
            }
        }
    }

    fun removeCommentFavorite(replyId: Long) {
        val old = commentFavArray()
        val out = JSONArray()
        for (i in 0 until old.length()) {
            val o = old.optJSONObject(i) ?: continue
            if (o.optLong("replyId") != replyId) out.put(o)
        }
        prefs.edit().putString("local_comment_favorites", out.toString()).apply()
    }

    fun clearCommentFavorites() = prefs.edit().remove("local_comment_favorites").apply()

    // ---------------- 本地聚合私信（收信来自通知，去信为发送记录；纯本地，不额外访问源站） ----------------

    private fun pmArray(): JSONArray =
        runCatching { JSONArray(prefs.getString("local_pm_messages", "[]") ?: "[]") }.getOrDefault(JSONArray())

    /** 追加一条私信消息，仅保留最近 300 条。
     *  - 收到的私信按「对方+内容」去重（通知页反复拉全量），命中回填资料并刷新天级 ts；
     *    兼容旧版把引用拼进正文的拼法（"引用 正文"），命中时迁移为分离后的干净正文。
     *  - synthetic（依据对方回复引用补造的我方消息）：已存在同内容则不再补。
     *  - App 内真实发送的消息不去重，总是追加。
     *  排序规则见 pmThread：先按 ts 自然日分组，同一天内按 seq。 */
    fun addPmMessage(
        partnerId: Long, partnerName: String, avatarUrl: String,
        incoming: Boolean, content: String, ts: Long = 0L,
        quoteText: String = "", synthetic: Boolean = false,
    ) {
        if (partnerId <= 0) return
        val t = if (ts > 0) ts else System.currentTimeMillis()
        val old = pmArray()
        // 新 seq 必须大于所有已存 seq，也要大于旧记录的读取兜底值（数组长度，见 pmThread）
        var maxSeq = old.length().toLong()
        for (i in 0 until old.length()) {
            val o = old.optJSONObject(i) ?: continue
            maxSeq = maxOf(maxSeq, o.optLong("seq", 0L))
        }
        // 旧版正文 = "引用 + 空格 + 正文"，迁移期去重同时匹配该拼法
        val legacyContent = if (quoteText.isNotBlank()) "$quoteText $content" else ""
        val rest = JSONArray()
        var dedupHit = false       // 收到的消息命中去重（回填后不追加）
        var syntheticExists = false// 补造的我方消息已存在（或真发过同内容）则不再补
        for (i in 0 until old.length()) {
            val o = old.optJSONObject(i) ?: continue
            val samePartner = o.optLong("partnerId") == partnerId
            val stored = o.optString("content")
            if (incoming && !dedupHit && samePartner &&
                (stored == content || (legacyContent.isNotBlank() && stored == legacyContent))
            ) {
                rest.put(
                    JSONObject(o.toString())
                        .put("ts", t)
                        .put("content", content)
                        .put("quote", quoteText)
                        .put("partnerName", partnerName.ifBlank { o.optString("partnerName") })
                        .put("avatarUrl", avatarUrl.ifBlank { o.optString("avatarUrl") })
                )
                dedupHit = true
            } else {
                if (synthetic && !syntheticExists && samePartner && stored == content) syntheticExists = true
                rest.put(o)
            }
        }
        val out = JSONArray()
        if (!dedupHit && !syntheticExists) {
            out.put(
                JSONObject()
                    .put("partnerId", partnerId).put("partnerName", partnerName)
                    .put("avatarUrl", avatarUrl).put("incoming", incoming)
                    .put("content", content).put("ts", t)
                    .put("seq", maxSeq + 1L)
                    .put("quote", quoteText)
                    .put("synthetic", synthetic)
                    // App 内发送的有精确时间；通知解析与引用补造的只有天级
                    .put("timeExact", !incoming && !synthetic)
                    .put("read", incoming)   // 我发出的始终视为已读；对方发来的以此作为标记（markPmRead 会翻成 true）
            )
        }
        for (i in 0 until rest.length()) {
            if (out.length() >= 300) break
            out.put(rest.optJSONObject(i))
        }
        prefs.edit().putString("local_pm_messages", out.toString()).apply()
    }

    /** 与某人的会话里是否已有指定内容的消息（判重：决定是否按引用补造我方消息） */
    fun pmHasContent(partnerId: Long, content: String): Boolean {
        if (content.isBlank()) return false
        val arr = pmArray()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            if (o.optLong("partnerId") == partnerId && o.optString("content") == content) return true
        }
        return false
    }

    /** 时间戳 → 聊天式时间（QQ/微信风格，固定不随查看时刻漂移）：
     *  精确（我发出的）：今天 HH:mm / 昨天 HH:mm / 今年 MM-dd HH:mm / 跨年 yyyy-MM-dd HH:mm
     *  天级（通知解析的只有"昨天/2天前"）：今天 / 昨天 / MM-dd / yyyy-MM-dd，不编造时刻 */
    private fun relativeTimeText(ts: Long, exact: Boolean): String {
        if (ts <= 0) return ""
        val cal = java.util.Calendar.getInstance()
        val now = java.util.Calendar.getInstance()
        cal.timeInMillis = ts
        val sameDay = cal.get(java.util.Calendar.YEAR) == now.get(java.util.Calendar.YEAR) &&
            cal.get(java.util.Calendar.DAY_OF_YEAR) == now.get(java.util.Calendar.DAY_OF_YEAR)
        val yesterday = run {
            val y = java.util.Calendar.getInstance()
            y.add(java.util.Calendar.DAY_OF_YEAR, -1)
            cal.get(java.util.Calendar.YEAR) == y.get(java.util.Calendar.YEAR) &&
                cal.get(java.util.Calendar.DAY_OF_YEAR) == y.get(java.util.Calendar.DAY_OF_YEAR)
        }
        val sameYear = cal.get(java.util.Calendar.YEAR) == now.get(java.util.Calendar.YEAR)
        return when {
            !exact -> when {
                sameDay -> "今天"
                yesterday -> "昨天"
                sameYear -> java.text.SimpleDateFormat("MM-dd", java.util.Locale.getDefault()).format(java.util.Date(ts))
                else -> java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date(ts))
            }
            sameDay -> java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date(ts))
            yesterday -> "昨天 " + java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date(ts))
            sameYear -> java.text.SimpleDateFormat("MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date(ts))
            else -> java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date(ts))
        }
    }

    /** ts 的自然日键（同年同日相等），供线程按天分组 */
    private fun dayKey(ts: Long): Long {
        val c = java.util.Calendar.getInstance()
        c.timeInMillis = ts
        return c.get(java.util.Calendar.YEAR).toLong() * 1000L + c.get(java.util.Calendar.DAY_OF_YEAR)
    }

    /** 与某人的私信线程（时间正序，用于聊天界面）：
     *  先按 ts 自然日分组，同一天内按 seq（写入顺序）。通知只提供天级时间，
     *  同日先后无法从时间戳分出，靠写入序保证稳定；旧记录无 seq 时按数组位置兜底
     *  （数组最新在前，位置越靠后越早 → 兜底 seq = length - index 保持旧数据先后）。 */
    fun pmThread(partnerId: Long): List<sb.linux.client.data.PmMessage> {
        val arr = pmArray()
        val len = arr.length()
        return buildList {
            for (i in 0 until len) {
                val o = arr.optJSONObject(i) ?: continue
                if (o.optLong("partnerId") != partnerId) continue
                val ts = o.optLong("ts", 0L)
                val seq = if (o.has("seq")) o.optLong("seq") else (len - i).toLong()
                val exact = o.optBoolean("timeExact", !o.optBoolean("incoming"))
                add(
                    sb.linux.client.data.PmMessage(
                        partnerId = partnerId,
                        partnerName = o.optString("partnerName"),
                        avatarUrl = o.optString("avatarUrl"),
                        incoming = o.optBoolean("incoming"),
                        content = o.optString("content"),
                        timeText = relativeTimeText(ts, exact),
                        ts = ts,
                        seq = seq,
                        timeExact = exact,
                        quoteText = o.optString("quote"),
                        synthetic = o.optBoolean("synthetic"),
                    )
                )
            }
        }.sortedWith(compareBy({ dayKey(it.ts) }, { it.seq }))
    }

    /** 源站完整线程覆盖该联系人本地快照，保留其他会话；避免同文消息被错误去重。 */
    fun savePmThread(thread: DirectMessageThread) {
        val out = JSONArray()
        val old = pmArray()
        for (i in 0 until old.length()) old.optJSONObject(i)?.takeIf { it.optLong("partnerId") != thread.partnerId }?.let(out::put)
        thread.messages.forEachIndexed { index, message ->
            out.put(JSONObject().put("partnerId", thread.partnerId).put("partnerName", thread.partnerName)
                .put("avatarUrl", thread.avatarUrl).put("incoming", message.incoming).put("content", message.content)
                .put("ts", message.ts).put("seq", index.toLong()).put("timeExact", true).put("read", true)
                .put("quote", message.quoteText))
        }
        prefs.edit().putString("local_pm_messages", out.toString()).apply()
    }

    /** 把某人的未读私信标记为已读 */
    fun markPmRead(partnerId: Long) {
        val arr = pmArray()
        val out = JSONArray()
        for (i in 0 until arr.length()) {
            val o = JSONObject(arr.optJSONObject(i).toString())
            if (o.optLong("partnerId") == partnerId && !o.optBoolean("read", false)) o.put("read", true)
            out.put(o)
        }
        prefs.edit().putString("local_pm_messages", out.toString()).apply()
    }

    // ---------------- 设置导出 / 导入 ----------------

    fun exportJson(): String {
        val o = JSONObject()
        o.put("ai_summaries", JSONObject(aiRecords.all))
        o.put("drafts", JSONObject(drafts.all))
        o.put("reading_positions", JSONObject(readingRecords.all))
        o.put("infinite_scroll", infiniteScroll)
        o.put("danmaku_on", danmakuOn)
        o.put("sidebar_two_columns", sidebarTwoColumns)
        o.put("sidebar_show_online_users", sidebarShowOnlineUsers)
        o.put("home_keep_cache", homeKeepCache)
        o.put("collapse_new_topics", collapseNewTopics)
        o.put("topics_per_page", topicsPerPage)
        o.put("comment_sort_order", commentSortOrder)
        o.put("comments_per_page", commentsPerPage)
        o.put("table_display_mode", tableDisplayMode)
        o.put("show_uid", showUid)
        o.put("smart_decode_enabled", smartDecodeEnabled)
        o.put("auto_clear_items", JSONArray(autoClearItems.toList()))
        o.put("scroll_mode_overrides", JSONObject(scrollModeOverrides))
        o.put("home_sort_drawer_open", homeSortDrawerOpen)
        o.put("unfavorite_confirm", unfavoriteConfirm)
        o.put("link_open_mode", linkOpenMode)
        o.put("link_preview_enabled", linkPreviewEnabled)
        o.put("doh_enabled", dohEnabled)
        o.put("doh_url", dohUrl)
        o.put("doh_disable_on_vpn", dohDisableOnVpn)
        o.put("doh_servers", JSONArray().also { array -> dohServers().forEach { server ->
            array.put(JSONObject().put("id", server.id).put("name", server.name).put("url", server.url).put("note", server.note))
        } })
        o.put("use_builtin_image_host", useBuiltInImageHost)
        o.put("image_host_url", imageHostUrl)
        o.put("image_host_token", imageHostToken)
        o.put("image_host_field", imageHostField)
        o.put("update_check_mode", updateCheckMode)
        o.put("update_check_interval_hours", updateCheckIntervalHours)
        o.put("tablet_mode", tabletMode)
        o.put("bottom_bar_style", bottomBarStyle)
        o.put("bottom_bar_items", JSONArray(bottomBarItems))
        o.put("reply_bar_style", replyBarStyle)
        o.put("webdav_url", webdavUrl)
        o.put("webdav_user", webdavUser)
        o.put("webdav_pass", webdavPass)
        o.put("webdav_dir", webdavDir)
        o.put("ai_auto", aiEnabled)
        o.put("ai_auto_run", aiAutoRun)
        o.put("ai_url", aiUrl)
        o.put("ai_key", aiKey)
        o.put("ai_model", aiModel)
        o.put("ai_prompt", aiPrompt)
        o.put("ai_config_presets", JSONArray(prefs.getString("ai_config_presets", "[]") ?: "[]"))
        o.put("ai_temp", aiTemp.toDouble())
        o.put("ai_include_comments", aiIncludeComments)
        o.put("blocked_words", JSONArray(blockedWords))
        o.put("blocked_users", JSONArray(blockedUsers))
        // 26：补齐近期新增设置——对话串联开关与本地收藏的评论
        o.put("thread_dialog_enabled", threadDialogEnabled)
        o.put("comment_favorites", JSONArray(prefs.getString("local_comment_favorites", "[]") ?: "[]"))
        // 本地收藏的帖子与浏览历史：纯本地数据，源站不存，不导出则换机即丢
        o.put("favorites", JSONArray(prefs.getString("local_favorites", "[]") ?: "[]"))
        o.put("history", JSONArray(prefs.getString("local_history", "[]") ?: "[]"))
        o.put("card_redemptions", JSONArray(prefs.getString("card_redemptions", "[]") ?: "[]"))
        o.put("usage_events", JSONArray(prefs.getString("usage_events", "[]") ?: "[]"))
        o.put("usage_ai_summary", usageAiSummary)
        o.put("source_points", JSONArray(prefs.getString("source_points", "[]")))
        o.put("source_points_synced_at", sourcePointsSyncedAt)
        o.put("source_usage", sourceUsageJson)
        o.put("builtin_image_provider", builtInImageHostProvider)
        o.put("confirm_gacha_pull", confirmGachaPull)
        // 26：外观主题与字体（lsb_prefs 全量导出，导入后立即生效）
        val t = JSONObject()
        t.put("theme_mode", themePrefs.getInt("theme_mode", 0))
        t.put("dynamic_color", themePrefs.getBoolean("dynamic_color", true))
        t.put("theme_color", themePrefs.getString("theme_color", "default") ?: "default")
        t.put("oled_dark", themePrefs.getBoolean("oled_dark", false))
        t.put("theme_style", themePrefs.getInt("theme_style", 0))
        t.put("theme_contrast", themePrefs.getFloat("theme_contrast", 0f).toDouble())
        t.put("theme_primary_override", themePrefs.getString("theme_primary_override", "") ?: "")
        t.put("theme_secondary_override", themePrefs.getString("theme_secondary_override", "") ?: "")
        t.put("theme_tertiary_override", themePrefs.getString("theme_tertiary_override", "") ?: "")
        t.put("theme_background_override", themePrefs.getString("theme_background_override", "") ?: "")
        t.put("keep_background_color", themePrefs.getBoolean("keep_background_color", true))
        t.put("exact_theme_colors", themePrefs.getBoolean("exact_theme_colors", false))
        t.put("background_image", themePrefs.getString("background_image", ""))
        t.put("background_image_enabled", themePrefs.getBoolean("background_image_enabled", false))
        t.put("background_image_opacity", themePrefs.getFloat("background_image_opacity", 0.25f).toDouble())
        t.put("surface_opacity", themePrefs.getFloat("surface_opacity", 1f).toDouble())
        t.put(
            "theme_custom_colors",
            JSONArray((themePrefs.getStringSet("theme_custom_colors", emptySet()) ?: emptySet()).toList())
        )
        t.put("card_colors", themePrefs.getString("card_colors", "") ?: "")
        t.put("font_key", themePrefs.getString("font_key", "default") ?: "default")
        t.put("font_scale", themePrefs.getFloat("font_scale", 1f).toDouble())
        t.put("font_weight_level", themePrefs.getInt("font_weight_level", 0))
        // 自定义字体的显示名：不导出会导致导入后字体档位显示为「自定义」但名称空白
        // （字体文件本身在私有目录，无法随 JSON 一起搬走，导入端会自动回落默认字体）
        t.put("custom_font_name", themePrefs.getString("custom_font_name", "") ?: "")
        o.put("theme", t)
        return o.toString(2)
    }

    /** 按用户勾选的范围生成备份；本地文件与 WebDAV 使用同一份逻辑。 */
    fun exportJson(items: Set<String>): String {
        val full = JSONObject(exportJson())
        val dataKeys = mapOf(
            "history" to setOf("history", "reading_positions"),
            "drafts" to setOf("drafts"),
            "favorites" to setOf("favorites", "comment_favorites"),
            "cards" to setOf("card_redemptions"),
            "usage" to setOf("usage_events", "usage_ai_summary", "source_points", "source_points_synced_at", "source_usage"),
            "ai" to setOf("ai_auto", "ai_auto_run", "ai_url", "ai_key", "ai_model", "ai_prompt", "ai_config_presets", "ai_temp", "ai_include_comments", "ai_summaries"),
        )
        dataKeys.forEach { (group, keys) -> if (group !in items) keys.forEach(full::remove) }
        if ("settings" !in items) {
            val keep = dataKeys.filterKeys { it in items }.values.flatten().toSet()
            full.keys().asSequence().toList().filterNot { it in keep }.forEach(full::remove)
        }
        full.put("backup_scope", JSONArray(items.sorted()))
        return full.toString(2)
    }

    /** @return null 表示导入成功，否则为错误信息 */
    fun importJson(text: String): String? {
        return try {
            val o = JSONObject(text)
            // 先验证独立数据区再写入，缺少的区不触碰本地数据。
            val sections = listOf("ai_summaries" to aiRecords, "drafts" to drafts, "reading_positions" to readingRecords)
            sections.forEach { (name, _) -> if (o.has(name)) require(o.get(name) is JSONObject) }
            val p = prefs.edit()
            if (o.has("infinite_scroll")) p.putBoolean("infinite_scroll", o.getBoolean("infinite_scroll"))
            if (o.has("danmaku_on")) p.putBoolean("danmaku_on", o.getBoolean("danmaku_on"))
            if (o.has("sidebar_two_columns")) p.putBoolean("sidebar_two_columns", o.getBoolean("sidebar_two_columns"))
            if (o.has("sidebar_show_online_users")) p.putBoolean("sidebar_show_online_users", o.getBoolean("sidebar_show_online_users"))
            if (o.has("home_keep_cache")) p.putBoolean("home_keep_cache", o.getBoolean("home_keep_cache"))
            if (o.has("collapse_new_topics")) p.putBoolean("collapse_new_topics", o.getBoolean("collapse_new_topics"))
            if (o.has("topics_per_page")) p.putInt("topics_per_page", o.optInt("topics_per_page", 15).coerceIn(5, 50))
            if (o.has("comment_sort_order")) p.putInt("comment_sort_order", o.optInt("comment_sort_order", 0).coerceIn(0, 2))
            if (o.has("comments_per_page")) p.putInt("comments_per_page", o.optInt("comments_per_page", 20).coerceIn(5, 100))
            if (o.has("table_display_mode")) p.putInt("table_display_mode", o.optInt("table_display_mode", 0).coerceIn(0, 1))
            if (o.has("show_uid")) p.putBoolean("show_uid", o.getBoolean("show_uid"))
            if (o.has("smart_decode_enabled")) p.putBoolean("smart_decode_enabled", o.getBoolean("smart_decode_enabled"))
            if (o.has("auto_clear_items")) {
                val arr = o.getJSONArray("auto_clear_items")
                p.putStringSet("auto_clear_items", (0 until arr.length()).map { arr.getString(it) }.toSet())
            }
            if (o.has("scroll_mode_overrides")) p.putString("scroll_mode_overrides", o.getJSONObject("scroll_mode_overrides").toString())
            if (o.has("home_sort_drawer_open")) p.putBoolean("home_sort_drawer_open", o.getBoolean("home_sort_drawer_open"))
            if (o.has("unfavorite_confirm")) p.putBoolean("unfavorite_confirm", o.getBoolean("unfavorite_confirm"))
            if (o.has("link_open_mode")) p.putInt("link_open_mode", o.optInt("link_open_mode", 0).coerceIn(0, 1))
            if (o.has("link_preview_enabled")) p.putBoolean("link_preview_enabled", o.getBoolean("link_preview_enabled"))
            if (o.has("doh_enabled")) p.putBoolean("doh_enabled", o.getBoolean("doh_enabled"))
            if (o.has("doh_url")) p.putString("doh_url", o.getString("doh_url"))
            if (o.has("doh_disable_on_vpn")) p.putBoolean("doh_disable_on_vpn", o.getBoolean("doh_disable_on_vpn"))
            if (o.has("doh_servers")) {
                val servers = o.getJSONArray("doh_servers")
                for (index in 0 until servers.length()) {
                    val server = servers.getJSONObject(index)
                    require(server.getString("id").isNotBlank() && server.getString("name").isNotBlank() && AppNetwork.isValidEndpoint(server.getString("url")))
                }
                p.putString("doh_servers", servers.toString())
            }
            if (o.has("use_builtin_image_host")) p.putBoolean("use_builtin_image_host", o.getBoolean("use_builtin_image_host"))
            if (o.has("image_host_url")) p.putString("image_host_url", o.getString("image_host_url"))
            if (o.has("image_host_token")) p.putString("image_host_token", o.getString("image_host_token"))
            if (o.has("image_host_field")) p.putString("image_host_field", o.getString("image_host_field"))
            // 0 = 每次打开，1 = 按间隔，2 = 从不：上限必须是 2，夹到 1 会把用户
            // 选的「从不」在恢复备份后悄悄改成「按间隔」
            if (o.has("update_check_mode")) p.putInt("update_check_mode", o.optInt("update_check_mode", 1).coerceIn(0, 2))
            if (o.has("update_check_interval_hours")) p.putInt("update_check_interval_hours", o.optInt("update_check_interval_hours", 24).coerceIn(1, 24 * 30))
            if (o.has("tablet_mode")) p.putBoolean("tablet_mode", o.getBoolean("tablet_mode"))
            if (o.has("bottom_bar_style")) p.putInt("bottom_bar_style", o.optInt("bottom_bar_style", 1).coerceIn(0, 1))
            if (o.has("bottom_bar_items")) {
                val arr = o.getJSONArray("bottom_bar_items")
                val allowed = setOf("home", "forums", "topicCollections", "directMessages", "newTopic", "me")
                val items = (0 until arr.length()).map { arr.getString(it) }.filter { it in allowed }.distinct()
                if (items.size >= 2) p.putString("bottom_bar_items", items.joinToString(","))
            }
            if (o.has("reply_bar_style")) p.putInt("reply_bar_style", o.optInt("reply_bar_style", 1).coerceIn(0, 1))
            if (o.has("webdav_url")) p.putString("webdav_url", o.getString("webdav_url"))
            if (o.has("webdav_user")) p.putString("webdav_user", o.getString("webdav_user"))
            if (o.has("webdav_pass")) p.putString("webdav_pass", o.getString("webdav_pass"))
            if (o.has("webdav_dir")) p.putString("webdav_dir", o.getString("webdav_dir"))
            if (o.has("ai_auto")) p.putBoolean("ai_auto", o.getBoolean("ai_auto"))
            if (o.has("ai_auto_run")) p.putBoolean("ai_auto_run", o.getBoolean("ai_auto_run"))
            if (o.has("ai_url")) p.putString("ai_url", o.getString("ai_url"))
            if (o.has("ai_key")) p.putString("ai_key", o.getString("ai_key"))
            if (o.has("ai_model")) p.putString("ai_model", o.getString("ai_model"))
            if (o.has("ai_prompt")) p.putString("ai_prompt", o.getString("ai_prompt"))
            if (o.has("ai_config_presets")) p.putString("ai_config_presets", o.getJSONArray("ai_config_presets").toString())
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
            // 26：对话串联开关
            if (o.has("thread_dialog_enabled")) p.putBoolean("thread_dialog_enabled", o.getBoolean("thread_dialog_enabled"))
            // 26：本地收藏的评论
            if (o.has("comment_favorites")) p.putString("local_comment_favorites", o.getJSONArray("comment_favorites").toString())
            // 本地收藏的帖子与浏览历史
            if (o.has("favorites")) p.putString("local_favorites", o.getJSONArray("favorites").toString())
            if (o.has("history")) p.putString("local_history", o.getJSONArray("history").toString())
            if (o.has("card_redemptions")) p.putString("card_redemptions", o.getJSONArray("card_redemptions").toString())
            if (o.has("usage_events")) p.putString("usage_events", o.getJSONArray("usage_events").toString())
            if (o.has("usage_ai_summary")) p.putString("usage_ai_summary", o.getString("usage_ai_summary"))
            if (o.has("source_points")) p.putString("source_points", o.getJSONArray("source_points").toString())
            if (o.has("source_points_synced_at")) p.putLong("source_points_synced_at", o.getLong("source_points_synced_at"))
            if (o.has("source_usage")) p.putString("source_usage", o.getString("source_usage"))
            if (o.has("builtin_image_provider")) p.putString("builtin_image_provider", o.getString("builtin_image_provider"))
            if (o.has("confirm_gacha_pull")) p.putBoolean("confirm_gacha_pull", o.getBoolean("confirm_gacha_pull"))
            val pendingEditors = mutableListOf(p)
            sections.forEach { (name, destination) ->
                if (o.has(name)) {
                    val values = o.getJSONObject(name)
                    pendingEditors += destination.edit().also { editor ->
                        values.keys().forEach { key ->
                            when (val value = values.get(key)) {
                                is Boolean -> editor.putBoolean(key, value)
                                is Number -> editor.putLong(key, value.toLong())
                                else -> editor.putString(key, value.toString())
                            }
                        }
                    }
                }
            }
            // 26：外观主题与字体写入 lsb_prefs
            if (o.has("theme")) {
                val t = o.getJSONObject("theme")
                val tp = themePrefs.edit()
                if (t.has("theme_mode")) tp.putInt("theme_mode", t.optInt("theme_mode", 0).coerceIn(0, 2))
                if (t.has("dynamic_color")) tp.putBoolean("dynamic_color", t.getBoolean("dynamic_color"))
                if (t.has("exact_theme_colors")) tp.putBoolean("exact_theme_colors", t.getBoolean("exact_theme_colors"))
                if (t.has("background_image")) {
                    val encoded = t.getString("background_image")
                    require(encoded.length <= 6 * 1024 * 1024) { "备份中的背景图片过大" }
                    tp.putString("background_image", encoded)
                }
                if (t.has("background_image_enabled")) tp.putBoolean("background_image_enabled", t.getBoolean("background_image_enabled"))
                if (t.has("background_image_opacity")) tp.putFloat("background_image_opacity", t.getDouble("background_image_opacity").toFloat().coerceIn(0.05f, 0.65f))
                if (t.has("surface_opacity")) tp.putFloat("surface_opacity", t.getDouble("surface_opacity").toFloat().coerceIn(0.2f, 1f))
                if (t.has("theme_color")) tp.putString("theme_color", t.getString("theme_color"))
                if (t.has("oled_dark")) tp.putBoolean("oled_dark", t.getBoolean("oled_dark"))
                if (t.has("theme_style")) tp.putInt("theme_style", t.optInt("theme_style", 0).coerceIn(0, 15))
                if (t.has("theme_contrast")) tp.putFloat("theme_contrast", t.getDouble("theme_contrast").toFloat())
                if (t.has("theme_primary_override")) tp.putString("theme_primary_override", t.getString("theme_primary_override"))
                if (t.has("theme_secondary_override")) tp.putString("theme_secondary_override", t.getString("theme_secondary_override"))
                if (t.has("theme_tertiary_override")) tp.putString("theme_tertiary_override", t.getString("theme_tertiary_override"))
                if (t.has("theme_background_override")) tp.putString("theme_background_override", t.getString("theme_background_override"))
                if (t.has("keep_background_color")) tp.putBoolean("keep_background_color", t.getBoolean("keep_background_color"))
                if (t.has("theme_custom_colors")) {
                    val arr = t.getJSONArray("theme_custom_colors")
                    tp.putStringSet("theme_custom_colors", (0 until arr.length()).map { arr.getString(it) }.toSet())
                }
                if (t.has("card_colors")) tp.putString("card_colors", t.getString("card_colors"))
                if (t.has("font_key")) tp.putString("font_key", t.getString("font_key"))
                if (t.has("font_scale")) tp.putFloat("font_scale", t.getDouble("font_scale").toFloat().coerceIn(0.85f, 1.30f))
                if (t.has("font_weight_level")) tp.putInt("font_weight_level", t.optInt("font_weight_level", 0).coerceIn(0, 2))
                // 字体文件在私有目录、随不了 JSON；导入后 MainActivity 发现文件不存在会
                // 自动回落默认字体，这里只把名称补上以免设置页显示为空白的「自定义」
                if (t.has("custom_font_name")) tp.putString("custom_font_name", t.getString("custom_font_name"))
                pendingEditors += tp
            }
            pendingEditors.forEach { it.apply() }
            null
        } catch (e: Exception) {
            "导入失败：不是有效的设置 JSON"
        }
    }
}
