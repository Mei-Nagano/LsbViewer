package sb.linux.client.domain.gacha

interface GachaOperationRepository {
    suspend fun load(path: String): GachaOperationPage
    suspend fun submit(path: String, fields: List<Pair<String, String>>): String?
}
