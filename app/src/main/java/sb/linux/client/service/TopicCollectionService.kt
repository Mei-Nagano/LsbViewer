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
import sb.linux.client.domain.collection.TopicCollectionPageRepository

/** Coordinates source mutations and always reloads the source state after a successful submit. */
class TopicCollectionService(private val repository: TopicCollectionRepository) : TopicCollectionPageRepository {
    private val mutationMutex = Mutex()

    override suspend fun list(tab: TopicCollectionTab): TopicCollectionListPage = repository.list(tab)
    override suspend fun detail(path: String): TopicCollectionDetail = repository.detail(path)
    override suspend fun manage(path: String): TopicCollectionManagePage = repository.manage(path)
    override suspend fun picker(topicPath: String): TopicCollectionPicker? = repository.picker(topicPath)

    override suspend fun execute(form: TopicCollectionActionForm, reloadPath: String?): TopicCollectionDetail? =
        mutationMutex.withLock {
            repository.submit(form)
            if (reloadPath == null) null else repository.detail(reloadPath)
        }
}
