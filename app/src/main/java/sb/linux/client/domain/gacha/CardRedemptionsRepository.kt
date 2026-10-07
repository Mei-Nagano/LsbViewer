package sb.linux.client.domain.gacha

import sb.linux.client.domain.topic.VirtualCard

interface CardRedemptionsRepository {
    fun records(): List<CardRedemptionRecord>
    fun record(topicId: Long, topicTitle: String, card: VirtualCard?)
    fun clear()
}
