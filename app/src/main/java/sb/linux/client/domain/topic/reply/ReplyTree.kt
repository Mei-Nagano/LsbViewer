package sb.linux.client.domain.topic.reply

import sb.linux.client.domain.topic.PostEntry

/** 树形评论节点（由父楼层关系构造的多级回复结构）。 */
internal data class ReplyNode(val post: PostEntry, val depth: Int, val children: List<ReplyNode>)

/** 父楼层已加载且楼层号更小时挂为子节点，否则视为顶层。 */
internal fun buildReplyTree(replies: List<PostEntry>, sortOrder: Int): List<ReplyNode> {
    val byFloor = replies.filter { it.floor > 0 }.associateBy { it.floor }
    val childrenOf = HashMap<Int, MutableList<PostEntry>>()
    val tops = mutableListOf<PostEntry>()
    replies.sortedBy { it.floor }.forEach { post ->
        val parentFloor = post.parentFloor
        if (parentFloor > 0 && parentFloor < post.floor && byFloor.containsKey(parentFloor)) {
            childrenOf.getOrPut(parentFloor) { mutableListOf() }.add(post)
        } else {
            tops.add(post)
        }
    }
    val ordered = when (sortOrder) {
        2 -> tops.sortedByDescending { it.floor }
        0 -> tops.sortedWith(compareByDescending<PostEntry> { it.likeCount }.thenBy { it.floor })
        else -> tops
    }
    fun node(post: PostEntry, depth: Int): ReplyNode = ReplyNode(
        post,
        depth,
        (childrenOf[post.floor] ?: emptyList()).sortedBy { it.floor }.map { node(it, depth + 1) },
    )
    return ordered.map { node(it, 0) }
}

/** 折叠时整棵子树跟随顶层节点，翻页不会拆散对话。 */
internal fun flattenTree(
    nodes: List<ReplyNode>,
    collapsedIds: Set<Long> = emptySet(),
): List<Pair<PostEntry, Int>> = nodes.flatMap { node ->
    listOf(node.post to node.depth) +
        if (node.post.id in collapsedIds) emptyList() else flattenTree(node.children, collapsedIds)
}
