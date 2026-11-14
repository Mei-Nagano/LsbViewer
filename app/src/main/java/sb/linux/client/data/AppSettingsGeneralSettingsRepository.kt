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
        settings.singleContentColumn,
        settings.bottomBarStyle,
        settings.bottomBarItems,
        settings.replyBarStyle,
        settings.hideExternalJumpReminder,
        settings.hideStatusBar,
        settings.skippedUpdateVersion,
        settings.sourceDomain,
    )

    override fun setSourceDomain(host: String) { settings.sourceDomain = host }
    override fun setLinkOpenMode(mode: Int) { settings.linkOpenMode = mode.coerceIn(0, 1) }
    override fun setUpdateCheckMode(mode: Int) { settings.updateCheckMode = mode.coerceIn(0, 2) }
    override fun setUpdateCheckIntervalHours(hours: Int) { settings.updateCheckIntervalHours = hours }
    override fun markUpdateCheck(atMillis: Long) { settings.lastUpdateCheck = atMillis }
    override fun setLinkPreviewEnabled(enabled: Boolean) { settings.linkPreviewEnabled = enabled }
    override fun setHideExternalJumpReminder(enabled: Boolean) { settings.hideExternalJumpReminder = enabled }
    override fun setHideStatusBar(enabled: Boolean) { settings.hideStatusBar = enabled }
    override fun skipUpdateVersion(version: String) { settings.skippedUpdateVersion = version }
    override fun setTabletMode(enabled: Boolean) { settings.tabletMode = enabled }
    override fun setBottomBarStyle(style: Int) { settings.bottomBarStyle = style }
    override fun setBottomBarItems(items: List<String>) { settings.bottomBarItems = items }
    override fun setReplyBarStyle(style: Int) { settings.replyBarStyle = style }
    override fun reset() { settings.resetGeneral() }
}
