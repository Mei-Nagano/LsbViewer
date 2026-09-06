package sb.linux.client.data

/** Search scope supported by the current linux.sb Meilisearch page. */
enum class SearchScope(val value: String, val label: String) {
    ALL("all", "全部内容"),
    TITLE("title", "标题"),
    BODY("body", "主题正文"),
    REPLY("reply", "回帖"),
    USER("user", "用户");

    companion object {
        fun fromValue(value: String?): SearchScope = entries.firstOrNull { it.value == value } ?: ALL
    }
}

/** Topic-result sorting supported by the source. */
enum class SearchSort(val value: String, val label: String) {
    RELEVANCE("relevance", "相关性"),
    LATEST("latest", "最新回复"),
    CREATED("created", "最新发布"),
    REPLIES("replies", "回复最多"),
    VIEWS("views", "浏览最多");

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

    /** The source ignores sort for user search; keep the URL contract explicit. */
    fun toPath(): String = buildString {
        append("/search?q=")
        append(java.net.URLEncoder.encode(normalizedText, Charsets.UTF_8.name()))
        if (scope != SearchScope.ALL) append("&scope=").append(scope.value)
        if (scope != SearchScope.USER && normalizedSort != SearchSort.RELEVANCE) append("&sort=").append(normalizedSort.value)
        if (normalizedPage > 1) append("&p=").append(normalizedPage)
    }
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
