package sb.linux.client.domain.topic

interface TopicLinkRepository {
    suspend fun replyFloor(topicId: Long, replyId: Long): Int?
}
