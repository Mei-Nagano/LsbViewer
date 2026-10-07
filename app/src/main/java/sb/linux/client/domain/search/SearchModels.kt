package sb.linux.client.domain.search

/** Search scope supported by the current linux.sb Meilisearch page. */
enum class SearchScope(val value: String) {
    ALL("all"),
    TITLE("title"),
    BODY("body"),
    REPLY("reply"),
    USER("user");

    companion object {
        fun fromValue(value: String?): SearchScope = entries.firstOrNull { it.value == value } ?: ALL
    }
}

/** Topic-result sorting supported by the source. */
enum class SearchSort(val value: String) {
    RELEVANCE("relevance"),
    LATEST("latest"),
    CREATED("created"),
    REPLIES("replies"),
    VIEWS("views");

    companion object {
        fun fromValue(value: String?): SearchSort = entries.firstOrNull { it.value == value } ?: RELEVANCE
    }
}

data class SearchQuery(
    val text: String,
    val scope: SearchScope = SearchScope.ALL,
    val sort: SearchSort = SearchSort.RELEVANCE,
    val page: Int = 1,
) {
    val normalizedText: String get() = text.trim()
    val normalizedPage: Int get() = page.coerceAtLeast(1)
    val normalizedSort: SearchSort get() = if (scope == SearchScope.USER) SearchSort.RELEVANCE else sort

    fun normalized(): SearchQuery = copy(
        text = normalizedText,
        sort = normalizedSort,
        page = normalizedPage,
    )

}

sealed interface SearchResultItem {
    data class Topic(
        val topicId: Long,
        val title: String,
        val snippet: String,
        val matchedBy: String,
        val forumName: String,
        val timeText: String,
        val replies: Int,
        val views: Int,
    ) : SearchResultItem

    data class User(
        val userId: Long,
        val username: String,
        val avatarUrl: String,
        val meta: String,
    ) : SearchResultItem
}

data class SearchPage(
    val query: SearchQuery,
    val totalCount: Int,
    val items: List<SearchResultItem>,
    val hasPrevious: Boolean,
    val hasNext: Boolean,
)

/** One-page in-memory shortcut for returning to the same search during the current process. */
data class SearchResultCache(
    val request: SearchQuery,
    val page: SearchPage,
    val time: Long,
)
