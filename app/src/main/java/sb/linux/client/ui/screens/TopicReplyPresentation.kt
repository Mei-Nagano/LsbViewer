package sb.linux.client.ui.screens

import sb.linux.client.domain.topic.PostEntry
import sb.linux.client.domain.topic.TopicPageData
import sb.linux.client.domain.topic.reply.ReplyNode
import sb.linux.client.domain.topic.reply.buildReplyTree
import sb.linux.client.domain.topic.reply.flattenTree

internal data class TopicReplyPresentation(
    val replies: List<PostEntry>,
    val repliesByFloor: Map<Int, PostEntry>,
    val hasTree: Boolean,
    val replyTree: List<ReplyNode>,
    val treeChildCounts: Map<Long, Int>,
    val threadReplyCount: Map<Int, Int>,
    val pagedReplies: List<PostEntry>,
    val flatTree: List<Pair<PostEntry, Int>>,
    val localReplyTotal: Int,
)

internal fun sortTopicPosts(posts: List<PostEntry>, sortOrder: Int): List<PostEntry> {
    val main = posts.firstOrNull { it.floor == 0 }
    val comments = posts.filter { it.id != main?.id }
    val rest = when (sortOrder) {
        1 -> comments.sortedBy { it.floor }
        2 -> comments.sortedByDescending { it.floor }
        else -> comments.sortedWith(compareByDescending<PostEntry> { it.likeCount }.thenBy { it.floor })
    }
    return if (main != null) listOf(main) + rest else rest
}

internal fun buildTopicReplyPresentation(
    sortedPosts: List<PostEntry>,
    data: TopicPageData?,
    sortOrder: Int,
    localReplyPage: Int,
    infinite: Boolean,
    repliesPerPage: Int,
    collapsedIds: Set<Long>,
): TopicReplyPresentation {
    val replies = sortedPosts.filter { it.floor > 0 }
    val repliesByFloor = replies.associateBy { it.floor }
    val hasTree = replies.any { it.parentFloor > 0 }
    val tree = if (hasTree) buildReplyTree(replies, sortOrder) else emptyList()
    val childCounts = buildMap<Long, Int> {
        fun collect(nodes: List<ReplyNode>) {
            nodes.forEach { node ->
                if (node.children.isNotEmpty()) put(node.post.id, flattenTree(node.children).size)
                collect(node.children)
            }
        }
        collect(tree)
    }
    val threadReplies = mutableMapOf<Int, Int>()
    sortedPosts.forEach { post ->
        post.referencedFloors.forEach { floor ->
            if (floor > 0) threadReplies[floor] = (threadReplies[floor] ?: 0) + 1
        }
    }
    val paged = if (infinite) replies
    else replies.drop((localReplyPage - 1) * repliesPerPage).take(repliesPerPage)
    val flat = when {
        !hasTree -> emptyList()
        infinite -> flattenTree(tree, collapsedIds)
        else -> flattenTree(
            tree.drop((localReplyPage - 1) * repliesPerPage).take(repliesPerPage),
            collapsedIds,
        )
    }
    val total = if (infinite) data?.totalPages ?: 1 else {
        val base = if (hasTree) tree.size else replies.size
        val full = ((base + repliesPerPage - 1) / repliesPerPage).coerceAtLeast(1)
        (if ((data?.page ?: 1) < (data?.totalPages ?: 1)) full + 1 else full)
            .coerceAtLeast(localReplyPage)
    }
    return TopicReplyPresentation(
        replies = replies,
        repliesByFloor = repliesByFloor,
        hasTree = hasTree,
        replyTree = tree,
        treeChildCounts = childCounts,
        threadReplyCount = threadReplies,
        pagedReplies = paged,
        flatTree = flat,
        localReplyTotal = total,
    )
}
