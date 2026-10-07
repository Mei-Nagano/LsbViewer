package sb.linux.client.data

import sb.linux.client.domain.settings.AutoClearItemsRepository

internal class AppSettingsAutoClearItemsRepository(
    private val settings: AppSettings,
) : AutoClearItemsRepository {
    override fun items(): Set<String> = settings.autoClearItems.toSet()

    override fun save(items: Set<String>) {
        settings.autoClearItems = items.toSet()
    }
}
