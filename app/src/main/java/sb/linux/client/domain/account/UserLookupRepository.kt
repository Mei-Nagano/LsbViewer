package sb.linux.client.domain.account

interface UserLookupRepository {
    suspend fun resolve(username: String): Long
}
