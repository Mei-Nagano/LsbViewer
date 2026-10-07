package sb.linux.client.domain.community

import sb.linux.client.common.filter.KeywordFilterSettings
import sb.linux.client.common.filter.KeywordFilterSnapshot

/** Account-scoped source and local-cache operations for home keyword filtering. */
interface KeywordFilterRepository {
    fun snapshot(): KeywordFilterSnapshot
    suspend fun refresh(force: Boolean = false): KeywordFilterSnapshot
    suspend fun save(settings: KeywordFilterSettings): KeywordFilterSnapshot
    fun clearCache()
}
