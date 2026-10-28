package sb.linux.client.ui.session

internal class SessionUiPreferenceController(
    private val session: Session,
) {
    fun saveTabletMode(value: Boolean) {
        session.generalSettingsRepository.setTabletMode(value)
        val current = session.generalSettingsRepository.current()
        session.tabletMode = current.tabletMode
        session.singleContentColumn = current.singleContentColumn
    }

    fun saveBottomBarStyle(value: Int) {
        session.generalSettingsRepository.setBottomBarStyle(value.coerceIn(0, 1))
        session.bottomBarStyle = session.generalSettingsRepository.current().bottomBarStyle
    }

    fun saveBottomBarItems(items: List<String>) {
        session.generalSettingsRepository.setBottomBarItems(items)
        session.bottomBarItems = session.generalSettingsRepository.current().bottomBarItems
    }

    fun saveReplyBarStyle(value: Int) {
        session.generalSettingsRepository.setReplyBarStyle(value.coerceIn(0, 1))
        session.replyBarStyle = session.generalSettingsRepository.current().replyBarStyle
    }

    fun saveLinkPreviewEnabled(value: Boolean) {
        session.generalSettingsRepository.setLinkPreviewEnabled(value)
        session.linkPreviewEnabled = session.generalSettingsRepository.current().linkPreviewEnabled
    }

    fun saveDanmaku(value: Boolean) = updateBrowse { it.saveDanmaku(value) }
    fun saveSidebarTwoColumns(value: Boolean) = updateBrowse { it.saveSidebarTwoColumns(value) }
    fun saveShowOnlineUsers(value: Boolean) = updateBrowse { it.saveSidebarShowOnlineUsers(value) }
    fun saveThreadDialog(value: Boolean) = updateBrowse { it.saveThreadDialogEnabled(value) }
    fun saveUnfavoriteConfirm(value: Boolean) = updateBrowse { it.saveUnfavoriteConfirm(value) }
    fun saveTopicsPerPage(value: Int) = updateBrowse { it.saveTopicsPerPage(value.coerceIn(5, 50)) }
    fun saveCommentSortOrder(value: Int) = updateBrowse { it.saveCommentSortOrder(value.coerceIn(0, 2)) }
    fun saveTableDisplayMode(value: Int) = updateBrowse { it.saveTableDisplayMode(value.coerceIn(0, 1)) }
    fun saveShowUid(value: Boolean) = updateBrowse { it.saveShowUid(value) }
    fun saveSmartDecodeEnabled(value: Boolean) = updateBrowse { it.saveSmartDecodeEnabled(value) }

    private fun updateBrowse(save: (sb.linux.client.domain.settings.BrowseSettingsRepository) -> Unit) {
        val repository = session.browseSettingsRepository
        save(repository)
        session.applyBrowseSettings(repository.current())
    }
}
