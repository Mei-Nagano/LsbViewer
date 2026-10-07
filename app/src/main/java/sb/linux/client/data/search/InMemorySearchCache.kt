package sb.linux.client.data.search

import sb.linux.client.domain.search.SearchCache
import sb.linux.client.domain.search.SearchResultCache

/** Single-process search shortcut, matching the previous Session-owned cache lifetime. */
class InMemorySearchCache : SearchCache {
    @Volatile
    private var entry: SearchResultCache? = null

    override fun get(): SearchResultCache? = entry

    override fun save(entry: SearchResultCache) {
        this.entry = entry
    }
}
