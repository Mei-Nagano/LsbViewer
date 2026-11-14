package sb.linux.client.data

import sb.linux.client.domain.topic.TopicLinkRepository

internal class TopicLinkService(private val client: LsbClient) : TopicLinkRepository {
    override suspend fun replyFloor(topicId: Long, replyId: Long): Int? {
        require(topicId > 0 && replyId > 0)
        val response = client.get("/topic/$topicId?replyid=$replyId")
        return TopicPageParser.parse(response.html, topicId).posts.firstOrNull { it.id == replyId }?.floor?.takeIf { it > 0 }
    }
}
