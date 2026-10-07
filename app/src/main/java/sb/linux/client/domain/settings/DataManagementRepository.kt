package sb.linux.client.domain.settings

/** Clears the selected categories of app-owned local data. */
interface DataManagementRepository {
    fun autoClearItems(): Set<String>
    fun saveAutoClearItems(items: Set<String>)
    fun clearAutoClearItems()
    suspend fun clearDataItems(items: Set<String>)
}
