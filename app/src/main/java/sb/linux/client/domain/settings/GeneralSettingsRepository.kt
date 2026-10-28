package sb.linux.client.domain.settings

data class GeneralSettings(
    val linkOpenMode: Int,
    val linkPreviewEnabled: Boolean,
    val updateCheckMode: Int,
    val updateCheckIntervalHours: Int,
    val lastUpdateCheck: Long,
    val tabletMode: Boolean,
    /** 用户显式选择「始终单内容栏」时为 true；键缺失或跟随窗口时为 false。 */
    val singleContentColumn: Boolean,
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
