package sb.linux.client.domain.topic

/** Loads bounded pages and assembles floors connected to a selected reply. */
internal class TopicThreadLoader(private val pages: TopicPageRepository) {
    suspend fun load(topicId: Long, startPost: PostEntry, availablePages: Int): List<PostEntry> {
        val all = mutableListOf<PostEntry>()
        val pageLimit = availablePages.coerceAtMost(MAX_PAGES)
        for (page in 1..pageLimit) all += pages.load(topicId, page).posts

        val byFloor = all.filter { it.floor > 0 }.associateBy { it.floor }
        val chain = linkedSetOf<Int>()
        val upward = ArrayDeque<Int>()
        if (startPost.floor > 0) {
            chain += startPost.floor
            upward += startPost.floor
        }
        while (upward.isNotEmpty()) {
            val post = byFloor[upward.removeFirst()] ?: continue
            post.referencedFloors.forEach { floor ->
                if (floor > 0 && chain.add(floor)) upward += floor
            }
        }

        val downward = ArrayDeque(chain)
        while (downward.isNotEmpty()) {
            val floor = downward.removeFirst()
            all.forEach { post ->
                if (post.floor > 0 && post.floor !in chain && floor in post.referencedFloors) {
                    chain += post.floor
                    downward += post.floor
                }
            }
        }
        return all.filter { it.floor > 0 && it.floor in chain }.sortedBy { it.floor }
    }

    private companion object {
        const val MAX_PAGES = 30
    }
}
