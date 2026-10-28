package sb.linux.client.data

import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import sb.linux.client.domain.network.DohServer

internal class AppSettingsGeneralStore(private val prefs: SharedPreferences) {
    var linkOpenMode: Int
        get() = prefs.getInt("link_open_mode", 0)
        set(v) = prefs.edit().putInt("link_open_mode", v).apply()

    var linkPreviewEnabled: Boolean
        get() = prefs.getBoolean("link_preview_enabled", true)
        set(v) = prefs.edit().putBoolean("link_preview_enabled", v).apply()

    /** 应用代理：0 = 关闭，1 = HTTP，2 = SOCKS5。 */
    var proxyType: Int
        get() = prefs.getInt("proxy_type", 0).coerceIn(0, 2)
        set(v) = prefs.edit().putInt("proxy_type", v.coerceIn(0, 2)).apply()

    var proxyHost: String
        get() = prefs.getString("proxy_host", "127.0.0.1") ?: "127.0.0.1"
        set(v) = prefs.edit().putString("proxy_host", v.trim()).apply()

    var proxyPort: Int
        get() = prefs.getInt("proxy_port", 7890).coerceIn(1, 65535)
        set(v) = prefs.edit().putInt("proxy_port", v.coerceIn(1, 65535)).apply()

    fun saveProxy(type: Int, host: String, port: Int) {
        prefs.edit()
            .putString("proxy_host", host.trim())
            .putInt("proxy_port", port.coerceIn(1, 65535))
            .putInt("proxy_type", type.coerceIn(0, 2))
            .apply()
    }

    /** DNS over HTTPS。VPN 存在时可自动旁路，防止与 VPN 自身 DNS 策略冲突。 */
    var dohEnabled: Boolean
        get() = prefs.getBoolean("doh_enabled", false)
        set(v) = prefs.edit()
            .putBoolean("doh_enabled", v)
            // DoH 与应用代理共用网络入口；启用 DoH 时关闭代理，避免两套
            // DNS/转发策略叠加后行为不一致。
            .apply { if (v) putInt("proxy_type", 0) }
            .apply()

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
            require(server.name.isNotBlank() && DohEndpointPolicy.isValid(server.url))
            array.put(JSONObject().put("id", server.id).put("name", server.name.trim()).put("url", server.url.trim()).put("note", server.note.trim()))
        }
        prefs.edit().putString("doh_servers", array.toString()).apply()
    }

    var dohDisableOnVpn: Boolean
        get() = prefs.getBoolean("doh_disable_on_vpn", true)
        set(v) = prefs.edit().putBoolean("doh_disable_on_vpn", v).apply()

    var dohAutoWithoutVpn: Boolean
        get() = prefs.getBoolean("doh_auto_without_vpn", true)
        set(v) = prefs.edit().putBoolean("doh_auto_without_vpn", v).apply()

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

    /** 布局偏好：键缺失或 true = 跟随窗口；显式 false = 始终单内容栏。
     *  旧「平板模式」只限制内容布局，不再阻止根导航按窗口切换 bar/rail。 */
    var tabletMode: Boolean
        get() = prefs.getBoolean("tablet_mode", true)
        set(v) = prefs.edit().putBoolean("tablet_mode", v).apply()

    /** 是否限制为单内容栏；只有用户显式保存过 false 才生效（键缺失跟随窗口）。 */
    val singleContentColumn: Boolean
        get() = prefs.contains("tablet_mode") && !prefs.getBoolean("tablet_mode", true)

    /** 底栏样式：0 = 经典，1 = 液态玻璃。新 UI 不再渲染旧效果，值仅作为兼容数据保留。 */
    var bottomBarStyle: Int
        get() = prefs.getInt("bottom_bar_style", 0).coerceIn(0, 1)
        set(v) = prefs.edit().putInt("bottom_bar_style", v.coerceIn(0, 1)).apply()

    /** 主底栏项目及顺序（归一别名、保留未知 ID；迁移与存储见 [BottomBarPreferenceStore]）。 */
    var bottomBarItems: List<String>
        get() = bottomBar.items
        set(v) { bottomBar.items = v }

    /** 旧底栏列表迁移（幂等），应用启动时调用一次。 */
    fun migrateBottomBarItems() = bottomBar.migrate()

    private val bottomBar = BottomBarPreferenceStore(prefs)

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
            val allowed = setOf("settings", "history", "favorites", "ai", "cards")
            return (prefs.getStringSet("backup_items", allowed) ?: allowed).filter { it in allowed }.toSet().ifEmpty { allowed }
        }
        set(v) {
            val allowed = setOf("settings", "history", "favorites", "ai", "cards")
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
        .remove("use_builtin_image_host")
        .remove("image_host_url").remove("image_host_token").remove("image_host_field")
        .remove("update_check_interval_hours")
        .apply()

    fun resetNetwork() = prefs.edit()
        .remove("proxy_type").remove("proxy_host").remove("proxy_port")
        .remove("doh_enabled").remove("doh_url").remove("doh_disable_on_vpn").remove("doh_auto_without_vpn")
        .remove("doh_servers")
        .apply()

    /** 重置 WebDAV 备份配置 */
    fun resetWebdav() = prefs.edit()
        .remove("webdav_url").remove("webdav_user").remove("webdav_pass").remove("webdav_dir")
        .apply()

    /**
     * AI 总结总开关：关闭后帖子内不显示 AI 入口。
     * 键名沿用历史 ai_auto（当初这个开关兼任「自动总结」），改名会让老用户/旧备份丢配置。
     */
}
