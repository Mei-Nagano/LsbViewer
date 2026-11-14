package sb.linux.client.ui.navigation
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import sb.linux.client.ui.session.Session
import sb.linux.client.ui.screens.*

internal fun NavGraphBuilder.extraSettingsRoutes(session: Session, nav: NavHostController) {
    settingsComposable("sourcePromotions") { SourcePromotionsScreen(session, nav) }
    settingsComposable("userPrivacy") { UserPrivacyScreen(session, nav) }
    settingsComposable("personalSignature") { PersonalSignatureScreen(session, nav) }
    settingsComposable("communityWallet") { CommunityWalletScreen(session, nav) }
    settingsComposable("myPromotions") { MyPromotionsScreen(session, nav) }
    settingsComposable("communityActivity") { CommunityActivityScreen(session, nav) }
    settingsComposable("attachments") { AttachmentsScreen(session, nav) }
    settingsComposable("uploadSettings") { UploadSettingsScreen(session, nav) }
    settingsComposable("storageSettings") { StorageSettingsScreen(session, nav) }
    settingsComposable("myContent") { MyContentScreen(session, nav) }
    settingsComposable("cardRedemptions") { CardRedemptionsScreen(session, nav) }
    settingsComposable("aiHistory") { AiSettingsScreen(session, nav, section = "history") }
    settingsComposable("themeColors") { ThemeSettingsDetailScreen(session, nav, section = "colors") }
    settingsComposable("themeBackground") { ThemeSettingsDetailScreen(session, nav, section = "background") }
    settingsComposable("themeFonts") { ThemeSettingsDetailScreen(session, nav, section = "fonts") }
    settingsComposable("themeLayout") { ThemeSettingsDetailScreen(session, nav, section = "layout") }
    settingsComposable("browseList") { BrowseSettingsDetailScreen(session, nav, "list") }
    settingsComposable("browseContent") { BrowseSettingsDetailScreen(session, nav, "content") }
    settingsComposable("browseInteraction") { BrowseSettingsDetailScreen(session, nav, "interaction") }
    settingsComposable("browseSidebar") { BrowseSettingsDetailScreen(session, nav, "sidebar") }
    settingsComposable("networkProxy") { NetworkSettingsDetailScreen(session, nav, "proxy") }
    settingsComposable("networkDns") { NetworkSettingsDetailScreen(session, nav, "dns") }
    settingsComposable("networkLinks") { NetworkSettingsDetailScreen(session, nav, "links") }
    settingsComposable("aiBehavior") { AiSettingsDetailScreen(session, nav, "behavior") }
    settingsComposable("aiConfiguration") { AiSettingsDetailScreen(session, nav, "configuration") }
    settingsComposable("appUpdates") { UpdateSettingsScreen(session, nav) }
    settingsComposable("storageAnalysis") { StorageSettingsDetailScreen(session, nav, "analysis") }
    settingsComposable("storageClear") { StorageSettingsDetailScreen(session, nav, "clear") }
    settingsComposable("storageAutomatic") { StorageSettingsDetailScreen(session, nav, "automatic") }
    settingsComposable("backupLocal") { StorageSettingsDetailScreen(session, nav, "local") }
    settingsComposable("backupCloud") { StorageSettingsDetailScreen(session, nav, "cloud") }
}
