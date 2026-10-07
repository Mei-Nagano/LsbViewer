package sb.linux.client.data

import android.content.SharedPreferences
import org.json.JSONObject

internal class AppSettingsBrowseStore(private val prefs: SharedPreferences) {
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
        get() = prefs.getInt("table_display_mode", 1).coerceIn(0, 1)
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
}
