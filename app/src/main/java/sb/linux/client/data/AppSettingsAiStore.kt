package sb.linux.client.data

import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import sb.linux.client.domain.ai.AiConfigPreset

internal class AppSettingsAiStore(private val prefs: SharedPreferences) {
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
}
