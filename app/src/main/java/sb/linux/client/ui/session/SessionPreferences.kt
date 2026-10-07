package sb.linux.client.ui.session

import sb.linux.client.domain.settings.BrowseSettings
import android.app.Application
import androidx.compose.ui.graphics.Color

internal class SessionPreferences(
    private val app: Application,
    private val session: Session,
) {
    internal val themePreferenceController by lazy {
        SessionThemePreferenceController(app, session)
    }

    fun saveTheme(mode: ThemeModePref, dyn: Boolean, colorKey: String = session.themeColorKey) =
        themePreferenceController.saveTheme(mode, dyn, colorKey)

    fun saveOledDark(v: Boolean) = themePreferenceController.saveOledDark(v)
    fun saveThemeStyle(index: Int) = themePreferenceController.saveThemeStyle(index)
    fun saveThemeContrast(v: Float) = themePreferenceController.saveThemeContrast(v)

    val customFontFile: java.io.File
        get() = themePreferenceController.customFontFile

    fun saveFont(key: String) = themePreferenceController.saveFont(key)
    fun saveFontScale(value: Float) = themePreferenceController.saveFontScale(value)
    fun saveFontWeightLevel(value: Int) = themePreferenceController.saveFontWeightLevel(value)
    suspend fun saveCustomFont(uri: android.net.Uri, displayName: String): Boolean =
        themePreferenceController.saveCustomFont(uri, displayName)
    fun clearCustomFont() = themePreferenceController.clearCustomFont()

    fun saveThemePrimary(c: Color?) = themePreferenceController.saveThemePrimary(c)
    fun saveThemeSecondary(c: Color?) = themePreferenceController.saveThemeSecondary(c)
    fun saveThemeTertiary(c: Color?) = themePreferenceController.saveThemeTertiary(c)
    fun saveThemeBackground(c: Color?) = themePreferenceController.saveThemeBackground(c)
    suspend fun importBackgroundImage(uri: android.net.Uri) =
        themePreferenceController.importBackgroundImage(uri)
    fun saveBackgroundImageEnabled(value: Boolean) = themePreferenceController.saveBackgroundImageEnabled(value)
    fun saveBackgroundImageOpacity(value: Float) = themePreferenceController.saveBackgroundImageOpacity(value)
    fun saveSurfaceOpacity(value: Float) = themePreferenceController.saveSurfaceOpacity(value)
    fun removeBackgroundImage() = themePreferenceController.removeBackgroundImage()
    fun saveKeepBackgroundColor(v: Boolean) = themePreferenceController.saveKeepBackgroundColor(v)
    fun addThemeCustomColor(key: String) = themePreferenceController.addThemeCustomColor(key)
    fun removeThemeCustomColor(key: String) = themePreferenceController.removeThemeCustomColor(key)
    fun saveCardColor(key: String, c: Color?) = themePreferenceController.saveCardColor(key, c)
    private val browsePreferenceController by lazy {
        SessionBrowsePreferenceController(session, session.browseSettingsRepository)
    }

    fun saveInfiniteScroll(v: Boolean) = browsePreferenceController.saveInfiniteScroll(v)
    fun saveHomeSortDrawerOpen(v: Boolean) = browsePreferenceController.saveHomeSortDrawerOpen(v)
    fun saveCollapseNewTopics(v: Boolean) = browsePreferenceController.saveCollapseNewTopics(v)
    fun effectiveInfiniteScroll(scope: String): Boolean = browsePreferenceController.effectiveInfiniteScroll(scope)
    fun saveScrollModeOverride(scope: String, infinite: Boolean) =
        browsePreferenceController.saveScrollModeOverride(scope, infinite)
    fun clearScrollModeOverrides() = browsePreferenceController.clearScrollModeOverrides()
    // ---------------- 分类重置（3.9） ----------------

    /** 重置外观设置（含卡片元素色与自定义色预选） */
    fun resetThemeSettings() = themePreferenceController.resetThemeSettings()
    /** 重置浏览设置并刷新内存镜像 */
    fun resetBrowseSettings() = browsePreferenceController.resetBrowseSettings()
    internal fun applyBrowseSettings(settings: BrowseSettings) = browsePreferenceController.apply(settings)
    /** 重置全部应用设置（一级菜单重置按钮）；平板模式迁入外观设置后此处显式恢复默认 */
    fun resetAllAppSettings() {
        resetThemeSettings()
        resetBrowseSettings()
        session.aiConfigurationRepository.reset()
        session.generalSettingsRepository.reset()
        session.proxySettingsRepository.resetNetworkSettings()
        session.backupSettingsRepository.resetWebDavSettings()
        saveTabletMode(false)
    }

    private val uiPreferenceController by lazy {
        SessionUiPreferenceController(session)
    }

    fun saveTabletMode(v: Boolean) = uiPreferenceController.saveTabletMode(v)
    fun saveBottomBarStyle(v: Int) = uiPreferenceController.saveBottomBarStyle(v)
    fun saveBottomBarItems(items: List<String>) = uiPreferenceController.saveBottomBarItems(items)
    fun saveReplyBarStyle(v: Int) = uiPreferenceController.saveReplyBarStyle(v)
    fun saveLinkPreviewEnabled(v: Boolean) = uiPreferenceController.saveLinkPreviewEnabled(v)

    fun pinFloatingTopic(topicId: Long, title: String) {
        session.floatingTopicId = topicId
        session.floatingTopicTitle = title.trim()
    }

    fun clearFloatingTopic(topicId: Long? = null) {
        if (topicId == null || session.floatingTopicId == topicId) {
            session.floatingTopicId = 0L
            session.floatingTopicTitle = ""
        }
    }

    fun saveDanmaku(v: Boolean) = uiPreferenceController.saveDanmaku(v)
    fun saveSidebarTwoColumns(v: Boolean) = uiPreferenceController.saveSidebarTwoColumns(v)
    fun saveShowOnlineUsers(v: Boolean) = uiPreferenceController.saveShowOnlineUsers(v)
    fun saveThreadDialog(v: Boolean) = uiPreferenceController.saveThreadDialog(v)
    fun saveUnfavoriteConfirm(v: Boolean) = uiPreferenceController.saveUnfavoriteConfirm(v)
    fun saveTopicsPerPage(v: Int) = uiPreferenceController.saveTopicsPerPage(v)
    fun saveCommentSortOrder(v: Int) = uiPreferenceController.saveCommentSortOrder(v)
    fun saveTableDisplayMode(v: Int) = uiPreferenceController.saveTableDisplayMode(v)
    fun saveShowUid(v: Boolean) = uiPreferenceController.saveShowUid(v)
    fun saveSmartDecodeEnabled(v: Boolean) = uiPreferenceController.saveSmartDecodeEnabled(v)

}
