package sb.linux.client.data

import sb.linux.client.domain.backup.BackupSettings
import sb.linux.client.domain.backup.BackupSettingsRepository

internal class AppSettingsBackupSettingsRepository(
    private val settings: AppSettings,
) : BackupSettingsRepository {
    override fun current() = BackupSettings(
        settings.webdavUrl,
        settings.webdavUser,
        settings.webdavPass,
        settings.webdavDir,
        settings.backupItems,
    )

    override fun setWebDavUrl(value: String) { settings.webdavUrl = value }
    override fun setWebDavUsername(value: String) { settings.webdavUser = value }
    override fun setWebDavPassword(value: String) { settings.webdavPass = value }
    override fun setWebDavDirectory(value: String) { settings.webdavDir = value }
    override fun setItems(items: Set<String>) { settings.backupItems = items.toSet() }
    override fun resetWebDavSettings() { settings.resetWebdav() }
    override fun exportJson(items: Set<String>): String = settings.exportJson(items)
    override fun importJson(json: String): String? = settings.importJson(json)
}
