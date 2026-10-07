package sb.linux.client.domain.collection

import sb.linux.client.common.collection.TopicCollectionActionForm
import sb.linux.client.common.collection.TopicCollectionDetail
import sb.linux.client.common.collection.TopicCollectionListPage
import sb.linux.client.common.collection.TopicCollectionManagePage
import sb.linux.client.common.collection.TopicCollectionPicker
import sb.linux.client.common.collection.TopicCollectionTab

interface TopicCollectionPageRepository {
    suspend fun list(tab: TopicCollectionTab): TopicCollectionListPage
    suspend fun detail(path: String): TopicCollectionDetail
    suspend fun manage(path: String): TopicCollectionManagePage
    suspend fun picker(topicPath: String): TopicCollectionPicker?
    suspend fun execute(form: TopicCollectionActionForm, reloadPath: String? = null): TopicCollectionDetail?
}
