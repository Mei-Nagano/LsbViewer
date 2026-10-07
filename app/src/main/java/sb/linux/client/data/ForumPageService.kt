package sb.linux.client.data

import sb.linux.client.domain.community.ForumInfo
import sb.linux.client.domain.community.ForumRepository
import sb.linux.client.domain.community.ForumTopicsPage

internal class ForumPageService(private val client: LsbClient) : ForumRepository {
    override suspend fun loadDirectory(): List<ForumInfo> {
        val directory = ForumParser.parseForumList(client.get("/forum_list").html)
        if (directory.isNotEmpty()) return directory
        return ForumParser.parseForumList(client.get("/").html)
    }

    override suspend fun loadTopics(forumId: Long, sort: String, page: Int): ForumTopicsPage {
        val path = when {
            page <= 1 && sort == "comment" -> "/forum/$forumId"
            page <= 1 -> "/forum/$forumId?sort=$sort"
            else -> "/forum/$forumId?sort=$sort&p=$page"
        }
        val (topics, pagination) = TopicListParser.parseTopicList(client.get(path).html)
        return ForumTopicsPage(topics, pagination.first, pagination.second)
    }
}
