package sb.linux.client.domain.community

import sb.linux.client.domain.topic.TopicCard

/** Reads and updates the locally stored browsing history. */
interface BrowsingHistoryRepository {
    fun history(): List<TopicCard>
    fun record(
        topicId: Long,
        title: String,
        authorId: Long,
        authorName: String,
        forumId: Long,
        forumName: String,
        avatarUrl: String,
        titleColor: String,
    )
    fun remove(topicId: Long)
    fun clear()
}
