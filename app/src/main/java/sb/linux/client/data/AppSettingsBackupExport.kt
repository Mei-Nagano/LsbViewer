package sb.linux.client.data

import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

internal fun AppSettings.exportBackupJson(
    prefs: SharedPreferences,
    themePrefs: SharedPreferences,
    aiRecords: SharedPreferences,
    readingRecords: SharedPreferences,
): String {
        val o = JSONObject()
        o.put("ai_summaries", JSONObject(aiRecords.all))
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
        o.put("proxy_type", proxyType)
        o.put("proxy_host", proxyHost)
        o.put("proxy_port", proxyPort)
        o.put("doh_enabled", dohEnabled)
        o.put("doh_url", dohUrl)
        o.put("doh_disable_on_vpn", dohDisableOnVpn)
        o.put("doh_auto_without_vpn", dohAutoWithoutVpn)
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
        t.put("keep_background_color", themePrefs.getBoolean("keep_background_color", false))
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
internal fun AppSettings.exportBackupJson(
    items: Set<String>,
    prefs: SharedPreferences,
    themePrefs: SharedPreferences,
    aiRecords: SharedPreferences,
    readingRecords: SharedPreferences,
): String {
        val full = JSONObject(exportJson())
        val dataKeys = mapOf(
            "history" to setOf("history", "reading_positions"),
            "favorites" to setOf("favorites", "comment_favorites"),
            "cards" to setOf("card_redemptions"),
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
