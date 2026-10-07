package sb.linux.client.data

import sb.linux.client.domain.settings.GeneralSettings
import sb.linux.client.domain.settings.GeneralSettingsRepository

internal class AppSettingsGeneralSettingsRepository(
    private val settings: AppSettings,
) : GeneralSettingsRepository {
    override fun current() = GeneralSettings(
        settings.linkOpenMode,
        settings.linkPreviewEnabled,
        settings.updateCheckMode,
        settings.updateCheckIntervalHours,
        settings.lastUpdateCheck,
        settings.tabletMode,
        settings.bottomBarStyle,
        settings.bottomBarItems,
        settings.replyBarStyle,
    )

    override fun setLinkOpenMode(mode: Int) { settings.linkOpenMode = mode.coerceIn(0, 1) }
    override fun setUpdateCheckMode(mode: Int) { settings.updateCheckMode = mode.coerceIn(0, 2) }
    override fun setUpdateCheckIntervalHours(hours: Int) { settings.updateCheckIntervalHours = hours }
    override fun markUpdateCheck(atMillis: Long) { settings.lastUpdateCheck = atMillis }
    override fun setLinkPreviewEnabled(enabled: Boolean) { settings.linkPreviewEnabled = enabled }
    override fun setTabletMode(enabled: Boolean) { settings.tabletMode = enabled }
    override fun setBottomBarStyle(style: Int) { settings.bottomBarStyle = style }
    override fun setBottomBarItems(items: List<String>) { settings.bottomBarItems = items }
    override fun setReplyBarStyle(style: Int) { settings.replyBarStyle = style }
    override fun reset() { settings.resetGeneral() }
}
