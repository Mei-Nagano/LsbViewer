package sb.linux.client.data.topic

import sb.linux.client.data.LsbClient
import sb.linux.client.domain.topic.TopicPageData
import sb.linux.client.data.TopicPageParser
import sb.linux.client.domain.topic.TopicFloorPage
import sb.linux.client.domain.topic.TopicFloorPageRepository

/** 源站按 replyid／floor 返回目标页，实际页码以 HTML 分页栏为准。 */
internal class TopicFloorPageLoader(
    private val client: LsbClient,
) : TopicFloorPageRepository {
    override suspend fun load(topicId: Long, floor: Int, replyId: Long): TopicFloorPage {
        val target = if (replyId > 0) "replyid=$replyId" else "floor=$floor"
        val response = client.get("/topic/$topicId?$target")
        val targetPage = TopicPageParser.parse(response.html, topicId)
        return TopicFloorPage(targetPage, targetPage.page, targetPage.posts.firstOrNull {
            if (replyId > 0) it.id == replyId else it.floor == floor
        })
    }
}
