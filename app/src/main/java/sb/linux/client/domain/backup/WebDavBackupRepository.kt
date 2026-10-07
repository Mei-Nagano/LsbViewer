package sb.linux.client.domain.backup

interface WebDavBackupRepository {
    suspend fun upload(url: String, user: String, password: String, content: String)
    suspend fun download(url: String, user: String, password: String): String
}
