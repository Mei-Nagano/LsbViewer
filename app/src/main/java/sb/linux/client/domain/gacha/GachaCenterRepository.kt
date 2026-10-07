package sb.linux.client.domain.gacha

interface GachaCenterRepository {
    suspend fun load(): GachaCenter
    suspend fun pull(action: GachaAction): GachaPullReceipt
    fun confirmPullEnabled(): Boolean
    fun setConfirmPullEnabled(enabled: Boolean)
}
