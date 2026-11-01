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
    settingsComposable("aiHistory") { AiSettingsScreen(session, nav, section = "history") }
}
