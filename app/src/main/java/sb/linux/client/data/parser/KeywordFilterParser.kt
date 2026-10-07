package sb.linux.client.data.parser

import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.Jsoup
import sb.linux.client.common.filter.FilterableForum
import sb.linux.client.common.filter.KeywordFilterPolicy
import sb.linux.client.common.filter.KeywordFilterRules
import sb.linux.client.common.filter.KeywordFilterSettings

/** 解析源站屏蔽设置接口和首页 data-* 配置，不承载网络或 UI 逻辑。 */
object KeywordFilterParser {
    /** 解析接口响应，返回是否已有账号设置及其规范化前的原始规则。 */
    fun parseSettingsResponse(json: JSONObject): Pair<Boolean, KeywordFilterSettings> {
        val settings = json.optJSONObject("settings") ?: return false to KeywordFilterSettings()
        fun strings(key: String, limit: Int): List<String> = jsonArrayStrings(settings.optJSONArray(key), limit)
        fun longs(key: String): List<Long> = jsonArrayLongs(settings.optJSONArray(key))
        return (json.optInt("exists", 0) == 1) to KeywordFilterSettings(
            presets = strings("presets", KeywordFilterRules.MAX_CUSTOM_WORDS),
            custom = strings("custom", KeywordFilterRules.MAX_CUSTOM_WORDS),
            users = strings("users", KeywordFilterRules.MAX_USERS),
            forumExcludedIds = longs("forum_excluded_ids"),
            forumExtraIds = longs("forum_extra_ids"),
        )
    }

    /** 从首页按钮的 data 属性读取可选预设、版块列表及默认策略。 */
    fun parsePolicy(html: String): KeywordFilterPolicy {
        val document = Jsoup.parse(html)
        val button = document.selectFirst("[data-home-keyword-filter-open]") ?: return KeywordFilterPolicy()
        val presets = jsonArrayLabels(
            parseJsonArray(button.attr("data-home-keyword-filter-presets")),
            KeywordFilterRules.MAX_CUSTOM_WORDS,
        )
        val policy = runCatching {
            JSONObject(button.attr("data-home-keyword-filter-forum-policy").ifBlank { "{}" })
        }.getOrDefault(JSONObject())
        val forums = (policy.optJSONArray("forums") ?: JSONArray()).let { array ->
            (0 until array.length()).mapNotNull { index ->
                val item = array.optJSONObject(index) ?: return@mapNotNull null
                val id = item.optLong("id", 0L)
                val name = item.optString("name").trim()
                if (id > 0L && name.isNotBlank()) FilterableForum(id, name, item.optBoolean("default")) else null
            }
        }
        val allowed = forums.map { it.id }.toSet()
        val defaults = jsonArrayLongs(policy.optJSONArray("defaultForumIds")).filter { it in allowed }.toSet() +
            forums.filter { it.isDefault }.map { it.id }
        return KeywordFilterPolicy(
            availablePresets = presets,
            forums = forums,
            defaultForumIds = defaults,
            currentForumId = policy.optLong("currentForumId", 0L),
            enabled = policy.optBoolean("enabled"),
            warning = policy.optString("warning").trim(),
        )
    }

    private fun parseJsonArray(raw: String): JSONArray =
        runCatching { JSONArray(raw.ifBlank { "[]" }) }.getOrDefault(JSONArray())

    private fun jsonArrayStrings(array: JSONArray?, limit: Int): List<String> =
        (0 until (array?.length() ?: 0)).mapNotNull { index ->
            stringValue(array?.opt(index))?.let(KeywordFilterRules::normalize)?.takeIf(String::isNotBlank)
        }.distinct().take(limit)

    private fun jsonArrayLabels(array: JSONArray?, limit: Int): List<String> {
        val seen = mutableSetOf<String>()
        val labels = mutableListOf<String>()
        for (index in 0 until (array?.length() ?: 0)) {
            val label = stringValue(array?.opt(index))?.trim().orEmpty()
            if (label.isNotBlank() && seen.add(KeywordFilterRules.normalize(label))) labels += label
            if (labels.size >= limit) break
        }
        return labels
    }

    private fun jsonArrayLongs(array: JSONArray?): List<Long> =
        (0 until (array?.length() ?: 0)).mapNotNull { index ->
            val value = array?.opt(index)
            when (value) {
                is Number -> value.toLong()
                is String -> value.toLongOrNull()
                else -> null
            }
        }.filter { it > 0L }.distinct()

    private fun stringValue(value: Any?): String? =
        if (value == null || value == JSONObject.NULL) null else value.toString()
}
