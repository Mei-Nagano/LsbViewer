package sb.linux.client.ui.navigation
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import sb.linux.client.ui.session.Session
import sb.linux.client.ui.screens.*

internal fun NavGraphBuilder.extraSettingsRoutes(session: Session, nav: NavHostController) {
    settingsComposable("sourcePromotions") { SourcePromotionsScreen(session, nav) }
    settingsComposable("userPrivacy") { UserPrivacyScreen(session, nav) }
    settingsComposable("communityActivity") { CommunityActivityScreen(session, nav) }
    settingsComposable("attachments") { AttachmentsScreen(session, nav) }
    settingsComposable("uploadSettings") { UploadSettingsScreen(session, nav) }
    settingsComposable("storageSettings") { StorageSettingsScreen(session, nav) }
    settingsComposable("myContent") { MyContentScreen(session, nav) }
    settingsComposable("cardRedemptions") { CardRedemptionsScreen(session, nav) }
    settingsComposable("autoClearSettings") { DataManagementScreen(session, nav, automatic = true) }
    settingsComposable("webDavSettings") { TransferSettingsScreen(session, nav, cloud = true) }
    settingsComposable("aiConfiguration") { AiSettingsScreen(session, nav, section = "configuration") }
    settingsComposable("aiHistory") { AiSettingsScreen(session, nav, section = "history") }
    listOf("colors" to "themeColors", "background" to "themeBackground", "fonts" to "themeFonts", "layout" to "themeLayout")
        .forEach { (section, route) -> settingsComposable(route) { ThemeSettingsScreen(session, nav, section) } }
}
