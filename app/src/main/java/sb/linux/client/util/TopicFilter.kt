package sb.linux.client.util

import sb.linux.client.common.filter.KeywordFilterRules
import sb.linux.client.common.filter.KeywordFilterSettings
import sb.linux.client.data.TopicCard

/** 帖子列表本地屏蔽；规则与源站一致，搜索等非帖子列表不调用此方法。 */
object TopicFilter {
    /** 返回屏蔽后的帖子，不改变输入列表顺序。 */
    fun apply(
        topics: List<TopicCard>,
        settings: KeywordFilterSettings,
        configuredForumIds: Set<Long> = emptySet(),
        isHome: Boolean = false,
    ): List<TopicCard> = applyWithCount(topics, settings, configuredForumIds, isHome).visible

    /** 返回屏蔽后的帖子和本页被隐藏数量。 */
    fun applyWithCount(
        topics: List<TopicCard>,
        settings: KeywordFilterSettings,
        configuredForumIds: Set<Long> = emptySet(),
        isHome: Boolean = false,
    ) = run {
        val words = (settings.presets + settings.custom).map(KeywordFilterRules::normalize).filter(String::isNotBlank).distinct()
        val users = settings.users.map(KeywordFilterRules::normalize).filter(String::isNotBlank).toSet()
        val visible = topics.filterNot { topic ->
            val title = KeywordFilterRules.normalize(topic.title)
            val author = KeywordFilterRules.normalize(topic.authorName)
            val wordMatched = words.any(title::contains)
            val userMatched = author.isNotBlank() && author in users
            val forumMatched = isHome && topic.forumId in configuredForumIds
            wordMatched || userMatched || forumMatched
        }
        sb.linux.client.common.filter.TopicFilterResult(visible, topics.size - visible.size)
    }
}
