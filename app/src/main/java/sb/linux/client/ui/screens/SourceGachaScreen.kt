package sb.linux.client.ui.screens

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import sb.linux.client.ui.session.Session

/** 称号各分区完整使用源站页面，保留原生实现供后续修改。 */
@Composable
fun SourceGachaScreen(session: Session, nav: NavHostController, kind: String = "profile") {
    val path = when (kind) {
        "center" -> "/gacha"
        "market" -> "/gacha_market"
        "forge" -> "/gacha_forge_center"
        "recycle" -> "/gacha_recycle_center"
        "recipes" -> "/gacha_recipes"
        "marketMine" -> "/gacha_market_mine"
        "marketOrders" -> "/gacha_market_orders"
        "identity" -> "/identity_center"
        else -> "/gacha_profile"
    }
    WebScreen(session, nav, sourceUrl = path,
        initialContentSelector = ".gacha-profile-page, .gacha-center-page, main")
}
