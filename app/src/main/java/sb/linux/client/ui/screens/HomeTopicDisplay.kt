package sb.linux.client.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import sb.linux.client.common.filter.KeywordFilterRules
import sb.linux.client.ui.session.HomeTabState
import sb.linux.client.ui.session.Session
import sb.linux.client.domain.topic.TopicCard
import sb.linux.client.util.TopicFilter

internal data class HomeTopicDisplay(
    val visibleTopics: List<TopicCard>,
    val pagedTopics: List<TopicCard>,
    val totalPages: Int,
    val filterTopics: (List<TopicCard>) -> List<TopicCard>,
)

@Composable
internal fun rememberHomeTopicDisplay(
    session: Session,
    current: HomeTabState,
    combo: Int,
    homeInfinite: Boolean,
): HomeTopicDisplay {
    val filterTopics: (List<TopicCard>) -> List<TopicCard> = { list ->
        val filter = session.keywordFilter
        val base = TopicFilter.apply(
            topics = list,
            settings = filter.settings,
            configuredForumIds = KeywordFilterRules.configuredForumIds(filter.settings, filter.policy),
            isHome = true,
        )
        when (combo) {
            1 -> base.filter { it.lotteryStatus.isNotBlank() }
            2 -> base.filter { it.cardStatus.isNotBlank() }
            else -> base
        }
    }
    val visible = remember(current.topics, session.keywordFilter, combo) {
        filterTopics(current.topics)
    }
    val perPage = session.topicsPerPage.coerceAtLeast(1)
    val paged = if (homeInfinite) visible else visible.drop((current.page - 1) * perPage).take(perPage)
    val total = if (homeInfinite) current.totalPages else {
        val full = ((visible.size + perPage - 1) / perPage).coerceAtLeast(1)
        (if (current.sourcePage < current.sourceTotalPages) full + 1 else full).coerceAtLeast(current.page)
    }
    return HomeTopicDisplay(visible, paged, total, filterTopics)
}
