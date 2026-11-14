package sb.linux.client.ui.screens

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import sb.linux.client.ui.session.Session

/** 账号管理入口共用原生主体解析与 M3E 卡片。 */
@Composable
internal fun CommunityWalletScreen(session: Session, nav: NavHostController) {
    NativeAccountScreen(session, nav, "我的烧饼", "/community_wallet", onChanged = { session.refreshSession() })
}

@Composable
internal fun MyPromotionsScreen(session: Session, nav: NavHostController) {
    NativeAccountScreen(session, nav, "我的广告", "/promotions", onChanged = { session.refreshSession() })
}
