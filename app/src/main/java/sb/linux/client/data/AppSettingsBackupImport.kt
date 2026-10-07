package sb.linux.client.data

import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

internal fun AppSettings.importBackupJson(
    text: String,
    prefs: SharedPreferences,
    themePrefs: SharedPreferences,
    aiRecords: SharedPreferences,
    drafts: SharedPreferences,
    readingRecords: SharedPreferences,
): String? {
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
            if (o.has("table_display_mode")) p.putInt("table_display_mode", o.optInt("table_display_mode", 1).coerceIn(0, 1))
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
            if (o.has("proxy_type")) p.putInt("proxy_type", o.optInt("proxy_type", 0).coerceIn(0, 2))
            if (o.has("proxy_host")) p.putString("proxy_host", o.getString("proxy_host").trim())
            if (o.has("proxy_port")) p.putInt("proxy_port", o.optInt("proxy_port", 7890).coerceIn(1, 65535))
            if (o.has("doh_enabled")) p.putBoolean("doh_enabled", o.getBoolean("doh_enabled"))
            if (o.has("doh_url")) p.putString("doh_url", o.getString("doh_url"))
            if (o.has("doh_disable_on_vpn")) p.putBoolean("doh_disable_on_vpn", o.getBoolean("doh_disable_on_vpn"))
            if (o.has("doh_auto_without_vpn")) p.putBoolean("doh_auto_without_vpn", o.getBoolean("doh_auto_without_vpn"))
            if (o.has("doh_servers")) {
                val servers = o.getJSONArray("doh_servers")
                for (index in 0 until servers.length()) {
                    val server = servers.getJSONObject(index)
                    require(server.getString("id").isNotBlank() && server.getString("name").isNotBlank() && DohEndpointPolicy.isValid(server.getString("url")))
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
