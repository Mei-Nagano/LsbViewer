package sb.linux.client.domain.gacha

interface GachaProfileRepository {
    suspend fun load(): GachaProfile
    suspend fun submit(action: GachaAction): String?
}
