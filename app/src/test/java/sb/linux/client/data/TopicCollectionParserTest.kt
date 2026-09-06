package sb.linux.client.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import sb.linux.client.common.collection.TopicCollectionOperation
import sb.linux.client.common.collection.TopicCollectionTab
import sb.linux.client.data.parser.TopicCollectionParser

class TopicCollectionParserTest {
    @Test
    fun parsesCurrentSourceListFields() {
        val page = TopicCollectionParser.parseList(
            """
            <main><ul class="topic-collections-collection-list">
              <li class="post-item topic-collections-collection-row">
                <div class="post-avatar"><img src="/app/avatar.jpg"></div>
                <div class="post-body">
                  <div class="post-title-row"><a class="post-title" href="/topic_collection/63">水贴</a><span class="topic-collections-tag topic-collections-tag-public">公开</span></div>
                  <div class="post-meta"><span><a href="/user/41">db</a></span><span>278 篇文章</span><span>更新于 20小时前</span><span>创建于 2026-08-31</span></div>
                  <div class="topic-collections-card-desc">信息熵不够的帖子</div>
                </div>
              </li>
            </ul></main>
            """.trimIndent(),
            TopicCollectionTab.EVERYONE,
            "https://linux.sb/topic_collections?tab=everyone",
        )

        val item = page.items.single()
        assertEquals(63, item.collectionId)
        assertEquals("水贴", item.title)
        assertEquals("278 篇文章", item.articleCount)
        assertEquals("创建于 2026-08-31", item.createdText)
        assertEquals("信息熵不够的帖子", item.description)
    }

    @Test
    fun parsesDetailPaginationAndTopicCards() {
        val detail = TopicCollectionParser.parseDetail(
            """
            <main>
              <div class="topic-collections-collection-head">
                <div class="topic-collections-collection-title"><h1>水贴</h1></div>
                <div class="topic-collections-collection-meta"><span><a href="/user/41">db</a></span><span>278 篇</span><span>6 人订阅</span></div>
                <p class="topic-collections-collection-description">信息熵不够的帖子</p>
              </div>
              <ul class="post-list topic-collections-topic-list"><li class="post-item"><a class="post-title" href="/topic/19138">第一篇</a></li></ul>
              <nav class="pagination"><a href="/topic_collection/63?p=1">1</a><a href="/topic_collection/63?p=3">下一页</a></nav>
            </main>
            """.trimIndent(),
            "https://linux.sb/topic_collection/63?p=2",
        )

        assertEquals("水贴", detail.summary.title)
        assertEquals("6 人订阅", detail.subscriberCount)
        assertEquals(1, detail.topics.size)
        assertEquals(2, detail.pageInfo.currentPage)
        assertEquals(3, detail.pageInfo.totalPages)
        assertEquals("/topic_collection/63?p=3", detail.pageInfo.nextPath)
    }

    @Test
    fun parsesTopicPickerAddRemoveAndRemoveAll() {
        val picker = TopicCollectionParser.parsePicker(
            """
            <main>
              <form action="/topic_collections_action" method="post" data-topic-collections-add-form>
                <input type="hidden" name="_csrf" value="csrf"><input type="hidden" name="topic_id" value="19138">
                <select name="collection_id" data-topic-collections-select>
                  <option value="63" data-included="1">水贴</option>
                  <option value="5">机器学习</option>
                </select>
                <input type="hidden" name="action" data-topic-collections-action value="item_add">
                <button name="submit" value="1" data-topic-collections-add-btn>收录</button>
              </form>
              <form action="/topic_collections_action" method="post"><input type="hidden" name="action" value="item_remove_all"><button>全部取消收录</button></form>
            </main>
            """.trimIndent(),
        )

        assertNotNull(picker)
        assertEquals(19138, picker!!.topicId)
        assertTrue(picker.options.first { it.collectionId == 63L }.included)
        assertEquals(TopicCollectionOperation.REMOVE_ALL_ITEMS, picker.removeAllForm?.operation)
    }
}
