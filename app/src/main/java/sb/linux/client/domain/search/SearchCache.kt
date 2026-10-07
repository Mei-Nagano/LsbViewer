package sb.linux.client.domain.search

interface SearchCache {
    fun get(): SearchResultCache?
    fun save(entry: SearchResultCache)
}
