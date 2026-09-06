package sb.linux.client.service

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import sb.linux.client.common.collection.TopicCollectionActionForm
import sb.linux.client.common.collection.TopicCollectionDetail
import sb.linux.client.common.collection.TopicCollectionListPage
import sb.linux.client.common.collection.TopicCollectionManagePage
import sb.linux.client.common.collection.TopicCollectionPicker
import sb.linux.client.common.collection.TopicCollectionTab
import sb.linux.client.repository.TopicCollectionRepository

/** Coordinates source mutations and always reloads the source state after a successful submit. */
class TopicCollectionService(private val repository: TopicCollectionRepository) {
    private val mutationMutex = Mutex()

    suspend fun list(tab: TopicCollectionTab): TopicCollectionListPage = repository.list(tab)
    suspend fun detail(path: String): TopicCollectionDetail = repository.detail(path)
    suspend fun manage(path: String): TopicCollectionManagePage = repository.manage(path)
    suspend fun picker(topicPath: String): TopicCollectionPicker? = repository.picker(topicPath)

    suspend fun execute(form: TopicCollectionActionForm, reloadPath: String? = null): TopicCollectionDetail? =
        mutationMutex.withLock {
            repository.submit(form)
            if (reloadPath == null) null else repository.detail(reloadPath)
        }
}
