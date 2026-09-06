package sb.linux.client.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import sb.linux.client.data.parser.SearchPageParser

class SearchPageParserTest {
    @Test
    fun buildsCurrentGetContract() {
        assertEquals(
            "/search?q=hello+world&scope=body&sort=latest&p=3",
            SearchQuery("hello world", SearchScope.BODY, SearchSort.LATEST, 3).toPath(),
        )
        assertEquals(SearchSort.RELEVANCE, SearchQuery("linux", SearchScope.USER, SearchSort.VIEWS).normalized().sort)
    }

    @Test
    fun parsesTopicResultsAndSourcePagination() {
        val page = SearchPageParser.parse(
            """
            <section class="meilisearch-search-page">
              <form class="meilisearch-search-form">
                <input name="q" value="linux">
                <input name="scope" value="all">
              </form>
              <div class="meilisearch-search-summary">搜索“linux” · 全部内容 · 相关性 · 1,836 个主题</div>
              <ol class="meilisearch-search-results">
                <li class="meilisearch-search-result">
                  <a class="meilisearch-search-result-title" href="/topic/42"><mark>linux</mark> 主题</a>
                  <div class="meilisearch-search-result-snippet">正文摘要</div>
                  <div class="meilisearch-search-result-meta">
                    <span>命中标题</span><span>技术交流</span><span>昨天</span>
                    <span>12 条回复</span><span>1,024 次浏览</span>
                  </div>
                </li>
              </ol>
              <nav class="meilisearch-search-pagination">
                <a href="/search?q=linux&amp;p=1">上一页</a><span>第 2 页</span>
                <a href="/search?q=linux&amp;p=3">下一页</a>
              </nav>
            </section>
            """.trimIndent(),
            SearchQuery("linux", page = 2),
        )

        val topic = page.items.single() as SearchResultItem.Topic
        assertEquals(1_836, page.totalCount)
        assertEquals(42L, topic.topicId)
        assertEquals("linux 主题", topic.title)
        assertEquals(12, topic.replies)
        assertEquals(1_024, topic.views)
        assertTrue(page.hasPrevious)
        assertTrue(page.hasNext)
    }

    @Test
    fun parsesUserResults() {
        val page = SearchPageParser.parse(
            """
            <section class="meilisearch-search-page">
              <form class="meilisearch-search-form">
                <input name="q" value="linux">
                <input name="scope" value="user">
              </form>
              <div class="meilisearch-search-summary">搜索“linux” · 用户 · 210 个用户</div>
              <ol class="meilisearch-search-results meilisearch-search-user-results">
                <li class="meilisearch-search-user-result">
                  <a class="meilisearch-search-user-link" href="/user/52">
                    <img src="/avatar.jpg"><span class="meilisearch-search-user-main">
                      <strong>Linux</strong><span class="meilisearch-search-user-meta">饼友 · 注册于 2026</span>
                    </span>
                  </a>
                </li>
              </ol>
            </section>
            """.trimIndent(),
            SearchQuery("linux", SearchScope.USER),
        )

        val user = page.items.single() as SearchResultItem.User
        assertEquals(52L, user.userId)
        assertEquals("Linux", user.username)
        assertEquals("https://linux.sb/avatar.jpg", user.avatarUrl)
        assertEquals(210, page.totalCount)
    }
}
