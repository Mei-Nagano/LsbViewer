package sb.linux.client.data

import org.jsoup.Jsoup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import sb.linux.client.common.collection.TopicCollectionOperation
import sb.linux.client.common.collection.TopicCollectionTab
import sb.linux.client.common.collection.actionFor
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
                <input type="hidden" name="topic_collections_action" data-topic-collections-action value="item_add">
                <select name="collection_id" data-topic-collections-select>
                  <option value="63" data-included="1">水贴</option>
                  <option value="5">机器学习</option>
                  <option value="__topic_collections_remove_all__">全部取消收录</option>
                  <option value="__topic_collections_create__">新建专辑</option>
                </select>
                <button name="submit" value="1" data-topic-collections-add-btn>收录</button>
              </form>
              <form action="/topic_collections_action" method="post" data-topic-collections-create-form>
                <input type="hidden" name="_csrf" value="csrf">
                <input type="hidden" name="topic_id" value="19138">
                <input type="hidden" name="topic_collections_action" value="collection_create_add">
                <input name="name" required placeholder="专辑名称">
                <textarea name="description" placeholder="专辑描述"></textarea>
                <label><input type="checkbox" name="private" value="1">设为私密</label>
                <button type="submit">创建并收录</button>
              </form>
            </main>
            """.trimIndent(),
        )

        assertNotNull(picker)
        assertEquals(19138, picker!!.topicId)
        assertTrue(picker.options.first { it.collectionId == 63L }.included)
        assertEquals(2, picker.options.size)
        assertEquals(TopicCollectionOperation.CREATE, picker.createForm?.operation)
        assertEquals("", picker.createForm?.controls?.first { it.name == "private" }?.value)
        assertEquals(TopicCollectionOperation.REMOVE_ALL_ITEMS, picker.removeAllForm?.operation)
        assertTrue(picker.removeAllForm?.fields?.contains("topic_collections_action" to "item_remove_all") == true)
    }

    @Test
    fun buildsExplicitAddAndRemoveActionsPerOption() {
        val picker = TopicCollectionParser.parsePicker(PICKER_HTML)
        assertNotNull(picker)

        val included = picker!!.options.first { it.collectionId == 63L }
        val removeForm = picker.actionFor(included)
        assertEquals(TopicCollectionOperation.REMOVE_ITEM, removeForm?.operation)
        assertTrue(removeForm?.fields?.contains("topic_collections_action" to "item_remove") == true)
        assertTrue(removeForm?.fields?.contains("collection_id" to "63") == true)

        val absent = picker.options.first { it.collectionId == 5L }
        val addForm = picker.actionFor(absent)
        assertEquals(TopicCollectionOperation.ADD_ITEM, addForm?.operation)
        assertTrue(addForm?.fields?.contains("topic_collections_action" to "item_add") == true)
        assertTrue(addForm?.fields?.contains("collection_id" to "5") == true)
        // 源站表单只有一份 collection_id / action，改写后不能留下旧值
        assertEquals(1, addForm?.fields?.count { it.first == "collection_id" })
        assertEquals(1, addForm?.fields?.count { it.first == "topic_collections_action" })
        // topic_id 等其他 hidden 字段必须原样保留
        assertTrue(addForm?.fields?.contains("topic_id" to "19138") == true)
    }

    @Test
    fun parsesRemovalTargetsFromManageLists() {
        val actions = TopicCollectionParser.parseActions(
            Jsoup.parse(
                """
                <ul class="topic-collections-manage-list">
                  <li>
                    <span><a href="/topic/19138">第一篇帖子</a></span>
                    <form action="/topic_collections_action" method="post">
                      <input type="hidden" name="topic_id" value="19138">
                      <input type="hidden" name="topic_collections_action" value="item_remove">
                      <button type="submit">移除</button>
                    </form>
                  </li>
                  <li>
                    <span><a href="/user?username=db">db</a></span>
                    <form action="/topic_collections_action" method="post">
                      <input type="hidden" name="user_id" value="41">
                      <input type="hidden" name="topic_collections_action" value="collaborator_remove">
                      <button type="submit">移除</button>
                    </form>
                  </li>
                </ul>
                """.trimIndent(),
            ),
        )

        assertEquals("第一篇帖子", actions.first { it.operation == TopicCollectionOperation.REMOVE_ITEM }.targetLabel)
        assertEquals("db", actions.first { it.operation == TopicCollectionOperation.REMOVE_COLLABORATOR }.targetLabel)
    }
}

private val PICKER_HTML = """
    <main>
      <form action="/topic_collections_action" method="post" data-topic-collections-add-form>
        <input type="hidden" name="_csrf" value="csrf"><input type="hidden" name="topic_id" value="19138">
        <input type="hidden" name="topic_collections_action" data-topic-collections-action value="item_add">
        <select name="collection_id" data-topic-collections-select>
          <option value="63" data-included="1">水贴</option>
          <option value="5">机器学习</option>
        </select>
        <button name="submit" value="1" data-topic-collections-add-btn>收录</button>
      </form>
    </main>
""".trimIndent()
