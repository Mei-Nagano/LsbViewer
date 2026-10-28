package sb.linux.client.data

import android.content.SharedPreferences

/**
 * 根导航项目及顺序的持久化与迁移。
 *
 * 原始值保留旧别名与未知 ID，不补齐、不排序、不写回默认四项；
 * 迁移幂等：先备份原始字符串，再写归一化结果并记录版本。
 * 从 `AppSettingsGeneralStore` 拆出，保持该文件在 250 行以内。
 */
internal class BottomBarPreferenceStore(private val prefs: SharedPreferences) {

    /** 主底栏项目及顺序的原始值（已归一别名并去重）。 */
    var items: List<String>
        get() = normalize(prefs.getString(KEY_ITEMS, DEFAULT_ITEMS).orEmpty())
        set(value) {
            prefs.edit()
                .putString(KEY_ITEMS, normalize(value.joinToString(",")).joinToString(","))
                .putInt(KEY_VERSION, VERSION)
                .apply()
        }

    /**
     * 把旧别名归一到稳定 ID，并按语义去重（保留首个顺序）。
     * 未知 ID 保留在原始数据里，由界面派生时忽略。
     */
    private fun normalize(raw: String): List<String> = raw
        .split(',')
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .map { token ->
            when (token) {
                "directMessages", "directMessagesRoot", "notifications" -> "messages"
                "topicCollectionsRoot" -> "topicCollections"
                else -> token
            }
        }
        .distinct()

    /** 旧底栏列表迁移（幂等）：迁移中断后重跑不会再次重排。 */
    fun migrate() {
        if (prefs.getInt(KEY_VERSION, 0) >= VERSION) return
        val raw = prefs.getString(KEY_ITEMS, null)
        val editor = prefs.edit()
        if (raw != null) {
            if (!prefs.contains(KEY_LEGACY)) editor.putString(KEY_LEGACY, raw)
            val normalized = normalize(raw).joinToString(",")
            if (normalized != raw) editor.putString(KEY_ITEMS, normalized)
        }
        editor.putInt(KEY_VERSION, VERSION).apply()
    }

    private companion object {
        const val KEY_ITEMS = "bottom_bar_items"
        const val KEY_LEGACY = "bottom_bar_items_pre_v3"
        const val KEY_VERSION = "bottom_navigation_version"
        const val VERSION = 3
        const val DEFAULT_ITEMS = "home,topicCollections,messages,me"
    }
}
