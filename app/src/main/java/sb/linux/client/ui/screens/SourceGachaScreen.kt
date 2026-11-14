package sb.linux.client.ui.screens

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import sb.linux.client.ui.session.Session

/** 各称号路由共享原生分区页面和源站表单。 */
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
    if (kind == "identity") WebScreen(session, nav, sourceUrl = path)
    else GachaProfileScreen(session, nav, if (path == "/gacha_profile") null else "https://linux.sb$path")
}
