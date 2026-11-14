package sb.linux.client.ui.screens

import sb.linux.client.domain.topic.TopicPageData

/** 与 TopicScreenPostList 的顶部条目顺序一致；卡片增减不改变楼层跳转目标。 */
internal fun topicLeadingItemCount(page: TopicPageData?, hasMainPost: Boolean, hasDanmaku: Boolean): Int =
    1 +
        (if (page?.redPacket != null) 1 else 0) +
        (if (hasMainPost) 1 else 0) +
        (if (page?.treasurePool != null) 1 else 0) +
        (if (page != null && (page.poll != null || page.essenceReview != null ||
            page.essenceApplication != null || page.resourceVote != null || page.footerPanels.isNotEmpty())) 1 else 0) +
        (if (hasDanmaku) 1 else 0)

internal fun topicMainItemIndex(page: TopicPageData?): Int = 1
