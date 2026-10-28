package sb.linux.client.ui.screens

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import sb.linux.client.ui.session.Session

/** 称号系统直接使用原站页面，不解析称号页面或代为提交表单。 */
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
    WebScreen(session, nav, sourceUrl = path)
}
