package sb.linux.client.domain.backup

data class BackupSettings(
    val webDavUrl: String,
    val webDavUsername: String,
    val webDavPassword: String,
    val webDavDirectory: String,
    val items: Set<String>,
)

interface BackupSettingsRepository {
    fun current(): BackupSettings
    fun setWebDavUrl(value: String)
    fun setWebDavUsername(value: String)
    fun setWebDavPassword(value: String)
    fun setWebDavDirectory(value: String)
    fun setItems(items: Set<String>)
    fun resetWebDavSettings()
    fun exportJson(items: Set<String>): String
    fun importJson(json: String): String?
}
