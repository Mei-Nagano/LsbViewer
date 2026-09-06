package sb.linux.client.repository

import sb.linux.client.common.collection.TopicCollectionActionForm
import sb.linux.client.common.collection.TopicCollectionDetail
import sb.linux.client.common.collection.TopicCollectionListPage
import sb.linux.client.common.collection.TopicCollectionManagePage
import sb.linux.client.common.collection.TopicCollectionPicker
import sb.linux.client.common.collection.TopicCollectionTab

/** Source boundary for the server-rendered topic_collections plugin. */
interface TopicCollectionRepository {
    suspend fun list(tab: TopicCollectionTab): TopicCollectionListPage
    suspend fun detail(path: String): TopicCollectionDetail
    suspend fun manage(path: String): TopicCollectionManagePage
    suspend fun picker(topicPath: String): TopicCollectionPicker?
    suspend fun submit(form: TopicCollectionActionForm)
}
