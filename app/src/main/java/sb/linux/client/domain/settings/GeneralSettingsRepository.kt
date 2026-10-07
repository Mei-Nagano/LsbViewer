package sb.linux.client.domain.settings

data class GeneralSettings(
    val linkOpenMode: Int,
    val linkPreviewEnabled: Boolean,
    val updateCheckMode: Int,
    val updateCheckIntervalHours: Int,
    val lastUpdateCheck: Long,
    val tabletMode: Boolean,
    val bottomBarStyle: Int,
    val bottomBarItems: List<String>,
    val replyBarStyle: Int,
)

interface GeneralSettingsRepository {
    fun current(): GeneralSettings
    fun setLinkOpenMode(mode: Int)
    fun setUpdateCheckMode(mode: Int)
    fun setUpdateCheckIntervalHours(hours: Int)
    fun markUpdateCheck(atMillis: Long)
    fun setLinkPreviewEnabled(enabled: Boolean)
    fun setTabletMode(enabled: Boolean)
    fun setBottomBarStyle(style: Int)
    fun setBottomBarItems(items: List<String>)
    fun setReplyBarStyle(style: Int)
    fun reset()
}
