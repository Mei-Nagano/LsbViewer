package sb.linux.client.data

import sb.linux.client.domain.gacha.CardRedemptionRecord
import sb.linux.client.domain.gacha.CardRedemptionsRepository
import sb.linux.client.domain.topic.VirtualCard

internal class AppSettingsCardRedemptionsRepository(
    private val settings: AppSettings,
) : CardRedemptionsRepository {
    override fun records(): List<CardRedemptionRecord> = settings.cardRedemptions()
    override fun record(topicId: Long, topicTitle: String, card: VirtualCard?) =
        settings.recordCardRedemptions(topicId, topicTitle, card)
    override fun clear() = settings.clearCardRedemptions()
}
