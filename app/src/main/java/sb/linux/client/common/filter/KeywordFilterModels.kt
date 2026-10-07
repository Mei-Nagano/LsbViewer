package sb.linux.client.common.filter

import java.util.Locale

/** 当前账号保存的五类屏蔽规则。 */
data class KeywordFilterSettings(
    val presets: List<String> = emptyList(),
    val custom: List<String> = emptyList(),
    val users: List<String> = emptyList(),
    val forumExcludedIds: List<Long> = emptyList(),
    val forumExtraIds: List<Long> = emptyList(),
)

/** 源站允许用户选择的版块。 */
data class FilterableForum(
    val id: Long,
    val name: String,
    val isDefault: Boolean = false,
)

/** 从首页 HTML 下发的可选项及默认值。 */
data class KeywordFilterPolicy(
    val availablePresets: List<String> = emptyList(),
    val forums: List<FilterableForum> = emptyList(),
    val defaultForumIds: Set<Long> = emptySet(),
    val currentForumId: Long = 0L,
    val enabled: Boolean = false,
    val warning: String = "",
) {
    val forumBlockingEnabled: Boolean
        get() = enabled && currentForumId == 0L && warning.isBlank()
}

/** 本地缓存与远端同步状态。 */
data class KeywordFilterSnapshot(
    val settings: KeywordFilterSettings = KeywordFilterSettings(),
    val policy: KeywordFilterPolicy = KeywordFilterPolicy(),
    val exists: Boolean = false,
    val pending: Boolean = false,
    val syncedAt: Long = 0L,
)

/** 过滤后的列表及隐藏计数。 */
data class TopicFilterResult<T>(
    val visible: List<T>,
    val hiddenCount: Int,
)

/** 与源站 plugins.js 保持一致的清洗规则。 */
object KeywordFilterRules {
    const val MAX_CUSTOM_WORDS = 20
    const val MAX_USERS = 5
    const val MAX_VALUE_LENGTH = 40

    fun normalize(value: String): String = value.trim().lowercase(Locale.ROOT)

    fun splitInput(value: String): List<String> =
        value.split(Regex("[\\n,，;；]+"))
            .map(::normalize)
            .filter(String::isNotBlank)
            .distinct()

    fun clean(values: Iterable<String>, limit: Int): List<String> =
        values.asSequence()
            .map(::normalize)
            .filter(String::isNotBlank)
            .distinct()
            .take(limit)
            .map { it.take(MAX_VALUE_LENGTH) }
            .toList()

    fun sanitize(settings: KeywordFilterSettings, policy: KeywordFilterPolicy): KeywordFilterSettings {
        val allowedPresets = policy.availablePresets.map(::normalize).toSet()
        val allowedForums = policy.forums.map { it.id }.toSet()
        val excluded = settings.forumExcludedIds
            .filter { allowedForums.isEmpty() || it in allowedForums }
            .distinct()
        return KeywordFilterSettings(
            presets = clean(settings.presets, MAX_CUSTOM_WORDS)
                .filter { allowedPresets.isEmpty() || it in allowedPresets },
            custom = clean(settings.custom, MAX_CUSTOM_WORDS),
            users = clean(settings.users, MAX_USERS),
            forumExcludedIds = excluded,
            forumExtraIds = settings.forumExtraIds
                .filter { (allowedForums.isEmpty() || it in allowedForums) && it !in excluded }
                .distinct(),
        )
    }

    /** 合并本地待迁移规则与源站规则，并按源站限制重新清洗。 */
    fun merge(
        base: KeywordFilterSettings,
        additions: KeywordFilterSettings,
        policy: KeywordFilterPolicy,
    ): KeywordFilterSettings = sanitize(
        base.copy(
            presets = base.presets + additions.presets,
            custom = base.custom + additions.custom,
            users = base.users + additions.users,
            forumExcludedIds = base.forumExcludedIds + additions.forumExcludedIds,
            forumExtraIds = base.forumExtraIds + additions.forumExtraIds,
        ),
        policy,
    )

    fun configuredForumIds(settings: KeywordFilterSettings, policy: KeywordFilterPolicy): Set<Long> {
        if (!policy.forumBlockingEnabled) return emptySet()
        val ids = policy.defaultForumIds.toMutableSet()
        ids.removeAll(settings.forumExcludedIds.toSet())
        ids.addAll(settings.forumExtraIds)
        return ids
    }
}
