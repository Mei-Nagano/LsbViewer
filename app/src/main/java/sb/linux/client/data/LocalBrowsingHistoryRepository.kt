package sb.linux.client.data

import sb.linux.client.domain.community.BrowsingHistoryRepository
import sb.linux.client.domain.topic.TopicCard

/** Adapts the existing local history store without changing its persisted format. */
internal class LocalBrowsingHistoryRepository(
    private val settings: AppSettings,
) : BrowsingHistoryRepository {
    override fun history(): List<TopicCard> = settings.historyList()

    override fun record(
        topicId: Long,
        title: String,
        authorId: Long,
        authorName: String,
        forumId: Long,
        forumName: String,
        avatarUrl: String,
        titleColor: String,
    ) = settings.addHistory(topicId, title, authorId, authorName, forumId, forumName, avatarUrl, titleColor)

    override fun remove(topicId: Long) = settings.removeHistory(topicId)

    override fun clear() = settings.clearHistory()
}
