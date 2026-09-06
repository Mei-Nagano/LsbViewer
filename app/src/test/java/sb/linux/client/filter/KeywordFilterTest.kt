package sb.linux.client.filter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import sb.linux.client.common.filter.FilterableForum
import sb.linux.client.common.filter.KeywordFilterPolicy
import sb.linux.client.common.filter.KeywordFilterRules
import sb.linux.client.common.filter.KeywordFilterSettings
import sb.linux.client.data.TopicCard
import sb.linux.client.util.TopicFilter

class KeywordFilterTest {
    @Test
    fun `normalization and source limits are applied`() {
        val settings = KeywordFilterRules.clean(
            listOf(" GPT ", "gpt", "A".repeat(60)),
            KeywordFilterRules.MAX_CUSTOM_WORDS,
        )
        assertEquals(listOf("gpt", "a".repeat(40)), settings)
        assertEquals(listOf("one", "two"), KeywordFilterRules.splitInput("one, two；one"))
    }

    @Test
    fun `preset and user matching is case insensitive while user stays exact`() {
        val topics = listOf(
            topic(1, "Free GPT service", "Alice"),
            topic(2, "Normal title", "alice-extra"),
            topic(3, "Normal title", "BOB"),
        )
        val settings = KeywordFilterSettings(presets = listOf("gpt"), users = listOf("bob"))
        val result = TopicFilter.applyWithCount(topics, settings)
        assertEquals(listOf(2L), result.visible.map { it.topicId })
        assertEquals(2, result.hiddenCount)
    }

    @Test
    fun `forum rules only apply on home`() {
        val topics = listOf(topic(1, "Title", "user", forumId = 4L))
        val settings = KeywordFilterSettings()
        assertTrue(TopicFilter.apply(topics, settings, setOf(4L), isHome = false).isNotEmpty())
        assertTrue(TopicFilter.apply(topics, settings, setOf(4L), isHome = true).isEmpty())
    }

    @Test
    fun `forum configuration removes defaults and adds explicit forums`() {
        val policy = KeywordFilterPolicy(
            forums = listOf(FilterableForum(1L, "推广", true), FilterableForum(2L, "闲聊")),
            defaultForumIds = setOf(1L),
            enabled = true,
        )
        val settings = KeywordFilterSettings(forumExcludedIds = listOf(1L), forumExtraIds = listOf(2L))
        assertEquals(setOf(2L), KeywordFilterRules.configuredForumIds(settings, policy))
        assertTrue(KeywordFilterRules.configuredForumIds(settings, policy.copy(enabled = false)).isEmpty())
    }

    @Test
    fun `sanitize keeps only source supplied presets`() {
        val policy = KeywordFilterPolicy(availablePresets = listOf("GPT", "广告"))
        val settings = KeywordFilterSettings(presets = listOf("gpt", "unknown", "广告"))
        assertEquals(listOf("gpt", "广告"), KeywordFilterRules.sanitize(settings, policy).presets)
    }

    @Test
    fun `merge preserves remote and legacy local rules within source limits`() {
        val policy = KeywordFilterPolicy(availablePresets = listOf("GPT"))
        val merged = KeywordFilterRules.merge(
            base = KeywordFilterSettings(presets = listOf("gpt"), custom = listOf("remote"), users = listOf("alice")),
            additions = KeywordFilterSettings(
                presets = listOf("unknown"),
                custom = listOf("local", "REMOTE"),
                users = listOf("bob", "ALICE"),
            ),
            policy = policy,
        )
        assertEquals(listOf("gpt"), merged.presets)
        assertEquals(listOf("remote", "local"), merged.custom)
        assertEquals(listOf("alice", "bob"), merged.users)
    }

    private fun topic(id: Long, title: String, author: String, forumId: Long = 1L) = TopicCard(
        topicId = id,
        title = title,
        authorId = 1L,
        authorName = author,
        forumId = forumId,
        forumName = "forum",
        replies = 0,
        lastReplier = "",
        timeText = "",
    )
}
