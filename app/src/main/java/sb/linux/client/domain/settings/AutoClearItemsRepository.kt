package sb.linux.client.domain.settings

interface AutoClearItemsRepository {
    fun items(): Set<String>
    fun save(items: Set<String>)
}
